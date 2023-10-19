/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.filesystem.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.FileSystemNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;

/**
 * Applies subfolder navigation to each input folder
 */
@JIPipeDocumentation(name = "Concatenate paths", description = "Concatenates two paths")
@JIPipeNode(menuPath = "Modify", nodeTypeCategory = FileSystemNodeTypeCategory.class)


@JIPipeInputSlot(value = PathData.class, slotName = "Left", autoCreate = true)
@JIPipeInputSlot(value = PathData.class, slotName = "Right", autoCreate = true)
@JIPipeOutputSlot(value = PathData.class, slotName = "Output", autoCreate = true)


public class ConcatenatePaths extends JIPipeIteratingAlgorithm {

    /**
     * @param info Algorithm info
     */
    public ConcatenatePaths(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public ConcatenatePaths(ConcatenatePaths other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        PathData left = iterationStep.getInputData("Left", PathData.class, progressInfo);
        PathData right = iterationStep.getInputData("Right", PathData.class, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new PathData(left.toPath().resolve(right.getPath())), progressInfo);
    }

}
