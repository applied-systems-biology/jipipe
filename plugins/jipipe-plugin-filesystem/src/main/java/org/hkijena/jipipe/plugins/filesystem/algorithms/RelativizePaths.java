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

package org.hkijena.jipipe.plugins.filesystem.algorithms;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.FileSystemNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.plugins.filesystem.dataypes.PathData;

/**
 * Applies subfolder navigation to each input folder
 */
@SetJIPipeDocumentation(name = "Relativize paths", description = "Converts the child path into a path relative to the parent path. This also works for children that " +
        "are located in a parent folder.")
@ConfigureJIPipeNode(menuPath = "Modify", nodeTypeCategory = FileSystemNodeTypeCategory.class)


@AddJIPipeInputSlot(value = PathData.class, slotName = "Parent", create = true)
@AddJIPipeInputSlot(value = PathData.class, slotName = "Child", create = true)
@AddJIPipeOutputSlot(value = PathData.class, slotName = "Output", create = true)


public class RelativizePaths extends JIPipeIteratingAlgorithm {

    /**
     * @param info Algorithm info
     */
    public RelativizePaths(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public RelativizePaths(RelativizePaths other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        PathData parent = iterationStep.getInputData("Parent", PathData.class, progressInfo);
        PathData child = iterationStep.getInputData("Child", PathData.class, progressInfo);

        iterationStep.addOutputData(getFirstOutputSlot(), new PathData(parent.toPath().relativize(child.toPath())), progressInfo);
    }
}
