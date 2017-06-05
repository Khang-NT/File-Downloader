package io.github.khangnt.downloader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import io.github.khangnt.downloader.model.Task;
import io.github.khangnt.downloader.model.TaskReport;

/**
 * Created by Khang NT on 6/3/17.
 * Email: khang.neon.1997@gmail.com
 */

class EventDispatcher implements EventListener {

    private final List<ListenerWrapper> mListenerList = new ArrayList<>();

    public void registerListener(Executor executor, EventListener listener) {
        synchronized (mListenerList) {
            mListenerList.add(new ListenerWrapper(executor, listener));
        }
    }

    public void unregisterListener(EventListener listener) {
        synchronized (mListenerList) {
            mListenerList.remove(listener);
        }
    }

    public void unregisterAllListener() {
        synchronized (mListenerList) {
            mListenerList.clear();
        }
    }

    @Override
    public void onTaskAdded(Task task) {
        synchronized (mListenerList) {
            for (ListenerWrapper listenerWrapper : mListenerList) {
                listenerWrapper.mExecutor.execute(() -> listenerWrapper.mListener.onTaskAdded(task));
            }
        }
    }

    @Override
    public void onTaskRemoved(Task task) {
        synchronized (mListenerList) {
            for (ListenerWrapper listenerWrapper : mListenerList) {
                listenerWrapper.mExecutor.execute(() -> listenerWrapper.mListener.onTaskRemoved(task));
            }
        }
    }

    @Override
    public void onTaskFinished(Task task) {
        synchronized (mListenerList) {
            for (ListenerWrapper listenerWrapper : mListenerList) {
                listenerWrapper.mExecutor.execute(() -> listenerWrapper.mListener.onTaskFinished(task));
            }
        }
    }

    @Override
    public void onQueueChanged(List<TaskReport> queue) {
        synchronized (mListenerList) {
            for (ListenerWrapper listenerWrapper : mListenerList) {
                listenerWrapper.mExecutor.execute(() -> listenerWrapper.mListener.onQueueChanged(queue));
            }
        }
    }

    @Override
    public void onResumed() {
        synchronized (mListenerList) {
            for (ListenerWrapper listenerWrapper : mListenerList) {
                listenerWrapper.mExecutor.execute(() -> listenerWrapper.mListener.onResumed());
            }
        }
    }

    @Override
    public void onPaused() {
        synchronized (mListenerList) {
            for (ListenerWrapper listenerWrapper : mListenerList) {
                listenerWrapper.mExecutor.execute(() -> listenerWrapper.mListener.onPaused());
            }
        }
    }

    private class ListenerWrapper {
        private Executor mExecutor;
        private EventListener mListener;

        public ListenerWrapper(Executor mExecutor, EventListener mListener) {
            this.mExecutor = mExecutor;
            this.mListener = mListener;
        }

        @Override
        public boolean equals(Object o) {
            return mListener == o;
        }
    }
}
