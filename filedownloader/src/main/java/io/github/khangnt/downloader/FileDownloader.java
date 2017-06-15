package io.github.khangnt.downloader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

import io.github.khangnt.downloader.model.Chunk;
import io.github.khangnt.downloader.model.ChunkReport;
import io.github.khangnt.downloader.model.Task;
import io.github.khangnt.downloader.model.TaskReport;
import io.github.khangnt.downloader.util.Utils;
import io.github.khangnt.downloader.worker.ChunkWorker;
import io.github.khangnt.downloader.worker.ChunkWorkerListener;
import io.github.khangnt.downloader.worker.MergeFileWorker;
import io.github.khangnt.downloader.worker.MergeFileWorkerListener;
import io.github.khangnt.downloader.worker.ModeratorExecutor;

/**
 * Created by Khang NT on 6/2/17.
 * Email: khang.neon.1997@gmail.com
 */

public class FileDownloader implements IFileDownloader, ChunkWorkerListener, MergeFileWorkerListener,
        OnChecksumMismatchListener {
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
    private Map<Integer, TaskReport> mTaskReportMap;
    private OnChecksumMismatchListener mOnChecksumMismatchListener;

    public FileDownloader() {
        this(new DefaultFileManager(), new DefaultHttpClient(), new NonPersistentTaskManager());
    }

    public FileDownloader(FileManager fileManager, HttpClient httpClient, TaskManager taskManager) {
        mFileManager = fileManager;
        mHttpClient = httpClient;
        mTaskManager = taskManager;

        mRunning = false;
        mEventDispatcher = new EventDispatcher();
        mDownloadSpeedMeter = new DownloadSpeedMeter();
        mWorkers = new HashMap<>();
        mTaskReportMap = new HashMap<>();
        mModeratorExecutor = new ModeratorExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                return new Thread(runnable, MODERATOR_THREAD);
            }
        });
    }

    @Override
    public Task addTask(Task task) {
        Task result = getTaskManager().insertTask(task);
        TaskReport taskReport = new TaskReport(result, Collections.<ChunkReport>emptyList());
        mTaskReportMap.put(result.getId(), taskReport);
        mEventDispatcher.onTaskAdded(taskReport);
        if (isRunning()) spawnWorker();
        return result;
    }

    @Override
    public void cancelTask(final int taskId) {
        mModeratorExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Task task = getTaskManager().findTask(taskId);
                if (task != null && !task.isDone()) {
                    stopAllWorkerOfTaskSync(task);
                    getFileManager().deleteFile(task.getFilePath());
                    Task cancelledTask = getTaskManager().updateTask(task.newBuilder()
                            .setState(Task.State.FAILED)
                            .setMessage("Cancelled").build());
                    updateTaskReport(cancelledTask, false);
                    mEventDispatcher.onTaskCancelled(getTaskReport(cancelledTask));
                }
            }
        });
    }

    @Override
    public void start() {
        synchronized (lock) {
            if (!isRunning()) {
                mRunning = true;
                mDownloadSpeedMeter.start();
                mEventDispatcher.onResumed();
            }
            spawnWorker();
        }
    }

    @Override
    public void pause() {
        synchronized (lock) {
            mModeratorExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    for (Thread thread : mWorkers.values()) {
                        thread.interrupt();
                    }
                    mWorkers.clear();

                    List<Task> undoneTasks = getTaskManager().getUndoneTasks();
                    for (Task undoneTask : undoneTasks) {
                        if (undoneTask.getState() == Task.State.DOWNLOADING
                                || undoneTask.getState() == Task.State.MERGING) {
                            Task updatedTask = getTaskManager().updateTask(undoneTask.newBuilder()
                                    .setState(Task.State.WAITING).build());
                            updateTaskReport(updatedTask, false);
                            mEventDispatcher.onTaskUpdated(getTaskReport(updatedTask));
                        }
                    }

                }
            });

            if (isRunning()) {
                mRunning = false;
                mDownloadSpeedMeter.pause();
                mEventDispatcher.onPaused();
            }
        }
    }

    @Override
    public void release() {
        synchronized (lock) {
            pause();
            mModeratorExecutor.executeAllPendingRunnable();
            mEventDispatcher.unregisterAllListener();
            mFileManager = null;
            mHttpClient = null;
            mTaskManager = null;
        }
    }

    @Override
    public boolean isRunning() {
        return mRunning;
    }

    @Override
    public boolean isReleased() {
        return getFileManager() == null || getTaskManager() == null || getHttpClient() == null;
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
            if (mRunning) spawnWorker();
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
    public void setOnChecksumMismatchListener(OnChecksumMismatchListener listener) {
        mOnChecksumMismatchListener = listener;
    }

    @Override
    public long getSpeed() {
        return mDownloadSpeedMeter.getSpeed();
    }

    @Override
    public TaskReport getTaskReport(Task task) {
        TaskReport taskReport = mTaskReportMap.get(task.getId());
        if (taskReport == null) {
            List<ChunkReport> chunkReports = new ArrayList<>();
            List<Chunk> chunks = getTaskManager().getChunksOfTask(task);
            for (Chunk chunk : chunks) {
                chunkReports.add(new ChunkReport(chunk, getFileManager()));
            }
            taskReport = new TaskReport(task, chunkReports);
            mTaskReportMap.put(task.getId(), taskReport);
        }
        return taskReport;
    }

    private void updateTaskReport(Task task, boolean chunkChanged) {
        TaskReport taskReport = mTaskReportMap.get(task.getId());
        if (taskReport != null) {
            List<ChunkReport> chunkReports = taskReport.getChunkReports();
            if (chunkChanged) {
                chunkReports = new ArrayList<>();
                List<Chunk> chunks = getTaskManager().getChunksOfTask(task);
                for (Chunk chunk : chunks) {
                    chunkReports.add(new ChunkReport(chunk, getFileManager()));
                }
            }
            taskReport = new TaskReport(task, chunkReports);
            mTaskReportMap.put(task.getId(), taskReport);
        }
    }

    @Override
    public List<TaskReport> getTaskReports(Collection<Task> tasks) {
        List<TaskReport> taskReports = new ArrayList<>();
        for (Task task : tasks) {
            taskReports.add(getTaskReport(task));
        }
        return taskReports;
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

    protected void spawnWorker() {
        mModeratorExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (!Thread.currentThread().getName().equals(MODERATOR_THREAD))
                    throw new IllegalStateException("Spawn worker must run on Moderator thread");
                if (!isRunning() || Thread.interrupted()) return;
                List<Task> unfinishedTasks = getTaskManager().getUndoneTasks();
                for (Task task : unfinishedTasks) {
                    if (!isRunning() || Thread.interrupted()) return;
                    if (task.getState() == Task.State.IDLE) try {
                        task = initTask(task);
                        updateTaskReport(task, true);
                        mEventDispatcher.onTaskUpdated(getTaskReport(task));
                    } catch (Exception e) {
                        Log.e(e, "Failed to initialize task-%d", task.getId());
                        // INIT -> FAILED
                        Task failedTask = getTaskManager().updateTask(task.newBuilder()
                                .setState(Task.State.FAILED)
                                .setMessage("Failed to read content length: " + e.getMessage())
                                .build());
                        updateTaskReport(failedTask, false);
                        mEventDispatcher.onTaskFailed(getTaskReport(failedTask));
                        continue;
                    }
                    if (mWorkers.size() < getMaxWorkers()) {
                        List<Chunk> chunks = mTaskManager.getChunksOfTask(task);
                        if (areAllChunkFinished(chunks)) {
                            spawnMergeFileWorkerIfNotExists(task, chunks);
                        } else {
                            spawnChunkWorkerIfNotExists(task, chunks);
                            splitLargeChunkIfPossible(task);
                        }
                    }
                }
            }
        });
    }

    protected Task initTask(Task task) {
        Log.d("Initializing task-%d...", task.getId());
        mTaskManager.removeChunksOfTask(task);
        Task.Builder after = task.newBuilder();
        if (after.getLength() == C.UNSET) {
            HttpClient.ContentDescription contentDescription = getHttpClient().fetchContentDescription(task);
            after.setLength(contentDescription.getLength())
                    .setResumable(contentDescription.isAcceptRange());
        }
        if (!after.isResumable()) {
            getTaskManager().insertChunk(new Chunk.Builder(after.getId(),
                    mFileManager.getUniqueTempFile(task)).build());
        } else {
            long length = after.getLength();
            int numberOfChunks = 1;
            while (numberOfChunks < after.getMaxChunks()
                    && length / (numberOfChunks + 1) > C.MIN_CHUNK_LENGTH)
                numberOfChunks++;
            final long lengthPerChunk = length / numberOfChunks;
            for (int i = 0; i < numberOfChunks - 1; i++) {
                getTaskManager().insertChunk(new Chunk.Builder(after.getId(),
                        mFileManager.getUniqueTempFile(task))
                        .setRange(i * lengthPerChunk, (i + 1) * lengthPerChunk - 1)
                        .build());
            }
            getTaskManager().insertChunk(new Chunk.Builder(after.getId(),
                    mFileManager.getUniqueTempFile(task))
                    .setRange((numberOfChunks - 1) * lengthPerChunk, length - 1)
                    .build());
        }
        // INIT -> WAITING
        return mTaskManager.updateTask(after.setState(Task.State.WAITING).build());
    }

    protected void spawnChunkWorkerIfNotExists(Task task, List<Chunk> chunks) {
        for (Chunk chunk : chunks) {
            if (!isRunning() || Thread.interrupted()) return;
            if (chunk.isFinished()) continue;
            if (mWorkers.size() < getMaxWorkers()) {
                ChunkWorker chunkWorker = (ChunkWorker) mWorkers.get(CHUNK_KEY_PREFIX + chunk.getId());
                if (chunkWorker == null) {
                    chunkWorker = new ChunkWorker(chunk, getHttpClient(), getTaskManager(),
                            getFileManager(), mDownloadSpeedMeter, this);
                    chunkWorker.start();
                    Log.d("Spawn worker %s for task %d", CHUNK_KEY_PREFIX + chunk.getId(), task.getId());
                    mWorkers.put(CHUNK_KEY_PREFIX + chunk.getId(), chunkWorker);

                    if (task.getState() == Task.State.WAITING) {
                        task = getTaskManager().updateTask(task.newBuilder()
                                .setState(Task.State.DOWNLOADING).build());
                        updateTaskReport(task, false);
                        mEventDispatcher.onTaskUpdated(getTaskReport(task));
                    }
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

            if (task.getState() != Task.State.MERGING) {
                task = getTaskManager().updateTask(task.newBuilder()
                        .setState(Task.State.MERGING).build());
                updateTaskReport(task, false);
                mEventDispatcher.onTaskUpdated(getTaskReport(task));
            }
        }
    }

    protected void splitLargeChunkIfPossible(Task task) {
        if (!task.isResumable()) return;
        List<ChunkWorker> runningChunks = new ArrayList<ChunkWorker>();
        for (Thread thread : mWorkers.values()) {
            if (thread instanceof ChunkWorker) {
                ChunkWorker worker = (ChunkWorker) thread;
                if (worker.getChunk().getTaskId() == task.getId())
                    runningChunks.add(worker);
            }
        }
        int maxWorkersCanSpawn = Math.min(getMaxWorkers() - mWorkers.size(),
                task.getMaxParallelConnections() - runningChunks.size());
        if (maxWorkersCanSpawn > 0) {
            // sort running chunk workers by remaining bytes of chunk
            Collections.sort(runningChunks, new Comparator<ChunkWorker>() {
                @Override
                public int compare(ChunkWorker c1, ChunkWorker c2) {
                    return - Utils.compare(c1.getRemainingBytes(), c2.getRemainingBytes());
                }
            });
            for (ChunkWorker worker : runningChunks) {
                Chunk newChunk = worker.splitChunk(task);
                if (newChunk == null) return;
                spawnChunkWorkerIfNotExists(task, Collections.singletonList(newChunk));
                updateTaskReport(task, true);
                mEventDispatcher.onTaskUpdated(getTaskReport(task));
                if (--maxWorkersCanSpawn == 0) return;
            }
        }
    }

    protected boolean areAllChunkFinished(List<Chunk> chunks) {
        for (Chunk chunk : chunks) {
            if (!chunk.isFinished()) return false;
        }
        return true;
    }

//    private Task cancelTaskInternal(final Task task, final Callback1<Task> onCanceleld) {
////        Task cancelledTask = task.newBuilder().setState(Task.State.FAILED)
////                .setMessage(message)
////                .build();
////        getTaskManager().updateTask(cancelledTask);
////        updateTaskReport(cancelledTask, false);
//
//        mModeratorExecutor.execute(new Runnable() {
//            @Override
//            public void run() {
//                List<Chunk> chunksOfTask = getTaskManager().getChunksOfTask(task);
//                for (Chunk chunk : chunksOfTask) {
//                    ChunkWorker worker = (ChunkWorker) mWorkers.remove(CHUNK_KEY_PREFIX + chunk.getId());
//                    if (worker != null) {
//                        worker.interrupt();
//                        try {
//                            worker.join();
//                        } catch (InterruptedException ignore) {
//                        }
//                        getFileManager().deleteFile(chunk.getChunkFile());
//                    }
//                }
//                if (deleteFile) getFileManager().deleteFile(task.getFilePath());
//            }
//        });
//        return cancelledTask;
//    }

    private void stopAllWorkerOfTaskSync(Task task) {
        List<Chunk> chunksOfTask = getTaskManager().getChunksOfTask(task);
        for (Chunk chunk : chunksOfTask) {
            ChunkWorker worker = (ChunkWorker) mWorkers.remove(CHUNK_KEY_PREFIX + chunk.getId());
            if (worker != null) {
                worker.interrupt();
                try {
                    worker.join();
                } catch (InterruptedException ignore) {
                }
                getFileManager().deleteFile(chunk.getChunkFile());
            }
        }
        MergeFileWorker mergeFileWorker = (MergeFileWorker) mWorkers.remove(MERGE_KEY_PREFIX + task.getId());
        if (mergeFileWorker != null) {
            mergeFileWorker.interrupt();
            try {
                mergeFileWorker.join();
            } catch (InterruptedException ignore) {
            }
        }
    }

    @Override
    public void onChunkFinished(final ChunkWorker worker) {
        Log.d("Chunk-%d finished", worker.getChunk().getId());
        mModeratorExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mWorkers.remove(CHUNK_KEY_PREFIX + worker.getChunk().getId());
            }
        });
        synchronized (lock) {
            if (isRunning()) spawnWorker();
        }
    }

    @Override
    public void onChunkError(final ChunkWorker worker, final String reason, Throwable throwable) {
        Log.e(throwable, "Chunk-%d failed: %s", worker.getChunk().getId(), reason);
        // download chunk error ==> the task also error
        synchronized (lock) {
            if (isRunning()) {
                spawnWorker();
            }
            if (!isReleased()) {
                mModeratorExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        Task task = getTaskManager().findTask(worker.getChunk().getTaskId());
                        if (task != null && task.getState() != Task.State.FAILED) {
                            stopAllWorkerOfTaskSync(task);
                            getFileManager().deleteFile(task.getFilePath());
                            Task failedTask = getTaskManager().updateTask(task.newBuilder()
                                    .setState(Task.State.FAILED)
                                    .setMessage(reason).build());
                            updateTaskReport(failedTask, false);
                            mEventDispatcher.onTaskFailed(getTaskReport(failedTask));
                        }
                    }
                });
            }
        }
    }

    @Override
    public void onChunkInterrupted(final ChunkWorker worker) {
        mModeratorExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mWorkers.remove(CHUNK_KEY_PREFIX + worker.getChunk().getId());
            }
        });
        Log.d("Chunk-%d is interrupted", worker.getChunk().getId());
    }

    @Override
    public void onMergeFileFinished(final MergeFileWorker worker, final long fileLength, String checkSum) {
        final Task task = worker.getTask();
        Log.d("Merge task-%d is finished", task.getId());
        synchronized (lock) {
            if (isRunning()) spawnWorker();
            if (!isReleased()) {
                Task finishedTask = getTaskManager().updateTask(task.newBuilder()
                        .setLength(fileLength)
                        .setState(Task.State.FINISHED)
                        .setCheckSum(task.getCheckSumAlgorithm(), checkSum)
                        .setMessage("Successful").build());
                updateTaskReport(finishedTask, false);
                mEventDispatcher.onTaskFinished(getTaskReport(finishedTask));
                mModeratorExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        mWorkers.remove(MERGE_KEY_PREFIX + worker.getTask().getId());
                        List<Chunk> chunks = getTaskManager().getChunksOfTask(task);
                        for (Chunk chunk : chunks) {
                            getFileManager().deleteFile(chunk.getChunkFile());
                        }
                    }
                });
            }
        }
    }

    @Override
    public void onMergeFileError(final MergeFileWorker worker, final String reason, Throwable error) {
        final Task task = worker.getTask();
        Log.e(error, "Merge task-%d failed: %s", task.getId(), reason);
        mModeratorExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mWorkers.remove(MERGE_KEY_PREFIX + worker.getTask().getId());
            }
        });
        synchronized (lock) {
            if (isRunning()) spawnWorker();
            if (!isReleased()) {
                mModeratorExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        stopAllWorkerOfTaskSync(task);
                        getFileManager().deleteFile(task.getFilePath());
                        Task failedTask = getTaskManager().updateTask(task.newBuilder()
                                .setState(Task.State.FAILED)
                                .setMessage(reason).build());
                        updateTaskReport(failedTask, false);
                        mEventDispatcher.onTaskFailed(getTaskReport(failedTask));
                    }
                });
            }
        }
    }

    @Override
    public void onMergeFileInterrupted(final MergeFileWorker worker) {
        Log.d("Merge file interrupted (task-%d)", worker.getTask().getId());
        mModeratorExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mWorkers.remove(MERGE_KEY_PREFIX + worker.getTask().getId());
            }
        });
    }

    @Override
    public void onCheckSumFailed(final MergeFileWorker worker, final String algorithm, String expect, String found) {
        final Task task = worker.getTask();
        Log.e("task-%d onCheckSumFailed (%s) [%s] [%s]", task.getId(), algorithm, expect, found);
        mModeratorExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mWorkers.remove(MERGE_KEY_PREFIX + worker.getTask().getId());
            }
        });
        final boolean shouldDeleteFile = onChecksumMismatch(task, algorithm, expect, found);
        synchronized (lock) {
            if (isRunning()) spawnWorker();
            if (!isReleased()) {
                mModeratorExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        stopAllWorkerOfTaskSync(task);
                        if (shouldDeleteFile) getFileManager().deleteFile(task.getFilePath());
                        Task failedTask = getTaskManager().updateTask(task.newBuilder()
                                .setState(Task.State.FAILED)
                                .setMessage(algorithm + " checksum mismatch").build());
                        updateTaskReport(failedTask, false);
                        mEventDispatcher.onTaskFailed(getTaskReport(failedTask));
                    }
                });
            }
        }
    }

    @Override
    public boolean onChecksumMismatch(Task task, String algorithm, String expected, String found) {
        if (mOnChecksumMismatchListener != null)
            return mOnChecksumMismatchListener.onChecksumMismatch(task, algorithm, expected, found);
        return false;
    }
}
