package io.github.khangnt.downloader;

import java.util.List;

import io.github.khangnt.downloader.model.Chunk;
import io.github.khangnt.downloader.model.Task;

/**
 * Created by Khang NT on 6/2/17.
 * Email: khang.neon.1997@gmail.com
 */

public interface TaskManager {

    Task insertTask(Task task);
    Task updateTask(Task task);
    void removeTask(int taskId);
    Task findTask(int taskId);

    List<Chunk> getChunksOfTask(Task task);
    void removeChunksOfTask(Task task);

    Chunk insertChunk(Chunk chunk);
    Chunk updateChunk(Chunk chunk);

    List<Task> getUndoneTasks();
    List<Task> getDoneTasks();

    void cleanUpFinishedTasks();

}
