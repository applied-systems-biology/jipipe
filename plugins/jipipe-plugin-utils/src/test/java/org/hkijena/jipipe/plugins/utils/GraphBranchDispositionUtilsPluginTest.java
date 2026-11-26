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

package org.hkijena.jipipe.plugins.utils;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.DebuggingFunction;
import org.hkijena.jipipe.plugins.utils.algorithms.ImportJIPipeSlotFolderAlgorithm;
import org.hkijena.jipipe.plugins.utils.algorithms.iterationsteps.CrasherAlgorithm;
import org.hkijena.jipipe.plugins.utils.algorithms.meta.GetJIPipeSlotFolderAlgorithm;
import org.hkijena.jipipe.plugins.utils.algorithms.meta.RunJIPipeProjectAlgorithm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the 1403 disposition for the user-facing debug/meta-run nodes of the
 * Utilities plugin: they must sit on {@link DebuggingFunction}, whose
 * default flags are permissive (visible in the node browsers, extractable,
 * hence deletable). Before the 1403 split they hung on the deprecated
 * Graph &gt; Debugging leaf and inherited its internal flags (hidden, not
 * extractable, not deletable).
 * <p>
 * Pure unit test: reads the {@link ConfigureJIPipeNode} annotation; no
 * {@code JIPipe} context is required.
 */
class GraphBranchDispositionUtilsPluginTest {

    private static void assertAssignedToDebugging(Class<?> nodeClass) {
        ConfigureJIPipeNode annotation = nodeClass.getAnnotation(ConfigureJIPipeNode.class);
        assertNotNull(annotation, nodeClass.getSimpleName() + " must carry @ConfigureJIPipeNode");
        assertTrue(annotation.function().length > 0,
                nodeClass.getSimpleName() + " must declare a primary function");
        assertEquals(DebuggingFunction.class, annotation.function()[0],
                nodeClass.getSimpleName() + " must be assigned to the DebuggingFunction leaf");
    }

    @Test
    void userFacingDebugAndMetaRunNodesAreAssignedToDebuggingFunction() {
        assertAssignedToDebugging(GetJIPipeSlotFolderAlgorithm.class);
        assertAssignedToDebugging(ImportJIPipeSlotFolderAlgorithm.class);
        assertAssignedToDebugging(CrasherAlgorithm.class);
        assertAssignedToDebugging(RunJIPipeProjectAlgorithm.class);
    }

    @Test
    void debuggingFunctionFlagsArePermissive() {
        JIPipeNodeTaxonomyFunction fn = new DebuggingFunction();
        assertTrue(fn.canExtract(), "DebuggingFunction members must be user-extractable (deletable)");
        assertTrue(fn.isVisibleInPipeline(), "DebuggingFunction members must be visible in the pipeline node browser");
    }
}
