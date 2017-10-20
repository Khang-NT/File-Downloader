package io.github.khangnt.downloader;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
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
            Iterator<ListenerWrapper> iterator = mListenerList.iterator();
            while (iterator.hasNext()) {
                ListenerWrapper listenerWrapper = iterator.next();
                if (listenerWrapper.mWeakRefListener.get() == listener
                        || listenerWrapper.mWeakRefListener.get() == null) {
                    iterator.remove();
                }
            }
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
                        EventListener listener = listenerWrapper.mWeakRefListener.get();
                        if (listener != null) {
                            listener.onTaskAdded(taskReport);
                        }
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
                        EventListener listener = listenerWrapper.mWeakRefListener.get();
                        if (listener != null) {
                            listener.onTaskUpdated(taskReport);
                        }
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
                        EventListener listener = listenerWrapper.mWeakRefListener.get();
                        if (listener != null) {
                            listener.onTaskCancelled(taskReport);
                        }
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
                        EventListener listener = listenerWrapper.mWeakRefListener.get();
                        if (listener != null) {
                            listener.onTaskFinished(taskReport);
                        }
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
                        EventListener listener = listenerWrapper.mWeakRefListener.get();
                        if (listener != null) {
                            listener.onTaskFailed(taskReport);
                        }
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
                        EventListener listener = listenerWrapper.mWeakRefListener.get();
                        if (listener != null) {
                            listener.onResumed();
                        }
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
                        EventListener listener = listenerWrapper.mWeakRefListener.get();
                        if (listener != null) {
                            listener.onPaused();
                        }
                    }
                });
            }
        }
    }

    private class ListenerWrapper {
        private Executor mExecutor;
        private WeakReference<EventListener> mWeakRefListener;

        public ListenerWrapper(Executor mExecutor, EventListener listener) {
            this.mExecutor = mExecutor;
            this.mWeakRefListener = new WeakReference<>(listener);
        }
    }
}
