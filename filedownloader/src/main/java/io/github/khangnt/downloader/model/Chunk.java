package io.github.khangnt.downloader.model;

import static io.github.khangnt.downloader.C.UNSET;

/**
 * Created by Khang NT on 6/2/17.
 * Email: khang.neon.1997@gmail.com
 */
public class Chunk {

    private int mId = UNSET;
    private int mTaskId;
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

    public boolean isFinished() {
        return mFinished;
    }

    public Builder newBuilder() {
        return new Builder(getTaskId())
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
        return result;
    }

    public static class Builder {
        private Chunk mChunk = new Chunk();

        public Builder(int taskId) {
            mChunk.mTaskId = taskId;
        }

        public Builder setId(int id) {
            mChunk.mId = id;
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

        public boolean isFinished() {
            return mChunk.mFinished;
        }

        public Chunk build() {
            return mChunk;
        }
    }
}
