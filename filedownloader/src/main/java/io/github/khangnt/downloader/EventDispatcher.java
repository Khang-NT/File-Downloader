package io.github.khangnt.downloader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import io.github.khangnt.downloader.model.TaskReport;

/**
 * Created by Khang NT on 6/3/17.
 * Email: khang.neon.1997@gmail.com
 */

class EventDispatcher implements EventListener {

    private final List<ListenerWrapper> mListenerList = new ArrayList<ListenerWrapper>();

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
    public void onTaskAdded(final TaskReport taskReport) {
        synchronized (mListenerList) {
            for (final ListenerWrapper listenerWrapper : mListenerList) {
                listenerWrapper.mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        listenerWrapper.mListener.onTaskAdded(taskReport);
                    }
                });
            }
        }
    }

    @Override
    public void onTaskUpdated(final TaskReport taskReport) {
        synchronized (mListenerList) {
            for (final ListenerWrapper listenerWrapper : mListenerList) {
                listenerWrapper.mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        listenerWrapper.mListener.onTaskUpdated(taskReport);
                    }
                });
            }
        }
    }

    @Override
    public void onTaskCancelled(final TaskReport taskReport) {
        synchronized (mListenerList) {
            for (final ListenerWrapper listenerWrapper : mListenerList) {
                listenerWrapper.mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        listenerWrapper.mListener.onTaskCancelled(taskReport);
                    }
                });
            }
        }
    }

    @Override
    public void onTaskFinished(final TaskReport taskReport) {
        synchronized (mListenerList) {
            for (final ListenerWrapper listenerWrapper : mListenerList) {
                listenerWrapper.mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        listenerWrapper.mListener.onTaskFinished(taskReport);
                    }
                });
            }
        }
    }

    @Override
    public void onTaskFailed(final TaskReport taskReport) {
        synchronized (mListenerList) {
            for (final ListenerWrapper listenerWrapper : mListenerList) {
                listenerWrapper.mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        listenerWrapper.mListener.onTaskFailed(taskReport);
                    }
                });
            }
        }
    }

    @Override
    public void onResumed() {
        synchronized (mListenerList) {
            for (final ListenerWrapper listenerWrapper : mListenerList) {
                listenerWrapper.mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        listenerWrapper.mListener.onResumed();
                    }
                });
            }
        }
    }

    @Override
    public void onPaused() {
        synchronized (mListenerList) {
            for (final ListenerWrapper listenerWrapper : mListenerList) {
                listenerWrapper.mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        listenerWrapper.mListener.onPaused();
                    }
                });
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
