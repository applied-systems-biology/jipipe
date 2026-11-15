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

package org.hkijena.jipipe.desktop.app.grapheditor.commons;

import org.junit.jupiter.api.Test;

import javax.swing.KeyStroke;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the shared-chord tool cycle computation
 * ({@link JIPipeDesktopGraphEditorUI#computeToolCycleTarget}): design
 * Section 7 keeps cycling on a repeated press of a shared chord. A group of
 * tools that shares one effective chord (e.g. the LEGACY preset's ESC on the
 * default tool and the notes-edit tool) cycles on repeated presses; a solo
 * chord always selects its own tool.
 * <p>
 * The test drives the pure static helper directly: no editor, no registry,
 * no Swing interaction. The state map mirrors the editor's
 * {@code lastFiredToolIdByChord} (chord to last selected tool id), which
 * {@code selectTool} updates after every selection.
 */
class JIPipeDesktopToolChordCycleTest {
    private static final String DEFAULT_TOOL_ID = "org.hkijena.jipipe.test.tool.default";
    private static final String NOTES_EDIT_TOOL_ID = "org.hkijena.jipipe.test.tool.notes-edit";
    private static final String MOVE_TOOL_ID = "org.hkijena.jipipe.test.tool.move";

    /**
     * The ESC chord shared by the default tool and the notes-edit tool in
     * the LEGACY preset.
     */
    private static final List<List<KeyStroke>> ESC_CHORD =
            List.of(List.of(KeyStroke.getKeyStroke("pressed ESCAPE")));
    /**
     * The deterministic chord-group order for that chord (registry order:
     * "Default tool" sorts before "Notes: Edit").
     */
    private static final List<String> ESC_GROUP = List.of(DEFAULT_TOOL_ID, NOTES_EDIT_TOOL_ID);

    /**
     * The solo chord of the move tool (Ctrl+T then M in the new preset).
     */
    private static final List<List<KeyStroke>> MOVE_CHORD =
            List.of(List.of(KeyStroke.getKeyStroke("control pressed T"),
                    KeyStroke.getKeyStroke("pressed M")));

    /**
     * Simulates one press of the shared chord through the dispatcher's
     * candidate walk: the candidates are tried in the given order, the first
     * candidate whose cycle target is itself resolves and "fires" (its
     * selection records the cycle state, as {@code selectTool} does), the
     * others fall through. Returns the id that fired, or null when no
     * candidate resolved.
     */
    private static String firePress(List<String> candidates, List<String> group,
                                    List<List<KeyStroke>> chord,
                                    Map<List<List<KeyStroke>>, String> state,
                                    String currentToolId) {
        for (String candidate : candidates) {
            String target = JIPipeDesktopGraphEditorUI.computeToolCycleTarget(
                    candidate, chord, group, state, currentToolId);
            if (candidate.equals(target)) {
                state.put(chord, candidate);
                return candidate;
            }
        }
        return null;
    }

    @Test
    void sharedChordCyclesOnRepeatedPress() {
        // The LEGACY ESC group, registry-order candidates, cold cycle with
        // the default tool currently selected.
        Map<List<List<KeyStroke>>, String> state = new HashMap<>();
        String current = DEFAULT_TOOL_ID;

        // Press 1: cold cycle from the default tool selects the notes-edit
        // tool (the default tool's candidate falls through).
        assertEquals(NOTES_EDIT_TOOL_ID,
                firePress(ESC_GROUP, ESC_GROUP, ESC_CHORD, state, current));
        current = NOTES_EDIT_TOOL_ID;
        // Press 2: advances back to the default tool.
        assertEquals(DEFAULT_TOOL_ID,
                firePress(ESC_GROUP, ESC_GROUP, ESC_CHORD, state, current));
        current = DEFAULT_TOOL_ID;
        // Press 3: wraps around to the notes-edit tool again.
        assertEquals(NOTES_EDIT_TOOL_ID,
                firePress(ESC_GROUP, ESC_GROUP, ESC_CHORD, state, current));
        current = NOTES_EDIT_TOOL_ID;
        // Press 4: and back to the default tool.
        assertEquals(DEFAULT_TOOL_ID,
                firePress(ESC_GROUP, ESC_GROUP, ESC_CHORD, state, current));
    }

    @Test
    void coldCycleStartsFromCurrentlySelectedTool() {
        // Empty state (no recorded fire), the notes-edit tool currently
        // active: the next press of ESC must select the default tool,
        // whichever candidate the dispatcher tries.
        Map<List<List<KeyStroke>>, String> state = new HashMap<>();
        assertEquals(DEFAULT_TOOL_ID,
                JIPipeDesktopGraphEditorUI.computeToolCycleTarget(
                        DEFAULT_TOOL_ID, ESC_CHORD, ESC_GROUP, state, NOTES_EDIT_TOOL_ID));
        assertEquals(DEFAULT_TOOL_ID,
                JIPipeDesktopGraphEditorUI.computeToolCycleTarget(
                        NOTES_EDIT_TOOL_ID, ESC_CHORD, ESC_GROUP, state, NOTES_EDIT_TOOL_ID));
        // And with nothing selected (no tool active), the cold cycle starts
        // from the group's first tool and advances one step: both
        // candidates compute the same target (the notes-edit tool), so the
        // default tool's candidate falls through and the notes-edit
        // candidate fires.
        assertEquals(NOTES_EDIT_TOOL_ID,
                JIPipeDesktopGraphEditorUI.computeToolCycleTarget(
                        DEFAULT_TOOL_ID, ESC_CHORD, ESC_GROUP, new HashMap<>(), null));
        assertEquals(NOTES_EDIT_TOOL_ID,
                JIPipeDesktopGraphEditorUI.computeToolCycleTarget(
                        NOTES_EDIT_TOOL_ID, ESC_CHORD, ESC_GROUP, new HashMap<>(), null));
        assertEquals(NOTES_EDIT_TOOL_ID,
                firePress(ESC_GROUP, ESC_GROUP, ESC_CHORD, new HashMap<>(), null));
    }

    @Test
    void staleCycleStateFallsBackToColdCycle() {
        // The recorded fire names a tool that is no longer in the group
        // (e.g. the chord was re-bound and the group shrank): the state is
        // ignored and the cycle is cold again.
        Map<List<List<KeyStroke>>, String> state = new HashMap<>();
        state.put(ESC_CHORD, "org.hkijena.jipipe.test.tool.gone");
        assertEquals(NOTES_EDIT_TOOL_ID,
                JIPipeDesktopGraphEditorUI.computeToolCycleTarget(
                        DEFAULT_TOOL_ID, ESC_CHORD, ESC_GROUP, state, DEFAULT_TOOL_ID));
        assertEquals(NOTES_EDIT_TOOL_ID,
                JIPipeDesktopGraphEditorUI.computeToolCycleTarget(
                        NOTES_EDIT_TOOL_ID, ESC_CHORD, ESC_GROUP, state, DEFAULT_TOOL_ID));
    }

    @Test
    void soloChordAlwaysSelectsItsOwnTool() {
        List<String> soloGroup = List.of(MOVE_TOOL_ID);
        Map<List<List<KeyStroke>>, String> state = new HashMap<>();
        // Regardless of the cycle state, the current tool, or which
        // candidate is tried, a group of one selects itself.
        assertEquals(MOVE_TOOL_ID,
                JIPipeDesktopGraphEditorUI.computeToolCycleTarget(
                        MOVE_TOOL_ID, MOVE_CHORD, soloGroup, state, MOVE_TOOL_ID));
        assertEquals(MOVE_TOOL_ID,
                JIPipeDesktopGraphEditorUI.computeToolCycleTarget(
                        MOVE_TOOL_ID, MOVE_CHORD, soloGroup, state, null));
        state.put(MOVE_CHORD, DEFAULT_TOOL_ID);
        assertEquals(MOVE_TOOL_ID,
                JIPipeDesktopGraphEditorUI.computeToolCycleTarget(
                        MOVE_TOOL_ID, MOVE_CHORD, soloGroup, state, DEFAULT_TOOL_ID));
    }
}
