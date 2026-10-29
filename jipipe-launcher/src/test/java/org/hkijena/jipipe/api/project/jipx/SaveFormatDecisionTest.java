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

package org.hkijena.jipipe.api.project.jipx;

import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.desktop.app.project.JIPipeProjectSaveTarget;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The JIPX migration prompt decision (spec §7): only projects whose
 * current save target is a legacy {@code .jip} file get the per-save
 * "save as JIPipe 6 project?" prompt — and only when the save is a
 * user-initiated one (Ctrl+S / save chooser). Untitled projects and
 * projects already saved as {@code .jipx} proceed directly, and
 * AUTOSAVES (the {@code (avoidDialog, updateSavePath)} combination
 * {@code (true, false)}) never prompt — a modal prompt on every autosave
 * interval would break autosave.
 */
public class SaveFormatDecisionTest {

    private static final Path NULL = null;
    private static final Path JIPX = Path.of("/home/user/projects/project.jipx");
    private static final Path JIPX_UPPER = Path.of("/home/user/projects/project.JIPX");
    private static final Path JIP = Path.of("/home/user/projects/project.jip");
    private static final Path JIPC = Path.of("/home/user/projects/project.jipc");
    private static final Path JSON = Path.of("/home/user/projects/project.json");

    // ------------------------------------------------------------------
    // shouldPrompt: the full decision table
    // ------------------------------------------------------------------

    @Test
    public void untitledProjectNeverPrompts() {
        assertFalse(JIPipeProjectSaveTarget.shouldPrompt(NULL, true, false),
                "(null, true, false) — untitled autosave must not prompt");
        assertFalse(JIPipeProjectSaveTarget.shouldPrompt(NULL, true, true),
                "(null, true, true) — untitled save must not prompt (the chooser decides)");
        assertFalse(JIPipeProjectSaveTarget.shouldPrompt(NULL, false, true),
                "(null, false, true) — untitled chooser save must not prompt");
    }

    @Test
    public void jipxProjectNeverPrompts() {
        assertFalse(JIPipeProjectSaveTarget.shouldPrompt(JIPX, true, false),
                "(x.jipx, true, false) — autosave of a .jipx must not prompt");
        assertFalse(JIPipeProjectSaveTarget.shouldPrompt(JIPX, true, true),
                "(x.jipx, true, true) — Ctrl+S of a .jipx must not prompt (it round-trips)");
        assertFalse(JIPipeProjectSaveTarget.shouldPrompt(JIPX, false, true),
                "(x.jipx, false, true) — chooser save of a .jipx must not prompt");
        assertFalse(JIPipeProjectSaveTarget.shouldPrompt(JIPX_UPPER, true, true),
                "(x.JIPX, true, true) — extension check is case-insensitive");
    }

    @Test
    public void autosaveOfLegacyJipNeverPrompts() {
        assertFalse(JIPipeProjectSaveTarget.shouldPrompt(JIP, true, false),
                "(x.jip, true, false) — autosave must NEVER prompt (the modal prompt would fire every autosave interval)");
    }

    @Test
    public void ctrlSOfLegacyJipPrompts() {
        assertTrue(JIPipeProjectSaveTarget.shouldPrompt(JIP, true, true),
                "(x.jip, true, true) — Ctrl+S of a legacy project must prompt");
    }

    @Test
    public void chooserSaveOfLegacyJipPrompts() {
        assertTrue(JIPipeProjectSaveTarget.shouldPrompt(JIP, false, true),
                "(x.jip, false, true) — a .jip chosen in the save dialog must prompt");
    }

    @Test
    public void nonProjectExtensionsNeverPrompt() {
        // Files that are neither format go through the normal save flow
        // (which will fail at the writer dispatch) — no prompt.
        for (Path path : new Path[]{JIPC, JSON}) {
            for (int avoid = 0; avoid < 2; avoid++) {
                for (int update = 0; update < 2; update++) {
                    assertFalse(JIPipeProjectSaveTarget.shouldPrompt(path, avoid == 1, update == 1),
                            path + " with avoidDialog=" + (avoid == 1) + ", updateSavePath=" + (update == 1)
                                    + " must not prompt");
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // decideSaveTarget (the legacy enum API, kept for the JIPX_PROMPT
    // / DIRECT decision)
    // ------------------------------------------------------------------

    @Test
    public void decideSaveTargetMatchesPromptDecision() {
        assertEquals(JIPipeProjectSaveTarget.SaveTarget.DIRECT,
                JIPipeProjectSaveTarget.decideSaveTarget(NULL));
        assertEquals(JIPipeProjectSaveTarget.SaveTarget.DIRECT,
                JIPipeProjectSaveTarget.decideSaveTarget(JIPX));
        assertEquals(JIPipeProjectSaveTarget.SaveTarget.JIPX_PROMPT,
                JIPipeProjectSaveTarget.decideSaveTarget(JIP));
    }

    // ------------------------------------------------------------------
    // isLegacyCompatible (the format-compatibility gate, JIPipe spec §7):
    // a project whose state cannot be expressed in the legacy format is
    // ALWAYS saved as .jipx (the prompt is skipped)
    // ------------------------------------------------------------------

    @Test
    public void legacyCompatibilityGateNullSafe() {
        assertTrue(JIPipeProjectSaveTarget.isLegacyCompatible(null),
                "an unknown project is not proven to carry modern-only state");
    }

    @Test
    public void legacyCompatibilityGateCurrentlyEveryProjectIsCompatible() {
        // No jipx-exclusive state exists yet — every project is still
        // expressible in the legacy format, so the prompt and the legacy
        // save path stay reachable. (That old-format projects round-trip
        // losslessly is pinned by the corpus done-gate.)
        assertTrue(JIPipeProjectSaveTarget.isLegacyCompatible(new JIPipeProject()));
    }
}
