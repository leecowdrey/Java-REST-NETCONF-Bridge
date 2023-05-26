/*======================================================*/
// Module: XMPP Driver
// Author: Lee Cowdrey
// Version: 1.0
// History:
// 1.0	  Initial Version
//
// Notes: quick and dirty
//
/*======================================================*/
package com.brocade.bwc.xmpp;

import com.brocade.bwc.netconf.common.Constants;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import rocks.xmpp.addr.Jid;
import rocks.xmpp.core.XmppException;
import rocks.xmpp.core.session.TcpConnectionConfiguration;
import rocks.xmpp.core.session.XmppClient;
import rocks.xmpp.core.session.XmppSessionConfiguration;
import rocks.xmpp.core.session.debug.ConsoleDebugger;
import rocks.xmpp.core.stanza.model.Message;
import rocks.xmpp.extensions.last.LastActivityManager;
import rocks.xmpp.extensions.last.model.LastActivity;
import rocks.xmpp.extensions.muc.ChatRoom;
import rocks.xmpp.extensions.muc.ChatService;
import rocks.xmpp.extensions.muc.MultiUserChatManager;
import rocks.xmpp.extensions.muc.OccupantEvent;
import rocks.xmpp.extensions.pubsub.model.event.Event;
import rocks.xmpp.extensions.receipts.MessageDeliveryReceiptsManager;
import rocks.xmpp.extensions.rpc.RpcException;
import rocks.xmpp.extensions.rpc.RpcManager;
import rocks.xmpp.extensions.rpc.model.Value;
import rocks.xmpp.extensions.time.EntityTimeManager;
import rocks.xmpp.extensions.version.SoftwareVersionManager;
import rocks.xmpp.extensions.version.model.SoftwareVersion;
import com.brocade.bwc.xmpp.TrustSSL;
import java.util.List;
import rocks.xmpp.core.session.ConnectionEvent;
import rocks.xmpp.core.stanza.MessageEvent;
import rocks.xmpp.core.stanza.PresenceEvent;
import rocks.xmpp.core.stanza.model.Presence;
import rocks.xmpp.extensions.receipts.MessageDeliveredEvent;

public class Driver {

    private static boolean DEBUG = false;
    private final static String jidDelimiter = new Character((char) 64).toString();
    private final static String resourceDelimiter = new Character((char) 47).toString();
    private XmppClient xmppClient = null;
    private MessageDeliveryReceiptsManager messageDeliveryReceiptsManager = null;
    private boolean running = false;
    protected TrustSSL ssl = new TrustSSL();
    private String domain = "";
    private String username = "";
    private String password = "";
    private String resource = "";
    private int port = 5222;
    private int priority = 5;
    private String host = "";
    private com.brocade.bwc.xmpp.Logger log = new com.brocade.bwc.xmpp.Logger(Driver.class);

    public Driver(Boolean debug, Boolean loggerRequired) {
        this.DEBUG = debug;
        if (loggerRequired) {
            log = new com.brocade.bwc.xmpp.Logger(Driver.class);
        }

    }

    public void init(String fullJid, String password) {
        this.domain = getJidDomain(fullJid);
        this.username = getJidBare(fullJid);
        this.password = password;
        this.resource = getJidResource(fullJid);
    }

    public void init(String completeJid, String password, String resource) {
        this.domain = getJidDomain(completeJid);
        this.username = getJidBare(completeJid);
        this.password = password;
        this.resource = Constants.s_module_name;
    }

    public void init(String domain, String username, String password, String resource, String host, int port) {
        this.domain = domain;
        this.username = username;
        this.password = password;
        this.resource = resource;
        this.port = port;
        this.host = host;
    }

    public int tokenNumberOf(String src, String delimiter) {
        int segments = 0;
        String[] segmentSplit = src.split("\\" + delimiter);
        for (String segmentValue : segmentSplit) {
            segments++;
        }
        return segments;
    }

    public String tokenSegment(String src, String delimiter, int segmentNumber) {
        int segments = 0;
        String segmentRequired = "";
        String[] segmentSplit = src.split("\\" + delimiter);
        for (String segmentValue : segmentSplit) {
            segments++;
            if (segments == segmentNumber) {
                segmentRequired = segmentValue;
            }
        }
        return segmentRequired;
    }

    private String tokenReduce(String src, String delimiter) {
        return src.substring((src.indexOf(delimiter) + 1));
    }

    private String getJidBare(String jid) {
        return tokenSegment(jid, jidDelimiter, 1).toLowerCase();
    }

