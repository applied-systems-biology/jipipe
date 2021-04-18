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
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameters;
import org.hkijena.jipipe.extensions.parameters.expressions.StringQueryExpression;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "Remove NA annotation columns", description = "Removes annotation columns that have missing values.")
@JIPipeOrganization(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Filter")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", autoCreate = true, inheritedSlot = "Input")
public class RemoveNAAnnotationColumnsAlgorithm extends JIPipeParameterSlotAlgorithm {

    private StringQueryExpression annotationNameFilter = new StringQueryExpression();

    public RemoveNAAnnotationColumnsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public RemoveNAAnnotationColumnsAlgorithm(RemoveNAAnnotationColumnsAlgorithm other) {
        super(other);
        this.annotationNameFilter = new StringQueryExpression(other.annotationNameFilter);
    }

    @Override
    public void runParameterSet(JIPipeProgressInfo progressInfo, List<JIPipeAnnotation> parameterAnnotations) {
        Set<String> toRemove = annotationNameFilter.queryAll(getFirstInputSlot().getAnnotationColumns(), new ExpressionParameters()).stream().filter(columnName -> {
            for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
                JIPipeAnnotation existing = getFirstInputSlot().getAnnotationOr(row, columnName, null);
                if (existing == null)
                    return true;
            }
            return false;
        }).collect(Collectors.toSet());
        getFirstOutputSlot().addData(getFirstInputSlot(), progressInfo);
        for (String name : toRemove) {
            getFirstOutputSlot().removeAllAnnotationsFromData(name);
        }
    }

    @JIPipeDocumentation(name = "Annotation column filter", description = "Allows to filter for specific annotation columns. " + StringQueryExpression.DOCUMENTATION_DESCRIPTION)
    @JIPipeParameter("annotation-name-filter")
    public StringQueryExpression getAnnotationNameFilter() {
        return annotationNameFilter;
    }

    @JIPipeParameter("annotation-name-filter")
    public void setAnnotationNameFilter(StringQueryExpression annotationNameFilter) {
        this.annotationNameFilter = annotationNameFilter;
    }
}
