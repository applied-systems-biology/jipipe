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

package org.hkijena.jipipe.api;

import gnu.trove.queue.TLongQueue;
import gnu.trove.stack.TLongStack;
import gnu.trove.stack.array.TLongArrayStack;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Allows to provide users with an ETA
 */
public class JIPipeProgressInfoETA {
    private long lastLogTime = System.currentTimeMillis();
    private long lastUpdateTime = System.currentTimeMillis();
    private long elapsedTime = 0;
    private int lastItemIndex = -1;
    private int averageWindow = 5;
    private long logInterval = 60 * 1000;
    private final Queue<Long> updateTimes = new ArrayDeque<>();

    /**
     * Gets the window size for the ETA calculation
     * @return the window size
     */
    public int getAverageWindow() {
        return averageWindow;
    }

    /**
     * Sets the window size for the ETA calculation
     * @param averageWindow the window size
     */
    public void setAverageWindow(int averageWindow) {
        this.averageWindow = averageWindow;
    }

    /**
     * Gets the log interval (milliseconds)
     * @return the log interval (milliseconds)
     */
    public long getLogInterval() {
        return logInterval;
    }

    /**
     * Sets the log interval (milliseconds)
     * @param logInterval the log interval (milliseconds)
     */
    public void setLogInterval(long logInterval) {
        this.logInterval = logInterval;
    }

    /**
     * Updates the ETA and logs it if needed
     * @param currentIndex the current index
     * @param totalItems the total number of items
     * @param progressInfo the progress info
     */
    public void update(int currentIndex, int totalItems, JIPipeProgressInfo progressInfo) {
        try {
            if (currentIndex <= lastItemIndex || totalItems <= 0) {
                return;
            }

            final long now = System.currentTimeMillis();
            int numCompletedInStep = currentIndex - lastItemIndex;
            long stepDuration = now - lastUpdateTime;
            long iterationDuration = stepDuration / numCompletedInStep;
            elapsedTime += stepDuration;
            lastUpdateTime = now;
            lastItemIndex = currentIndex;

            // Push into window and remove old items
            updateTimes.add(iterationDuration);
            while (updateTimes.size() > averageWindow) {
                updateTimes.remove();
            }

            // Check if we need a log item
            if ((now - lastLogTime) < logInterval) {
                return;
            }

            lastLogTime = now;

            // Calculate average time based on index
            double averageTime = 0;
            for (long l : updateTimes) {
                averageTime += l;
            }
            averageTime /= updateTimes.size();

            // Calculate ETA and log it
            int numItemsToDo = Math.max(1, totalItems - 1 - lastItemIndex);
            double eta = numItemsToDo * averageTime;
            Duration etaDuration = Duration.ofMillis((long) eta);
            Duration elapsedDuration = Duration.ofMillis(elapsedTime);
            Duration averageDuration = Duration.ofMillis((long)averageTime);

            progressInfo.log("Elapsed: " + String.format("%02d:%02d:%02d", elapsedDuration.toHours(), elapsedDuration.toMinutes() % 60, elapsedDuration.getSeconds() % 60) +
                    ", ETA: " + String.format("%02d:%02d:%02d", etaDuration.toHours(), etaDuration.toMinutes() % 60, etaDuration.getSeconds() % 60) +
                    ", Average time per item: " + String.format("%02d:%02d:%02d", averageDuration.toHours(), averageDuration.toMinutes() % 60, averageDuration.getSeconds() % 60));
        }
        catch (Throwable e) {
            progressInfo.log(e);
        }

    }
}
