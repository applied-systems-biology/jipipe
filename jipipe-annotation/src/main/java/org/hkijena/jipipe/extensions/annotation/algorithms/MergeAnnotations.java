package org.hkijena.jipipe.extensions.annotation.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.categories.AnnotationNodeTypeCategory;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that merges the annotations of all inputs and outputs the data with the shared annotations
 */
@JIPipeDocumentation(name = "Merge annotations", description = "Merges the annotations of all incoming data and outputs the same data with those merged annotations.")
@JIPipeOrganization(nodeTypeCategory = AnnotationNodeTypeCategory.class)
public class MergeAnnotations extends JIPipeIteratingAlgorithm {
    /**
     * Creates a new instance
     *
     * @param info the info
     */
    public MergeAnnotations(JIPipeNodeInfo info) {
        super(info, new JIPipeIOSlotConfiguration());
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public MergeAnnotations(MergeAnnotations other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        for (JIPipeDataSlot inputSlot : getInputSlots()) {
            JIPipeDataSlot outputSlot = getOutputSlot(inputSlot.getName());
            dataBatch.addOutputData(outputSlot, dataBatch.getInputData(inputSlot, JIPipeData.class));
        }
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
