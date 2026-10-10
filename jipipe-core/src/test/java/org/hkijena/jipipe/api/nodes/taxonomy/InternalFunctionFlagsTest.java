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

import org.hkijena.jipipe.api.nodes.taxonomy.functions.graph.GraphCompartmentOutputsFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.graph.GraphDebugFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.graph.GraphWrappersFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.FilteringFunction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the behavior-flag contract of the taxonomy seed functions, in
 * particular the "internal" trio that mirrors the legacy
 * {@code InternalNodeTypeCategory} (hidden from the pipeline/compartment
 * editors <em>and</em> not user-extractable).
 * <p>
 * Regression background (Task 21b): the node database / graph consumers read
 * the behavior flags through the taxonomy function
 * ({@code TaxonomyView.behaviorFlags(info)}). The interface default for
 * {@link JIPipeNodeTaxonomyFunction#canExtract()} is {@code true}; the three
 * internal graph leaves override the two visibility flags to {@code false}
 * and must ALSO override {@code canExtract()} to {@code false}, otherwise
 * {@code JIPipeGraph.extract(..., withInternal = false)} would start including
 * internal nodes (a behavior change from the pre-conversion category-based
 * flags). This test pins that contract.
 * <p>
 * Pure unit test: instantiates the concrete function objects directly; no
 * {@code JIPipe} context is required.
 */
class InternalFunctionFlagsTest {

    @Test
    void internalGraphLeavesAreNotExtractableAndHiddenFromPipeline() {
        JIPipeNodeTaxonomyFunction[] internalLeaves = {
                new GraphDebugFunction(),
                new GraphWrappersFunction(),
                new GraphCompartmentOutputsFunction(),
        };
        for (JIPipeNodeTaxonomyFunction leaf : internalLeaves) {
            assertFalse(leaf.isVisibleInPipeline(),
                    leaf.getClass().getSimpleName() + " must be hidden from the pipeline");
            assertFalse(leaf.isVisibleInCompartments(),
                    leaf.getClass().getSimpleName() + " must be hidden from compartments");
            assertFalse(leaf.canExtract(),
                    leaf.getClass().getSimpleName()
                            + " must not be user-extractable (mirror legacy InternalNodeTypeCategory)");
        }
    }

    @Test
    void normalLeafIsExtractable() {
        // A regular leaf does not override canExtract(), so it keeps the
        // interface default (true) and stays extractable.
        FilteringFunction filtering = new FilteringFunction();
        assertTrue(filtering.canExtract(),
                "a normal leaf must remain user-extractable (interface default true)");
    }
}
