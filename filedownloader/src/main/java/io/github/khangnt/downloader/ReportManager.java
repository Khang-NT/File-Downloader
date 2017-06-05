package io.github.khangnt.downloader;

import java.util.List;

import io.github.khangnt.downloader.model.Chunk;
import io.github.khangnt.downloader.model.ChunkReport;
import io.github.khangnt.downloader.model.Task;
import io.github.khangnt.downloader.model.TaskReport;

/**
 * Created by Khang NT on 6/2/17.
 * Email: khang.neon.1997@gmail.com
 */

public interface ReportManager {

    List<TaskReport> getAllRunningTask();
    List<TaskReport> getAllFinishedTask();

    TaskReport getTaskReport(Task task);
    ChunkReport getChunkReport(Chunk chunk);
    void invalidateTask(Task task);
    void invalidateChunk(Chunk chunk);

    void increaseDownloadedCount(long byteCount);
    long getDownloadedCount();
    long resetDownloadedCount();
}
