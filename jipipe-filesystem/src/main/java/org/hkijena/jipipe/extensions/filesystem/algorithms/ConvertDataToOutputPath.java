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
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.categories.ConverterNodeTypeCategory;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Applies subfolder navigation to each input folder
 */
@JIPipeDocumentation(name = "Get output path", description = "Obtains the output path of the current run where the receiving data is stored.")
@JIPipeOrganization(nodeTypeCategory = ConverterNodeTypeCategory.class)

// Algorithm flow
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Data", autoCreate = true)
@JIPipeOutputSlot(value = FolderData.class, slotName = "Output path", autoCreate = true)

// Traits
public class ConvertDataToOutputPath extends JIPipeSimpleIteratingAlgorithm {

    /**
     * @param info Algorithm info
     */
    public ConvertDataToOutputPath(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public ConvertDataToOutputPath(ConvertDataToOutputPath other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        JIPipeDataSlot sourceSlot = getGraph().getSourceSlot(getFirstInputSlot());
        dataBatch.addOutputData(getFirstOutputSlot(), new FolderData(sourceSlot.getStoragePath()));
    }

}
