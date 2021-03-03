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

package org.hkijena.jipipe.extensions.utils.algorithms;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;

/**
 * Algorithm that converts any type to any other type
 */
@JIPipeDocumentation(name = "Convert", description = "Converts the input data to the output data. This node is " +
        "only required for special occasions, as JIPipe applies an automated conversion (blue edges).")
@JIPipeOrganization(nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
public class ConverterAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public ConverterAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .restrictInputSlotCount(1)
                .restrictOutputSlotCount(1).build());
    }

    public ConverterAlgorithm(JIPipeSimpleIteratingAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        dataBatch.addOutputData(getFirstOutputSlot(), dataBatch.getInputData(getFirstInputSlot(), getFirstInputSlot().getAcceptedDataType(), progressInfo), progressInfo);
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        super.reportValidity(report);
        if (!JIPipe.getDataTypes().isConvertible(getFirstInputSlot().getAcceptedDataType(), getFirstOutputSlot().getAcceptedDataType())) {
            report.forCategory("Data").reportIsInvalid("Cannot convert input to output data!",
                    "JIPipe is not aware of any conversion from the input type to the output type.",
                    "Please select compatible types.",
                    this);
        }
    }
}
