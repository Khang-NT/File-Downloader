package io.github.khangnt.downloader.exception;

/**
 * Created by Khang NT on 6/5/17.
 * Email: khang.neon.1997@gmail.com
 */

public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException() {
    }

    public TaskNotFoundException(String s) {
        super(s);
    }

    public TaskNotFoundException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public TaskNotFoundException(Throwable throwable) {
        super(throwable);
    }

    public TaskNotFoundException(String s, Throwable throwable, boolean b, boolean b1) {
        super(s, throwable, b, b1);
    }
}
