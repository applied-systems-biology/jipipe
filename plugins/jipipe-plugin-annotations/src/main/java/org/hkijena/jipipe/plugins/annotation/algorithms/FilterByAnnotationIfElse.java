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

package org.hkijena.jipipe.plugins.annotation.algorithms;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.annotations.AnnotationsEditFunction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;

/**
 * Algorithm that splits the input data by a specified annotation
 */
// Algorithm metadata
@SetJIPipeDocumentation(name = "Filter by annotation (If else)", description = "Filters the data by an annotation condition and splits it into an if output with the matching data and an else output with all data that does not match.")
@ConfigureJIPipeNode(function = AnnotationsEditFunction.class)
@AddJIPipeInputSlot(value = JIPipeData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = JIPipeData.class, name = "Matched", create = true, description = "Data that matched the filter")
@AddJIPipeOutputSlot(value = JIPipeData.class, name = "Unmatched", create = true, description = "Data that does not match the filter")
public class FilterByAnnotationIfElse extends JIPipeSimpleIteratingAlgorithm {
    private AnnotationFilterExpression filter = new AnnotationFilterExpression();

    /**
     * @param info algorithm info
     */
    public FilterByAnnotationIfElse(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public FilterByAnnotationIfElse(FilterByAnnotationIfElse other) {
        super(other);
        this.filter = new AnnotationFilterExpression(other.filter);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);

        JIPipeData data = iterationStep.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo);
        if (filter.test(iterationStep.getMergedTextAnnotations().values(), data.toString(), variables)) {
            iterationStep.addOutputData("Matched", data, progressInfo);
        } else {
            iterationStep.addOutputData("Unmatched", data, progressInfo);
        }
    }

    @SetJIPipeDocumentation(name = "Only match data if", description = "The filter is an expression that should return a boolean value " +
            "that indicates whether a data item should be put into the corresponding output." +
            "Annotation values are available as variables. If an annotation has spaces special characters, use $ to access its value. Examples: <pre>" +
            "#Dataset CONTAINS \"Raw\" AND condition EQUALS \"mock\"</pre>" +
            "<pre>TO_NUMBER($\"my column\") < 10</pre>")
    @JIPipeParameter("filter")
    public AnnotationFilterExpression getFilter() {
        return filter;
    }

    @JIPipeParameter("filter")
    public void setFilter(AnnotationFilterExpression filter) {
        this.filter = filter;
    }
}
