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
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.*;
import org.hkijena.jipipe.api.nodes.categories.FileSystemNodeTypeCategory;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Applies subfolder navigation to each input folder
 */
@JIPipeDocumentation(name = "Relativize paths", description = "Converts the child path into a path relative to the parent path. This also works for children that " +
        "are located in a parent folder.")
@JIPipeOrganization(menuPath = "Modify", nodeTypeCategory = FileSystemNodeTypeCategory.class)

// Algorithm flow
@JIPipeInputSlot(value = PathData.class, slotName = "Parent", autoCreate = true)
@JIPipeInputSlot(value = PathData.class, slotName = "Child", autoCreate = true)
@JIPipeOutputSlot(value = PathData.class, slotName = "Output", autoCreate = true)

// Traits
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        PathData parent = dataBatch.getInputData("Parent", PathData.class);
        PathData child = dataBatch.getInputData("Child", PathData.class);

        dataBatch.addOutputData(getFirstOutputSlot(), new PathData(parent.getPath().relativize(child.getPath())));
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
    }
}
