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
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.FileSystemNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;

import java.io.IOException;
import java.nio.file.Files;

/**
 * Applies subfolder navigation to each input folder
 */
@JIPipeDocumentation(name = "Create directory", description = "Creates directories in the filesystem according to the input paths. " +
        "If the path already exists, it will be silently skipped.")
@JIPipeOrganization(nodeTypeCategory = FileSystemNodeTypeCategory.class)


@JIPipeInputSlot(value = PathData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = FolderData.class, slotName = "Output", autoCreate = true)

// Traits
public class CreateDirectory extends JIPipeSimpleIteratingAlgorithm {

    private boolean withParents = true;

    /**
     * @param info Algorithm info
     */
    public CreateDirectory(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public CreateDirectory(CreateDirectory other) {
        super(other);
        this.withParents = other.withParents;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        FolderData inputFolder = dataBatch.getInputData(getFirstInputSlot(), FolderData.class, progressInfo);
        if (!Files.exists(inputFolder.getPath())) {
            try {
                if (withParents) {
                    Files.createDirectory(inputFolder.getPath());
                } else {
                    Files.createDirectories(inputFolder.getPath());
                }
            } catch (IOException e) {
                throw new UserFriendlyRuntimeException(e, "Could not create directory!",
                        getName(),
                        "There was an error creating the directory '" + inputFolder.getPath() + "'.",
                        "Please check if the path is correct and you have sufficient rights to create such path.");
            }
        }
        dataBatch.addOutputData("Subfolders", inputFolder, progressInfo);
    }

    /**
     * @return The subfolder
     */
    @JIPipeParameter("with-parents")
    @JIPipeDocumentation(name = "With parents", description = "If enabled, parent folders are created if necessary. Otherwise an error is thrown.")
    public boolean isWithParents() {
        return withParents;
    }

    /**
     * Sets the subfolder
     *
     * @param withParents the subfolder
     */
    @JIPipeParameter("with-parents")
    public void setWithParents(boolean withParents) {
        this.withParents = withParents;
    }
}
