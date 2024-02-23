package org.hkijena.jipipe.extensions.utils.algorithms;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.strings.StringData;

@SetJIPipeDocumentation(name = "To data string", description = "Converts the incoming data into a data object containing the string representation of the data")
@AddJIPipeInputSlot(value = JIPipeData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = StringData.class, slotName = "Output", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Convert")
public class ToDataStringAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private boolean detailedDataString = true;

    public ToDataStringAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ToDataStringAlgorithm(ToDataStringAlgorithm other) {
        super(other);
        this.detailedDataString = other.detailedDataString;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        JIPipeData data = iterationStep.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new StringData(detailedDataString ? data.toDetailedString() : data.toString()), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Prefer detailed string", description = "If enabled, a more detailed string information (if available) is used instead of the string shown in the UI.")
    @JIPipeParameter("detailed-data-string")
    public boolean isDetailedDataString() {
        return detailedDataString;
    }

    @JIPipeParameter("detailed-data-string")
    public void setDetailedDataString(boolean detailedDataString) {
        this.detailedDataString = detailedDataString;
    }
}
