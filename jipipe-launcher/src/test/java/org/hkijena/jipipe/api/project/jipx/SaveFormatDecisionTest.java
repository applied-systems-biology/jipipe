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

import org.hkijena.jipipe.desktop.app.project.JIPipeProjectSaveTarget;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The JIPX migration save-target decision (spec §7): only projects whose
 * current save target is a legacy {@code .jip} file get the per-save
 * "save as JIPipe 6 project?" prompt; untitled projects and projects
 * already saved as {@code .jipx} proceed directly (the chooser decides
 * the format for untitled projects).
 */
public class SaveFormatDecisionTest {

    @Test
    public void untitledProjectIsDirect() {
        assertEquals(JIPipeProjectSaveTarget.SaveTarget.DIRECT,
                JIPipeProjectSaveTarget.decideSaveTarget(null));
    }

    @Test
    public void existingJipxIsDirect() {
        assertEquals(JIPipeProjectSaveTarget.SaveTarget.DIRECT,
                JIPipeProjectSaveTarget.decideSaveTarget(Path.of("/home/user/projects/project.jipx")));
    }

    @Test
    public void existingJipxIsCaseInsensitive() {
        assertEquals(JIPipeProjectSaveTarget.SaveTarget.DIRECT,
                JIPipeProjectSaveTarget.decideSaveTarget(Path.of("/home/user/projects/project.JIPX")));
    }

    @Test
    public void existingJipGetsThePrompt() {
        assertEquals(JIPipeProjectSaveTarget.SaveTarget.JIPX_PROMPT,
                JIPipeProjectSaveTarget.decideSaveTarget(Path.of("/home/user/projects/project.jip")));
    }

    @Test
    public void nonProjectExtensionsAreDirect() {
        // Files that are neither format go through the normal save flow
        // (which will fail at the writer dispatch) — no prompt.
        assertEquals(JIPipeProjectSaveTarget.SaveTarget.DIRECT,
                JIPipeProjectSaveTarget.decideSaveTarget(Path.of("/home/user/projects/project.jipc")));
        assertEquals(JIPipeProjectSaveTarget.SaveTarget.DIRECT,
                JIPipeProjectSaveTarget.decideSaveTarget(Path.of("/home/user/projects/project.json")));
    }
}
