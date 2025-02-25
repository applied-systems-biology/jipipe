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
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;

import java.util.List;

/**
 * Algorithm that splits the input data by a specified annotation
 */
// Algorithm metadata
@SetJIPipeDocumentation(name = "Filter by annotation", description = "Filters data based on the annotation value.")
@ConfigureJIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Filter")
@AddJIPipeInputSlot(value = JIPipeData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = JIPipeData.class, name = "Output", create = true)
public class FilterByAnnotation extends JIPipeAlgorithm {

    private AnnotationFilterExpression filter = new AnnotationFilterExpression();
    private boolean enableFilter = true;

    /**
     * @param info algorithm info
     */
    public FilterByAnnotation(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public FilterByAnnotation(FilterByAnnotation other) {
        super(other);
        this.filter = new AnnotationFilterExpression(other.filter);
        this.enableFilter = other.enableFilter;
    }

    @SetJIPipeDocumentation(name = "Enable filter", description = "Determines if the filter is enabled")
    @JIPipeParameter(value = "enable-filter", important = true)
    public boolean isEnableFilter() {
        return enableFilter;
    }

    @JIPipeParameter("enable-filter")
    public void setEnableFilter(boolean enableFilter) {
        this.enableFilter = enableFilter;
    }

    @Override
    public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
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
            JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(this);
            variables.putCommonVariables(this);
            if (!enableFilter || expression.test(annotations, dataString, variables)) {
                getFirstOutputSlot().addData(inputSlot.getData(row, JIPipeData.class, progressInfo),
                        annotations,
                        JIPipeTextAnnotationMergeMode.Merge,
                        inputSlot.getDataAnnotations(row),
                        JIPipeDataAnnotationMergeMode.Merge,
                        inputSlot.getDataContext(row).branch(this),
                        progressInfo);
            }
        }
    }

    @SetJIPipeDocumentation(name = "Only keep data if", description = "The filter is an expression that should return a boolean value " +
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
