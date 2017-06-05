package io.github.khangnt.downloader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import io.github.khangnt.downloader.exception.TaskNotFoundException;
import io.github.khangnt.downloader.model.Chunk;
import io.github.khangnt.downloader.model.Task;
import io.github.khangnt.downloader.worker.ChunkWorker;
import io.github.khangnt.downloader.worker.ChunkWorkerListener;
import io.github.khangnt.downloader.worker.MergeFileWorker;
import io.github.khangnt.downloader.worker.MergeFileWorkerListener;
import io.github.khangnt.downloader.worker.ModeratorExecutor;

/**
 * Created by Khang NT on 6/2/17.
 * Email: khang.neon.1997@gmail.com
 */

public class FileDownloader implements IFileDownloader, ChunkWorkerListener, MergeFileWorkerListener {
    public static final String MODERATOR_THREAD = "ModeratorThread";

    private static final String CHUNK_KEY_PREFIX = "chunk:";
    private static final String MERGE_KEY_PREFIX = "merge:";

    private final Object lock = new Object();

    private FileManager mFileManager;
    private HttpClient mHttpClient;
    private TaskManager mTaskManager;
    private DownloadSpeedMeter mDownloadSpeedMeter;

    private EventDispatcher mEventDispatcher;
    private Map<String, Thread> mWorkers;
    private ModeratorExecutor mModeratorExecutor;

    private boolean mRunning;

    private int mMaxWorker;

    public FileDownloader(FileManager fileManager, HttpClient httpClient, TaskManager taskManager,
                          DownloadSpeedMeter downloadSpeedMeter) {
        mFileManager = fileManager;
        mHttpClient = httpClient;
        mTaskManager = taskManager;
        mDownloadSpeedMeter = downloadSpeedMeter;

        mRunning = false;
        mEventDispatcher = new EventDispatcher();
        mWorkers = new HashMap<>();
        mModeratorExecutor = new ModeratorExecutor(runnable -> new Thread(runnable, MODERATOR_THREAD));
    }

    @Override
    public Task addTask(Task task) {
        Task result = getTaskManager().insertTask(task);
        if (isRunning()) mModeratorExecutor.execute(this::spawnWorker);
        return result;
    }

    @Override
    public void cancelTask(int taskId) {
        Task task = getTaskManager().findTask(taskId);
        if (task == null) {
            throw new TaskNotFoundException("No task exists with this ID: " + taskId);
        }
        cancelTaskInternal(task, "Cancelled");
    }

    @Override
    public void start() {
        synchronized (lock) {
            mRunning = true;
            mModeratorExecutor.execute(this::spawnWorker);
            mDownloadSpeedMeter.start();
        }
    }

    @Override
    public void pause() {
        synchronized (lock) {
            mDownloadSpeedMeter.pause();
            mModeratorExecutor.execute(() -> {
                for (Map.Entry<String, Thread> entry : mWorkers.entrySet()) {
                    entry.getValue().interrupt();
                }
                mWorkers.clear();
            });
            mModeratorExecutor.interrupt();
            mRunning = false;
        }
    }

    @Override
    public void release() {
        pause();
        synchronized (lock) {
            mEventDispatcher.unregisterAllListener();
            mTaskManager.release();
            mDownloadSpeedMeter.release();
            mFileManager = null;
            mHttpClient = null;
            mTaskManager = null;
            mDownloadSpeedMeter = null;
            mEventDispatcher = null;
            mModeratorExecutor = null;
        }
    }

    @Override
    public boolean isRunning() {
        return mRunning;
    }

    @Override
    public int getMaxWorkers() {
        return mMaxWorker;
    }

    @Override
    public void setMaxWorkers(int maxWorkers) {
        synchronized (lock) {
            if (maxWorkers != mMaxWorker) {
                if (maxWorkers < 0)
                    throw new IllegalArgumentException("Number of workers must > 0");
                mMaxWorker = maxWorkers;
            }
            if (mRunning) mModeratorExecutor.execute(this::spawnWorker);
        }
    }

    @Override
    public void registerListener(EventListener listener, Executor executor) {
        mEventDispatcher.registerListener(executor, listener);
    }

    @Override
    public void clearAllListener() {
        mEventDispatcher.unregisterAllListener();
    }

    @Override
    public void unregisterListener(EventListener listener) {
        mEventDispatcher.unregisterListener(listener);
    }

    @Override
    public long getSpeed() {
        return mDownloadSpeedMeter.getSpeed();
    }

    @Override
    public TaskManager getTaskManager() {
        return mTaskManager;
    }

    @Override
    public HttpClient getHttpClient() {
        return mHttpClient;
    }

