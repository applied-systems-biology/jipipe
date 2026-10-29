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

import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.utils.PathUtils;

import java.nio.file.Path;

/**
 * The save-target decision of the JIPX migration UX (JIPX spec §7).
 * <p>
 * JIPipe 6.0 behavior: existing legacy {@code .jip} projects get a
 * per-save prompt offering the modern format ({@code .jipx},
 * recommended) or the legacy format ({@code .jip}). New (untitled)
 * projects and projects already saved as {@code .jipx} proceed
 * directly: untitled projects go straight to the save chooser, whose
 * first filter is {@code .jipx} (so they default to the modern
 * format), and {@code .jipx} projects keep round-tripping in the modern
 * format. Autosaves (the {@code (avoidDialog, updateSavePath)}
 * combination {@code (true, false)}) never prompt — they silently keep
 * the project's current format.
 * <p>
 * The compatibility gate ({@link #isLegacyCompatible(JIPipeProject)}):
 * when a project carries state that only the modern format can express,
 * it cannot be saved as {@code .jip} losslessly — the prompt is skipped
 * and the project is always saved as {@code .jipx}. No such state exists
 * yet; the gate is in place for when it lands.
 * <p>
 * The decisions are pure statics so they are testable without a GUI
 * (the prompt dialog itself is verified by eye in the app, per the
 * project's test policy).
 */
public final class JIPipeProjectSaveTarget {

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
        return shouldPrompt(currentSavePath, false, true)
                ? SaveTarget.JIPX_PROMPT
                : SaveTarget.DIRECT;
    }

    /**
     * Decides whether the JIPX migration prompt is shown for a save with
     * the given parameters (see the class Javadoc for the migration UX).
     * <p>
     * Rules:
     * <ul>
     * <li>{@code false} when {@code currentSavePath} is {@code null}
     * (untitled project — the save chooser decides the format) or is
     * already a {@code .jipx} file (it keeps round-tripping);</li>
     * <li>{@code false} for autosaves — {@code avoidDialog} without
     * {@code updateSavePath} ({@code (true, false)}): the prompt must
     * not fire on every autosave interval;</li>
     * <li>{@code true} when {@code currentSavePath} is a legacy
     * {@code .jip} file (Ctrl+S and the chooser path);</li>
     * <li>{@code false} for any other extension (a misnamed file — the
     * write dispatch will reject it later).</li>
     * </ul>
     *
     * @param currentSavePath the project's current save target, or
     *                        {@code null} for an untitled project
     * @param avoidDialog     the {@code avoidDialog} flag of
     *                        {@code saveProjectAs(avoidDialog, updateSavePath)}
     * @param updateSavePath  the {@code updateSavePath} flag of
     *                        {@code saveProjectAs(avoidDialog, updateSavePath)}
     * @return whether the JIPX migration prompt is shown before saving
     */
    public static boolean shouldPrompt(Path currentSavePath, boolean avoidDialog, boolean updateSavePath) {
        if (currentSavePath == null || PathUtils.isJipxPath(currentSavePath)) {
            return false;
        }
        // Autosave (avoidDialog without updateSavePath): never prompt.
        if (avoidDialog && !updateSavePath) {
            return false;
        }
        return PathUtils.isJipPath(currentSavePath);
    }

    /**
     * Compatibility gate: can this project be FULLY EXRESSED in the legacy
     * {@code .jip} format? (JIPX spec §7, forward hook.)
     * <p>
     * The save flow uses the answer:
     * <ul>
     * <li>{@code true} (the default — every project today is expressible
     * in {@code .jip}) — the normal rules apply: legacy projects get the
     * format prompt, modern projects save in place;</li>
     * <li>{@code false} — the project carries state that only the modern
     * format ({@code .jipx}) can hold. Saving it as {@code .jip} would
     * lose data, so the prompt is skipped and the project is ALWAYS saved
     * as {@code .jipx}.</li>
     * </ul>
     * No such state exists yet; when a jipx-exclusive feature lands
     * (e.g. the multi-graph or history extensions), it checks the
     * project here — the save flow and its tests already honor this
     * decision, no further rewiring.
     * <p>
     * Null-safe: {@code null} returns {@code true} (an unknown project
     * is not proven to carry modern-only state).
     *
     * @param project the project to check
     * @return true if the project can be saved losslessly in the legacy
     * format
     */
    public static boolean isLegacyCompatible(JIPipeProject project) {
        if (project == null) {
            return true;
        }
        // Forward hook: the first jipx-exclusive feature (e.g. multi-graph,
        // history) reports its projects as incompatible here. Until then,
        // every project is expressible in the legacy format.
        return true;
    }

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
}
