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
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.FileSystemNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.plugins.filesystem.dataypes.PathData;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Applies subfolder navigation to each input folder
 */
@SetJIPipeDocumentation(name = "Relativize paths by string", description = "Modifies the incoming paths so that they are relative to a parent " +
        "path defined by the parameter. If you leave the parameter empty, the paths are un-changed.")
@ConfigureJIPipeNode(menuPath = "Modify", nodeTypeCategory = FileSystemNodeTypeCategory.class)


@AddJIPipeInputSlot(value = PathData.class, name = "Child", create = true)
@AddJIPipeOutputSlot(value = PathData.class, name = "Output", create = true)


public class RelativizeByParameter extends JIPipeSimpleIteratingAlgorithm {

    private Path parentPath = Paths.get("");

    /**
     * @param info Algorithm info
     */
    public RelativizeByParameter(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public RelativizeByParameter(RelativizeByParameter other) {
        super(other);
        this.parentPath = other.parentPath;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        FolderData inputFolder = iterationStep.getInputData(getFirstInputSlot(), FolderData.class, progressInfo);
        if (parentPath == null || parentPath.toString().isEmpty())
            iterationStep.addOutputData(getFirstOutputSlot(), inputFolder, progressInfo);
        else
            iterationStep.addOutputData(getFirstOutputSlot(), new PathData(parentPath.relativize(inputFolder.toPath())), progressInfo);
    }

    /**
     * @return The subfolder
     */
    @JIPipeParameter("parent-path")
    @SetJIPipeDocumentation(name = "Path name", description = "The file or folder name of the output paths. If empty, the original path is unchanged.")
    public Path getParentPath() {
        return parentPath;
    }

    /**
     * Sets the subfolder
     *
     * @param parentPath the subfolder
     */
    @JIPipeParameter("parent-path")
    public void setParentPath(Path parentPath) {
        this.parentPath = parentPath;
    }
}
