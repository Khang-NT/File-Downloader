package io.github.khangnt.downloader;

import java.util.List;

import io.github.khangnt.downloader.model.Task;
import io.github.khangnt.downloader.model.TaskReport;

/**
 * Created by Khang NT on 6/3/17.
 * Email: khang.neon.1997@gmail.com
 */

public interface EventListener {
    void onTaskAdded(Task task);
    void onTaskRemoved(Task task);
    void onTaskFinished(Task task);
    void onQueueChanged(List<TaskReport> queue);
    void onResumed();
    void onPaused();
}
