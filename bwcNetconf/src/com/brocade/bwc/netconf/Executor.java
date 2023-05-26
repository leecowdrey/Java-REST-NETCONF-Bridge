/*======================================================*/
// Module: Servlet Executor
// Author: Lee Cowdrey
// Version: 1.0
// History:
// 1.0	  Initial Version
//
// Notes: quick and dirty
//
/*======================================================*/
package com.brocade.bwc.netconf;

import com.brocade.bwc.netconf.common.Constants;
import com.brocade.bwc.netconf.common.HttpCallback;
import com.brocade.bwc.netconf.common.ServletDetail;
import com.brocade.bwc.xmpp.Driver;

import org.mortbay.http.SocketListener;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.ServletHttpContext;

import org.apache.commons.cli.*;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import javax.servlet.ServletException;

public class Executor {

    private static String HTTP_ROOT_CONTEXT = Constants.BROCADE_HTTP_ROOT_CONTEXT;
    private static int HTTP_PORT = Constants.BROCADE_HTTP_PORT;
    private static String HTTP_BIND_ADDR = Constants.BROCADE_HTTP_BIND;
    private static int HTTP_MIN_THREADS = Constants.BROCADE_HTTP_MIN_THREADS;
    private static int HTTP_MAX_THREADS = Constants.BROCADE_HTTP_MAX_THREADS;
    private static String HTTP_AUTH_REALM = Constants.BROCADE_HTTP_AUTH_REALM;
    private static String HTTP_AUTH_USERNAME = Constants.BROCADE_HTTP_AUTH_USERNAME;
    private static String HTTP_AUTH_PASSWORD = Constants.BROCADE_HTTP_AUTH_PASSWORD;
    private static boolean HTTP_PERMIT_REMOTE = Constants.BROCADE_HTTP_PERMIT_REMOTE;
    private static boolean HTTP_ASYNC_DYANMIC = Constants.BROCADE_HTTP_ASYNC_DYNAMIC;
    private static int HTTP_ASYNC_FIXED_POOL_THREADS = Constants.BROCADE_HTTP_ASYNC_FIXED_POOL_THREADS;
    private static String HTTP_ASYNC_AUTH_HEADER_NAME = Constants.BROCADE_HTTP_AUTH_HEADER_NAME;
    private static String HTTP_ASYNC_AUTH_HEADER_VALUE = "";
    private static boolean DEBUG = false;
    private static boolean XMPP = false;
    private static Driver xmppClient = null;

    private static List<ServletDetail> servlets = new ArrayList<ServletDetail>();

