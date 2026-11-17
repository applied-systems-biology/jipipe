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

import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;

import java.util.List;

/**
 * Emitted by {@link LLMActivityTracker} whenever a new activity entry is finalized
 * and added to the rolling history. Listeners (typically the status-bar graph)
 * use this to trigger a repaint.
 */
public class LLMActivityEvent extends AbstractJIPipeEvent {

    private final LLMActivityEntry entry;
    private final List<LLMActivityEntry> historySnapshot;

    /**
     * @param source          the emitting tracker
     * @param entry           the entry that was just added
     * @param historySnapshot an unmodifiable snapshot of the full rolling history at the time of emission
     */
    public LLMActivityEvent(LLMActivityTracker source, LLMActivityEntry entry,
                            List<LLMActivityEntry> historySnapshot) {
        super(source);
        this.entry = entry;
        this.historySnapshot = historySnapshot;
    }

    /**
     * @return the entry that was just added to the history
     */
    public LLMActivityEntry getEntry() {
        return entry;
    }

    /**
     * @return an unmodifiable snapshot of the full rolling history
     */
    public List<LLMActivityEntry> getHistorySnapshot() {
        return historySnapshot;
    }

    /**
     * @return the emitting tracker
     */
    @Override
    public LLMActivityTracker getSource() {
        return (LLMActivityTracker) super.getSource();
    }

    // ==================================================================
    // Emitter
    // ==================================================================

    public interface Listener {
        void onLLMActivity(LLMActivityEvent event);
    }

    public static class Emitter extends JIPipeEventEmitter<LLMActivityEvent, Listener> {
        @Override
        protected void call(Listener listener, LLMActivityEvent event) {
            listener.onLLMActivity(event);
        }
    }
}
