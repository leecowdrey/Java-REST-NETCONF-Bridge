/*======================================================*/
// Module: HTTP/HTTPS POST Client
// Author: Lee Cowdrey
// Version: 1.0
// History:
// 1.0	  Initial Version
//
// Notes: quick and dirty
//
/*======================================================*/
package com.brocade.bwc.netconf.common;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URLConnection;
import java.security.KeyManagementException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

public class HttpCallback {

    private RequestResponse rr = null;
    private Boolean DEBUG = false;

    public HttpCallback(RequestResponse rr, Boolean debug) {
        this.rr = rr;
        this.DEBUG = debug;
    }

    public int post() {
        int httpStatusCode = 0;
        URLConnection urlConn = null;
        if (rr.isCallbackRequired()) {
            String postResponse = "";
            String payload = this.rr.getResponseJSON().toString();
            System.out.println(Constants.s_module_name + ": async callback for [" + this.rr.getcallbackIdentifier() + "] requested to " + this.rr.getCallbackURL().toString() + " started");

            if (!this.rr.bypassEnabled()) {
                TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
                    new X509TrustManager() {

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        @Override
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
                };
                try {
                    SSLContext sc = SSLContext.getInstance("SSL");
                    try {
                        //sc.init(null, new X509TrustManager[]{new SSLTrustManager()}, null);
                        sc.init(null, trustAllCerts, new java.security.SecureRandom());
                        javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                    } catch (KeyManagementException kme) {
                        System.out.println(Constants.s_module_name + ": async callback for [" + this.rr.getcallbackIdentifier() + "] requested to " + this.rr.getCallbackURL().toString() + " SSL certificate exception: " + kme);
                    }
                } catch (NoSuchAlgorithmException nsae) {
                    System.out.println(Constants.s_module_name + ": async callback for [" + this.rr.getcallbackIdentifier() + "] requested to " + this.rr.getCallbackURL().toString() + " SSL certificate exception: " + nsae);
                }
                // Create all-trusting host name verifier
                HostnameVerifier allHostsValid = new javax.net.ssl.HostnameVerifier() {

                    @Override
                    public boolean verify(String string, SSLSession ssls) {
                        return true;
                    }
                };
                // Install the all-trusting host verifier
                // sort out HTTPS X509 certificate using local keystore TrustManager that ignores and accepts everything
                javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
                try {
                    urlConn = this.rr.getCallbackURL().openConnection();
                    urlConn.setConnectTimeout(Constants.i_connection_timeout);
                    urlConn.setReadTimeout(Constants.i_connection_timeout);
                    urlConn.setDoOutput(true);
                    urlConn.setUseCaches(false);
                    urlConn.setDefaultUseCaches(false);

                    // add required HTTP headers
                    // check and include custom async authorization token
                    if (this.rr.hasAsyncAuthorizationHeader()) {
                        urlConn.setRequestProperty(this.rr.getAsyncAuthorizationHeaderName(), this.rr.getAsyncAuthorizationHeaderValue());
                    }
                    urlConn.setRequestProperty(Constants.s_header_content_length, Integer.toString(payload.length()));
                    urlConn.setRequestProperty(Constants.s_header_content_type, Constants.JSON_FORMAT);
                    urlConn.setRequestProperty(Constants.s_header_accept, Constants.s_mime_application_json);
                    urlConn.setRequestProperty(Constants.s_header_user_agent, Constants.s_header_user_agent_version);
                    urlConn.setRequestProperty(Constants.s_header_cache_control, Constants.s_header_pragma_no_cache);
                    urlConn.setRequestProperty(Constants.s_header_pragma, Constants.s_header_pragma_no_cache);

                    if (DEBUG) {
                        Map<String, List<String>> headerMap = urlConn.getRequestProperties();
                        for (String headerName : headerMap.keySet()) {
                            List<String> headerValuesList = headerMap.get(headerName);
                            for (String headerValue : headerValuesList) {
                                if (headerName != null) {
                                    System.out.println(Constants.s_module_name + ": async callback for [" + this.rr.getcallbackIdentifier() + "] request HTTP POST header: " + headerName + "=" + headerValue);
                                } else {
                                    System.out.println(Constants.s_module_name + ": async callback for [" + this.rr.getcallbackIdentifier() + "] request HTTP POST: " + headerValue);
                                }
                            }
                        }
                    }

                    javax.net.ssl.HttpsURLConnection httpsConnection = null;
                    java.net.HttpURLConnection httpConnection = null;
                    try {
                        if (this.rr.getCallbackHttpsProtocol()) {
                            httpsConnection = (javax.net.ssl.HttpsURLConnection) urlConn;
                            httpsConnection.setRequestMethod(Constants.s_http_method_post);
                        } else {
                            httpConnection = (java.net.HttpURLConnection) urlConn;
                            httpConnection.setRequestMethod(Constants.s_http_method_post);
                        }
//        else
//        {
//            throw new Exception("Unknown protocol " + url.getProtocol());
//        }
                    } catch (java.net.ProtocolException pe) {
                        //postResponse = Constants.s_exception_prefix + pe.toString() + Constants.s_exception_suffix;
                    }
                    if (urlConn.getDoOutput()) {
                        try {
                            // send HTTP post body payload
                            if (DEBUG) {
                                System.out.println(Constants.s_module_name + ": async callback for [" + this.rr.getcallbackIdentifier() + "] request HTTP POST body follows: ");
                                System.out.println(payload);
                            }
                            OutputStream out = urlConn.getOutputStream();
                            Writer wout = new OutputStreamWriter(out);
                            wout.write(payload);
                            wout.flush();
                            wout.close();
                            out.flush();
                            out.close();

                            if (this.rr.getCallbackHttpsProtocol()) {
                                httpsConnection.connect();
                                httpStatusCode = httpsConnection.getResponseCode();
                            } else {
                                httpConnection.connect();
                                httpStatusCode = httpConnection.getResponseCode();
                            }
                            if (this.DEBUG) {
                                System.out.println(Constants.s_module_name + ": async callback for [" + this.rr.getcallbackIdentifier() + "] response HTTP POST status code: " + Integer.toString(httpStatusCode));
                            }
                            // get any available response
                            InputStream in = urlConn.getInputStream();
                            StringBuilder sb = new StringBuilder();
                            for (int c; (c = in.read()) != -1;) {
                                sb.append((char) c);
                            }
                            in.close();
                            postResponse = sb.toString();
                            if (this.DEBUG) {
                                System.out.println(Constants.s_module_name + ": async callback for [" + this.rr.getcallbackIdentifier() + "] response HTTP POST body follows: ");
                                System.out.println(postResponse);
                            }
                            if (DEBUG) {
                                Map<String, List<String>> headerMap = urlConn.getHeaderFields();
                                for (String headerName : headerMap.keySet()) {
                                    List<String> headerValuesList = headerMap.get(headerName);
                                    for (String headerValue : headerValuesList) {
                                        if (headerName != null) {
                                            System.out.println(Constants.s_module_name + ": async callback for [" + this.rr.getcallbackIdentifier() + "] response HTTP POST header: " + headerName + "=" + headerValue);
                                        } else {
                                            System.out.println(Constants.s_module_name + ": async callback for [" + this.rr.getcallbackIdentifier() + "] response HTTP POST: " + headerValue);
                                        }
                                    }
                                }
                            }
                            if (this.rr.getCallbackHttpsProtocol()) {
                                httpsConnection.disconnect();
                            } else {
                                httpConnection.disconnect();
                            }
                        } catch (java.io.IOException ex) {
                            postResponse = ex.toString();
                        }
                    }
                } catch (java.io.IOException ex) {
                    postResponse = ex.toString();
                }
            }

            System.out.println(Constants.s_module_name
                    + ": async callback for [" + this.rr.getcallbackIdentifier() + "] requested to " + this.rr.getCallbackURL().toString() + " completed, status code: " + Integer.toString(httpStatusCode));
        } else {
            System.out.println(Constants.s_module_name + ": async callback for [" + this.rr.getcallbackIdentifier() + "] requested to " + this.rr.getCallbackURL().toString() + " not required");
        }
        return httpStatusCode;
    }

    public void close() {
        this.rr = null;
    }

}
