package io.github.khangnt.downloader;

import io.github.khangnt.downloader.model.TaskReport;

/**
 * Created by Khang NT on 6/3/17.
 * Email: khang.neon.1997@gmail.com
 */

public interface EventListener {
    void onTaskAdded(TaskReport taskReport);
    void onTaskUpdated(TaskReport taskReport);
    void onTaskCancelled(TaskReport taskReport);
    void onTaskFinished(TaskReport taskReport);
    void onTaskFailed(TaskReport taskReport);
    void onResumed();
    void onPaused();
}