    @Override
    public FileManager getFileManager() {
        return mFileManager;
    }

    void spawnWorker() {
        if (!Thread.currentThread().getName().equals(MODERATOR_THREAD))
            throw new IllegalStateException("Spawn worker must run on Moderator thread");
        List<Task> unfinishedTasks = getTaskManager().getUnfinishedTasks();
        for (Task task : unfinishedTasks) {
            if (!isRunning() || Thread.interrupted()) return;
            if (task.getState() == Task.State.IDLE) try {
                task = initTask(task);
            } catch (Exception e) {
                Log.e(e, "Failed to get content length");
                // INIT -> FAILED
                getTaskManager().updateTask(task.newBuilder().setState(Task.State.FAILED)
                        .setMessage("Failed to read content length: " + e.getMessage())
                        .build());
            }
            if (mWorkers.size() < getMaxWorkers()) {
                List<Chunk> chunks = mTaskManager.getChunksOfTask(task);
                if (areAllChunkFinished(chunks)) {
                    getTaskManager().updateTask(task.newBuilder().setState(Task.State.MERGING)
                            .build());
                    spawnMergeFileWorkerIfNotExists(task, chunks);
                } else {
                    spawnChunkWorkerIfNotExists(task, chunks);
                    splitLargeChunkIfPossible(task);
                }
            }
        }
    }

    protected Task initTask(Task task) throws IOException {
        Log.d("Initializing task-%d...", task.getId());
        mTaskManager.removeChunksOfTask(task);
        Task.Builder after = task.newBuilder();
        if (after.getLength() == Task.UNSET)
            after.setLength(getHttpClient().fetchContentLength(task));
        if (!after.isResumable()) {
            getTaskManager().insertChunk(new Chunk.Builder(after.getId()).build());
        } else {
            long length = after.getLength();
            int numberOfChunks = 1;
            while (numberOfChunks < after.getMaxChunks()
                    && length / (numberOfChunks + 1) > Chunk.MIN_CHUNK_LENGTH)
                numberOfChunks++;
            final long lengthPerChunk = length / numberOfChunks;
            for (int i = 0; i < numberOfChunks - 1; i++) {
                getTaskManager().insertChunk(new Chunk.Builder(after.getId())
                        .setRange(i * lengthPerChunk, (i + 1) * lengthPerChunk - 1)
                        .build());
            }
            getTaskManager().insertChunk(new Chunk.Builder(after.getId())
                    .setRange((numberOfChunks - 1) * lengthPerChunk, length - 1)
                    .build());
        }
        // INIT -> PENDING
        return mTaskManager.updateTask(after.setState(Task.State.PENDING).build());
    }

    protected void spawnChunkWorkerIfNotExists(Task task, List<Chunk> chunks) {
        for (Chunk chunk : chunks) {
            if (!isRunning() || Thread.interrupted()) return;
            if (chunk.isFinished()) continue;
            if (mWorkers.size() < getMaxWorkers()) {
                ChunkWorker chunkWorker = (ChunkWorker) mWorkers.get(CHUNK_KEY_PREFIX + chunk.getId());
                if (chunkWorker == null) {
                    chunkWorker = new ChunkWorker(chunk, mFileManager.getChunkFile(task, chunk.getId()),
                            getHttpClient(), getTaskManager(), getFileManager(), mDownloadSpeedMeter, this);
                    chunkWorker.start();
                    Log.d("Spawn worker %s for task %d", CHUNK_KEY_PREFIX + chunk.getId(), task.getId());
                    mWorkers.put(CHUNK_KEY_PREFIX + chunk.getId(), chunkWorker);
                }
            } else {
                break;
            }
        }
    }

    protected void spawnMergeFileWorkerIfNotExists(Task task, List<Chunk> chunks) {
        MergeFileWorker mergeFileWorker = (MergeFileWorker) mWorkers.get(MERGE_KEY_PREFIX + task.getId());
        if (mergeFileWorker == null) {
            mergeFileWorker = new MergeFileWorker(task, chunks, getFileManager(), this);
            mergeFileWorker.start();
            Log.d("Spawn worker %s for task %d", MERGE_KEY_PREFIX + task.getId(), task.getId());
            mWorkers.put(MERGE_KEY_PREFIX + task.getId(), mergeFileWorker);

        }
    }

