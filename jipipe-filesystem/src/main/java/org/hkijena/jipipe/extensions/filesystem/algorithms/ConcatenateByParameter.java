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
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Applies subfolder navigation to each input folder
 */
@JIPipeDocumentation(name = "Concatenate paths by parameter", description = "Concatenates the input paths by a string.")
@JIPipeOrganization(menuPath = "Modify", algorithmCategory = JIPipeNodeCategory.FileSystem)

// Algorithm flow
@JIPipeInputSlot(value = PathData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = PathData.class, slotName = "Output", autoCreate = true)

// Traits
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        FolderData inputFolder = dataBatch.getInputData(getFirstInputSlot(), FolderData.class);
        dataBatch.addOutputData(getFirstOutputSlot(), new FolderData(inputFolder.getPath().resolve(subPath)));
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
