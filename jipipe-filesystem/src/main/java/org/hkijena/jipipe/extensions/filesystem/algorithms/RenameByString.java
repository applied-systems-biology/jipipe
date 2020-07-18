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
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Applies subfolder navigation to each input folder
 */
@JIPipeDocumentation(name = "Rename path", description = "Sets the file or folder name of each path to the specified string.")
@JIPipeOrganization(menuPath = "Modify", algorithmCategory = JIPipeNodeCategory.FileSystem)

// Algorithm flow
@JIPipeInputSlot(value = PathData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = PathData.class, slotName = "Output", autoCreate = true)

// Traits
public class RenameByString extends JIPipeSimpleIteratingAlgorithm {

    private String pathName = "";

    /**
     * @param info Algorithm info
     */
    public RenameByString(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public RenameByString(RenameByString other) {
        super(other);
        this.pathName = other.pathName;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        FolderData inputFolder = dataBatch.getInputData(getFirstInputSlot(), FolderData.class);
        if(!StringUtils.isNullOrEmpty(pathName))
            dataBatch.addOutputData(getFirstOutputSlot(), new FolderData(inputFolder.getPath().getParent().resolve(pathName)));
        else
            dataBatch.addOutputData(getFirstOutputSlot(), inputFolder);
    }

    /**
     * @return The subfolder
     */
    @JIPipeParameter("path-name")
    @JIPipeDocumentation(name = "Path name", description = "The file or folder name of the output paths. If empty, no renaming is applied.")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/algorithms/path.png")
    public String getPathName() {
        return pathName;
    }

    /**
     * Sets the subfolder
     *
     * @param pathName the subfolder
     */
    @JIPipeParameter("path-name")
    public void setPathName(String pathName) {
        this.pathName = pathName;
    }
}
