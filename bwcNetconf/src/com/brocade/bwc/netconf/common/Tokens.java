/*======================================================*/
// Module: Legacy Token helpers 
// Author: Lee Cowdrey
// Version: 1.0
// History:
// 1.0	  Initial Version
//
// Notes: quick and dirty
//        
/*======================================================*/
package com.brocade.bwc.netconf.common;

public class Tokens {
    
    public Tokens(){
        
    }
    
     public static int numberOf(String src, String delimiter) {
        int segments = 0;
        String[] segmentSplit = src.split("\\" + delimiter);
        for (String segmentValue : segmentSplit) {
            segments++;
        }
        return segments;
    }

    public static String segment(String src, String delimiter, int segmentNumber) {
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

    public static String reduce(String src, String delimiter) {
        return src.substring((src.indexOf(delimiter) + 1));
    }
   
}
