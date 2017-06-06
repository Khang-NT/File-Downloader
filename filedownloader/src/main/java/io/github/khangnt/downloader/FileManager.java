package io.github.khangnt.downloader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.github.khangnt.downloader.model.Task;

/**
 * Created by Khang NT on 6/2/17.
 * Email: khang.neon.1997@gmail.com
 */

public interface FileManager {
    OutputStream openWritableFile(String filePath, boolean append) throws IOException;
    InputStream openReadableFile(String filePath) throws IOException;
    boolean isFileExists(String filePath);
    long getFileSize(String filePath);
    void deleteFile(String filePath);
    String getChunkFile(Task task, int chunkId);
}
