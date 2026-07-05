package com.springrest.JavaHelpers;



public class JavaHelpers {

    public static String toUpper(String str) {
        return str != null ? str.toUpperCase() : null;
    }

    public static long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    public static void log(String msg) {
        System.out.println("[JS LOG] " + msg);
    }
    
}
