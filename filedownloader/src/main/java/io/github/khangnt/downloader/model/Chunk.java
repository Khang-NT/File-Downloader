package io.github.khangnt.downloader.model;

import io.github.khangnt.downloader.C;

import static io.github.khangnt.downloader.C.UNSET;

/**
 * Created by Khang NT on 6/2/17.
 * Email: khang.neon.1997@gmail.com
 */
public class Chunk {

    private int mId = C.UNSET;
    private int mTaskId;
    private String mChunkFile;
    private boolean mFinished = false;
    private boolean mResumable = false;
    private long mBegin = UNSET;
    private long mEnd = UNSET;

    private Chunk() {}

    public int getId() {
        return mId;
    }

    public int getTaskId() {
        return mTaskId;
    }

    public boolean isResumable() {
        return mResumable;
    }

    public long getBegin() {
        return mBegin;
    }

    public long getEnd() {
        return mEnd;
    }

    public long getLength() {
        if (isResumable()) return getEnd() - getBegin() + 1;
        return 0;
    }

    public String getChunkFile() {
        return mChunkFile;
    }

    public boolean isFinished() {
        return mFinished;
    }

    public Builder newBuilder() {
        return new Builder(getTaskId(), getChunkFile())
                .setId(getId())
                .setRange(getBegin(), getEnd())
                .setFinished(isFinished());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Chunk chunk = (Chunk) o;

        if (mId != chunk.mId) return false;
        return mTaskId == chunk.mTaskId;

    }

    @Override
    public int hashCode() {
        int result = mId;
        result = 31 * result + mTaskId;
        result = 31 * result + mChunkFile.hashCode();
        result = 31 * result + (mFinished ? 1 : 0);
        result = 31 * result + (mResumable ? 1 : 0);
        result = 31 * result + (int) (mBegin ^ (mBegin >>> 32));
        result = 31 * result + (int) (mEnd ^ (mEnd >>> 32));
        return result;
    }

    public static class Builder {
        private Chunk mChunk = new Chunk();

        public Builder(int taskId, String chunkFile) {
            mChunk.mTaskId = taskId;
            mChunk.mChunkFile = chunkFile;
        }

        public Builder setId(int chunkId) {
            mChunk.mId = chunkId;
            return this;
        }

        public Builder setRange(long begin, long end) {
            if (begin == UNSET || end == UNSET) {
                mChunk.mBegin = mChunk.mEnd = UNSET;
                mChunk.mResumable = false;
                return this;
            } else if (end < begin || end < 0) {
                throw new IllegalArgumentException("Invalid range " + begin + ".." + end);
            }
            mChunk.mBegin = begin;
            mChunk.mEnd = end;
            mChunk.mResumable = true;
            return this;
        }

        public Builder setChunkFile(String chunkFile) {
            mChunk.mChunkFile = chunkFile;
            return this;
        }

        public Builder setFinished(boolean finished) {
            mChunk.mFinished = finished;
            return this;
        }

        public int getId() {
            return mChunk.mId;
        }

        public int getTaskId() {
            return mChunk.mTaskId;
        }

        public boolean isResumable() {
            return mChunk.mResumable;
        }

        public long getBegin() {
            return mChunk.mBegin;
        }

        public long getEnd() {
            return mChunk.mEnd;
        }

        public long getLength() {
            if (isResumable()) return getEnd() - getBegin() + 1;
            return 0;
        }

        public String getChunkFile() {
            return mChunk.mChunkFile;
        }

        public boolean isFinished() {
            return mChunk.mFinished;
        }

        public Chunk build() {
            return mChunk;
        }
    }
}
