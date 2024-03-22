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

package org.hkijena.jipipe.extensions.utils.algorithms;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMissingDataGeneratorAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

@SetJIPipeDocumentation(name = "Use placeholder for missing data", description = "Creates a data batch of the reference and data inputs. " +
        "If the data is missing for a reference, it is extracted from the items in the placeholder slot. ")
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
@AddJIPipeInputSlot(value = JIPipeData.class, slotName = "Reference", create = true)
@AddJIPipeInputSlot(value = JIPipeData.class, slotName = "Data", create = true, optional = true)
@AddJIPipeInputSlot(value = JIPipeData.class, slotName = "Placeholder", create = true)
@AddJIPipeOutputSlot(value = JIPipeData.class, slotName = "Data", create = true)
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
    protected void runGenerator(JIPipeMultiIterationStep iterationStep, JIPipeInputDataSlot inputSlot, JIPipeOutputDataSlot outputSlot, JIPipeProgressInfo progressInfo) {
        if (iterationStep.getInputRows("Reference").isEmpty())
            return;
        JIPipeDataSlot placeholderSlot = getInputSlot("Placeholder");
        for (int row = 0; row < placeholderSlot.getRowCount(); row++) {
            iterationStep.addOutputData(outputSlot, placeholderSlot.getDataItemStore(row), placeholderSlot.getTextAnnotations(row), placeholderAnnotationMergeStrategy, progressInfo);
        }
    }

    @SetJIPipeDocumentation(name = "Merge placeholder annotations", description = "Determines how annotations of placeholder items are merged")
    @JIPipeParameter("placeholder-annotation-merge-strategy")
    public JIPipeTextAnnotationMergeMode getPlaceholderAnnotationMergeStrategy() {
        return placeholderAnnotationMergeStrategy;
    }

    @JIPipeParameter("placeholder-annotation-merge-strategy")
    public void setPlaceholderAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode placeholderAnnotationMergeStrategy) {
        this.placeholderAnnotationMergeStrategy = placeholderAnnotationMergeStrategy;
    }
}
