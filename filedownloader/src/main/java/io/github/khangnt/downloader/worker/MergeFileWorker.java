package io.github.khangnt.downloader.worker;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import io.github.khangnt.downloader.C;
import io.github.khangnt.downloader.FileManager;
import io.github.khangnt.downloader.model.Chunk;
import io.github.khangnt.downloader.model.Task;
import io.github.khangnt.downloader.util.Utils;

import static io.github.khangnt.downloader.util.Utils.checkInterrupted;

/**
 * Created by Khang NT on 6/4/17.
 * Email: khang.neon.1997@gmail.com
 */

public class MergeFileWorker extends Thread implements MergeFileWorkerListener {
    private static final int BUFFER_SIZE = 16 * 1014; // 16 KB

    private Task mTask;
    private List<Chunk> mChunkList;
    private FileManager mFileManager;
    private MergeFileWorkerListener mListener;

    public MergeFileWorker(Task task, List<Chunk> chunkList, FileManager fileManager,
                           MergeFileWorkerListener listener) {
        this.mTask = task;
        this.mFileManager = fileManager;
        this.mListener = listener;

        // sort chunks by begin position
        this.mChunkList = new ArrayList<>(chunkList);
        Collections.sort(mChunkList, new Comparator<Chunk>() {
            @Override
            public int compare(Chunk c1, Chunk c2) {
                return Utils.compare(c1.getBegin(), c2.getBegin());
            }
        });
    }

    public Task getTask() {
        return mTask;
    }

    @Override
    public void run() {
        OutputStream os = null;
        try {
            os = mFileManager.openWritableFile(mTask.getFilePath(), false);
            int len;
            byte buffer[] = new byte[BUFFER_SIZE];
            for (Chunk chunk : mChunkList) {
                checkInterrupted();
                String chunkFile = mFileManager.getChunkFile(mTask, chunk.getId());
                checkChunk(chunk, chunkFile);
                InputStream is = null;
                try {
                    is = mFileManager.openReadableFile(chunkFile);
                    while (checkInterrupted() && (len = is.read(buffer, 0, BUFFER_SIZE)) > 0) {
                        os.write(buffer, 0, len);
                    }
                } catch (IOException ex) {
                    onMergeFileError(this, "Can't concat chunks: " + ex.getMessage(), ex);
                    return;
                } finally {
                    try {
                        if (is != null) is.close();
                    } catch (Exception ignore) {
                    }
                }
            }
        } catch (InterruptedException ex) {
            onMergeFileInterrupted(this);
            return;
        } catch (Exception e) {
            onMergeFileError(this, "Can't concat chunks files: " + e.getMessage(), e);
            return;
        } finally {
            try {
                if (os != null) os.close();
            } catch (Exception ignore) {
            }
        }

        // merge successful
        onMergeFileFinished(this);
    }

    private void checkChunk(Chunk chunk, String chunkFile) {
        if (chunk.getEnd() == C.UNSET || chunk.getBegin() == C.UNSET) {
            throw new IllegalStateException("Chunk download range should be set after finished");
        }
        long fileSize = mFileManager.getFileSize(chunkFile);
        if (fileSize != chunk.getLength()) {
            throw new IllegalArgumentException(String.format(Locale.US,
                    "Chunk file size invalid, expect: %d but found %d", chunk.getLength(), fileSize));
        }
    }

    @Override
    public void onMergeFileFinished(MergeFileWorker worker) {
        if (mListener != null) mListener.onMergeFileFinished(worker);
    }

    @Override
    public void onMergeFileError(MergeFileWorker worker, String reason, Throwable error) {
        if (mListener != null) mListener.onMergeFileError(worker, reason, error);
    }

    @Override
    public void onMergeFileInterrupted(MergeFileWorker worker) {
        if (mListener != null) mListener.onMergeFileInterrupted(worker);
    }
}
