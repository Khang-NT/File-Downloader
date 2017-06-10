package io.github.khangnt.downloader.model;

import io.github.khangnt.downloader.FileManager;

/**
 * Created by Khang NT on 6/2/17.
 * Email: khang.neon.1997@gmail.com
 */

public class ChunkReport {
    private Chunk mChunk;
    private FileManager mFileManager;

    public ChunkReport(Chunk chunk, FileManager fileManager) {
        this.mChunk = chunk;
        this.mFileManager = fileManager;
    }

    public Chunk getChunk() {
        return mChunk;
    }

    public String getChunkFile() {
        return mChunk.getChunkFile();
    }

    public long getDownloadedLength() {
        if (mChunk.isFinished() && mChunk.isResumable()) {
            return mChunk.getEnd() - mChunk.getBegin();
        } else {
            return mFileManager.getFileSize(getChunkFile());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChunkReport that = (ChunkReport) o;

        return mChunk.equals(that.mChunk);

    }

    @Override
    public int hashCode() {
        return mChunk.hashCode();
    }
}
