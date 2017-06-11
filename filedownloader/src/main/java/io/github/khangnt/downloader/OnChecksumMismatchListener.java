package io.github.khangnt.downloader;

import io.github.khangnt.downloader.model.Task;

/**
 * Created by Khang NT on 6/11/17.
 * Email: khang.neon.1997@gmail.com
 */

public interface OnChecksumMismatchListener {
    boolean onChecksumMismatch(Task task, String algorithm, String expected, String found);
}
