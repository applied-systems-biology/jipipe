package org.hkijena.jipipe.api.run;

import java.util.Queue;
import java.util.function.Predicate;

public interface JIPipeRunnableExecutor {

    JIPipeRunnableWorker enqueue(JIPipeRunnable run);

    void cancelAll();

    void cancel(JIPipeRunnable run);

    void cancelIf(Predicate<JIPipeRunnable> predicate);

    boolean isEmpty();

    int size();

    boolean isRunningOrEnqueued(JIPipeRunnable runnable);

    JIPipeRunnable getCurrentRun();

    JIPipeRunnableWorker getCurrentRunWorker();

    JIPipeRunnableWorker findWorkerOf(JIPipeRunnable run);

    Queue<JIPipeRunnableWorker> getQueue();

    String getName();

    boolean isSilent();

    void setSilent(boolean silent);

    JIPipeRunnable.EnqueuedEventEmitter getEnqueuedEventEmitter();

    JIPipeRunnable.FinishedEventEmitter getFinishedEventEmitter();

    JIPipeRunnable.InterruptedEventEmitter getInterruptedEventEmitter();

    JIPipeRunnable.ProgressEventEmitter getProgressEventEmitter();

    JIPipeRunnable.StartedEventEmitter getStartedEventEmitter();
}
