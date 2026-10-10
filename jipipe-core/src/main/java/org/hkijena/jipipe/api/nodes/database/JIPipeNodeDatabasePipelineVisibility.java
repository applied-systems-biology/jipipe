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

package org.hkijena.jipipe.api.nodes.database;

import org.hkijena.jipipe.api.nodes.JIPipeNodeTypeCategory;

public enum JIPipeNodeDatabasePipelineVisibility {
    Pipeline,
    Compartments,
    Both;

    public static JIPipeNodeDatabasePipelineVisibility fromCategory(JIPipeNodeTypeCategory category) {
        if (category.isVisibleInPipeline() && category.isVisibleInCompartments()) {
            return Both;
        } else if (category.isVisibleInCompartments()) {
            return Compartments;
        } else {
            return Pipeline;
        }
    }

    /**
     * The taxonomy replacement for {@link #fromCategory(JIPipeNodeTypeCategory)}.
     * Takes the node's two visibility behavior flags (see
     * {@code TaxonomyView.behaviorFlags}) directly, so it works for converted
     * nodes whose legacy category is the generic {@code Internal} category
     * (whose flags are wrong). Uses the identical 3-value logic as
     * {@link #fromCategory}.
     *
     * @param visibleInPipeline    whether the node is visible in the pipeline editor
     * @param visibleInCompartments whether the node is visible in the compartment editor
     * @return the corresponding visibility constant
     */
    public static JIPipeNodeDatabasePipelineVisibility fromFlags(boolean visibleInPipeline, boolean visibleInCompartments) {
        if (visibleInPipeline && visibleInCompartments) {
            return Both;
        } else if (visibleInCompartments) {
            return Compartments;
        } else {
            return Pipeline;
        }
    }

    public boolean matches(JIPipeNodeDatabasePipelineVisibility other) {
        if (this == Both || other == Both) {
            return true;
        }
        return this == other;
    }
}
