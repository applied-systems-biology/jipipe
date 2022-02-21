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
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Applies subfolder navigation to each input folder
 */
@JIPipeDocumentation(name = "Concatenate paths by parameter", description = "Concatenates the input paths by a string.")
@JIPipeNode(menuPath = "Modify", nodeTypeCategory = FileSystemNodeTypeCategory.class)
@JIPipeInputSlot(value = PathData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = PathData.class, slotName = "Output", autoCreate = true)
public class ConcatenateByParameter extends JIPipeSimpleIteratingAlgorithm {

    private Path subPath = Paths.get("");

    /**
     * @param info Algorithm info
     */
    public ConcatenateByParameter(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public ConcatenateByParameter(ConcatenateByParameter other) {
        super(other);
        this.subPath = other.subPath;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        FolderData inputFolder = dataBatch.getInputData(getFirstInputSlot(), FolderData.class, progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), new FolderData(inputFolder.toPath().resolve(subPath)), progressInfo);
    }

    /**
     * @return The subfolder
     */
    @JIPipeParameter("sub-path")
    @JIPipeDocumentation(name = "Concatenated path", description = "The path is concatenated by this path.")
    public Path getSubPath() {
        return subPath;
    }

    /**
     * Sets the subfolder
     *
     * @param subPath the subfolder
     */
    @JIPipeParameter("sub-path")
    public void setSubPath(Path subPath) {
        this.subPath = subPath;
    }
}