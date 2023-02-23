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
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.util.List;

/**
 * Algorithm that splits the input data by a specified annotation
 */
// Algorithm metadata
@JIPipeDocumentation(name = "Filter by annotation", description = "Filters data based on the annotation value.")
@JIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Filter")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", autoCreate = true, inheritedSlot = "Input")
public class FilterByAnnotation extends JIPipeAlgorithm {

    private final CustomExpressionVariablesParameter customVariables;

    private AnnotationFilterExpression filter = new AnnotationFilterExpression();

    /**
     * @param info algorithm info
     */
    public FilterByAnnotation(JIPipeNodeInfo info) {
        super(info);
        this.customVariables = new CustomExpressionVariablesParameter(this);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public FilterByAnnotation(FilterByAnnotation other) {
        super(other);
        this.customVariables = new CustomExpressionVariablesParameter(other.customVariables, this);
        this.filter = new AnnotationFilterExpression(other.filter);
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {
        if (isPassThrough()) {
            for (JIPipeDataSlot outputSlot : getOutputSlots()) {
                outputSlot.addDataFromSlot(getFirstInputSlot(), progressInfo);
            }
            return;
        }
        JIPipeDataSlot inputSlot = getFirstInputSlot();
        for (int row = 0; row < inputSlot.getRowCount(); ++row) {
            List<JIPipeTextAnnotation> annotations = inputSlot.getTextAnnotations(row);
            String dataString = inputSlot.getData(row, JIPipeData.class, progressInfo).toString();
            AnnotationFilterExpression expression = filter;
            ExpressionVariables variables = new ExpressionVariables();
            customVariables.writeToVariables(variables, true, "custom.", true, "custom");
            if (expression.test(annotations, dataString, variables)) {
                getFirstOutputSlot().addData(inputSlot.getData(row, JIPipeData.class, progressInfo), annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
            }
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

    @JIPipeDocumentation(name = "Custom variables", description = "Here you can add parameters that will be included into the expressions as variables <code>custom.[key]</code>. Alternatively, you can access them via <code>GET_ITEM(\"custom\", \"[key]\")</code>.")
    @JIPipeParameter(value = "custom-variables", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-math-expression.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/insert-math-expression.png", persistence = JIPipeParameterPersistence.NestedCollection)
    public CustomExpressionVariablesParameter getCustomVariables() {
        return customVariables;
    }
}
