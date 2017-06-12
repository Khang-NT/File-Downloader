package io.github.khangnt.downloader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import io.github.khangnt.downloader.model.Task;

/**
 * Created by Khang NT on 6/2/17.
 * Email: khang.neon.1997@gmail.com
 */

public interface HttpClient {
    class ContentDescription {
        private long mLength = C.UNSET;
        private boolean mAcceptRange = false;

        public ContentDescription(long mLength, boolean mAcceptRange) {
            this.mLength = mLength;
            this.mAcceptRange = mAcceptRange;
        }

        public long getLength() {
            return mLength;
        }

        public boolean isAcceptRange() {
            return mAcceptRange;
        }
    }

    InputStream openConnection(Task task, Map<String, String> headers) throws IOException;
    ContentDescription fetchContentDescription(Task task);
}
