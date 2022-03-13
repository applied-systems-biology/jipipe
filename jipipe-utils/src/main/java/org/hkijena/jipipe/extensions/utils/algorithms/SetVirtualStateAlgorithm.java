package org.hkijena.jipipe.extensions.utils.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeVirtualData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

import java.util.ArrayList;

@JIPipeDocumentation(name = "Set virtual state", description = "Pushes data into the 'reduce memory' cache or pull the data from it.")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
@JIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
public class SetVirtualStateAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private VirtualState targetState = VirtualState.Unchanged;

    public SetVirtualStateAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SetVirtualStateAlgorithm(SetVirtualStateAlgorithm other) {
        super(other);
        this.targetState = other.targetState;
    }

    @JIPipeDocumentation(name = "Data storage location", description = "Determines where the data should be stored.")
    @JIPipeParameter(value = "target-state", important = true)
    public VirtualState getTargetState() {
        return targetState;
    }

    @JIPipeParameter("target-state")
    public void setTargetState(VirtualState targetState) {
        this.targetState = targetState;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        JIPipeVirtualData virtualData = getFirstInputSlot().getVirtualData(dataBatch.getInputRow(getFirstInputSlot()));
        switch (targetState) {
            case StoreToMemory:
                virtualData.makeNonVirtual(progressInfo, true);
                break;
            case StoreToDisk:
                virtualData.makeVirtual(progressInfo, false);
                break;
        }
        dataBatch.addOutputData(getFirstOutputSlot(),
                virtualData,
                new ArrayList<>(),
                JIPipeTextAnnotationMergeMode.OverwriteExisting,
                new ArrayList<>(),
                JIPipeDataAnnotationMergeMode.OverwriteExisting,
                progressInfo);
    }

    public enum VirtualState {
        Unchanged,
        StoreToDisk,
        StoreToMemory;


        @Override
        public String toString() {
            switch (this) {
                case StoreToDisk:
                    return "Store to disk";
                case StoreToMemory:
                    return "Store to memory";
                default:
                    return name();
            }
        }
    }
}