    private static boolean isLocal(InetAddress inetAddr) {
        boolean result = false;
        if (inetAddr.isLoopbackAddress()) {
            result = true;
        } else {
            boolean nicIpMatch = false;
            try {
                // scan all local interfaces
                for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                    for (InetAddress nicIpAddress : Collections.list(nic.getInetAddresses())) {
                        if (inetAddr.getHostAddress().equalsIgnoreCase(nicIpAddress.getHostAddress())) {
                            result = true;
                            nicIpMatch = true;
                            break;
                        }
                    }
                    if (nicIpMatch) {
                        break;
                    }
                }
            } catch (SocketException se) {
                result = false;
            }
        }
        return result;
    }

    private static void cliHelp(Options options, HelpFormatter formatter) {
        System.out.println("\n" + Constants.s_pkg_description + ", version: " + Integer.toString(Constants.s_pkg_major) + "." + Integer.toString(Constants.s_pkg_minor) + ", " + Constants.BROCADE_COPYRIGHT + "\n");
        formatter.printHelp(Constants.s_pkg_name, options);
    }

    public static void main(String[] args) {
        try {
            //
            Options options = new Options();

            Option debug = new Option("d", "debug", false, "Additional debug output");
            debug.setRequired(false);
            options.addOption(debug);

            Option xmpp = new Option("x", "xmpp", false, "XMPP");
            xmpp.setRequired(false);
            options.addOption(xmpp);

            Option httpMinThreads = new Option("hn", "http-min-threads", true, "HTTP Minimum Threads [" + Integer.toString(HTTP_MIN_THREADS) + "]");
            httpMinThreads.setRequired(false);
            options.addOption(httpMinThreads);

            Option httpMaxThreads = new Option("hm", "http-max-threads", true, "HTTP Maximum Threads [" + Integer.toString(HTTP_MAX_THREADS) + "]");
            httpMaxThreads.setRequired(false);
            options.addOption(httpMaxThreads);

            Option httpContextPath = new Option("hc", "http-context", true, "HTTP Context [" + HTTP_ROOT_CONTEXT + "]");
            httpContextPath.setRequired(false);
            options.addOption(httpContextPath);

            Option httpPort = new Option("hp", "http-port", true, "HTTP Listen Port [" + Integer.toString(HTTP_PORT) + "]");
            httpPort.setRequired(false);
            options.addOption(httpPort);

            Option httpBind = new Option("hb", "http-bind", true, "HTTP Bind IP Address [" + HTTP_BIND_ADDR + "]");
            httpBind.setRequired(false);
            options.addOption(httpBind);

            Option httpAuthRealm = new Option("har", "http-realm", true, "HTTP Auth Realm [" + HTTP_AUTH_REALM + "]");
            httpAuthRealm.setRequired(false);
            options.addOption(httpAuthRealm);

            Option httpAuthUsername = new Option("hau", "http-username", true, "HTTP Auth Username [" + HTTP_AUTH_USERNAME + "]");
            httpAuthUsername.setRequired(false);
            options.addOption(httpAuthUsername);

            Option httpAuthPassword = new Option("hap", "http-password", true, "HTTP Auth Password [" + HTTP_AUTH_PASSWORD + "] (Base64)");
            httpAuthPassword.setRequired(false);
            options.addOption(httpAuthPassword);

            Option httpPermitRemote = new Option("hpr", "http-permit-remote", false, "HTTP Permit Non-Local Connections");
            httpPermitRemote.setRequired(false);
            options.addOption(httpPermitRemote);

            Option httpAsyncThreads = new Option("hatps", "http-async-thread-pool-size", true, "HTTP Async Thread Pool size [" + Integer.toString(HTTP_ASYNC_FIXED_POOL_THREADS) + "]");
            httpAsyncThreads.setRequired(false);
            options.addOption(httpAsyncThreads);

            Option httpAsyncAuthorizationName = new Option("haahn", "http-async-authorization-header-name", true, "HTTP Async Header Authorization name [" + HTTP_ASYNC_AUTH_HEADER_NAME + "]");
            httpAsyncAuthorizationName.setRequired(false);
            options.addOption(httpAsyncAuthorizationName);

            Option httpAsyncAuthorizationValue = new Option("haahv", "http-async-authorization-header-value", true, "HTTP Async Header Authorization value");
            httpAsyncAuthorizationValue.setRequired(false);
            options.addOption(httpAsyncAuthorizationValue);

            Option help = new Option("?", "help", false, "help");
            help.setRequired(false);
            options.addOption(help);

            CommandLineParser parser = new DefaultParser();
            HelpFormatter formatter = new HelpFormatter();
            CommandLine cmd;

            try {
                cmd = parser.parse(options, args);
            } catch (ParseException e) {
                cliHelp(options, formatter);
                System.out.println(Constants.s_module_name + ": " + e.getMessage());
                System.exit(1);
                return;
            }

            DEBUG = cmd.hasOption("debug");
            XMPP = cmd.hasOption("xmpp");

            if (cmd.hasOption("http-async-thread-pool-size")) {
                int argsAsyncThreads = Integer.parseInt(cmd.getOptionValue("http-async-thread-pool-size"));
                if (argsAsyncThreads >= 2 && argsAsyncThreads <= 65535) {
                    HTTP_ASYNC_FIXED_POOL_THREADS = argsAsyncThreads;
                    HTTP_ASYNC_DYANMIC = false;
                } else {
                    HTTP_ASYNC_DYANMIC = true;
                }
            }

            if (cmd.hasOption("http-async-authorization-header-name") && cmd.hasOption("http-async-authorization-header-value")) {
                String headerName = cmd.getOptionValue("http-async-authorization-header-name");
                if (!headerName.isEmpty()) {
                    HTTP_ASYNC_AUTH_HEADER_NAME = headerName;
                }
                String headerValue = cmd.getOptionValue("http-async-authorization-header-value");
                if (!headerValue.isEmpty()) {
                    HTTP_ASYNC_AUTH_HEADER_VALUE = headerValue;
                }
                if (DEBUG) {
                    System.out.println(Constants.s_module_name + ": added async HTTP authorization header: " + HTTP_ASYNC_AUTH_HEADER_NAME + "=" + HTTP_ASYNC_AUTH_HEADER_VALUE);
                }
            }

            if (cmd.hasOption("http-min-threads")) {
                int argsMinThreads = Integer.parseInt(cmd.getOptionValue("http-min-threads"));
                if (argsMinThreads >= 2 && argsMinThreads <= 65535) {
                    HTTP_MIN_THREADS = argsMinThreads;
                }
            }

            if (cmd.hasOption("http-max-threads")) {
                int argsMaxThreads = Integer.parseInt(cmd.getOptionValue("http-max-threads"));
                if (argsMaxThreads >= 2 && argsMaxThreads <= 65535) {
                    HTTP_MAX_THREADS = argsMaxThreads;
                }
            }

            if (cmd.hasOption("http-port")) {
                int argsHttpPort = Integer.parseInt(cmd.getOptionValue("http-port"));
                if (argsHttpPort >= 1 && argsHttpPort <= 65535) {
                    HTTP_PORT = argsHttpPort;
                }
            }

            if (cmd.hasOption("http-context")) {
                String argsHttpContext = cmd.getOptionValue("http-context");
                if (argsHttpContext.startsWith("/")) {
                    HTTP_ROOT_CONTEXT = argsHttpContext;
                }
            }

            if (cmd.hasOption("http-realm")) {
                String argsHttpRealm = cmd.getOptionValue("http-realm");
                if (!argsHttpRealm.isEmpty()) {
                    HTTP_AUTH_REALM = argsHttpRealm;
                } else {
                    HTTP_AUTH_REALM = Constants.BROCADE_HTTP_AUTH_REALM;
                }
            }

            if (cmd.hasOption("http-username")) {
                String argsHttpUsername = cmd.getOptionValue("http-username");
                if (!argsHttpUsername.isEmpty()) {
                    HTTP_AUTH_USERNAME = argsHttpUsername;
                } else {
                    HTTP_AUTH_USERNAME = Constants.BROCADE_HTTP_AUTH_USERNAME;
                }
            }

            if (cmd.hasOption("http-password")) {
                String argsHttpPassword = cmd.getOptionValue("http-password");
                if (!argsHttpPassword.isEmpty()) {
                    HTTP_AUTH_PASSWORD = argsHttpPassword;
                } else {
                    HTTP_AUTH_PASSWORD = Constants.BROCADE_HTTP_AUTH_PASSWORD;
                }
            }

            HTTP_PERMIT_REMOTE = cmd.hasOption("http-permit-remote");

            if (cmd.hasOption("http-bind")) {
                try {
                    InetAddress bindAddr = InetAddress.getByName(cmd.getOptionValue("http-bind"));
                    if (isLocal(bindAddr)) {
                        HTTP_BIND_ADDR = bindAddr.getHostAddress();
                    }
                } catch (UnknownHostException ex) {
                    System.out.println(Constants.s_module_name + ": exception " + ex.toString());
                }
            }

            // define servlets within context
            servlets.add(new ServletDetail(HTTP_ROOT_CONTEXT, "/about", "About", "com.brocade.bwc.netconf.About", ServletDetail.Methods.GET));
            servlets.add(new ServletDetail(HTTP_ROOT_CONTEXT, "/action", "Action", "com.brocade.bwc.netconf.Action", ServletDetail.Methods.POST));
            servlets.add(new ServletDetail(HTTP_ROOT_CONTEXT, "/config/copy", "CopyConfig", "com.brocade.bwc.netconf.CopyConfig", ServletDetail.Methods.POST));
            servlets.add(new ServletDetail(HTTP_ROOT_CONTEXT, "/config/delete", "DeleteConfig", "com.brocade.bwc.netconf.DeleteConfig", ServletDetail.Methods.POST));
            servlets.add(new ServletDetail(HTTP_ROOT_CONTEXT, "/config/edit", "EditConfig", "com.brocade.bwc.netconf.EditConfig", ServletDetail.Methods.POST));
            servlets.add(new ServletDetail(HTTP_ROOT_CONTEXT, "/config/get", "GetConfig", "com.brocade.bwc.netconf.GetConfig", ServletDetail.Methods.POST));
            servlets.add(new ServletDetail(HTTP_ROOT_CONTEXT, "/get", "Get", "com.brocade.bwc.netconf.Get", ServletDetail.Methods.POST));
            servlets.add(new ServletDetail(HTTP_ROOT_CONTEXT, "/schema/get", "GetSchema", "com.brocade.bwc.netconf.GetSchema", ServletDetail.Methods.POST));
//            servlets.add(new ServletDetail(HTTP_ROOT_CONTEXT, "/reload", "Reload", "com.brocade.bwc.netconf.Reload", ServletDetail.Methods.GET));

            // now check for help argument as HTTP_ROOT_CONTEXT may not be default
            if (cmd.hasOption("help")) {
                cliHelp(options, formatter);
                System.out.println("\n");
                System.out.println("Available Servlets:");
                int servletId = 0;
                for (ServletDetail servlet : servlets) {
                    servletId++;
                    System.out.println("[" + Integer.toString(servletId) + "]\t[" + servlet.getMethod() + "]\t" + HTTP_ROOT_CONTEXT + servlet.getPathSpec());
                }
                System.exit(0);
                return;
            }

            //
            System.out.println(Constants.s_module_name + ": startup in progress");
            
            // if XMPP required, start first
            if (XMPP) {
                Executors.newFixedThreadPool(1).execute(() -> {
                   xmppClient = new Driver(DEBUG,false); 
                   xmppClient.init("services@cowdrey.co.uk/bwc","br1dg3w2015"); 
                   xmppClient.run();
                });
            }
            //
            Server server = new Server();
            SocketListener listener = new SocketListener();
            server.setStatsOn(false);
            server.setTrace(false);
            server.statsReset();
            listener.setHost(HTTP_BIND_ADDR);
            listener.setPort(HTTP_PORT);
            listener.setMinThreads(HTTP_MIN_THREADS);
            listener.setMaxThreads(HTTP_MAX_THREADS);
            listener.setMaxIdleTimeMs(Constants.BROCADE_HTTP_IDLE_TIMEOUT_MAX_MS);
            listener.setBufferSize(Constants.BROCADE_HTTP_BUFFER_SIZE);
            server.setStopAtShutdown(true);
            server.setStopGracefully(true);
            server.addListener(listener);
            if (DEBUG) {
                System.out.println(Constants.s_module_name + ": listener threads min=" + listener.getMinThreads() + ", max=" + listener.getMaxThreads());
            }
            ServletHttpContext context = (ServletHttpContext) server.getContext("/");

            // Prepare async dynamic or fixed thread pool
            ExecutorService es = null;
            if (HTTP_ASYNC_DYANMIC) {
                es = Executors.newCachedThreadPool();
                if (DEBUG) {
                    System.out.println(Constants.s_module_name + ": async thread pool size=dynamic");
                }
            } else {
                es = Executors.newFixedThreadPool(HTTP_ASYNC_FIXED_POOL_THREADS);
                if (DEBUG) {
                    System.out.println(Constants.s_module_name + ": async thread pool size=" + Integer.toString(HTTP_ASYNC_FIXED_POOL_THREADS));
                }
            }

            // populate context attributes
            context.setAttribute("bwcNetconf.debug", DEBUG);
            context.setAttribute("bwcNetconf.session.id", "1");
            context.setAttribute("bwcNetconf.message.id", "1");
            context.setAttribute("bwcNetconf.http.auth.realm", HTTP_AUTH_REALM);
            context.setAttribute("bwcNetconf.http.auth.username", HTTP_AUTH_USERNAME);
            context.setAttribute("bwcNetconf.http.auth.password", HTTP_AUTH_PASSWORD);
            context.setAttribute("bwcNetconf.http.threads.minimum", HTTP_MIN_THREADS);
            context.setAttribute("bwcNetconf.http.threads.maximum", HTTP_MAX_THREADS);
            context.setAttribute("bwcNetconf.http.bind.address", HTTP_BIND_ADDR);
            context.setAttribute("bwcNetconf.http.bind.port", HTTP_PORT);
            context.setAttribute("bwcNetconf.http.permit.remote.requests", Boolean.toString(HTTP_PERMIT_REMOTE).toLowerCase());
            context.setAttribute("bwcNetconf.ha.remote.host", null);
            context.setAttribute("bwcNetconf.ha.remote.port", Constants.BROCADE_HTTP_PORT);
            context.setAttribute("bwcNetconf.http.callback.thread.pool", es);
            context.setAttribute("bwcNetconf.http.callback.thread.pool.fixed", HTTP_ASYNC_FIXED_POOL_THREADS);
            context.setAttribute("bwcNetconf.http.callback.thread.pool.dyanmic", Boolean.toString(HTTP_ASYNC_DYANMIC).toLowerCase());
            context.setAttribute("bwcNetconf.http.callback.authorization.header.name", HTTP_ASYNC_AUTH_HEADER_NAME);
            context.setAttribute("bwcNetconf.http.callback.authorization.header.value", HTTP_ASYNC_AUTH_HEADER_VALUE);
            context.setAttribute("bwcNetconf.http.permit.remote.requests", Boolean.toString(HTTP_PERMIT_REMOTE).toLowerCase());

            for (ServletDetail servlet : servlets) {
                context.addServlet(servlet.getName(), servlet.getContext() + servlet.getPathSpec(), servlet.getClassFullName());
                if (DEBUG) {
                    System.out.println(Constants.s_module_name + ": added servlet: " + listener.getHost() + ":" + Integer.toString(listener.getPort()) + servlet.getContext() + servlet.getPathSpec() + " [" + servlet.getMethod() + "]");
                }
            }

            server.start();
            System.out.println(Constants.s_module_name + ": startup completed");
            server.join();

            // clean up
            System.out.println(Constants.s_module_name + ": shutting down");
            if (xmppClient != null) {
                if (xmppClient.isRunning()) {
                    xmppClient.destroy();
                }
            }
            server.removeListener(listener);
            server.destroy();
            servlets.clear();
            // clean  up async thread pool - allow pending to finish otherwise specify shutdownNow()
            if (es != null) {
                es.shutdown();
            }

            System.out.println(Constants.s_module_name + ": shutdown completed");

        } catch (Exception ex) {
            System.out.println(Constants.s_module_name + ": exception " + ex.toString());
        }

    }
}
