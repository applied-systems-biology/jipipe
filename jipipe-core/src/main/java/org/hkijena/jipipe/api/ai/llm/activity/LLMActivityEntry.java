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

package org.hkijena.jipipe.api.ai.llm.activity;

/**
 * An immutable snapshot of a single LLM activity phase that was active during
 * a time interval. The {@link LLMActivityTracker} maintains a rolling history
 * of these entries for the status-bar activity graph.
 *
 * @param phase            the activity phase
 * @param startTimestampMs wall-clock start time (System.currentTimeMillis)
 * @param endTimestampMs   wall-clock end time
 * @param tokenCount       number of tokens produced during this interval (0 if N/A)
 * @param detail           optional human-readable detail (e.g. model ID, function name); may be null
 */
public record LLMActivityEntry(
        LLMActivityPhase phase,
        long startTimestampMs,
        long endTimestampMs,
        int tokenCount,
        String detail
) {
    /**
     * @return duration of this phase in milliseconds
     */
    public long durationMs() {
        return endTimestampMs - startTimestampMs;
    }
}
