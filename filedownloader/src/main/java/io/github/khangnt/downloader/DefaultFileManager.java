package io.github.khangnt.downloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import io.github.khangnt.downloader.model.Task;

/**
 * Created by Khang NT on 6/5/17.
 * Email: khang.neon.1997@gmail.com
 */

public class DefaultFileManager implements FileManager {
    @Override
    public FileOutputStream openWritableFile(String filePath, boolean append) throws IOException {
        return new FileOutputStream(filePath, append);
    }

    @Override
    public FileInputStream openReadableFile(String filePath) throws IOException {
        return new FileInputStream(filePath);
    }

    @Override
    public boolean isFileExists(String filePath) {
        return new File(filePath).exists();
    }

    @Override
    public long getFileSize(String filePath) {
        return new File(filePath).length();
    }

    @Override
    public void deleteFile(String filePath) {
        boolean delete = new File(filePath).delete();
        if (!delete) Log.d("Delete file %d failed", filePath);
    }

    @Override
    public synchronized String getUniqueTempFile(Task task) {
        return task.getId() + "-" + UUID.randomUUID().toString() + ".temp";
    }
}
