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

import javax.swing.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class StaticDebouncer implements Disposable {
    private final Timer timer;
    private final Runnable runnable;
    private final AtomicLong lastFireTime = new AtomicLong(System.currentTimeMillis());
    private final long intervalMillis;

    public StaticDebouncer(long delay, TimeUnit unit, Runnable runnable) {
        this.runnable = runnable;
        this.intervalMillis = unit.toMillis(delay);
        timer = new Timer((int) unit.convert(delay, TimeUnit.MILLISECONDS), e -> doRun());
        timer.setRepeats(false);
    }

    private void doRun() {
        lastFireTime.set(System.currentTimeMillis());
        runnable.run();
    }

    public StaticDebouncer(long delay, Runnable runnable) {
        this(delay, TimeUnit.MILLISECONDS, runnable);
    }

    public Runnable getRunnable() {
        return runnable;
    }

    public void debounce() {
        long timeDiff = System.currentTimeMillis() - lastFireTime.get();
        if(timeDiff > intervalMillis) {
            doRun();
        }
        else {
            timer.restart();
        }
    }
}
