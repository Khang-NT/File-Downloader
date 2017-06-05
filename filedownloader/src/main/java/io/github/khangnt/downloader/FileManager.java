package io.github.khangnt.downloader;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import io.github.khangnt.downloader.model.Task;

/**
 * Created by Khang NT on 6/2/17.
 * Email: khang.neon.1997@gmail.com
 */

public interface FileManager {
    FileOutputStream openWritableFile(String filePath, boolean append) throws IOException;
    FileInputStream openReadableFile(String filePath) throws IOException;
    boolean isFileExists(String filePath);
    long getFileSize(String filePath);
    void deleteFile(String filePath);
    String getChunkFile(Task task, int chunkId);
}
