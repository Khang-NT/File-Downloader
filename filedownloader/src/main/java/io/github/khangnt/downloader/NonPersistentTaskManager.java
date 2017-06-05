package io.github.khangnt.downloader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private Map<Integer, Chunk> mChunkMap;

    public NonPersistentTaskManager() {
        this(new HashMap<>(), new HashMap<>(), 0, 0);
    }

    public NonPersistentTaskManager(Map<Integer, Task> taskMap, Map<Integer, Chunk> chunkMap,
                                    int taskAutoIncreaseId, int chunkAutoIncreaseId) {
        this.mTaskAutoIncreaseId = taskAutoIncreaseId;
        this.mChunkAutoIncreaseId = chunkAutoIncreaseId;
        this.mTaskMap = taskMap;
        this.mChunkMap = chunkMap;
    }

    @Override
    public Task insertTask(Task task) {
        if (task.getId() != Task.UNSET)
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
        if (!mTaskMap.containsKey(task.getId()))
            Log.d("[WARNING] Updating task not exists in map");
        synchronized (this) {
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
            List<Chunk> chunks = new ArrayList<>();
            for (Map.Entry<Integer, Chunk> entry : mChunkMap.entrySet()) {
                if (entry.getValue().getTaskId() == task.getId())
                    chunks.add(entry.getValue());
            }
            return chunks;
        }
    }

    @Override
    public void removeChunksOfTask(Task task) {
        synchronized (this) {
            List<Chunk> chunks = new ArrayList<>();
            for (Map.Entry<Integer, Chunk> entry : mChunkMap.entrySet()) {
                if (entry.getValue().getTaskId() == task.getId())
                    chunks.add(entry.getValue());
            }
            for (Chunk chunk : chunks) {
                mChunkMap.remove(chunk.getId());
            }
        }
    }

    @Override
    public Chunk insertChunk(Chunk chunk) {
        if (chunk.getId() != Chunk.UNSET)
            throw new IllegalArgumentException("Can't insert chunk has assigned an ID");
        synchronized (this) {
            int chunkId = ++mChunkAutoIncreaseId;
            chunk = chunk.newBuilder().setId(chunkId).build();
            mChunkMap.put(chunkId, chunk);
            return chunk;
        }
    }

    @Override
    public Chunk updateChunk(Chunk chunk) {
        if (!mChunkMap.containsKey(chunk.getId()))
            Log.d("[WARNING] Updating chunk not exists in map");
        synchronized (this) {
            mChunkMap.put(chunk.getId(), chunk);
            return chunk;
        }
    }

    @Override
    public void removeChunk(int chunkId) {
        synchronized (this) {
            mChunkMap.remove(chunkId);
        }
    }

    @Override
    public Chunk findChunk(int chunkId) {
        synchronized (this) {
            return mChunkMap.get(chunkId);
        }
    }

    @Override
    public List<Task> getUnfinishedTasks() {
        synchronized (this) {
            List<Task> unfinishedTasks = new ArrayList<>();
            for (Map.Entry<Integer, Task> entry : mTaskMap.entrySet()) {
                Task task = entry.getValue();
                if (task.getState() != Task.State.FINISHED
                        && task.getState() != Task.State.FAILED)
                    unfinishedTasks.add(task);
            }
            return unfinishedTasks;
        }
    }

    @Override
    public List<Task> getFinishedTasks() {
        synchronized (this) {
            List<Task> unfinishedTasks = new ArrayList<>();
            for (Map.Entry<Integer, Task> entry : mTaskMap.entrySet()) {
                Task task = entry.getValue();
                if (task.getState() == Task.State.FINISHED
                        || task.getState() == Task.State.FAILED)
                    unfinishedTasks.add(task);
            }
            return unfinishedTasks;
        }
    }

    @Override
    public void cleanUpFinishedTasks() {
        synchronized (this) {
            List<Task> finishedTasks = getFinishedTasks();
            for (Task finishedTask : finishedTasks) {
                mTaskMap.remove(finishedTask.getId());
                List<Chunk> chunksOfTask = getChunksOfTask(finishedTask);
                for (Chunk chunk : chunksOfTask) {
                    mChunkMap.remove(chunk.getId());
                }
            }
        }
    }

    @Override
    public void release() {
        mTaskMap.clear();
        mChunkMap.clear();
        mTaskMap = null;
        mChunkMap = null;
    }
}
