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

import org.hkijena.jipipe.api.shortcuts.JIPipeChordStateMachine.EventResult;
import org.hkijena.jipipe.api.shortcuts.JIPipeChordStateMachine.KeyEventResult;

import javax.swing.KeyStroke;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JIPipeChordStateMachineTest {
    private static final KeyStroke CTRL_T = KeyStroke.getKeyStroke("control pressed T");
    private static final KeyStroke D = KeyStroke.getKeyStroke("pressed D");
    private static final KeyStroke X = KeyStroke.getKeyStroke("pressed X");
    private static final KeyStroke ESC = KeyStroke.getKeyStroke("pressed ESCAPE");
    private static final KeyStroke SHIFT = KeyStroke.getKeyStroke("shift pressed SHIFT");
    private static final KeyStroke SHIFT_P = KeyStroke.getKeyStroke("shift pressed P");

    @Test void prefixStartsAndCompletes() {
        JIPipeChordStateMachine m = new JIPipeChordStateMachine();
        m.configure(2_000_000_000L, Set.of(List.of(CTRL_T)));
        assertEquals(EventResult.PREFIX_STARTED, m.keyPressed(CTRL_T, 0).result());
        assertTrue(m.isPending());
        KeyEventResult r = m.keyPressed(D, 100);
        assertEquals(EventResult.CHORD_COMPLETED, r.result());
        assertEquals(List.of(CTRL_T, D), r.completedChord());
    }

    @Test void nonMatchingSecondKeyStillCompletesForCallerToCancel() {
        // configure prefix Ctrl+T; press Ctrl+T, then X -> CHORD_COMPLETED([Ctrl+T, X]) (swallowed; dispatcher cancels)
        JIPipeChordStateMachine m = new JIPipeChordStateMachine();
        m.configure(2_000_000_000L, Set.of(List.of(CTRL_T)));
        assertEquals(EventResult.PREFIX_STARTED, m.keyPressed(CTRL_T, 0).result());
        KeyEventResult r = m.keyPressed(X, 100);
        assertEquals(EventResult.CHORD_COMPLETED, r.result());
        assertEquals(List.of(CTRL_T, X), r.completedChord());
        assertFalse(m.isPending());
    }

    @Test void escapeCancelsPending() {
        JIPipeChordStateMachine m = new JIPipeChordStateMachine();
        m.configure(2_000_000_000L, Set.of(List.of(CTRL_T)));
        assertEquals(EventResult.PREFIX_STARTED, m.keyPressed(CTRL_T, 0).result());
        KeyEventResult r = m.keyPressed(ESC, 100);
        assertEquals(EventResult.CANCELLED, r.result());
        assertFalse(m.isPending());
    }

    @Test void timeoutExpiresThenStrokeIsEvaluatedAsIdle() {
        // configure timeout 100ns; press Ctrl+T at 0 (PREFIX_STARTED); press D at 1_000_000 -> PASSTHROUGH (D not a prefix)
        JIPipeChordStateMachine m = new JIPipeChordStateMachine();
        m.configure(100L, Set.of(List.of(CTRL_T)));
        assertEquals(EventResult.PREFIX_STARTED, m.keyPressed(CTRL_T, 0).result());
        KeyEventResult r = m.keyPressed(D, 1_000_000);
        assertEquals(EventResult.PASSTHROUGH, r.result());
        assertNull(r.completedChord());
        assertFalse(m.isPending());
    }

    @Test void zeroTimeoutWaitsIndefinitely() {
        JIPipeChordStateMachine m = new JIPipeChordStateMachine();
        m.configure(0L, Set.of(List.of(CTRL_T)));
        assertEquals(EventResult.PREFIX_STARTED, m.keyPressed(CTRL_T, 0).result());
        KeyEventResult r = m.keyPressed(D, 10_000_000_000L);
        assertEquals(EventResult.CHORD_COMPLETED, r.result());
        assertEquals(List.of(CTRL_T, D), r.completedChord());
    }

    @Test void prefixAloneIsNeverAnAction() {
        // Ctrl+T pressed twice: first PREFIX_STARTED, second CHORD_COMPLETED([Ctrl+T, Ctrl+T]) - the dispatcher finds no action and cancels
        JIPipeChordStateMachine m = new JIPipeChordStateMachine();
        m.configure(2_000_000_000L, Set.of(List.of(CTRL_T)));
        assertEquals(EventResult.PREFIX_STARTED, m.keyPressed(CTRL_T, 0).result());
        KeyEventResult r = m.keyPressed(CTRL_T, 100);
        assertEquals(EventResult.CHORD_COMPLETED, r.result());
        assertEquals(List.of(CTRL_T, CTRL_T), r.completedChord());
    }

    @Test void pendingModifierOnlyPressKeepsChordPending() {
        // configure prefix Ctrl+T; press Ctrl+T -> PREFIX_STARTED; a naked Shift press
        // (the modifier keyboards emit before the letter) -> PASSTHROUGH with the chord
        // still pending; then Shift+P -> CHORD_COMPLETED([Ctrl+T, Shift+P]) (a shifted
        // second key of the keymap)
        JIPipeChordStateMachine m = new JIPipeChordStateMachine();
        m.configure(2_000_000_000L, Set.of(List.of(CTRL_T)));
        assertEquals(EventResult.PREFIX_STARTED, m.keyPressed(CTRL_T, 0).result());
        KeyEventResult r = m.keyPressed(SHIFT, 100);
        assertEquals(EventResult.PASSTHROUGH, r.result());
        assertNull(r.completedChord());
        assertTrue(m.isPending());
        KeyEventResult completed = m.keyPressed(SHIFT_P, 200);
        assertEquals(EventResult.CHORD_COMPLETED, completed.result());
        assertEquals(List.of(CTRL_T, SHIFT_P), completed.completedChord());
    }

    @Test void idleModifierOnlyPressNeverStartsOrCancelsAChord() {
        // A naked Shift press while idle passes through without arming a chord
        // (and without touching a pending one - pinned because the modifier
        // exemption deliberately wins over the prefix lookup)
        JIPipeChordStateMachine m = new JIPipeChordStateMachine();
        m.configure(2_000_000_000L, Set.of(List.of(CTRL_T)));
        KeyEventResult r = m.keyPressed(SHIFT, 0);
        assertEquals(EventResult.PASSTHROUGH, r.result());
        assertFalse(m.isPending());
        // the chord is still startable afterwards
        assertEquals(EventResult.PREFIX_STARTED, m.keyPressed(CTRL_T, 100).result());
    }
}
