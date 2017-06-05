package io.github.khangnt.downloader;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Khang NT on 6/2/17.
 * Email: khang.neon.1997@gmail.com
 */

public class Log {
    private static final Logger sLogger = Logger.getLogger("FileDownloader");
    static {
        sLogger.setLevel(Level.OFF);
    }

    public static void setLogLevel(Level level) {
        sLogger.setLevel(level);
    }

    public static void d(Throwable e, String format, Object... args) {
        sLogger.log(Level.INFO, String.format(format, args), e);
    }

    public static void d(String format, Object... args) {
        sLogger.info(String.format(format, args));
    }

    public static void e(Throwable e, String format, Object... args) {
        sLogger.log(Level.SEVERE, String.format(format, args), e);
    }

    public static void e(String format, Object... args) {
        sLogger.severe(String.format(format, args));
    }
}
