package io.github.khangnt.downloader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.khangnt.downloader.exception.TaskNotFoundException;
import io.github.khangnt.downloader.model.Chunk;
import io.github.khangnt.downloader.model.Task;

/**
 * Created by Khang NT on 6/2/17.
 * Email: khang.neon.1997@gmail.com
 */

public class NonPersistentTaskManager implements TaskManager {

    private int mTaskAutoIncreaseId;
    private int mChunkAutoIncreaseId;

    private Map<Integer, Task> mTaskMap;
    private Map<Integer, Map<Integer, Chunk>> mChunkMap;

    public NonPersistentTaskManager() {
        this(new HashMap<Integer, Task>(), new HashMap<Integer, Map<Integer, Chunk>>(), 0, 0);
    }

    public NonPersistentTaskManager(Map<Integer, Task> taskMap, Map<Integer, Map<Integer, Chunk>> chunkMap,
                                    int taskAutoIncreaseId, int chunkAutoIncreaseId) {
        this.mTaskAutoIncreaseId = taskAutoIncreaseId;
        this.mChunkAutoIncreaseId = chunkAutoIncreaseId;
        this.mTaskMap = taskMap;
        this.mChunkMap = chunkMap;
    }

    @Override
    public Task insertTask(Task task) {
        if (task.getId() != C.UNSET)
            throw new IllegalArgumentException("Can't insert task has assigned an ID");
        synchronized (this) {
            int taskId = ++mTaskAutoIncreaseId;
            task = task.newBuilder().setId(taskId).build();
            mTaskMap.put(taskId, task);
            return task;
        }
    }

    @Override
    public Task updateTask(Task task) {
        synchronized (this) {
            if (!mTaskMap.containsKey(task.getId()))
                throw new TaskNotFoundException("This task doesn't exist");
            mTaskMap.put(task.getId(), task);
            return task;
        }
    }

    @Override
    public void removeTask(int taskId) {
        synchronized (this) {
            mTaskMap.remove(taskId);
        }
    }

    @Override
    public Task findTask(int taskId) {
        synchronized (this) {
            return mTaskMap.get(taskId);
        }
    }

    @Override
    public List<Chunk> getChunksOfTask(Task task) {
        synchronized (this) {
            Map<Integer, Chunk> chunkMap = mChunkMap.get(task.getId());
            if (chunkMap == null) {
                return Collections.emptyList();
            } else {
                return new ArrayList<>(chunkMap.values());
            }
        }
    }

    @Override
    public void removeChunksOfTask(Task task) {
        synchronized (this) {
            mChunkMap.remove(task.getId());
        }
    }

    @Override
    public Chunk insertChunk(Chunk chunk) {
        if (chunk.getId() != C.UNSET)
            throw new IllegalArgumentException("Can't insert chunk has assigned an ID");
        synchronized (this) {
            int chunkId = ++mChunkAutoIncreaseId;
            chunk = chunk.newBuilder().setId(chunkId).build();
            Map<Integer, Chunk> chunkMap = mChunkMap.get(chunk.getTaskId());
            if (chunkMap == null) {
                chunkMap = new HashMap<>();
                mChunkMap.put(chunk.getTaskId(), chunkMap);
            }
            chunkMap.put(chunkId, chunk);
            return chunk;
        }
    }

    @Override
    public Chunk updateChunk(Chunk chunk) {
        synchronized (this) {
            Map<Integer, Chunk> chunkMap = mChunkMap.get(chunk.getTaskId());
            if (chunkMap == null || !chunkMap.containsKey(chunk.getId()))
                throw new IllegalArgumentException("This chunk doesn't exist");
            chunkMap.put(chunk.getId(), chunk);
            return chunk;
        }
    }

    @Override
    public List<Task> getUndoneTasks() {
        synchronized (this) {
            List<Task> unfinishedTasks = new ArrayList<Task>();
            for (Task task : mTaskMap.values()) {
                if (task.getState() != Task.State.FINISHED
                        && task.getState() != Task.State.FAILED)
                    unfinishedTasks.add(task);
            }
            return unfinishedTasks;
        }
    }

    @Override
    public List<Task> getDoneTasks() {
        synchronized (this) {
            List<Task> unfinishedTasks = new ArrayList<Task>();
            for (Task task : mTaskMap.values()) {
                if (task.getState() == Task.State.FINISHED
                        || task.getState() == Task.State.FAILED)
                    unfinishedTasks.add(task);
            }
            return unfinishedTasks;
        }
    }

    @Override
    public List<Task> getAllTasks() {
        synchronized (this) {
            return new ArrayList<>(mTaskMap.values());
        }
    }

    @Override
    public void cleanUpFinishedTasks() {
        synchronized (this) {
            List<Task> finishedTasks = getDoneTasks();
            for (Task finishedTask : finishedTasks) {
                removeChunksOfTask(finishedTask);
                removeTask(finishedTask.getId());
            }
        }
    }
}
