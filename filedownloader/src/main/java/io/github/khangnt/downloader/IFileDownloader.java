package io.github.khangnt.downloader;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;

import io.github.khangnt.downloader.model.Task;
import io.github.khangnt.downloader.model.TaskReport;

/**
 * Created by Khang NT on 6/3/17.
 * Email: khang.neon.1997@gmail.com
 */

public interface IFileDownloader {

    void start();
    void pause();
    void release();

    boolean isRunning();
    boolean isReleased();

    Task addTask(Task task);
    void cancelTask(int taskId);

    int getMaxWorkers();
    void setMaxWorkers(int maxWorkers);

    void registerListener(EventListener listener, Executor executor);
    void clearAllListener();
    void unregisterListener(EventListener listener);

    void setOnChecksumMismatchListener(OnChecksumMismatchListener listener);

    long getSpeed();

    TaskReport getTaskReport(Task task);
    List<TaskReport> getTaskReports(Collection<Task> tasks);

    TaskManager getTaskManager();
    HttpClient getHttpClient();
    FileManager getFileManager();
}
