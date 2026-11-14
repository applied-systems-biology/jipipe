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

package org.hkijena.jipipe.api.shortcuts;

import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Tracks the pending first key of a two-key chord (a "Microsoft-style"
 * multi-keybind, e.g. {@code Ctrl+T} then {@code M}).
 * <p>
 * This is a pure state machine with no Swing event sources and no action
 * resolution: a desktop dispatcher normalizes each {@code KEY_PRESSED} event
 * to a {@link KeyStroke} and feeds it through {@link #keyPressed(KeyStroke, long)}.
 * The machine only decides how the stroke interacts with a possibly pending
 * chord prefix:
 * <ul>
 * <li>idle + stroke that starts a chord -> {@link EventResult#PREFIX_STARTED}</li>
 * <li>idle + any other stroke -> {@link EventResult#PASSTHROUGH}</li>
 * <li>pending + {@code ESCAPE} without modifiers -> {@link EventResult#CANCELLED}</li>
 * <li>a modifier-only press (a bare Shift, Ctrl, Alt, Meta or Alt Graph)
 * -> {@link EventResult#PASSTHROUGH}: it never interacts with the chord
 * state, so a pending chord stays pending. Keyboards emit the modifier
 * press before the letter, and the exemption also applies while idle (a
 * modifier-only stroke never starts a chord: the capture dialogs never
 * record one)</li>
 * <li>pending + any other stroke -> {@link EventResult#CHORD_COMPLETED}, carrying
 * the completed chord. The stroke is <em>not</em> matched against the known
 * chords: the dispatcher resolves the chord against the registry and cancels
 * when it finds no action.</li>
 * <li>pending + timeout expired -> the pending chord is forgotten and the
 * stroke is evaluated as if idle</li>
 * </ul>
 * A prefix alone can never be an action. Every result except
 * {@link EventResult#PASSTHROUGH} means the caller must consume the event.
 * <p>
 * Not thread-safe; drive it from a single thread (the event dispatch thread).
 */
public final class JIPipeChordStateMachine {
    /**
     * How a key press interacts with the pending chord state.
     */
    public enum EventResult {
        /** The stroke is not part of a chord; the caller may handle or pass it on normally. */
        PASSTHROUGH,
        /** The stroke started a chord; the caller must consume the event. */
        PREFIX_STARTED,
        /** A second stroke completed a chord; the caller must consume the event
         * and resolve the chord against the registry. */
        CHORD_COMPLETED,
        /** The pending chord was cancelled (escape); the caller must consume the event. */
        CANCELLED
    }

    /**
     * The outcome of one key press: the {@link EventResult} plus, for
     * {@link EventResult#CHORD_COMPLETED}, the completed chord (prefix stroke
     * followed by the new stroke). The chord is null for every other result.
     */
    public record KeyEventResult(EventResult result, List<KeyStroke> completedChord) {
        public KeyEventResult {
            Objects.requireNonNull(result, "result");
        }
    }

    private long timeoutNanos;
    private Set<List<KeyStroke>> prefixes = Set.of();
    private boolean pending;
    private long pendingSince;
    private List<KeyStroke> pendingPrefix;

    /**
     * Installs the chord configuration and cancels any pending chord.
     *
     * @param timeoutNanos maximum time to wait for the second stroke, or 0 to
     * wait indefinitely
     * @param prefixes the first strokes of all length-2 bindings, each
     * wrapped as a one-element list; a stroke that matches one of them starts
     * a chord
     */
    public void configure(long timeoutNanos, Set<List<KeyStroke>> prefixes) {
        if (timeoutNanos < 0) {
            throw new IllegalArgumentException("timeoutNanos must be >= 0: " + timeoutNanos);
        }
        this.timeoutNanos = timeoutNanos;
        this.prefixes = Objects.requireNonNull(prefixes, "prefixes");
        cancel();
    }

    /**
     * Feeds one normalized key press into the machine.
     *
     * @param stroke the key press, already normalized by the caller
     * @param nanoTime a monotonic timestamp, e.g. {@link System#nanoTime()},
     * used for timeout expiry
     * @return the outcome of the press
     */
    public KeyEventResult keyPressed(KeyStroke stroke, long nanoTime) {
        Objects.requireNonNull(stroke, "stroke");
        if (isModifierOnly(stroke)) {
            // A bare modifier press never completes a stroke: it neither
            // starts, completes nor cancels a chord. Returning PASSTHROUGH
            // keeps a pending chord armed: the dispatcher's single-stroke
            // lookup finds nothing bound for a bare modifier, the key
            // passes through to the app, and the next (possibly shifted)
            // press resolves the chord. Keyboards emit the modifier press
            // before the letter, so exempting it is what makes shifted
            // second keys (e.g. Ctrl+T then Shift+P) usable at all.
            return new KeyEventResult(EventResult.PASSTHROUGH, null);
        }
        if (pending && timeoutNanos > 0 && nanoTime - pendingSince > timeoutNanos) {
            // the chord expired before this stroke: forget it and act as if idle
            pending = false;
            pendingPrefix = null;
        }
        if (pending) {
            if (isEscape(stroke)) {
                pending = false;
                pendingPrefix = null;
                return new KeyEventResult(EventResult.CANCELLED, null);
            }
            List<KeyStroke> chord = List.of(pendingPrefix.get(0), stroke);
            pending = false;
            pendingPrefix = null;
            return new KeyEventResult(EventResult.CHORD_COMPLETED, chord);
        }
        if (prefixes.contains(List.of(stroke))) {
            pending = true;
            pendingSince = nanoTime;
            pendingPrefix = List.of(stroke);
            return new KeyEventResult(EventResult.PREFIX_STARTED, null);
        }
        return new KeyEventResult(EventResult.PASSTHROUGH, null);
    }

    /**
     * Cancels a pending chord, if any.
     */
    public void cancel() {
        pending = false;
        pendingPrefix = null;
    }

    /**
     * @return true when a chord prefix is currently waiting for its second stroke
     */
    public boolean isPending() {
        return pending;
    }

    /**
     * @return the pending prefix wrapped as a one-element list, or null when
     * no chord is pending
     */
    public List<KeyStroke> pendingPrefix() {
        return pending ? pendingPrefix : null;
    }

    private static boolean isEscape(KeyStroke stroke) {
        return stroke.getKeyCode() == KeyEvent.VK_ESCAPE
                && stroke.getModifiers() == 0;
    }

    /**
     * @param stroke a normalized key press
     * @return whether the stroke is a pure modifier press (Shift, Ctrl,
     *         Alt, Meta or Alt Graph alone; the key codes of the left/right
     *         variants are the same as the plain ones in AWT)
     */
    private static boolean isModifierOnly(KeyStroke stroke) {
        int keyCode = stroke.getKeyCode();
        return keyCode == KeyEvent.VK_SHIFT
                || keyCode == KeyEvent.VK_CONTROL
                || keyCode == KeyEvent.VK_ALT
                || keyCode == KeyEvent.VK_META
                || keyCode == KeyEvent.VK_ALT_GRAPH;
    }
}
