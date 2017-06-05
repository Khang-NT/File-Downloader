package io.github.khangnt.downloader.worker;

import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.khangnt.downloader.FileManager;
import io.github.khangnt.downloader.model.Chunk;
import io.github.khangnt.downloader.model.Task;

import static io.github.khangnt.downloader.util.Utils.checkInterrupted;

/**
 * Created by Khang NT on 6/4/17.
 * Email: khang.neon.1997@gmail.com
 */

public class MergeFileWorker extends Thread implements MergeFileWorkerListener {

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
        Collections.sort(mChunkList, (c1, c2) -> Long.compare(c1.getBegin(), c2.getBegin()));
    }

    public Task getTask() {
        return mTask;
    }

    @Override
    public void run() {
        FileChannel os = null;
        try {
            os = mFileManager.openWritableFile(mTask.getFilePath(), false).getChannel();
            for (Chunk chunk : mChunkList) {
                checkInterrupted();
                String chunkFile = mFileManager.getChunkFile(mTask, chunk.getId());
                try {
                    FileChannel is = mFileManager.openReadableFile(chunkFile).getChannel();
                    try {
                        is.transferTo(0, chunk.isResumable() ?
                                chunk.getLength() : mFileManager.getFileSize(chunkFile), os);
                    } finally {
                        try {
                            is.close();
                        } catch (Exception ignore) {
                        }
                    }
                } catch (IOException ex) {
                    onMergeFileError(this, "Can't merge chunk-" + chunk.getId() +
                            " to destination file: " + ex.getMessage(), ex);
                    return;
                }
            }
        } catch (InterruptedException | ClosedByInterruptException ex) {
            onMergeFileInterrupted(this);
            return;
        } catch (IOException e) {
            onMergeFileError(this, "Can't create/write destination file: " + e.getMessage(), e);
            return;
        } finally {
            try {
                if (os != null) os.close();
            } catch (Exception ignore) {
            }
        }

        // merge successful
        if (mTask.isResumable()) {
            // test
            if (mTask.getLength() != mFileManager.getFileSize(mTask.getFilePath()))
                throw new RuntimeException("File size mismatch");
        }
        onMergeFileFinished(this);
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
