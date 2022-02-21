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

package org.hkijena.jipipe.extensions.annotation.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;

/**
 * Merges the input slot tables into one data slot
 */
@JIPipeDocumentation(name = "Merge data slots", description = "Merges the data rows from all input slots into one output slot")
@JIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
@JIPipeInputSlot(JIPipeData.class)
public class MergeDataSlots extends JIPipeAlgorithm {

    /**
     * @param info the algorithm info
     */
    public MergeDataSlots(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .restrictOutputSlotCount(1)
                .build());
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public MergeDataSlots(MergeDataSlots other) {
        super(other);
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {
        JIPipeDataSlot outputSlot = getFirstOutputSlot();
        for (JIPipeDataSlot inputSlot : getInputSlots()) {
            outputSlot.addData(inputSlot, progressInfo);
        }
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        if (getOutputSlots().isEmpty()) {
            report.reportIsInvalid("No output slot!",
                    "The result is put into the output slot.",
                    "Please add an output slot that is compatible to the input data.", this);
        } else {
            JIPipeDataSlot outputSlot = getFirstOutputSlot();
            for (JIPipeDataSlot inputSlot : getInputSlots()) {
                if (!outputSlot.getAcceptedDataType().isAssignableFrom(inputSlot.getAcceptedDataType())) {
                    report.resolve("Slots").resolve(inputSlot.getName())
                            .reportIsInvalid("Input slot is incompatible!",
                                    "Output data must fit to the input data.",
                                    "Please add an output slot that is compatible to the input data.", this);
                }
            }
        }
    }
}