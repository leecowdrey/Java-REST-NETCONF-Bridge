/*======================================================*/
// Module: Request Helper
// Author: Lee Cowdrey
// Version: 1.0
// History:
// 1.0	  Initial Version
//
// Notes: quick and dirty
//        
/*======================================================*/
package com.brocade.bwc.netconf.common;

import com.brocade.bwc.netconf.jnc.NetconfSession;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.brocade.bwc.netconf.json.JSONArray;
import com.brocade.bwc.netconf.json.JSONObject;
import java.net.MalformedURLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.List;
import java.net.URL;

public class RequestResponse {

    private String asyncAuthorizationHeaderName = Constants.BROCADE_HTTP_AUTH_HEADER_NAME;
    private String asyncAuthorizationHeaderValue = "";
    private String username = "admin";
    private String password = "admin";
    private String privateKey = "";
    private String passphrase = "";
    private boolean strictHostCheck = true;
    private int readTimeOut = 600; // seconds
    private int datastore = NetconfSession.RUNNING;
    private int operation = NetconfSession.NONE;
    private String operationName = "none";
    private String requestXML = "";
    private String config = "";
    private String url = "";
    private List<Target> targets = null;
    private String callbackIdentifier = "";
    private URL callbackURL = null;
    private Boolean callbackHttps = false;

// {
//   "response": [{
//     "sessionId": 0,
//     "messageId": 0,
//     "xml": "",
//     "rpcException": false,
//     "rpcWarning": false,
//     "rpcError": false,
//     "rpcOK": true,
//     "rpcMessages": [{}],
//   }]
// }
    private int sessionId = 0;
    private int messageId = 1;
    private String responseXML = "";
    private Boolean rpcOK = false;
    private Boolean rpcException = false;
    private Boolean rpcWarning = false;
    private Boolean rpcError = false;
    private Boolean rpcCallback = false;
    private JSONObject rpcMessagesJSON = new JSONObject();

    // common
    private ServletContext context = null;
    private boolean sshDebug = false;
    private boolean bypass = false;
    JSONObject requestJSON = new JSONObject();
    JSONObject responseJSON = new JSONObject();

    public RequestResponse(ServletContext context) {
        this.context = context;
    }

    private String getTimeStamp(String mask) {
        SimpleDateFormat sdfDate = new SimpleDateFormat(mask);
        Date now = new Date();
        return sdfDate.format(now);
    }

    private String getTimeStamp() {
        return getTimeStamp("yyyy-MM-dd HH:mm:ss");
    }

    public void prepare() {
        this.asyncAuthorizationHeaderName = Constants.BROCADE_HTTP_AUTH_HEADER_NAME;
        this.asyncAuthorizationHeaderValue = "";
        this.username = "admin";
        this.password = "admin";
        this.privateKey = "";
        this.passphrase = "";
        this.strictHostCheck = true;
        this.datastore = NetconfSession.RUNNING;
        this.operation = NetconfSession.NONE;
        this.operationName = "none";
        this.requestXML = "";
        this.config = "";
        this.url = "";
        this.sessionId = 0;
        this.messageId = 1;
        this.responseXML = "";
        this.rpcOK = false;
        this.rpcException = false;
        this.rpcWarning = false;
        this.rpcError = false;
        this.rpcCallback = false;
        this.sshDebug = false;
        this.bypass = false;
        this.requestJSON = new JSONObject();
        this.responseJSON = new JSONObject();
        this.rpcMessagesJSON = new JSONObject();
        this.rpcMessagesJSON.accumulate("message", "started:" + getTimeStamp());
        this.targets = new ArrayList<Target>();
        this.callbackIdentifier = "";
        this.callbackURL = null;
        this.callbackHttps = false;
    }

