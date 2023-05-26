/*======================================================*/
// Module: Constants
// Author: Lee Cowdrey
// Version: 1.0
// History:
// 1.0	  Initial Version
//
// Notes: quick and dirty
//
/*======================================================*/
package com.brocade.bwc.netconf.common;

public class Constants {

    // package metadata
    public static final String s_pkg_name = "bwcNetconf";
    public static final String s_pkg_class = "com.brocade.bwc.netconf";
    public static final String s_pkg_description = "BWC Netconf Proxy/Transport";
    public static final String s_module_name = "bwcNetconf";
    public static final int s_pkg_major = 0;
    public static final int s_pkg_minor = 2;
    public static final int s_pkg_build = 2;

    public static final String CHARACTER_SET = "UTF-8";
    public static final String XML_ENCODING = "<?xml version=\"1.0\" encoding=\"" + CHARACTER_SET + "\"?>";
    public static final String HTML_FORMAT = "text/html;charset=" + CHARACTER_SET;
    public static final String XML_FORMAT = "text/xml;charset=" + CHARACTER_SET;
    public static final String JSON_FORMAT = "application/json;charset=" + CHARACTER_SET;
    public static final String REGEX_STRIP_XMLNS = "\\sxmlns[^\"]+\"[^\"]+\"";    // REGEX \sxmlns[^"]+"[^"]+"

    public static final String BROCADE_HTTP_AUTH_TYPE = "Basic";
    public static final String BROCADE_HTTP_AUTH_REALM = "brocade.com";
    public static final String BROCADE_HTTP_AUTH_USERNAME = "admin";
    public static final String BROCADE_HTTP_AUTH_PASSWORD = "YWRtaW4=";
    public static final boolean BROCADE_HTTP_PERMIT_REMOTE = false;

    public static final String BROCADE_HTTP_ROOT_CONTEXT = "/netconf";

    public static final String BROCADE_XML_DOCROOT_TAG = "bwcNetconf";
    public static final String BROCADE_XML_DOCROOT_XPATH = "/bwcNetconf/*";

    public static final String BROCADE_HTTP_BIND = "0.0.0.0";
    public static final int BROCADE_HTTP_PORT = 8070;
    public static final int BROCADE_HTTP_MIN_THREADS = 2;
    public static final int BROCADE_HTTP_MAX_THREADS = 250;
    public static final Boolean BROCADE_HTTP_ASYNC_DYNAMIC = true;
    public static final int BROCADE_HTTP_ASYNC_FIXED_POOL_THREADS = 10;
    public static final int BROCADE_HTTP_IDLE_TIMEOUT_MAX_MS = 30000;
    public static final int BROCADE_HTTP_BUFFER_SIZE = 64738;
    public static final String BROCADE_HTTP_AUTH_HEADER_NAME = "X-Auth-Token";

    public static final int BROCADE_NETCONF_SSH_PORT = 22;

    // connection timesouts
    public static final int i_connection_timeout = 180000; // milliseconds > (1000*60)*3 = 3 minutes
    public static final String s_post_suffix = ":";
    public static final String s_fragment_delimiter = "?";
    public static final String s_mime_application_json = "application/json";
    public static final String s_mime_application_xml = "application/xml";
    public static final String s_header_www_authenticate = "WWW-Authenticate";
    public static final String s_header_user_agent = "User-Agent";
    public static final String s_header_user_agent_version = s_module_name + "/" + Integer.toString(s_pkg_major) + "." + Integer.toString(s_pkg_minor);
    public static final String s_header_content_length = "Content-Length";
    public static final String s_header_content_type = "Content-Type";
    public static final String s_header_authorization = "Authorization";
    public static final String s_header_accept = "Accept";
    public static final String s_header_cache_control = "Cache-Control";
    public static final String s_header_pragma = "Pragma";
    public static final String s_header_pragma_no_cache = "no-cache";
    public static final String s_mime_charset = ";charset=" + CHARACTER_SET;
    public static final String s_http_method_post = "post";
    public static final String s_http_method_get = "get";
    public static final String s_http_method_patch = "patch";
    public static final String s_http_method_update = "update";
    public static final String s_http_method_delete = "delete";

    // globals for async callback URLS
    public static final String s_http_protocol = "http";
    public static final String s_https_protocol = "https";

    public static final String s_unix_timestamp_format = "MMM dd HH:mm:ss";
    public static final String s_timestamp_format = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    // banner and copyright
    public static final String BROCADE_BANNER
            = "\n"
            + "           ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ     TM    \n"
            + "            ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZN        \n"
            + "              ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ       \n"
            + "                ZZZZZZZZZZZOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOZZZZZZZZZZZ      \n"
            + "                  ZZZZZZZZZZ8                                ZZZZZZZZ     \n"
            + "                    ZZZZZZZZZZZ                               ZZZZZZZN    \n"
            + "                      ZZZZZZZZZZZ8                             ZZZZZZZ    \n"
            + "                        ZZZZZZZZZZZZ                           ZZZZZZZ    \n"
            + "                          8ZZZZZZZZZZZZD                       ZZZZZZZ    \n"
            + "                             ZZZZZZZZZZZZZZ                   ZZZZZZZN    \n"
            + "                                ZZZZZZZZZZZZZZZN             ZZZZZZZZ     \n"
            + "                                   ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ      \n"
            + "                                      OZZZZZZZZZZZZZZZZZZZZZZZZZZZZ       \n"
            + "                                          OZZZZZZZZZZZZZZZZZZZZZZN        \n"
            + "                                      OZZZZZZZZZZZZZZZZZZZZZZZZZZZZ       \n"
            + "                                  NZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ      \n"
            + "                               NZZZZZZZZZZZZZZZD             ZZZZZZZZ     \n"
            + "                             ZZZZZZZZZZZZZZ                   ZZZZZZZ     \n"
            + "                          8ZZZZZZZZZZZZD                       ZZZZZZZ    \n"
            + "                        ZZZZZZZZZZZZ                           ZZZZZZZ    \n"
            + "                      ZZZZZZZZZZZ8                             ZZZZZZZ    \n"
            + "                    ZZZZZZZZZZZ                               ZZZZZZZN    \n"
            + "                  ZZZZZZZZZZ8                                ZZZZZZZZ     \n"
            + "                ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ      \n"
            + "              ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ       \n"
            + "            OZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ8        \n"
            + "           ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ           \n"
            + "                                                                          \n"
            + "\n";

    public static final String BROCADE_COPYRIGHT = "Copyright (c) 2016 by Brocade";

}
