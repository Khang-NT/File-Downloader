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
    private final BlockingQueue<Runnable> runnableBlockingQueue = new LinkedBlockingQueue<>();
    private ThreadFactory mThreadFactory;
    private Thread moderatorThread;

    public ModeratorExecutor(ThreadFactory threadFactory) {
        mThreadFactory = threadFactory;
    }

    @Override
    public void execute(Runnable runnable) {
        synchronized (this) {
            runnableBlockingQueue.offer(runnable);
            if (moderatorThread == null) {
                moderatorThread = mThreadFactory.newThread(this);
                moderatorThread.start();
            }
            notify();
        }
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            synchronized (this) {
                if (runnableBlockingQueue.peek() == null) try {
                    wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
            Runnable runnable = runnableBlockingQueue.poll();
            if (runnable != null) runnable.run();
        }
    }

    public void interrupt() {
        synchronized (runnableBlockingQueue) {
            if (moderatorThread != null) {
                moderatorThread.interrupt();
                try {
                    moderatorThread.join();
                } catch (InterruptedException ignore) {
                }
            }
            moderatorThread = null;
        }
    }
}
