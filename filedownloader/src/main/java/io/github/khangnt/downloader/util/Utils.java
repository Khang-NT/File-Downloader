package io.github.khangnt.downloader.util;

/**
 * Created by Khang NT on 6/2/17.
 * Email: khang.neon.1997@gmail.com
 */

public class Utils {
    public static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }

    public static boolean checkInterrupted() throws InterruptedException {
        if (Thread.interrupted()) throw new InterruptedException();
        return true;
    }

    public static int compare(long l1, long l2) {
        // copy of Long.compare which missing in Java 6
        return l1 < l2 ? -1 : (l1 == l2 ? 0 : 1);
    }

    public static String byteArrToHex(byte[] arr) {
        String result = "";
        for (int i = 0; i < arr.length; i++) {
            result += Integer.toString((arr[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }
}
