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

package org.hkijena.jipipe.api;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.plugins.parameters.library.taxonomy.JIPipeTaxonomyFunctionRef;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the one-shot migration of the legacy {@code menu-path} key to the
 * {@code taxonomy-function} reference (spec §9): the pure first-segment
 * name→id matcher, the transient legacy capture on the model (order-safety,
 * idempotency, the auditable "Previously" footer, and the drop-on-save of the
 * legacy key). Boots the JIPipe instance so the {@code FunctionRegistry} is
 * populated for the name match.
 */
class JIPipeNodeTemplateMigrationTest {

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    // --- the pure name→id matcher ---
    @Test
    void imageSegmentMapsToTopLevelBranchNotExportLeaf() {
        // "Image" (→ "Images") must map to the top-level branch, NOT the
        // childless export sub-leaf jipipe:function:export-images that
        // shadows it with the same display name.
        assertThat(JIPipeNodeTemplateMigration.migrate("Image").getId()).isEqualTo("jipipe:function:images");
    }

    @Test
    void tableSegmentMapsToTopLevelBranchNotExportLeaf() {
        // "Table" (→ "Tables") must map to the top-level branch, NOT
        // jipipe:function:export-tables.
        assertThat(JIPipeNodeTemplateMigration.migrate("Table").getId()).isEqualTo("jipipe:function:tables");
    }

    @Test
    void leafOnlyNameFallsThroughToLeaf() {
        // "Thresholding" has no same-named top-level branch → the child pass
        // must still resolve it to the leaf under Images.
        assertThat(JIPipeNodeTemplateMigration.migrate("Thresholding").getId()).isEqualTo("jipipe:function:thresholding");
    }

    @Test
    void unknownSegmentYieldsUnset() {
        JIPipeTaxonomyFunctionRef ref = JIPipeNodeTemplateMigration.migrate("TotallyUnknownXyz");
        assertThat(ref.isResolved()).isFalse();
        assertThat(ref.getId()).isNull();
    }

    @Test
    void emptySegmentYieldsUnset() {
        assertThat(JIPipeNodeTemplateMigration.migrate("").isResolved()).isFalse();
    }

    // --- the model migration (transient capture + order-safe + footer + drop-on-save) ---
    @Test
    void legacyTemplateMigratesFunctionAndFooters() throws Exception {
        String json = "{\"name\":\"legacy\",\"menu-path\":[\"Image\",\"Threshold\"]}";
        JIPipeNodeTemplate t = JsonUtils.readFromString(json, JIPipeNodeTemplate.class);
        t.migrateLegacyMenuPath();
        assertThat(t.getTaxonomyFunction().getId()).isEqualTo("jipipe:function:images");   // "Image"→top-level branch
        assertThat(t.getDescription().getHtml()).contains("Previously");  // auditable footer
    }

    @Test
    void existingFunctionWinsOverMenuPath() throws Exception {  // order-safety
        String json = "{\"name\":\"x\",\"menu-path\":[\"Image\"],\"taxonomy-function\":\"jipipe:function:tables\"}";
        JIPipeNodeTemplate t = JsonUtils.readFromString(json, JIPipeNodeTemplate.class);
        t.migrateLegacyMenuPath();
        assertThat(t.getTaxonomyFunction().getId()).isEqualTo("jipipe:function:tables");  // preserved
        assertThat(t.getDescription().getHtml()).doesNotContain("Previously");            // no footer
    }

    @Test
    void migrationIsIdempotent() throws Exception {
        JIPipeNodeTemplate t = JsonUtils.readFromString(
            "{\"name\":\"x\",\"menu-path\":[\"Image\"]}", JIPipeNodeTemplate.class);
        t.migrateLegacyMenuPath();
        String afterFirst = t.getDescription().getHtml();
        t.migrateLegacyMenuPath();
        assertThat(t.getDescription().getHtml()).isEqualTo(afterFirst);  // no double footer
    }

    @Test
    void saveDropsLegacyKey() throws Exception {
        JIPipeNodeTemplate t = JsonUtils.readFromString(
            "{\"name\":\"x\",\"menu-path\":[\"Image\",\"Threshold\"]}", JIPipeNodeTemplate.class);
        t.migrateLegacyMenuPath();
        assertThat(JsonUtils.toPrettyJsonString(t)).doesNotContain("menu-path");
    }
}
