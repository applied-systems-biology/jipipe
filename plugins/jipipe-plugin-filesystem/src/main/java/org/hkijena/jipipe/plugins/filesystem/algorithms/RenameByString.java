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
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

/**
 * Applies subfolder navigation to each input folder
 */
@SetJIPipeDocumentation(name = "Rename path", description = "Sets the file or folder name of each path to the specified string.")
@ConfigureJIPipeNode(menuPath = "Modify", nodeTypeCategory = FileSystemNodeTypeCategory.class)


@AddJIPipeInputSlot(value = PathData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = PathData.class, slotName = "Output", create = true)


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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        FolderData inputFolder = iterationStep.getInputData(getFirstInputSlot(), FolderData.class, progressInfo);
        if (!StringUtils.isNullOrEmpty(pathName))
            iterationStep.addOutputData(getFirstOutputSlot(), new FolderData(inputFolder.toPath().getParent().resolve(pathName)), progressInfo);
        else
            iterationStep.addOutputData(getFirstOutputSlot(), inputFolder, progressInfo);
    }

    /**
     * @return The subfolder
     */
    @JIPipeParameter("path-name")
    @SetJIPipeDocumentation(name = "Path name", description = "The file or folder name of the output paths. If empty, no renaming is applied.")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/path.png")
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
