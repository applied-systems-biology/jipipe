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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.metadata.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The pure FAIR-metadata checker the publish button's badge and the
 * "FAIR checker" dialog rely on (feature #753). {@link
 * JIPipeProjectFairCheck#missing(JIPipeProject)} is metadata-only — it does
 * NOT build a RO-Crate — and mirrors the crate-compliance intent at the
 * point where the user is deciding whether to publish: a shareable project
 * must declare a license, a summary, and at least one author.
 * <p>
 * <b>No Swing.</b> {@code new JIPipeProject()} needs the plugin registry
 * (the project-settings sheet types), so the instance is required — the
 * same {@code @BeforeAll JIPipe.ensureInstance()} pattern the other JIPX
 * tests in this module use. The legacy-patcher listener registered for this
 * module makes {@code ensureInstance()} safe regardless of test ordering.
 */
class JIPipeProjectFairCheckTest {

    @BeforeAll
    static void init() {
        // The ImageJLegacyPatcherLauncherSessionListener registered in this
        // module preinits the ImageJ1 legacy patcher before any test class
        // loads, so ensureInstance() here is safe. Idempotent.
        JIPipe.ensureInstance();
    }

    /**
     * A fresh project has no license, no summary and no authors — all three
     * items must be flagged as missing.
     */
    @Test
    void bareProjectFlagsAllItems() {
        JIPipeProject project = new JIPipeProject();
        List<String> missing = JIPipeProjectFairCheck.missing(project);

        assertEquals(3, missing.size(), "A bare project must be missing all three FAIR items, got: " + missing);
        assertTrue(hasWord(missing, "license"),
                "License must be flagged; got: " + missing);
        assertTrue(hasWord(missing, "summary"),
                "Summary must be flagged; got: " + missing);
        assertTrue(hasWord(missing, "author"),
                "Author must be flagged; got: " + missing);
    }

    /**
     * A fully-populated project (license + summary + one author) is
     * FAIR-complete — nothing is missing.
     */
    @Test
    void fullyPopulatedProjectIsComplete() {
        JIPipeProject project = new JIPipeProject();
        project.getMetadata().setLicense("CC-BY-4.0");
        project.getMetadata().setSummary(new HTMLText("A short plain-text summary"));
        JIPipeAuthorMetadata author = new JIPipeAuthorMetadata();
        author.setFirstName("Ada");
        author.setLastName("Lovelace");
        project.getMetadata().getAuthors().add(author);

        List<String> missing = JIPipeProjectFairCheck.missing(project);

        assertTrue(missing.isEmpty(),
                "A fully-populated project must be FAIR-complete, got missing: " + missing);
    }

    /**
     * Test-only helper: true if any missing-item string mentions {@code word}
     * (case-insensitive). Keeps the production {@link
     * JIPipeProjectFairCheck} free of test-facing helpers.
     */
    private static boolean hasWord(List<String> missing, String word) {
        String needle = word.toLowerCase(java.util.Locale.ROOT);
        return missing.stream().anyMatch(s -> s.toLowerCase(java.util.Locale.ROOT).contains(needle));
    }

    /**
     * Each item is checked independently: filling only the license must
     * still leave summary + author flagged.
     */
    @Test
    void itemsAreCheckedIndependently() {
        JIPipeProject project = new JIPipeProject();
        project.getMetadata().setLicense("MIT");
        List<String> missing = JIPipeProjectFairCheck.missing(project);

        assertEquals(2, missing.size(), "Only license is set, so 2 items remain missing: " + missing);
        assertTrue(!hasWord(missing, "license"),
                "License is set — it must not be flagged; got: " + missing);
        assertTrue(hasWord(missing, "summary"),
                "Summary is still empty — it must be flagged; got: " + missing);
        assertTrue(hasWord(missing, "author"),
                "Author is still missing — it must be flagged; got: " + missing);
    }
}
