package io.github.khangnt.downloader.model;

/**
 * Created by Khang NT on 6/2/17.
 * Email: khang.neon.1997@gmail.com
 */
public class Chunk {
    public static final int UNSET = -1;
    public static final int NO_WHERE = -1;
    public static final long MIN_CHUNK_LENGTH = 250 * 1024L; // 250KB

    private int mId = UNSET;
    private int mTaskId;
    private boolean finished = false;
    private boolean mResumable = false;
    private long mBegin = NO_WHERE;
    private long mEnd = NO_WHERE;

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
        return finished;
    }

    public Builder newBuilder() {
        return new Builder(getTaskId())
                .setId(getId())
                .setRange(getBegin(), getEnd())
                .setFinished(isFinished());
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
            if (begin == NO_WHERE || end == NO_WHERE) {
                mChunk.mBegin = mChunk.mEnd = NO_WHERE;
                mChunk.mResumable = false;
                return this;
            } else if (end < begin) {
                throw new IllegalArgumentException("Invalid range " + begin + ".." + end);
            } else if (end - begin + 1 < MIN_CHUNK_LENGTH) {
                throw new IllegalArgumentException("Chunk size too small");
            }
            mChunk.mBegin = begin;
            mChunk.mEnd = end;
            mChunk.mResumable = true;
            return this;
        }

        public Builder setFinished(boolean finished) {
            mChunk.finished = finished;
            return this;
        }

        public Chunk build() {
            return mChunk;
        }
    }
}
