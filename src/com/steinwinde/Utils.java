package com.steinwinde;

public class Utils {

    public static void printStream(java.io.InputStream is) {
        java.util.Scanner sc = new java.util.Scanner(is);
        sc.useDelimiter("\\A");
        System.out.println(sc.hasNext() ? sc.next() : "");
        sc.close();
      }
}
