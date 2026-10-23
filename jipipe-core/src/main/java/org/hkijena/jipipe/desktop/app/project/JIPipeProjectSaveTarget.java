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

package org.hkijena.jipipe.desktop.app.project;

import java.nio.file.Path;
import java.util.Locale;

/**
 * The save-target decision of the JIPX migration UX (JIPX spec §7).
 * <p>
 * 6.0.0 behavior: existing legacy {@code .jip} projects get an
 * Office-style prompt on every save — "Save as JIPipe 6 project
 * ({@code .jipx}) / keep legacy project ({@code .jip}) / cancel".
 * New (untitled) projects and projects already saved as {@code .jipx}
 * proceed directly: untitled projects go straight to the save chooser,
 * whose first filter is {@code .jipx} (so they default to the new
 * format), and {@code .jipx} projects keep round-tripping in the new
 * format.
 * <p>
 * The decision is a pure static so it is testable without a GUI
 * (the prompt dialog itself is verified by eye in the app, per the
 * project's test policy).
 */
public final class JIPipeProjectSaveTarget {

    /**
     * The save-target decision
     */
    public enum SaveTarget {
        /**
         * The current save target is a legacy {@code .jip} file —
         * show the per-save JIPX migration prompt before saving.
         */
        JIPX_PROMPT,
        /**
         * Proceed directly (untitled projects go to the chooser,
         * {@code .jipx} projects save in place).
         */
        DIRECT
    }

    private JIPipeProjectSaveTarget() {
    }

    /**
     * Decides how a save with the given current save target proceeds
     * (see the class Javadoc for the migration UX).
     *
     * @param currentSavePath the project's current save target, or
     *                        {@code null} for an untitled project
     * @return {@link SaveTarget#JIPX_PROMPT} if the current save target
     * is a legacy {@code .jip} file, {@link SaveTarget#DIRECT} otherwise
     */
    public static SaveTarget decideSaveTarget(Path currentSavePath) {
        if (currentSavePath == null) {
            return SaveTarget.DIRECT;
        }
        String fileName = currentSavePath.getFileName().toString().toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".jip")) {
            return SaveTarget.JIPX_PROMPT;
        }
        return SaveTarget.DIRECT;
    }
}
