package io.github.khangnt.downloader.worker;

/**
 * Created by Khang NT on 6/4/17.
 * Email: khang.neon.1997@gmail.com
 */

public interface MergeFileWorkerListener {
    void onMergeFileFinished(MergeFileWorker worker, long fileLength);
    void onMergeFileError(MergeFileWorker worker, String reason, Throwable error);
    void onMergeFileInterrupted(MergeFileWorker worker);
}
