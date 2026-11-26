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

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.DataUtilitiesFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.DebuggingFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.graph.GraphStructuralFunction;
import org.hkijena.jipipe.api.compartments.algorithms.IOInterfaceAlgorithm;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartmentOutput;
import org.hkijena.jipipe.api.grouping.GraphWrapperAlgorithmInput;
import org.hkijena.jipipe.api.grouping.GraphWrapperAlgorithmOutput;
import org.hkijena.jipipe.api.looping.LoopEndNode;
import org.hkijena.jipipe.api.looping.LoopStartNode;
import org.hkijena.jipipe.plugins.core.nodes.JIPipeCommentNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the 1403 disposition: which leaf each Graph-branch node (core
 * module) is assigned to, and the resulting user-facing behavior. Work item
 * 1403 ("Comment nodes cannot be deleted") regressed exactly here: the
 * comment node inherited the internal behavior flags (hidden, not
 * extractable, hence not deletable) from its assigned function. User-facing
 * nodes must sit on functions whose flags are permissive; structural nodes
 * must sit on the hidden, non-extractable {@link GraphStructuralFunction}.
 * <p>
 * Pure unit test: reads the {@link ConfigureJIPipeNode} annotation and
 * instantiates the assigned function directly; no {@code JIPipe} context is
 * required.
 */
class GraphBranchDispositionTest {

    private static JIPipeNodeTaxonomyFunction primaryFunction(Class<?> nodeClass) {
        ConfigureJIPipeNode annotation = nodeClass.getAnnotation(ConfigureJIPipeNode.class);
        assertNotNull(annotation, nodeClass.getSimpleName() + " must carry @ConfigureJIPipeNode");
        assertEquals(1, annotation.function().length,
                nodeClass.getSimpleName() + " must have exactly one primary function");
        try {
            return annotation.function()[0].getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not instantiate the function of " + nodeClass.getSimpleName(), e);
        }
    }

    private static void assertPrimaryFunction(Class<?> nodeClass, Class<? extends JIPipeNodeTaxonomyFunction> expected) {
        JIPipeNodeTaxonomyFunction fn = primaryFunction(nodeClass);
        assertEquals(expected, fn.getClass(),
                nodeClass.getSimpleName() + " must be assigned to the " + expected.getSimpleName() + " leaf");
    }

    @Test
    void userFacingDebugMembersAreAssignedToDebuggingFunction() {
        assertPrimaryFunction(JIPipeCommentNode.class, DebuggingFunction.class);
    }

    @Test
    void structuralNodesAreAssignedToGraphStructuralFunction() {
        assertPrimaryFunction(LoopStartNode.class, GraphStructuralFunction.class);
        assertPrimaryFunction(LoopEndNode.class, GraphStructuralFunction.class);
        assertPrimaryFunction(GraphWrapperAlgorithmInput.class, GraphStructuralFunction.class);
        assertPrimaryFunction(GraphWrapperAlgorithmOutput.class, GraphStructuralFunction.class);
        assertPrimaryFunction(JIPipeProjectCompartmentOutput.class, GraphStructuralFunction.class);
    }

    @Test
    void ioInterfaceIsAUserFacingDataUtilitiesNode() {
        assertPrimaryFunction(IOInterfaceAlgorithm.class, DataUtilitiesFunction.class);
    }

    @Test
    void structuralFunctionIsHiddenAndNotExtractable() {
        GraphStructuralFunction fn = new GraphStructuralFunction();
        assertFalse(fn.isVisibleInPipeline(), "structural nodes must be hidden from the pipeline node browser");
        assertFalse(fn.isVisibleInCompartments(), "structural nodes must be hidden from the compartments node browser");
        assertFalse(fn.canExtract(), "structural nodes must not be user-extractable");
    }

    @Test
    void commentNodeIsExtractableThe1403RegressionContract() {
        JIPipeNodeTaxonomyFunction fn = primaryFunction(JIPipeCommentNode.class);
        assertTrue(fn.canExtract(),
                "the comment node must remain user-extractable (extractability gates user deletion, see work item 1403)");
        assertTrue(fn.isVisibleInPipeline(),
                "the comment node must be visible in the pipeline node browser");
    }
}
