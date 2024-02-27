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

package org.hkijena.jipipe.extensions.annotation.algorithms;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;

@SetJIPipeDocumentation(name = "Remove data annotations", description = "Removes data annotations by their column name. Data annotations are different to string annotations and contain data instead of strings.")
@ConfigureJIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Remove")
@AddJIPipeInputSlot(value = JIPipeData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", create = true)
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        for (String name : ImmutableList.copyOf(iterationStep.getMergedDataAnnotations().keySet())) {
            if (nameFilter.test(name)) {
                iterationStep.getMergedDataAnnotations().remove(name);
            }
        }
        iterationStep.addOutputData(getFirstOutputSlot(), iterationStep.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Name filter", description = "Determines which data annotations are removed. ")
    @JIPipeParameter("name-filter")
    public StringQueryExpression getNameFilter() {
        return nameFilter;
    }

    @JIPipeParameter("name-filter")
    public void setNameFilter(StringQueryExpression nameFilter) {
        this.nameFilter = nameFilter;
    }
}
