package io.github.khangnt.downloader;

import java.util.concurrent.Executor;

import io.github.khangnt.downloader.model.Task;

/**
 * Created by Khang NT on 6/3/17.
 * Email: khang.neon.1997@gmail.com
 */

public interface IFileDownloader {

    void start();
    void pause();
    void release();
    boolean isRunning();

    Task addTask(Task task);
    void cancelTask(int taskId);

    int getMaxWorkers();
    void setMaxWorkers(int maxWorkers);

    void registerListener(EventListener listener, Executor executor);
    void clearAllListener();
    void unregisterListener(EventListener listener);

    long getSpeed();

    TaskManager getTaskManager();
    HttpClient getHttpClient();
    FileManager getFileManager();
}
