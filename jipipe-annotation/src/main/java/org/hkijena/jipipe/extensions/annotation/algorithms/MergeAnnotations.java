package org.hkijena.jipipe.extensions.annotation.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that merges the annotations of all inputs and outputs the data with the shared annotations
 */
@JIPipeDocumentation(name = "Merge annotations", description = "Merges the annotations of all incoming data and outputs the same data with those merged annotations.")
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.Annotation)
public class MergeAnnotations extends JIPipeIteratingAlgorithm {
    /**
     * Creates a new instance
     * @param info the info
     */
    public MergeAnnotations(JIPipeNodeInfo info) {
        super(info, new JIPipeIOSlotConfiguration());
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        for (JIPipeDataSlot inputSlot : getInputSlots()) {
            JIPipeDataSlot outputSlot = getOutputSlot(inputSlot.getName());
            dataBatch.addOutputData(outputSlot, dataBatch.getInputData(inputSlot, JIPipeData.class));
        }
    }

    /**
     * Creates a copy
     * @param other the original
     */
    public MergeAnnotations(MergeAnnotations other) {
        super(other);
    }

    @Override
    protected boolean canPassThrough() {
        return true;
    }

    @Override
    protected void runPassThrough() {
        for (JIPipeDataSlot inputSlot : getInputSlots()) {
            JIPipeDataSlot outputSlot = getOutputSlot(inputSlot.getName());
            outputSlot.copyFrom(inputSlot);
        }
    }
}
