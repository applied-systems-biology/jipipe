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
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A pure, metadata-level "is this project shareable (FAIR)?" checker
 * (feature #753). It powers the publish button's static badge and the
 * "FAIR checker" dialog.
 * <p>
 * <b>Why a separate checker and not the crate's compliance list?</b>
 * {@link JIPipeProjectCrate#buildCrate} only reports compliance violations
 * <i>after</i> it has built a full RO-Crate (which also auto-fills a default
 * license, adds the README/mainEntity, etc.) — that is the post-save check.
 * For a live "is it FAIR?" signal <i>before</i> the user publishes, we need a
 * metadata-only check that does not build a crate. This class therefore
 * mirrors the crate-compliance <i>intent</i> at the metadata level: a project
 * that can be shared should declare a license, a summary, and at least one
 * author. It intentionally does NOT duplicate the crate's structural checks
 * (mainEntity, README, programmingLanguage, conformsTo) — those are always
 * satisfied by the writer itself.
 * <p>
 * <b>No side effects, no crate build, no Swing.</b> Safe to call from any
 * thread at any time (the publish badge recomputes it on metadata changes).
 */
public final class JIPipeProjectFairCheck {

    private JIPipeProjectFairCheck() {
    }

    /**
     * Returns the FAIR-sharing items the project is still missing. An empty
     * list means the project is FAIR-ready (license, summary and at least one
     * author are all present).
     *
     * @param project the project to check
     * @return human-readable descriptions of the missing items (never null)
     */
    public static List<String> missing(JIPipeProject project) {
        if (project == null || project.getMetadata() == null) {
            return Collections.emptyList();
        }
        List<String> missing = new ArrayList<>();
        String license = StringUtils.nullToEmpty(project.getMetadata().getLicense()).trim();
        if (license.isEmpty()) {
            missing.add("License — no license is set on the project metadata.");
        }
        String summary = project.getMetadata().getSummary() == null
                ? ""
                : StringUtils.nullToEmpty(project.getMetadata().getSummary().toPlainText()).trim();
        if (summary.isEmpty()) {
            missing.add("Summary — no summary is set on the project metadata.");
        }
        if (project.getMetadata().getAuthors() == null || project.getMetadata().getAuthors().isEmpty()) {
            missing.add("Authors — no author is set on the project metadata.");
        }
        return missing;
    }

    /**
     * Convenience: {@code true} if {@link #missing(JIPipeProject)} is empty.
     *
     * @param project the project to check
     * @return if the project is FAIR-complete
     */
    public static boolean isFair(JIPipeProject project) {
        return missing(project).isEmpty();
    }
}
