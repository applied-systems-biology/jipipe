/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.annotation.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterSerializationMode;
import org.hkijena.jipipe.extensions.expressions.custom.JIPipeCustomExpressionVariablesParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.utils.ResourceUtils;

/**
 * Algorithm that splits the input data by a specified annotation
 */
// Algorithm metadata
@JIPipeDocumentation(name = "Filter by annotation (If else)", description = "Filters data based on the annotation value. Has two outputs, one that contains the data that matches the filter, and " +
        "another output that contains all data that does not match the filter")
@JIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Filter")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Matched", autoCreate = true, description = "Data that matched the filter")
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Unmatched", autoCreate = true, description = "Data that does not match the filter")
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        getDefaultCustomExpressionVariables().writeToVariables(variables);
        JIPipeData data = iterationStep.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo);
        if (filter.test(iterationStep.getMergedTextAnnotations().values(), data.toString(), variables)) {
            iterationStep.addOutputData("Matched", data, progressInfo);
        } else {
            iterationStep.addOutputData("Unmatched", data, progressInfo);
        }
    }

    @JIPipeDocumentation(name = "Filter", description = "The filter is an expression that should return a boolean value " +
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

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
    }
}
