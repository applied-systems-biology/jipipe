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
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Applies subfolder navigation to each input folder
 */
@JIPipeDocumentation(name = "Subfolders", description = "Goes to the specified subfolder")
@JIPipeOrganization(menuPath = "Navigate", algorithmCategory = JIPipeNodeCategory.FileSystem)

// Algorithm flow
@JIPipeInputSlot(value = FolderData.class, slotName = "Folders", autoCreate = true)
@JIPipeOutputSlot(value = FolderData.class, slotName = "Subfolders", autoCreate = true)

// Traits
public class NavigateSubFolder extends JIPipeSimpleIteratingAlgorithm {

    private String subFolder;

    /**
     * @param info Algorithm info
     */
    public NavigateSubFolder(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public NavigateSubFolder(NavigateSubFolder other) {
        super(other);
        this.subFolder = other.subFolder;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        FolderData inputFolder = dataBatch.getInputData("Folders", FolderData.class);
        dataBatch.addOutputData("Subfolders", new FolderData(inputFolder.getPath().resolve(subFolder)));
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        if (subFolder == null || subFolder.isEmpty())
            report.forCategory("Subfolder name").reportIsInvalid("Invalid subfolder name!",
                    "The subfolder name is empty!",
                    "Please enter a name.",
                    this);
    }

    /**
     * @return The subfolder
     */
    @JIPipeParameter("subfolder")
    @JIPipeDocumentation(name = "Subfolder name")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/folder.png")
    public String getSubFolder() {
        return subFolder;
    }

    /**
     * Sets the subfolder
     *
     * @param subFolder the subfolder
     */
    @JIPipeParameter("subfolder")
    public void setSubFolder(String subFolder) {
        this.subFolder = subFolder;
    }
}
