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
 * Represents the phase of an LLM activity at a point in time.
 * <p>
 * The phases correspond to the bars shown in the activity graph:
 * <ul>
 *   <li>{@link #Embedding}: an embedding operation is in progress</li>
 *   <li>{@link #Thinking}: the LLM is producing reasoning content (streamed or batch)</li>
 *   <li>{@link #Response}: the LLM is producing output content (streamed or batch)</li>
 *   <li>{@link #ToolCall}: a tool/function call is being dispatched</li>
 *   <li>{@link #Queued}: a request is waiting in the concurrency queue (blocked/waiting)</li>
 *   <li>{@link #Idle}: no activity</li>
 * </ul>
 * <p>
 * Each phase carries a <em>hue</em> (HSB hue, 0-1) rather than a concrete color,
 * so the desktop UI can reconstruct theme-appropriate colors from
 * {@code ThemeUtils.getCurrentStyle()} without hardcoding AWT colors in the API layer.
 */
public enum LLMActivityPhase {
    /**
     * An embedding operation is in progress.
     */
    Embedding(0.58f),   // blue

    /**
     * The LLM is producing reasoning content.
     */
    Thinking(0.08f),    // orange

    /**
     * The LLM is producing output content.
     * When streaming is not supported, a single R bar is shown for the entire wait.
     */
    Response(0.33f),    // green

    /**
     * A tool/function call is being dispatched by the agent loop.
     */
    ToolCall(0.83f),    // purple

    /**
     * A request is waiting in the concurrency queue (blocked / waiting for a permit).
     * Red hue signals "waiting / blocked", visually distinct from the green
     * "actively generating" Response phase.
     */
    Queued(0.0f),       // red: waiting / blocked

    /**
     * No activity: used as the default/fill state.
     */
    Idle(-1f);          // neutral / no hue

    private final float hue;

    LLMActivityPhase(float hue) {
        this.hue = hue;
    }

    /**
     * @return the HSB hue (0-1) for this phase, or {@code -1} for neutral/idle
     */
    public float getHue() {
        return hue;
    }

    /**
     * @return the single-character label (E, T, R, C, I) used in the graph legend
     */
    public String getShortLabel() {
        return name().substring(0, 1);
    }
}
