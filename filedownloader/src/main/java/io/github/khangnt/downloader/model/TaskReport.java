package io.github.khangnt.downloader.model;

import java.util.Collections;
import java.util.List;

/**
 * Created by Khang NT on 6/2/17.
 * Email: khang.neon.1997@gmail.com
 */

public class TaskReport {
    private final Task mTask;
    private List<ChunkReport> mChunkReports;

    public TaskReport(Task task, List<ChunkReport> chunkReports) {
        this.mTask = task;
        this.mChunkReports = Collections.unmodifiableList(chunkReports);
    }

    public Task getTask() {
        return mTask;
    }

    public List<ChunkReport> getChunkReports() {
        return mChunkReports;
    }

    public float calculatePercentDownloaded() {
        switch (mTask.getState()) {
            case IDLE:
            case FAILED:
                return 0;
            case FINISHED:
            case MERGING:
                return 100;
            case DOWNLOADING:
            case WAITING:
            default:
                if (mTask.isResumable()) {
                    return calculateDownloadedLength() * 100f / mTask.getLength();
                } else {
                    // stream with unknown length, we can't calculate percent downloaded
                    return 0;
                }
        }
    }

    public long calculateDownloadedLength() {
        long length = 0;
        for (ChunkReport chunkReport : mChunkReports)
            length += chunkReport.getDownloadedLength();
        return length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskReport that = (TaskReport) o;

        if (!mTask.equals(that.mTask)) return false;
        return mChunkReports.equals(that.mChunkReports);

    }

    @Override
    public int hashCode() {
        int result = mTask.hashCode();
        result = 31 * result + mChunkReports.hashCode();
        return result;
    }
}
