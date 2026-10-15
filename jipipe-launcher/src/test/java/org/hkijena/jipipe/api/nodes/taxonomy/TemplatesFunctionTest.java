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

package org.hkijena.jipipe.api.nodes.taxonomy;

import org.hkijena.jipipe.JIPipe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration gate for the template-taxonomy integration (6.0.0): the
 * catch-all function leaf {@code jipipe:function:templates} must be
 * registered under the {@code Miscellaneous} top-level branch so that
 * node templates whose taxonomy function is unset or stale still have a
 * defined placement in the function tree.
 * <p>
 * Mirrors {@link TaxonomyFullValidationTest}: boots the full registry via
 * {@link JIPipe#ensureInstance()} and inspects the live
 * {@link FunctionRegistry}.
 */
class TemplatesFunctionTest {

    @BeforeAll
    static void init() {
        // The ImageJLegacyPatcherLauncherSessionListener registered in this
        // module preinits the ImageJ1 legacy patcher before any test class
        // loads, so ensureInstance() here is safe. Idempotent: reuses the
        // JIPipe singleton if another test class in the same JVM already
        // initialized it.
        JIPipe.ensureInstance();
    }

    @Test
    void isRegisteredAsLeafUnderMisc() {
        FunctionRegistry f = JIPipe.getNodes().getFunctions();
        JIPipeNodeTaxonomyFunction fn = f.byId("jipipe:function:templates");
        assertNotNull(fn, "jipipe:function:templates must be registered in the FunctionRegistry");
        assertEquals("Templates", fn.getName());
        assertEquals("jipipe:function:misc", fn.getParent().getId());

        // It is a leaf: nothing registers it as the parent of other functions.
        assertTrue(f.children(fn).isEmpty(), "jipipe:function:templates must be a leaf");
    }
}
