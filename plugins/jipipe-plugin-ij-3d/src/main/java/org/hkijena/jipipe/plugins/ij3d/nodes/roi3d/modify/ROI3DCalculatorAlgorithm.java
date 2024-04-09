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

package org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.modify;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.plugins.parameters.library.util.LogicalOperation;

@SetJIPipeDocumentation(name = "ROI 3D calculator", description = "Applies logical operations to the input ROI list. The logical operations are applied to " +
        "the whole list, meaning that an AND operation will create the union of all ROI in the list. If you want to apply the operation only to a sub-set of ROI," +
        " preprocess using a ROI splitter algorithm.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ROI3DListData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ROI3DListData.class, slotName = "Output", create = true)
public class ROI3DCalculatorAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private LogicalOperation operation = LogicalOperation.LogicalAnd;

    public ROI3DCalculatorAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ROI3DCalculatorAlgorithm(ROI3DCalculatorAlgorithm other) {
        super(other);
        this.operation = other.operation;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROI3DListData inputData = iterationStep.getInputData(getFirstInputSlot(), ROI3DListData.class, progressInfo);
        ROI3DListData outputData = new ROI3DListData(inputData);
        switch (operation) {
            case LogicalAnd:
                outputData.logicalAnd();
                break;
            case LogicalOr:
                outputData.logicalOr();
                break;
            case LogicalXor:
                outputData.logicalXor();
                break;
            default:
                throw new UnsupportedOperationException("Unsupported: " + operation);
        }
        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Operation", description = "The operation to apply on the list of ROI")
    @JIPipeParameter("operation")
    public LogicalOperation getOperation() {
        return operation;
    }

    @JIPipeParameter("operation")
    public void setOperation(LogicalOperation operation) {
        this.operation = operation;
    }
}
