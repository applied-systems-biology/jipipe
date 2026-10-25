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

package org.hkijena.jipipe.api.service;

/**
 * Immutable record representing a snapshot of memory status at a point in time.
 *
 * @param usedMemory JVM heap memory currently in use (bytes)
 * @param maxMemory  Maximum JVM heap size (bytes)
 * @param timestamp  Time when this status was captured (milliseconds since epoch)
 */
public record JIPipeMemoryStatus(
        long usedMemory,
        long maxMemory,
        long timestamp
) {
    /**
     * Creates a new memory status with the current timestamp.
     *
     * @param usedMemory JVM heap memory currently in use (bytes)
     * @param maxMemory  Maximum JVM heap size (bytes)
     */
    public JIPipeMemoryStatus(long usedMemory, long maxMemory) {
        this(usedMemory, maxMemory, System.currentTimeMillis());
    }

    /**
     * @return Used memory as percentage of maximum (0.0 to 100.0)
     */
    public double getUsagePercentage() {
        return maxMemory > 0 ? (double) usedMemory / maxMemory * 100.0 : 0.0;
    }

    /**
     * Checks if the total usage exceeds the given threshold percentage.
     *
     * @param threshold the threshold percentage (0.0 to 100.0)
     * @return True if total usage percentage exceeds the threshold
     */
    public boolean isWarning(double threshold) {
        return getUsagePercentage() >= threshold;
    }
}
