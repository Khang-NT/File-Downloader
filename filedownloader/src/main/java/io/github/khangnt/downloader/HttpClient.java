package io.github.khangnt.downloader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import io.github.khangnt.downloader.model.Task;

/**
 * Created by Khang NT on 6/2/17.
 * Email: khang.neon.1997@gmail.com
 */

public interface HttpClient {
    InputStream openConnection(Task task, Map<String, String> headers) throws IOException;
    long fetchContentLength(Task task);
}