    private String getJidDomain(String jid) {
        String domainSplit = "";
        if (jid.contains(resourceDelimiter)) {
            domainSplit = jid.substring(jid.indexOf(jidDelimiter) + 1, jid.indexOf(resourceDelimiter));
        } else {
            domainSplit = jid.substring(jid.indexOf(jidDelimiter) + 1);
        }
        return domainSplit;
    }

    private String getJidResource(String jid) {
        if (jid.contains(resourceDelimiter)) {
            return jid.substring(jid.indexOf(jidDelimiter) + 1);
        } else {
            return "";
        }
    }

    public void destroy() {
        if (running) {
            this.running = false;
            if (DEBUG) {
                System.out.println(Constants.s_module_name + ": XMPP driver shutting down");
            }
            if (xmppClient != null) {
                if (xmppClient.isConnected()) {
                    try {
                        xmppClient.send(new Presence(Presence.Type.UNAVAILABLE));
                        // XmppClient implements java.lang.AutoCloseable
                        xmppClient.close();
                        if (log != null) {
                            log.destroy();
                        }
                        this.running = false;
                    } catch (XmppException ex) {
                        System.out.println(Constants.s_module_name + ": XMPP driver exception " + ex.toString());
                    }
                }
            }
        }
    }

    private void messageInboundListener(MessageEvent e) {
        System.out.println("Inbound message: " + e.getMessage());
    }

    private void messageOutboundListener(MessageEvent e) {
        System.out.println("Outbound message: " + e.getMessage());
    }

    private void presenceListener(PresenceEvent e) {
        System.out.println("Received: " + e.getPresence());
    }

    private void receiptListener(MessageDeliveredEvent e) {
        System.out.println("Delivery Receipt: " + e.getMessageId() + " ");
    }

    private void pubsubListener(MessageDeliveredEvent e) {
        System.out.println("Delivery Receipt: " + e.getMessageId() + " ");
    }

    private void connectionListener(ConnectionEvent e) {
        switch (e.getType()) {
            case DISCONNECTED:
                // disconnected due to e.getCause()
                System.out.println(Constants.s_module_name + ": XMPP driver disconnected: " + e.getCause());
                break;
            case RECONNECTION_SUCCEEDED:
                // successfully reconnected
                System.out.println(Constants.s_module_name + ": XMPP driver reconnected");
                break;
            case RECONNECTION_FAILED:
                // reconnection failed due to e.getCause()
                System.out.println(Constants.s_module_name + ": XMPP driver reconnectiong failed: " + e.getCause());
                break;
            case RECONNECTION_PENDING:
                // reconnection pending, next reconnection attempt in e.getNextReconnectionAttempt()
                // emitted every second.
                System.out.println(Constants.s_module_name + ": XMPP driver reconnecting in " + Long.toString(e.getNextReconnectionAttempt().getSeconds())+ " seconds");
                break;
        }
    }

    public boolean isRunning() {
        return this.running;
    }

