package io.github.khangnt.downloader.model;

import io.github.khangnt.downloader.FileManager;

/**
 * Created by Khang NT on 6/2/17.
 * Email: khang.neon.1997@gmail.com
 */

public class ChunkReport {
    private Chunk mChunk;
    private String mChunkFile;
    private FileManager mFileManager;

    public ChunkReport(Chunk chunk, String chunkFile, FileManager fileManager) {
        this.mChunk = chunk;
        this.mFileManager = fileManager;
    }

    public Chunk getChunk() {
        return mChunk;
    }

    public String getCHunkFile() {
        return mChunkFile;
    }

    public long getDownloadedLength() {
        if (mChunk.isFinished() && mChunk.isResumable()) {
            return mChunk.getEnd() - mChunk.getBegin();
        } else {
            return mFileManager.getFileSize(mChunkFile);
        }
    }
}
