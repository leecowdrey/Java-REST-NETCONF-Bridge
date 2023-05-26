/*======================================================*/
// Module: Http Authorization
// Author: Lee Cowdrey
// Version: 1.0
// History:
// 1.0	  Initial Version
//
// Notes: quick and dirty
//
/*======================================================*/
package com.brocade.bwc.netconf.common;

import javax.servlet.ServletContext;
import javax.xml.bind.DatatypeConverter;

public class HttpAuth {

    private final String httpAuthType = Constants.BROCADE_HTTP_AUTH_TYPE;
    private String httpAuthRealm = Constants.BROCADE_HTTP_AUTH_REALM;
    private String httpAuthUsername = Constants.BROCADE_HTTP_AUTH_USERNAME;
    private String httpAuthPasswordHash = Constants.BROCADE_HTTP_AUTH_PASSWORD;
    private String httpAuthPassword = "";
    private boolean httpAuthorized = false;
    private boolean httpInitialized = false;

    public HttpAuth() {
        this.httpAuthorized = this.httpInitialized && false;
    }

    public void initialize(ServletContext context) {
        //    this.wlsRootDirectory = (String) context.getAttribute("netconf.weblogic.root.directory");
        this.httpAuthRealm = (String) context.getAttribute("bwcNetconf.http.auth.realm");
        this.httpAuthUsername = (String) context.getAttribute("bwcNetconf.http.auth.username");
        this.httpAuthPasswordHash = (String) context.getAttribute("bwcNetconf.http.auth.password");

        if (this.httpAuthPasswordHash != null) {
            this.httpAuthPassword = new String(DatatypeConverter.parseBase64Binary(httpAuthPasswordHash));
            if (httpAuthPassword != null) {
                this.httpInitialized = true;
            } else {
                System.out.println(Constants.s_module_name + ": encryption failed; logins disabled");
                this.httpInitialized = false;
            }
        } else {
            System.out.println(Constants.s_module_name + ": empty password supplied; login disabled");
            this.httpInitialized = false;
        }
    }

    public String getAuthType() {
        return this.httpAuthType;
    }

    public String getRealm() {
        return this.httpAuthRealm;
    }

    public Boolean isAuthorized() {
        return (this.httpInitialized && this.httpAuthorized);
    }

    /**
     * Gets the c6 error message.
     *
     * @param ex the ex
     * @return the c6 error message
     */
    private static String getC6ErrorMessage(Exception ex) {
        String origMessage = ex.getMessage();
        String err = extractC6ErrorMessage(origMessage);

        if (err == null) {
            Throwable t = ex.getCause();

            if (t != null) {
                err = extractC6ErrorMessage(t.getMessage());
            }
        }

        if (err == null) {
            err = origMessage;
        }

        // Still no err which happens in the case of things like NullPointerException which has no message
        if (err == null) {
            return err;
        }

        err = err.replace('\n', ' ');
        err = err.replace('"', ' ');
        err = err.replace('\'', ' ');

        return err;
    }

    /**
     * Extract c6 error message.
     *
     * @param msg the msg
     * @return the string
     */
    private static String extractC6ErrorMessage(String msg) {
        String err = null;

        if (msg != null) {
            int start = msg.indexOf((char) 6) + 1;
            if (start > 0) {
                int end = msg.indexOf((char) 6, start);
                err = msg.substring(start, end);
            } else {
                // no char 6s, just return the message
                err = msg;
            }
        }

        return err;
    }

    public void validate(String headerName, String headerValue) {

        if (headerValue != null) {
            if (!headerValue.isEmpty()) {
                if (headerValue.startsWith(httpAuthType)) {
                    String authValue = headerValue.substring(6);
                    if (!authValue.isEmpty()) {
                        String authDecoded = new String(DatatypeConverter.parseBase64Binary(authValue));
                        if (authDecoded != null) {
                            if (!authDecoded.isEmpty()) {
                                String credentials = authDecoded;
                                try {
                                    String username = credentials.split(":")[0];
                                    String password = credentials.split(":")[1];
                                    if (!username.isEmpty()) {
                                        if (username.equalsIgnoreCase(this.httpAuthUsername)) {
                                            if (!password.isEmpty()) { // crude
                                                if (this.httpAuthPassword.equals(password)) {
                                                    this.httpAuthorized = true;
                                                } else {
                                                    this.httpAuthorized = false;
                                                }
                                            } else {
                                                this.httpAuthorized = false;
                                            }
                                        } else {
                                            this.httpAuthorized = false;
                                        }
                                    } else {
                                        this.httpAuthorized = false;
                                    }
                                } catch (ArrayIndexOutOfBoundsException aioobe) {
                                    // bad authorization record
                                    this.httpAuthorized = false;
                                }
                            }
                        }
                    } else {
                        this.httpAuthorized = false;
                    }
                }
            }
        }
    }

}
