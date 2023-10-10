package org.hkijena.jipipe.extensions.utils.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeMultiDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMissingDataGeneratorAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

@JIPipeDocumentation(name = "Use placeholder for missing data", description = "Creates a data batch of the reference and data inputs. " +
        "If the data is missing for a reference, it is extracted from the items in the placeholder slot. ")
@JIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Reference", autoCreate = true)
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Data", autoCreate = true, optional = true)
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Placeholder", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Data", autoCreate = true)
public class UsePlaceholderForMissingDataAlgorithm extends JIPipeMissingDataGeneratorAlgorithm {

    private JIPipeTextAnnotationMergeMode placeholderAnnotationMergeStrategy = JIPipeTextAnnotationMergeMode.Merge;

    public UsePlaceholderForMissingDataAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public UsePlaceholderForMissingDataAlgorithm(UsePlaceholderForMissingDataAlgorithm other) {
        super(other);
        this.placeholderAnnotationMergeStrategy = other.placeholderAnnotationMergeStrategy;
    }

    @Override
    protected void runGenerator(JIPipeMultiDataBatch dataBatch, JIPipeInputDataSlot inputSlot, JIPipeOutputDataSlot outputSlot, JIPipeProgressInfo progressInfo) {
        if (dataBatch.getInputRows("Reference").isEmpty())
            return;
        JIPipeDataSlot placeholderSlot = getInputSlot("Placeholder");
        for (int row = 0; row < placeholderSlot.getRowCount(); row++) {
            dataBatch.addOutputData(outputSlot, placeholderSlot.getDataItemStore(row), placeholderSlot.getTextAnnotations(row), placeholderAnnotationMergeStrategy, progressInfo);
        }
    }

    @JIPipeDocumentation(name = "Merge placeholder annotations", description = "Determines how annotations of placeholder items are merged")
    @JIPipeParameter("placeholder-annotation-merge-strategy")
    public JIPipeTextAnnotationMergeMode getPlaceholderAnnotationMergeStrategy() {
        return placeholderAnnotationMergeStrategy;
    }

    @JIPipeParameter("placeholder-annotation-merge-strategy")
    public void setPlaceholderAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode placeholderAnnotationMergeStrategy) {
        this.placeholderAnnotationMergeStrategy = placeholderAnnotationMergeStrategy;
    }
}
