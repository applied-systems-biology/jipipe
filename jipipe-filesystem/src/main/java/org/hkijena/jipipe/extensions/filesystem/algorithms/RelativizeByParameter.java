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
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Applies subfolder navigation to each input folder
 */
@JIPipeDocumentation(name = "Relativize paths by string", description = "Modifies the incoming paths so that they are relative to a parent " +
        "path defined by the parameter. If you leave the parameter empty, the paths are un-changed.")
@JIPipeNode(menuPath = "Modify", nodeTypeCategory = FileSystemNodeTypeCategory.class)


@JIPipeInputSlot(value = PathData.class, slotName = "Child", autoCreate = true)
@JIPipeOutputSlot(value = PathData.class, slotName = "Output", autoCreate = true)


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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        FolderData inputFolder = dataBatch.getInputData(getFirstInputSlot(), FolderData.class, progressInfo);
        if (parentPath == null || parentPath.toString().isEmpty())
            dataBatch.addOutputData(getFirstOutputSlot(), inputFolder, progressInfo);
        else
            dataBatch.addOutputData(getFirstOutputSlot(), new PathData(parentPath.relativize(inputFolder.toPath())), progressInfo);
    }

    /**
     * @return The subfolder
     */
    @JIPipeParameter("parent-path")
    @JIPipeDocumentation(name = "Path name", description = "The file or folder name of the output paths. If empty, the original path is unchanged.")
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
