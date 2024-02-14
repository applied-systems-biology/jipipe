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
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "Remove array annotations", description = "Removes annotations or annotation columns that contain array values. " +
        "Array values are valid JSON arrays encased in [ and ].")
@JIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Filter")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", autoCreate = true)
public class RemoveArrayAnnotationsAlgorithm extends JIPipeParameterSlotAlgorithm {

    private boolean removeColumn = false;
    private StringQueryExpression annotationNameFilter = new StringQueryExpression();

    public RemoveArrayAnnotationsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public RemoveArrayAnnotationsAlgorithm(RemoveArrayAnnotationsAlgorithm other) {
        super(other);
        this.removeColumn = other.removeColumn;
        this.annotationNameFilter = new StringQueryExpression(other.annotationNameFilter);
    }

    @Override
    public void runParameterSet(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {
        if (removeColumn) {
            Set<String> toRemove = annotationNameFilter.queryAll(getFirstInputSlot().getTextAnnotationColumnNames(), new JIPipeExpressionVariablesMap()).stream().filter(columnName -> {
                for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
                    JIPipeTextAnnotation existing = getFirstInputSlot().getTextAnnotationOr(row, columnName, null);
                    if (existing != null && existing.isArray())
                        return true;
                }
                return false;
            }).collect(Collectors.toSet());
            getFirstOutputSlot().addDataFromSlot(getFirstInputSlot(), progressInfo);
            for (String name : toRemove) {
                getFirstOutputSlot().removeAllAnnotationsFromData(name);
            }
        } else {
            for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
                List<JIPipeTextAnnotation> annotations = getFirstInputSlot().getTextAnnotations(row);
                annotations.removeIf(annotation -> annotationNameFilter.test(annotation.getName()) && annotation.isArray());
                getFirstOutputSlot().addData(getFirstInputSlot().getDataItemStore(row),
                        annotations,
                        JIPipeTextAnnotationMergeMode.Merge,
                        getFirstInputSlot().getDataAnnotations(row),
                        JIPipeDataAnnotationMergeMode.OverwriteExisting,
                        getFirstInputSlot().getDataContext(row).branch(this),
                        progressInfo);
            }
        }
    }

    @JIPipeDocumentation(name = "Remove whole columns", description = "If true, a whole annotation column is removed if one of the values is an array.")
    @JIPipeParameter("remove-column")
    public boolean isRemoveColumn() {
        return removeColumn;
    }

    @JIPipeParameter("remove-column")
    public void setRemoveColumn(boolean removeColumn) {
        this.removeColumn = removeColumn;
    }

    @JIPipeDocumentation(name = "Annotation column filter", description = "Allows to filter for specific annotation columns. ")
    @JIPipeParameter("annotation-name-filter")
    public StringQueryExpression getAnnotationNameFilter() {
        return annotationNameFilter;
    }

    @JIPipeParameter("annotation-name-filter")
    public void setAnnotationNameFilter(StringQueryExpression annotationNameFilter) {
        this.annotationNameFilter = annotationNameFilter;
    }
}
