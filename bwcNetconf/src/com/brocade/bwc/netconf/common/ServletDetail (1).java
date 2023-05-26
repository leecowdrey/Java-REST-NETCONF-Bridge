/*======================================================*/
// Module: Servlet Detail
// Author: Lee Cowdrey
// Version: 1.0
// History:
// 1.0	  Initial Version
//
// Notes: quick and dirty
//        
/*======================================================*/
package com.brocade.bwc.netconf.common;

public class ServletDetail {

    public enum Methods {
        NONE, GET, POST, PUT, PATCH, DELETE
    }

    private String context = "";
    private String pathSpec = "";
    private String classFullName = "";
    private String name = "";
    private Methods httpMethod = Methods.NONE;

    public ServletDetail() {

    }

    public ServletDetail(String context, String pathSpec, String name, String classFullName, Methods httpMethod) {
        this.context = context;
        this.pathSpec = pathSpec;
        this.name = name;
        this.classFullName = classFullName;
        this.httpMethod = httpMethod;
    }

    public String getMethod() {
        return this.httpMethod.toString().toUpperCase();
    }

    public String getContext() {
        return this.context;
    }

    public String getPathSpec() {
        return this.pathSpec;
    }

    public String getClassFullName() {
        return this.classFullName;
    }

    public String getName() {
        return this.name;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public void setPathSpec(String pathSpec) {
        this.pathSpec = pathSpec;
    }

    public void setClassFullName(String classFullName) {
        this.classFullName = classFullName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMethod(String method) {
        String methodHttp = method.toLowerCase();
        switch (methodHttp) {
            case "get":
                this.httpMethod = Methods.GET;
            case "post":
                this.httpMethod = Methods.POST;
            case "put":
                this.httpMethod = Methods.PUT;
            case "patch":
                this.httpMethod = Methods.PATCH;
            case "delete":
                this.httpMethod = Methods.DELETE;
            default:
                this.httpMethod = Methods.DELETE;
        }
    }

}
