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
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.FileSystemNodeTypeCategory;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;

/**
 * Applies subfolder navigation to each input folder
 */
@JIPipeDocumentation(name = "File/folder name", description = "Extracts the file or folder name from incoming paths.")
@JIPipeOrganization(menuPath = "Extract", nodeTypeCategory = FileSystemNodeTypeCategory.class)


@JIPipeInputSlot(value = PathData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = PathData.class, slotName = "Output", autoCreate = true)


public class ExtractFileName extends JIPipeSimpleIteratingAlgorithm {


    /**
     * @param info Algorithm info
     */
    public ExtractFileName(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public ExtractFileName(ExtractFileName other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        PathData inputFolder = dataBatch.getInputData(getFirstInputSlot(), PathData.class, progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), new PathData(inputFolder.toPath().getFileName()), progressInfo);
    }
}
