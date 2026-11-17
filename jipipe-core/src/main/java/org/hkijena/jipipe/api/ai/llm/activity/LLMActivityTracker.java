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

import org.hkijena.jipipe.api.events.JIPipeEventEmitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Maintains a rolling history of {@link LLMActivityEntry} instances representing
 * the E (embedding) / T (thinking) / R (response) / C (tool-call) timeline.
 * <p>
 * Producers (the embedding model service, the MCP dispatch) call
 * {@link #recordPhase(LLMActivityPhase, int, String)} to finalize an interval
 * and add it to the history, or {@link #beginPhase(LLMActivityPhase, String)} and
 * {@link #finalizeCurrentPhase()} to record an interval whose end time is known only
 * later. The tracker caps the history at {@link #MAX_HISTORY} entries, discarding
 * the oldest when full.
 * </p>
 * <p>
 * The tracker is thread-safe: all methods are synchronized. A
 * {@link LLMActivityEvent.Emitter} notifies listeners (the status-bar graph)
 * whenever a new entry is added.
 * </p>
 */
public class LLMActivityTracker {

    /**
     * Maximum number of entries kept in the rolling history.
     */
    public static final int MAX_HISTORY = 200;

    private final List<LLMActivityEntry> history = new ArrayList<>();
    private final LLMActivityEvent.Emitter eventEmitter = new LLMActivityEvent.Emitter();

    private LLMActivityPhase currentPhase = LLMActivityPhase.Idle;
    private long phaseStartMs = 0;
    private int accumulatedTokens = 0;
    private String currentDetail = null;

    /**
     * Number of requests currently waiting in a concurrency gate queue.
     * Can be incremented/decremented by a concurrency-limiting component so the
     * status-bar graph can display a queue-depth badge without modifying the
     * phase timeline. Unused in the default wiring.
     */
    private int queueDepth = 0;

    /**
     * Begins a new activity phase. If a different phase is already active, it
     * is finalized first.
     *
     * @param phase  the phase to begin
     * @param detail optional detail string (e.g. model ID, function name); may be null
     */
    public synchronized void beginPhase(LLMActivityPhase phase, String detail) {
        if (currentPhase != LLMActivityPhase.Idle && currentPhase != phase) {
            finalizeCurrentPhase();
        }
        currentPhase = phase;
        phaseStartMs = System.currentTimeMillis();
        accumulatedTokens = 0;
        currentDetail = detail;
    }

    /**
     * Accumulates token counts during the active phase (e.g. from streaming deltas).
     *
     * @param tokens the number of tokens to add
     */
    public synchronized void addTokens(int tokens) {
        accumulatedTokens += tokens;
    }

    /**
     * Finalizes the current phase and adds it to the history. If the current
     * phase is Idle, this is a no-op.
     */
    public synchronized void finalizeCurrentPhase() {
        if (currentPhase == LLMActivityPhase.Idle) {
            return;
        }
        long now = System.currentTimeMillis();
        LLMActivityEntry entry = new LLMActivityEntry(
                currentPhase, phaseStartMs, now, accumulatedTokens, currentDetail);
        addToHistory(entry);
        currentPhase = LLMActivityPhase.Idle;
        phaseStartMs = 0;
        accumulatedTokens = 0;
        currentDetail = null;
    }

    /**
     * Convenience method: begins a phase, optionally adds tokens, then
     * immediately finalizes it. Useful for non-streaming calls where we know
     * the phase and token count up front.
     *
     * @param phase  the phase
     * @param tokens the token count (0 if N/A)
     * @param detail optional detail; may be null
     */
    public synchronized void recordPhase(LLMActivityPhase phase, int tokens, String detail) {
        beginPhase(phase, detail);
        if (tokens > 0) {
            addTokens(tokens);
        }
        finalizeCurrentPhase();
    }

    /**
     * @return an unmodifiable snapshot of the rolling history (oldest first)
     */
    public synchronized List<LLMActivityEntry> getHistory() {
        return Collections.unmodifiableList(new ArrayList<>(history));
    }

    /**
     * @return the currently active phase (Idle if nothing is in progress)
     */
    public synchronized LLMActivityPhase getCurrentPhase() {
        return currentPhase;
    }

    /**
     * @return the wall-clock start time of the current phase, or 0 if idle
     */
    public synchronized long getPhaseStartMs() {
        return phaseStartMs;
    }

    /**
     * @return the token count accumulated during the current phase
     */
    public synchronized int getAccumulatedTokens() {
        return accumulatedTokens;
    }

    /**
     * @return the detail string of the current phase, or null
     */
    public synchronized String getCurrentDetail() {
        return currentDetail;
    }

    /**
     * Increments the concurrency-gate queue depth counter. Called by a
     * concurrency-limiting component when a request enters the queue.
     */
    public synchronized void incrementQueueDepth() {
        queueDepth++;
    }

    /**
     * Decrements the concurrency-gate queue depth counter (floored at 0).
     * Called by a concurrency-limiting component when a request leaves the queue.
     */
    public synchronized void decrementQueueDepth() {
        queueDepth = Math.max(0, queueDepth - 1);
    }

    /**
     * @return the number of requests currently waiting in the concurrency gate queue
     */
    public synchronized int getQueueDepth() {
        return queueDepth;
    }

    /**
     * Clears the entire history.
     */
    public synchronized void clear() {
        history.clear();
    }

    /**
     * @return the event emitter that notifies on new activity entries
     */
    public JIPipeEventEmitter<LLMActivityEvent, LLMActivityEvent.Listener> getEventEmitter() {
        return eventEmitter;
    }

    private void addToHistory(LLMActivityEntry entry) {
        history.add(entry);
        while (history.size() > MAX_HISTORY) {
            history.remove(0);
        }
        eventEmitter.emit(new LLMActivityEvent(this, entry, getHistory()));
    }
}
