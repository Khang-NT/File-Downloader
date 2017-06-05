package io.github.khangnt.downloader;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Khang NT on 6/3/17.
 * Email: khang.neon.1997@gmail.com
 */

public class DownloadSpeedMeter {
    private static final long INTERVAL = 250;

    private final long[] bucket = new long[]{0, 0, 0, 0};
    private Timer mTimer;
    private int i = 0;
    private boolean paused = true;

    public void start() {
        synchronized (bucket) {
            bucket[0] = bucket[1] = bucket[2] = bucket[3] = 0;
            if (mTimer != null) {
                mTimer.cancel();
                mTimer.purge();
            }
            mTimer = new Timer(true);
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    update();
                }
            }, INTERVAL, INTERVAL);
            paused = false;
        }
    }

    public void pause() {
        synchronized (bucket) {
            if (mTimer != null) {
                mTimer.cancel();
                mTimer.purge();
            }
            mTimer = null;
            paused = true;
        }
    }

    public void release() {
        pause();
    }

    public void onBytesDownloaded(long count) {
        bucket[i] += count;
    }

    /**
     * Get speed as "bytes downloaded per second", the value is updated after {@link #INTERVAL}.
     *
     * @return number of bytes downloaded per second.
     */
    public long getSpeed() {
        return bucket[0] + bucket[1] + bucket[2] + bucket[3];
    }

    private void update() {
        i = (i + 1) % 4;
        bucket[i] = 0;
    }
}
