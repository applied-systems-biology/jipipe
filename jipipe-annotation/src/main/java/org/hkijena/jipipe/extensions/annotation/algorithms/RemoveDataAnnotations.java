package org.hkijena.jipipe.extensions.annotation.algorithms;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeSingleDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;

@JIPipeDocumentation(name = "Remove data annotations", description = "Removes data annotations by their column name. Data annotations are different to string annotations and contain data instead of strings.")
@JIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Remove")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", autoCreate = true)
public class RemoveDataAnnotations extends JIPipeSimpleIteratingAlgorithm {


    private StringQueryExpression nameFilter = new StringQueryExpression();

    public RemoveDataAnnotations(JIPipeNodeInfo info) {
        super(info);
    }

    public RemoveDataAnnotations(RemoveDataAnnotations other) {
        super(other);
        nameFilter = new StringQueryExpression(other.nameFilter);
    }

    @Override
    protected void runIteration(JIPipeSingleDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        for (String name : ImmutableList.copyOf(dataBatch.getMergedDataAnnotations().keySet())) {
            if (nameFilter.test(name)) {
                dataBatch.getMergedDataAnnotations().remove(name);
            }
        }
        dataBatch.addOutputData(getFirstOutputSlot(), dataBatch.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo), progressInfo);
    }

    @JIPipeDocumentation(name = "Name filter", description = "Determines which data annotations are removed. ")
    @JIPipeParameter("name-filter")
    public StringQueryExpression getNameFilter() {
        return nameFilter;
    }

    @JIPipeParameter("name-filter")
    public void setNameFilter(StringQueryExpression nameFilter) {
        this.nameFilter = nameFilter;
    }
}
