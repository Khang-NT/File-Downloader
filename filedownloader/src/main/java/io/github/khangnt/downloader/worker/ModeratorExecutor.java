package io.github.khangnt.downloader.worker;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

/**
 * Created by Khang NT on 6/4/17.
 * Email: khang.neon.1997@gmail.com
 */

public class ModeratorExecutor implements Executor, Runnable {
    private BlockingQueue<Runnable> mRunnableBlockingQueue;
    private ThreadFactory mThreadFactory;
    private Thread mModeratorThread;

    public ModeratorExecutor(ThreadFactory threadFactory) {
        mThreadFactory = threadFactory;
        mRunnableBlockingQueue = new LinkedBlockingQueue<>();
    }

    @Override
    public void execute(Runnable runnable) {
        synchronized (this) {
            mRunnableBlockingQueue.offer(runnable);
            if (mModeratorThread == null) {
                mModeratorThread = mThreadFactory.newThread(this);
                mModeratorThread.start();
            }
            notify();
        }
    }

    @Override
    public void run() {
        while (true) {
            synchronized (this) {
                if (mRunnableBlockingQueue.peek() == null) try {
                    wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
            Runnable runnable = mRunnableBlockingQueue.poll();
            if (runnable != null) runnable.run();
        }
    }

    public void executeAllPendingRunnable() {
        synchronized (this) {
            if (mModeratorThread != null) {
                mModeratorThread.interrupt();
                try {
                    mModeratorThread.join();
                } catch (InterruptedException ignore) {
                }
            }
            mModeratorThread = null;
        }
    }
}