    public void run() {
        this.running = true;
        try {
            if (DEBUG) {
                System.out.println(Constants.s_module_name + ": XMPP driver starting up");
            }
            TcpConnectionConfiguration tcpConfiguration = null;
            if (this.host != null) {
                tcpConfiguration = TcpConnectionConfiguration.builder()
                        .hostname(this.host) // The hostname.
                        .port(this.port) // The XMPP default port.
                        .sslContext(ssl.getTrustAllSslContext()) // Use an SSL context, which trusts every server. Only use it for testing!
                        .secure(true) // We want to negotiate a TLS connection.
                        .build();

            } else {
                tcpConfiguration = TcpConnectionConfiguration.builder()
                        .port(this.port) // The XMPP default port.
                        .sslContext(ssl.getTrustAllSslContext()) // Use an SSL context, which trusts every server. Only use it for testing!
                        .secure(true) // We want to negotiate a TLS connection.
                        .build();

            }
            XmppSessionConfiguration configuration = XmppSessionConfiguration.builder()
                    .debugger(ConsoleDebugger.class)
                    .build();

            xmppClient = XmppClient.create(this.domain, configuration, tcpConfiguration);

            SoftwareVersionManager softwareVersionManager = xmppClient.getManager(SoftwareVersionManager.class);
            softwareVersionManager.setSoftwareVersion(new SoftwareVersion(Constants.s_module_name, Constants.s_header_user_agent_version));

            // add standard listeners
            xmppClient.addInboundMessageListener(e -> messageInboundListener(e));
            xmppClient.addOutboundMessageListener(e -> messageOutboundListener(e));
            xmppClient.addInboundPresenceListener(e -> presenceListener(e));
            xmppClient.addConnectionListener(e -> connectionListener(e));

            // add XEP-0184 message delivery receipt listener
            messageDeliveryReceiptsManager = xmppClient.getManager(MessageDeliveryReceiptsManager.class);
            messageDeliveryReceiptsManager.addMessageDeliveredListener(e -> receiptListener(e));
            messageDeliveryReceiptsManager.setEnabled(true);

            // remove extensions we dont need

            // OUT: <iq from="services@cowdrey.co.uk/xmpp" id="89e4e93d-b0fe-47fd-bd17-2b3e39530fbe" to="services@cowdrey.co.uk/xmpp" type="result" xml:lang="en-GB"><query xmlns="jabber:iq:rpc"><methodResponse><params><param><value><string>Colorado</string></value></param></params></methodResponse></query></iq>
                    RpcManager rpcManagerListener = xmppClient.getManager(RpcManager.class);
            rpcManagerListener.setRpcHandler((requester, methodName, parameters) -> {
                if (methodName.equals("examples.getStateName")) {
                    if (!parameters.isEmpty()) {
                        if (parameters.get(0).getAsInteger() == 6) {
                            return Value.of("Colorado");
                        }
                    }
                }
                throw new RpcException(123, "Invalid method name or parameter.");
            });

            // other preferences
            EntityTimeManager entityTimeManager = xmppClient.getManager(EntityTimeManager.class);
            entityTimeManager.setEnabled(false);

            /*
            xmppClient.addInboundMessageListener(e -> {
                Message message = e.getMessage();
                Event event = message.getExtension(Event.class);
                if (event != null) {
                    for (rocks.xmpp.extensions.pubsub.model.Item item : event.getItems()) {
                        System.out.println("pubsub: id=" + item.getId() + ", node=" + item.getNode() + ", publisher=" + item.getPublisher());
                    }
                }
            });
             */
// Connect
            xmppClient.connect();
            // Login
            xmppClient.login(this.username, this.password, this.resource);

            // Last activity lookup
            LastActivityManager lastActivityManager = xmppClient.getManager(LastActivityManager.class);
            LastActivity lastActivity = lastActivityManager.getLastActivity(Jid.of("lee@cowdrey.co.uk")).getResult();
            Instant idleTime = Instant.now().minusSeconds(lastActivity.getSeconds());

// Send a message to myself, which is caught by the listener above.
            xmppClient.send(new Message(Jid.of("lee@cowdrey.co.uk"), Message.Type.CHAT, "Last seen:" + lastActivity.toString()));

            // some pubsub
            /*
                //Collection<PubSubService> pubSubServices = pubSubManager.discoverPubSubServices().getResult();
                PubSubService pubSubService = pubSubManager.createPubSubService(Jid.of("pubsub." + xmppSession.getDomain()));
                PubSubNode pubSubNode = pubSubService.node("bwc");
                // pubSubNode.subscribe();

                //pubSubNode.create();
                pubSubNode.subscribe();
                pubSubNode.publish(new Tune("Artist", "Title"));
             */
            // XML-RPC call
            // IN : <iq from="services@cowdrey.co.uk/xmpp" id="89e4e93d-b0fe-47fd-bd17-2b3e39530fbe" to="services@cowdrey.co.uk/xmpp" type="set" xml:lang="en-GB"><query xmlns="jabber:iq:rpc"><methodCall><methodName>examples.getStateName</methodName><params><param><value><int>6</int></value></param></params></methodCall></query></iq>
            RpcManager rpcManager = xmppClient.getManager(RpcManager.class);
            try {
                Value response = rpcManager.call(Jid.of(xmppClient.getConnectedResource().withResource(resource)), "examples.getStateName", Value.of(6)).getResult();
                System.out.println(response.getAsString()); // Colorado
            } catch (XmppException ex) {
                System.out.println(Constants.s_module_name + ": XMPP driver exception " + ex.toString());
                // E.g. a StanzaException, if the responder does not support the protocol or an internal-server-error has occurred.
            }

            // MUC
            /*
            MultiUserChatManager multiUserChatManager = xmppClient.getManager(MultiUserChatManager.class);
            ChatService chatService = multiUserChatManager.createChatService(Jid.of("conference." + xmppClient.getDomain()));
            ChatRoom chatRoom = chatService.createRoom("babbler");
            chatRoom.addOccupantListener(e -> {
                if (e.getType() == OccupantEvent.Type.ENTERED) {
                    System.out.println(e.getOccupant() + " has entered the room");
                }
            });
            chatRoom.enter("botty");
            chatRoom.sendMessage("Hello World!");
             */
            // spinning wheels here given listeners are operational
        } catch (XmppException | GeneralSecurityException ex) {
            this.running = false;
            System.out.println(Constants.s_module_name + ": XMPP driver exception " + ex.toString());
        }
    }

}
