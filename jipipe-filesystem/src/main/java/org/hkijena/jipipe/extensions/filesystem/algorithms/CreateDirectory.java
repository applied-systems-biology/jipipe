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

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.FileSystemNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;

import java.io.IOException;
import java.nio.file.Files;

/**
 * Applies subfolder navigation to each input folder
 */
@SetJIPipeDocumentation(name = "Create directory", description = "Creates directories in the filesystem according to the input paths. " +
        "If the path already exists, it will be silently skipped.")
@ConfigureJIPipeNode(nodeTypeCategory = FileSystemNodeTypeCategory.class)


@AddJIPipeInputSlot(value = PathData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = FolderData.class, slotName = "Output", create = true)


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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        FolderData inputFolder = iterationStep.getInputData(getFirstInputSlot(), FolderData.class, progressInfo);
        if (!Files.exists(inputFolder.toPath())) {
            try {
                if (withParents) {
                    Files.createDirectory(inputFolder.toPath());
                } else {
                    Files.createDirectories(inputFolder.toPath());
                }
            } catch (IOException e) {
                throw new JIPipeValidationRuntimeException(e, "Could not create directory!",
                        "There was an error creating the directory '" + inputFolder.getPath() + "'.",
                        "Please check if the path is correct and you have sufficient rights to create such path.");
            }
        }
        iterationStep.addOutputData(getFirstOutputSlot(), inputFolder, progressInfo);
    }

    /**
     * @return The subfolder
     */
    @JIPipeParameter("with-parents")
    @SetJIPipeDocumentation(name = "With parents", description = "If enabled, parent folders are created if necessary. Otherwise an error is thrown.")
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
