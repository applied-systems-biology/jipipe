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

package org.hkijena.jipipe.api.nodes.taxonomy.functions.annotations;

import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.AnnotationsFunction;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Collections;
import java.util.Set;

/**
 * Leaf of the Annotations branch for <em>graph (canvas) annotations</em>:
 * nodes that decorate a graph's canvas rather than operating on data —
 * text boxes, arrows, and image boxes
 * ({@code org.hkijena.jipipe.api.nodes.annotation.JIPipeGraphCanvasNote}).
 * <p>
 * This is the taxonomy successor of the legacy
 * {@code GraphAnnotationsNodeTypeCategory} (id
 * {@code org.hkijena.jipipe:graph-annotations}). Like that legacy category it
 * is visible in <em>both</em> the pipeline and the compartments graph, so the
 * canvas notes are addable from both editors' node browser (the
 * compartments graph carries only compartments and canvas notes).
 * <p>
 * It is kept separate from {@link AnnotationsEditFunction}: that leaf covers
 * manipulation of <em>data</em> annotations and is pipeline-only, so merging
 * canvas notes into it would wrongly flood the compartments list with data
 * annotation nodes.
 */
public final class GraphAnnotationsFunction implements JIPipeNodeTaxonomyFunction {

    @Override
    public String getId() {
        return "jipipe:function:graph-annotations";
    }

    @Override
    public String getName() {
        return "Graph annotations";
    }

    @Override
    public JIPipeNodeTaxonomyFunction getParent() {
        return new AnnotationsFunction();
    }

    @Override
    @Nullable
    public Float getColorHue() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Canvas annotations that decorate a graph (text boxes, arrows, image boxes)";
    }

    @Override
    @Nullable
    public Icon getIcon() {
        return null;
    }

    @Override
    public Set<String> getTags() {
        return Collections.emptySet();
    }

    @Override
    public boolean isVisibleInPipeline() {
        return true;
    }

    @Override
    public boolean isVisibleInCompartments() {
        return true;
    }
}