    public void parse(JSONObject request) {
        if (request.has("host")) {
            String host = request.getString("host");
            int port = 0;
            if (request.has("port")) {
                port = request.optInt("port", Constants.BROCADE_NETCONF_SSH_PORT);
            }
            if (request.has("username")) {
                this.username = request.getString("username");
            }
            if (request.has("password")) {
                this.password = request.getString("password");
            }
            Target target = new Target(host, port);
            targets.add(target);

            if (request.has("datastore")) {
                String datastore = request.optString("datastore", "running");
                if (datastore.equalsIgnoreCase("running")) {
                    this.datastore = NetconfSession.RUNNING;
                } else if (datastore.equalsIgnoreCase("candidate")) {
                    this.datastore = NetconfSession.CANDIDATE;
                } else if (datastore.equalsIgnoreCase("startup")) {
                    this.datastore = NetconfSession.STARTUP;
                } else {
                    this.datastore = NetconfSession.RUNNING;
                }
            }
            if (request.has("operation")) {
                String datastore = request.optString("operation", "none");
                if (datastore.equalsIgnoreCase("merge")) {
                    this.operation = NetconfSession.MERGE;
                    this.operationName = datastore.toLowerCase();
                } else if (datastore.equalsIgnoreCase("replace")) {
                    this.operation = NetconfSession.REPLACE;
                    this.operationName = datastore.toLowerCase();
                } else if (datastore.equalsIgnoreCase("create")) {
                    this.operation = NetconfSession.CREATE;
                    this.operationName = datastore.toLowerCase();
                } else if (datastore.equalsIgnoreCase("delete")) {
                    this.operation = NetconfSession.DELETE;
                    this.operationName = datastore.toLowerCase();
                } else if (datastore.equalsIgnoreCase("remove")) {
                    this.operation = NetconfSession.REMOVE;
                    this.operationName = datastore.toLowerCase();
                } else if (datastore.equalsIgnoreCase("none")) {
                    this.operation = NetconfSession.NONE;
                    this.operationName = datastore.toLowerCase();
                } else {
                    this.operation = NetconfSession.NONE;
                }
            }
            if (request.has("bypass")) {
                this.bypass = request.optBoolean("bypass", false);
            }
            if (request.has("xml")) {
                this.requestXML = request.getString("xml");
            }
            if (request.has("config")) {
                this.config = request.getString("config");
            }
            if (request.has("url")) {
                this.url = request.getString("url");
            }

            if (request.has("callback")) {
                JSONObject callbackRequest = (JSONObject) request.get("callback");
                if (callbackRequest.has("identifier")) {
                    this.callbackIdentifier = callbackRequest.getString("identifier");
                }
                if (callbackRequest.has("url")) {
                    String tmpURL = callbackRequest.getString("url");
                    if (!tmpURL.isEmpty()) {
                        try {
                            if (tmpURL.startsWith(Constants.s_https_protocol)) {
                                this.callbackURL = new java.net.URL(null, tmpURL, new sun.net.www.protocol.https.Handler());
                                this.rpcCallback = true;
                                this.callbackHttps = true;
                            } else {
                                this.callbackURL = new java.net.URL(null, tmpURL, new sun.net.www.protocol.http.Handler());
                                this.rpcCallback = true;
                                this.callbackHttps = false;
                            }
                        } catch (MalformedURLException ex) {
                            this.rpcCallback = false;
                        }
                    } else {
                        this.rpcCallback = false;
                    }
                }
            }

            if (request.has("ssh")) {
                JSONObject ssh = (JSONObject) request.get("ssh");
                if (ssh.has("privateKey")) {
                    this.privateKey = ssh.getString("privateKey");
                }
                if (ssh.has("passphrase")) {
                    this.passphrase = ssh.getString("passphrase");
                }
                if (ssh.has("strictHosts")) {
                    this.strictHostCheck = ssh.getBoolean("strictHosts");
                }
                if (ssh.has("readTimeOut")) {
                    this.readTimeOut = ssh.optInt("readTimeOut", 600);
                }
                if (ssh.has("debug")) {
                    this.sshDebug = ssh.optBoolean("debug", false);
                }
            }
            if (request.has("slaves")) {
                JSONArray slaves = request.getJSONArray("slaves");
                for (int s = 0; s < slaves.length(); s++) {
                    JSONObject slave = slaves.getJSONObject(s);
                    if (slave.has("host")) {
                        int slavePort = 0;
                        String slaveHost = slave.getString("host");
                        if (slave.has("port")) {
                            slavePort = slave.optInt("port", Constants.BROCADE_NETCONF_SSH_PORT);
                        }
                        Target slaveTarget = new Target(slaveHost, slavePort);
                        targets.add(slaveTarget);
                    }
                }
            }
        }
    }

    public boolean hasAsyncAuthorizationHeader() {
        return (!this.asyncAuthorizationHeaderName.isEmpty() && !this.asyncAuthorizationHeaderValue.isEmpty());
    }

    public void setAsyncAuthorizationHeaderName(String httpHeaderName) {
        if (!httpHeaderName.isEmpty()) {
            this.asyncAuthorizationHeaderName = httpHeaderName;
        }
    }

    public String getAsyncAuthorizationHeaderName() {
        return this.asyncAuthorizationHeaderName;
    }

    public void setAsyncAuthorizationHeaderValue(String httpHeaderValue) {
        if (!httpHeaderValue.isEmpty()) {
            this.asyncAuthorizationHeaderValue = httpHeaderValue;
        }
    }

    public String getAsyncAuthorizationHeaderValue() {
        return this.asyncAuthorizationHeaderValue;
    }

