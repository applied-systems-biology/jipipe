/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.utils.debounce;

import org.scijava.Disposable;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ConsumingDynamicDebouncer<T> implements Disposable {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final long delay;
    private final TimeUnit unit;
    private ScheduledFuture<?> future;

    public ConsumingDynamicDebouncer(long delay, TimeUnit unit) {
        this.delay = delay;
        this.unit = unit;
    }

    public ConsumingDynamicDebouncer(long delay) {
        this(delay, TimeUnit.MILLISECONDS);
    }

    public void debounce(T arg, Consumer<T> listener) {
        if (future != null) {
            future.cancel(false);
        }
        future = scheduler.schedule(() -> listener.accept(arg), delay, unit);
    }

    public void shutdown() {
        scheduler.shutdown();
    }

    @Override
    public void dispose() {
        shutdown();
    }
}
