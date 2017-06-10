package io.github.khangnt.downloader.worker;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.github.khangnt.downloader.C;
import io.github.khangnt.downloader.DownloadSpeedMeter;
import io.github.khangnt.downloader.FileManager;
import io.github.khangnt.downloader.HttpClient;
import io.github.khangnt.downloader.Log;
import io.github.khangnt.downloader.TaskManager;
import io.github.khangnt.downloader.model.Chunk;
import io.github.khangnt.downloader.model.Task;

import static io.github.khangnt.downloader.util.Utils.checkInterrupted;
import static io.github.khangnt.downloader.util.Utils.isEmpty;

/**
 * Created by Khang NT on 6/4/17.
 * Email: khang.neon.1997@gmail.com
 */

public class ChunkWorker extends Thread implements ChunkWorkerListener {
    public final static int MAX_RETRY = 3;
    public final static int BUFFER_SIZE = 1024 * 4; // 4KB

    private final Object lock = new Object();
    private final HttpClient mHttpClient;
    private final TaskManager mTaskManager;
    private final FileManager mFileManager;
    private final ChunkWorkerListener mListener;
    private final DownloadSpeedMeter mDownloadSpeedMeter;

    private Chunk mChunk;

    public ChunkWorker(Chunk chunk, HttpClient httpClient, TaskManager taskManager,
                       FileManager fileManager, DownloadSpeedMeter downloadSpeedMeter,
                       ChunkWorkerListener listener) {
        this.mHttpClient = httpClient;
        this.mTaskManager = taskManager;
        this.mFileManager = fileManager;
        this.mListener = listener;
        this.mDownloadSpeedMeter = downloadSpeedMeter;

        this.mChunk = chunk;
    }

    public Chunk getChunk() {
        return mChunk;
    }

    /**
     * Get remaining bytes will be downloaded by this chunk worker.
     *
     * @return remaining bytes.
     */
    public long getRemainingBytes() {
        if (!mChunk.isResumable())
            throw new IllegalStateException("Unknown remaining bytes of non-resumable chunk");
        return mChunk.getLength() - mFileManager.getFileSize(mChunk.getChunkFile());
    }

    public boolean isResumable() {
        return mChunk.isResumable();
    }

    /**
     * Split a half remaining length to new chunks.
     *
     * @return null if can't split for any reason, otherwise return a {@link Chunk} inserted to {@link TaskManager}.
     */
    public Chunk splitChunk(Task task) {
        synchronized (lock) {
            if (!mChunk.isResumable())
                throw new IllegalStateException("Can't split chunk not resumable");
            long remainingBytes = getRemainingBytes();
            Log.d("Chunk length: %d, remaining bytes: %d", mChunk.getLength(), remainingBytes);
            if (remainingBytes >= C.MIN_CHUNK_LENGTH * 4) {
                long splitPoint = mChunk.getEnd() - remainingBytes / 2;
                Chunk.Builder newChunkBuilder = new Chunk.Builder(mChunk.getTaskId(),
                        mFileManager.getUniqueTempFile(task))
                        .setRange(splitPoint + 1, mChunk.getEnd());
                Chunk newChunk = mTaskManager.insertChunk(newChunkBuilder.build());
                mChunk = mTaskManager.updateChunk(mChunk.newBuilder()
                        .setRange(mChunk.getBegin(), splitPoint).build());
                return newChunk;
            }
            return null;
        }
    }

    @Override
    public void run() {
        if (mChunk.isFinished()) {
            onChunkFinished(this);
            return;
        }
        Throwable lastException = null;
        int retryTime = 0;
        while (retryTime < MAX_RETRY) {
            try {
                long downloaded = execute();
                // download successful
                Chunk.Builder builder = mChunk.newBuilder();
                builder.setFinished(true);
                if (!builder.isResumable()) {
                    // chunk is finished, update the range if it is unknown
                    builder.setRange(0, downloaded - 1);
                }
                mTaskManager.updateChunk(mChunk = builder.build());
                onChunkFinished(this);
                return;
            } catch (InterruptedIOException | InterruptedException ex) {
                onChunkInterrupted(this);
                return;
            } catch (Exception ex) {
                lastException = ex;
                retryTime++;
                Log.d(ex, "[Chunk-%d] Chunk download failed, retry %d", mChunk.getId(), retryTime);
            }
        }
        onChunkError(this, "Exceed max retry: " + lastException.getMessage(), lastException);
    }

    private long execute() throws Exception {
        long downloaded;
        String range;
        synchronized (lock) {
            downloaded = mFileManager.getFileSize(mChunk.getChunkFile());
            if (!mChunk.isResumable() && downloaded > 0) {
                Log.d("[Chunk-%d] Re-download chunk from the beginning", mChunk.getId());
                downloaded = 0;
            } else if (mChunk.isResumable() && downloaded >= mChunk.getLength()) {
                // download completed
                return downloaded;
            }

            range = null;
            if (mChunk.isResumable())
                range = String.format(Locale.US,
                        "bytes=%d-%d", mChunk.getBegin() + downloaded, mChunk.getEnd());
        }

        checkInterrupted();
        OutputStream os = openChunkFile(downloaded > 0);
        InputStream is = openConnection(range);
        try {
            downloaded = download(os, is, downloaded);
            Log.d("Chunk-%d: %d/%d", mChunk.getId(), downloaded, mChunk.getLength());
            return downloaded;
        } finally {
            try {
                os.close();
            } catch (Exception ignore) {
            }
            try {
                is.close();
            } catch (Exception ignore) {
            }
        }
    }

    private long download(OutputStream os, InputStream is, long downloaded) throws IOException, InterruptedException {
        int read;
        byte buffer[] = new byte[BUFFER_SIZE];
        while (checkInterrupted() && (read = is.read(buffer, 0, BUFFER_SIZE)) > 0) {
            synchronized (lock) {
                if (!mChunk.isResumable() || downloaded + read <= mChunk.getLength()) {
                    os.write(buffer, 0, read);
                    downloaded += read;
                } else {
                    read = (int) (mChunk.getLength() - downloaded);
                    if (read > 0) {
                        os.write(buffer, 0, read);
                        downloaded += read;
                    }
                }
                mDownloadSpeedMeter.onBytesDownloaded(read);
                if (downloaded >= mChunk.getLength()) break;
            }
        }
        return downloaded;
    }

    private OutputStream openChunkFile(boolean append) throws IOException {
        try {
            return mFileManager.openWritableFile(mChunk.getChunkFile(), append);
        } catch (IOException ex) {
            throw new IOException("Can't create/open chunk file", ex);
        }
    }

    private InputStream openConnection(String range) throws IOException {
        Map<String, String> headers = new HashMap<>();
        if (!isEmpty(range)) headers.put("Range", range);
        return mHttpClient.openConnection(mTaskManager.findTask(mChunk.getTaskId()), headers);
    }

    @Override
    public void onChunkFinished(ChunkWorker worker) {
        if (mListener != null) mListener.onChunkFinished(worker);
    }

    @Override
    public void onChunkError(ChunkWorker worker, String reason, Throwable throwable) {
        if (mListener != null) mListener.onChunkError(worker, reason, throwable);
    }

    @Override
    public void onChunkInterrupted(ChunkWorker worker) {
        if (mListener != null) mListener.onChunkInterrupted(worker);
    }
}