    public JSONObject getRequestJSON() {
        return this.requestJSON;
    }

    public JSONObject getResponseJSON() {
        return this.responseJSON;
    }

    public boolean isSshDebugEnabled() {
        return this.sshDebug;
    }

    public void sshEnableDebug() {
        this.sshDebug = true;
    }

    public void sshDisableDebug() {
        this.sshDebug = false;
    }

    public boolean bypassEnabled() {
        return this.bypass;
    }

    public void enableBypass() {
        this.bypass = true;
    }

    public void disableBypass() {
        this.bypass = false;
    }

    public String getResponseXML() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        xml += "<netconf>";

        if (responseJSON.has("response")) {
            JSONArray responses = responseJSON.getJSONArray("response");
            for (int i = 0; i < responses.length(); i++) {
                JSONArray responseItem = responses.getJSONArray(i);
                for (int r = 0; r < responseItem.length(); r++) {
                    xml += "<response>";
                    JSONObject response = responseItem.getJSONObject(r);
                    if (response.has("sessionId")) {
                        int sid = response.getInt("sessionId");
                        xml += "<sessionId>" + Integer.toString(sid) + "</sessionId>";
                    }
                    if (response.has("callbackIdentifier")) {
                        String xmlResponse = response.getString("callbackIdentifier");
                        if (!xmlResponse.isEmpty()) {
                            xml += "<xml><![CDATA[" + xmlResponse + "]]></xml>";
                        } else {
                            xml += "<xml/>";
                        }
                    }
                    if (response.has("messageId")) {
                        int mid = response.getInt("messageId");
                        xml += "<messageId>" + Integer.toString(mid) + "</messageId>";
                    }
                    if (response.has("rpcException")) {
                        Boolean exception = response.getBoolean("rpcException");
                        xml += "<rpcException>" + exception.toString() + "</rpcException>";
                    }
                    if (response.has("rpcWarning")) {
                        Boolean warning = response.getBoolean("rpcWarning");
                        xml += "<rpcWarning>" + warning.toString() + "</rpcWarning>";
                    }
                    if (response.has("rpcError")) {
                        Boolean error = response.getBoolean("rpcError");
                        xml += "<rpcError>" + error.toString() + "</rpcError>";
                    }
                    if (response.has("rpcOk")) {
                        Boolean ok = response.getBoolean("rpcOk");
                        xml += "<rpcOk>" + ok.toString() + "</rpcOk>";
                    }
                    if (response.has("xml")) {
                        String xmlResponse = response.getString("xml");
                        if (!xmlResponse.isEmpty()) {
                            xml += "<xml><![CDATA[" + xmlResponse + "]]></xml>";
                        } else {
                            xml += "<xml/>";
                        }
                    }
                    if (response.has("rpcMessages")) {
                        JSONArray messages = response.getJSONArray("rpcMessages");
                        if (messages.length() == 0) {
                            xml += "<rpcMessages/>";
                        } else {
                            xml += "<rpcMessages>";
                            for (int m = 0; m < messages.length(); m++) {
                                JSONObject message = messages.getJSONObject(m);

                                if (message.has("message")) {
                                    JSONArray messageDetail = message.getJSONArray("message");
                                    for (int t = 0; t < messageDetail.length(); t++) {
                                        xml += "<message><![CDATA[" + messageDetail.getString(t) + "]]></message>";
                                    }
                                }

                            }
                            xml += "</rpcMessages>";
                        }
                    }
                    xml += "</response>";
                }
            }
        }
        xml += "</netconf>";
        return xml.trim();
    }

    public String getURL() {
        return url;
    }

    public void setURL(String url) {
        this.url = url;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public List<Target> getTargets() {
        return this.targets;
    }

    public int getTargetsSize() {
        return this.targets.size();
    }

    public String getHost(int index) {
        return this.targets.get(index).getHost();
    }

    public int getPort(int index) {
        return this.targets.get(index).getPort();
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setOperation(int operation) {
        this.operation = operation;
    }

    public int getOperation() {
        return operation;
    }

    public String getOperationName() {
        return operationName;
    }

    public int getSshReadTimeOut() {
        return readTimeOut * 1000;
    }

    public void setSshReadTimeOut(int timeOut) {
        this.readTimeOut = timeOut;
    }

    public String getSshPrivateKey() {
        return privateKey;
    }

    public String getSshPassphrase() {
        return privateKey;
    }

    public boolean getSshStricHosts() {
        return strictHostCheck;
    }

    public ServletContext getContext() {
        return context;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public int getSessionId() {
        return this.sessionId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public int getMessageId() {
        return this.messageId;
    }

    public String getcallbackIdentifier() {
        return this.callbackIdentifier;
    }

    public URL getCallbackURL() {
        return this.callbackURL;
    }

    public Boolean isCallbackRequired() {
        return this.rpcCallback;
    }

    public Boolean getCallbackHttpsProtocol() {
        return this.callbackHttps;
    }

    public void setRequest(String xml) {
        if (xml != null) {
            if (!xml.isEmpty()) {
                // remove \n added newlines that NCS includes
                Pattern nlPattern = Pattern.compile("\\n");
                Matcher matcher = nlPattern.matcher(xml);
                String tmp1XML = matcher.replaceAll("");
                // remove whitespace between elements
                Pattern wsPattern = Pattern.compile(">\\s*<");
                matcher = wsPattern.matcher(tmp1XML);
                String tmp2XML = matcher.replaceAll("><");
                // replace \" with just "
                Pattern quotePattern = Pattern.compile("\\\"");
                matcher = quotePattern.matcher(tmp2XML);
                this.requestXML = matcher.replaceAll("\"").trim();
            }
        }
    }

    public String getRequest() {
        return this.requestXML;
    }

    public void setReply(String xml) {
        // remove \n added newlines that NCS includes
        if (xml != null) {
            if (!xml.isEmpty()) {
                Pattern nlPattern = Pattern.compile("\\n");
                Matcher matcher = nlPattern.matcher(xml);
                String tmp1XML = matcher.replaceAll("");
                // remove whitespace between elements
                Pattern wsPattern = Pattern.compile(">\\s*<");
                matcher = wsPattern.matcher(tmp1XML);
                String tmp2XML = matcher.replaceAll("><");
                // replace \" with just "
                Pattern quotePattern = Pattern.compile("\\\"");
                matcher = quotePattern.matcher(tmp2XML);
                this.responseXML = matcher.replaceAll("\"").trim();
            }
        }
    }

    public String getReply() {
        return this.responseXML;
    }

    public void setException(String exception) {
        this.rpcException = true;
        rpcMessagesJSON.accumulate("message", "exception:" + exception);
        setStatusFail();
    }

    public void setOK(String ok) {
        rpcMessagesJSON.accumulate("message", "ok:" + ok);
    }

    public void sshSetDebug(String debug) {
        rpcMessagesJSON.accumulate("message", "debug:<![CDATA[" + debug.replace("<![CDATA[", "").replace("]]>", "") + "]]>");
    }

    public void setWarning(String warning) {
        this.rpcWarning = true;
        rpcMessagesJSON.accumulate("message", "warning:" + warning);
    }

    public void setError(String error) {
        this.rpcError = true;
        rpcMessagesJSON.accumulate("message", "error:" + error);
    }

    public void setException(Exception exception) {
        setException(exception.toString());
    }

    public boolean getException() {
        return this.rpcException;
    }

    public boolean getWarning() {
        return this.rpcWarning;
    }

    public boolean getError() {
        return this.rpcError;
    }

    public void setStatusOk() {
        this.rpcOK = true;
//        updateResponse();
    }

    public void setStatusFail() {
        this.rpcOK = false;
//        updateResponse();
    }

    public Boolean isOk() {
        return rpcOK;
    }

    public Boolean isException() {
        return rpcException;
    }

    public void updateResponse() {
        rpcMessagesJSON.accumulate("message", "stopped:" + getTimeStamp());
        JSONObject replyJSON = new JSONObject();
        replyJSON.put("sessionId", sessionId);
        replyJSON.put("messageId", messageId);
        replyJSON.put("xml", responseXML);
        replyJSON.put("rpcOk", rpcOK);
        replyJSON.put("rpcWarning", rpcWarning);
        replyJSON.put("rpcError", rpcError);
        replyJSON.put("rpcException", rpcException);
        replyJSON.put("rpcCallback", rpcCallback);
        if (this.rpcCallback) {
            JSONObject callback = new JSONObject();
            callback.put("url", callbackURL.toString());
            callback.put("identifier", callbackIdentifier);
            replyJSON.put("callback", callback);
        }
        replyJSON.append("rpcMessages", rpcMessagesJSON);
        responseJSON.accumulate("response", new JSONArray().put(replyJSON));
    }

    public int getDataStore() {
        return this.datastore;
    }

    public String getDataStoreName() {
        String dataStoreName;
        if (this.datastore == NetconfSession.RUNNING) {
            dataStoreName = "running";
        } else if (this.datastore == NetconfSession.CANDIDATE) {
            dataStoreName = "candidate";
        } else if (this.datastore == NetconfSession.STARTUP) {
            dataStoreName = "startup";
        } else {
            dataStoreName = "running";
        }
        return dataStoreName;
    }

}