    protected void splitLargeChunkIfPossible(Task task) {
        if (!task.isResumable()) return;
        List<ChunkWorker> runningChunks = new ArrayList<>();
        for (Thread thread : mWorkers.values()) {
            if (thread instanceof ChunkWorker) {
                ChunkWorker worker = (ChunkWorker) thread;
                if (worker.getChunk().getTaskId() == task.getId())
                    runningChunks.add(worker);
            }
        }
        int maxChunksCanSpawn = Math.min(getMaxWorkers() - mWorkers.size(),
                task.getMaxChunks() - runningChunks.size());
        if (maxChunksCanSpawn > 0) {
            // sort by remaining bytes of chunk
            Collections.sort(runningChunks, (c1, c2) -> -Long.compare(c1.getRemainingBytes(),
                    c2.getRemainingBytes()));
            for (ChunkWorker worker : runningChunks) {
                Chunk newChunk = worker.splitChunk();
                if (newChunk == null) return;
                spawnChunkWorkerIfNotExists(task, Collections.singletonList(newChunk));
                if (--maxChunksCanSpawn == 0) return;
            }
        }
    }

    protected boolean areAllChunkFinished(List<Chunk> chunks) {
        for (Chunk chunk : chunks) {
            if (!chunk.isFinished()) return false;
        }
        return true;
    }

    private void cancelTaskInternal(Task task, String message) {
        getTaskManager().updateTask(task.newBuilder().setState(Task.State.FAILED)
                .setMessage(message)
                .build());
        mModeratorExecutor.execute(() -> {
            List<Chunk> chunksOfTask = getTaskManager().getChunksOfTask(task);
            for (Chunk chunk : chunksOfTask) {
                ChunkWorker worker = (ChunkWorker) mWorkers.remove(CHUNK_KEY_PREFIX + chunk.getId());
                if (worker != null) {
                    worker.interrupt();
                    try {
                        worker.join();
                    } catch (InterruptedException e) {
                    }
                    String chunkFile = getFileManager().getChunkFile(task, chunk.getId());
                    getFileManager().deleteFile(chunkFile);
                }
            }
        });
    }

    @Override
    public void onChunkFinished(ChunkWorker worker) {
        Log.d("Chunk-%d finished", worker.getChunk().getId());
        mModeratorExecutor.execute(() -> mWorkers.remove(CHUNK_KEY_PREFIX + worker.getChunk().getId()));
        synchronized (lock) {
            if (isRunning()) mModeratorExecutor.execute(this::spawnWorker);
        }
    }

    @Override
    public void onChunkError(ChunkWorker worker, String reason, Throwable throwable) {
        Log.e(throwable, "Chunk-%d failed: %s", worker.getChunk().getId(), reason);
        // download chunk error ==> the task also error
        Task task = getTaskManager().findTask(worker.getChunk().getTaskId());
        if (task == null)
            throw new TaskNotFoundException("Something went wrong, task was removed while chunks were downloading");
        cancelTaskInternal(task, reason);
        synchronized (lock) {
            if (isRunning()) mModeratorExecutor.execute(this::spawnWorker);
        }
    }

    @Override
    public void onChunkInterrupted(ChunkWorker worker) {
        mModeratorExecutor.execute(() -> mWorkers.remove(CHUNK_KEY_PREFIX + worker.getChunk().getId()));
        Log.d("Chunk-%d is interrupted", worker.getChunk().getId());
    }

    @Override
    public void onMergeFileFinished(MergeFileWorker worker) {
        Task task = worker.getTask();
        Log.d("Task-%d is finished", task.getId());
        getTaskManager().updateTask(task.newBuilder().setState(Task.State.FINISHED)
                .setMessage("Successful").build());
        mModeratorExecutor.execute(() -> {
            mWorkers.remove(MERGE_KEY_PREFIX + worker.getTask().getId());
            List<Chunk> chunks = getTaskManager().getChunksOfTask(task);
            for (Chunk chunk : chunks) {
                String chunkFile = getFileManager().getChunkFile(task, chunk.getId());
                getFileManager().deleteFile(chunkFile);
            }
        });
        synchronized (lock) {
            if (isRunning()) mModeratorExecutor.execute(this::spawnWorker);
        }
    }

    @Override
    public void onMergeFileError(MergeFileWorker worker, String reason, Throwable error) {
        Task task = worker.getTask();
        Log.e(error, "Task-%d failed: %s", task.getId(), reason);
        cancelTaskInternal(task, reason);
        mModeratorExecutor.execute(() -> mWorkers.remove(MERGE_KEY_PREFIX + worker.getTask().getId()));
        synchronized (lock) {
            if (isRunning()) mModeratorExecutor.execute(this::spawnWorker);
        }
    }

    @Override
    public void onMergeFileInterrupted(MergeFileWorker worker) {
        Log.d("Merge file interrupted (task-%d)", worker.getTask().getId());
        mModeratorExecutor.execute(() -> mWorkers.remove(MERGE_KEY_PREFIX + worker.getTask().getId()));
    }
}
