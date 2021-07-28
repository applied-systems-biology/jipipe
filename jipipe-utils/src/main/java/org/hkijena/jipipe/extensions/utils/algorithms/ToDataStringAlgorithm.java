package org.hkijena.jipipe.extensions.utils.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.strings.StringData;

@JIPipeDocumentation(name = "To data string", description = "Converts the incoming data into a data object containing the string representation of the data")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = StringData.class, slotName = "Output", autoCreate = true)
@JIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        JIPipeData data = dataBatch.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), new StringData(detailedDataString ? data.toDetailedString() : data.toString()), progressInfo);
    }

    @JIPipeDocumentation(name = "Prefer detailed string", description = "If enabled, a more detailed string information (if available) is used instead of the string shown in the UI.")
    @JIPipeParameter("detailed-data-string")
    public boolean isDetailedDataString() {
        return detailedDataString;
    }

    @JIPipeParameter("detailed-data-string")
    public void setDetailedDataString(boolean detailedDataString) {
        this.detailedDataString = detailedDataString;
    }
}