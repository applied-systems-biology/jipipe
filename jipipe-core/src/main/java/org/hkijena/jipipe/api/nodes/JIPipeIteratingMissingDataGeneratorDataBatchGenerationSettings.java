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

package org.hkijena.jipipe.api.nodes;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.parameters.generators.IntegerRange;
import org.hkijena.jipipe.extensions.parameters.generators.OptionalIntegerRange;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.ResourceUtils;

/**
 * Groups data batch generation settings
 */
public class JIPipeIteratingMissingDataGeneratorDataBatchGenerationSettings implements JIPipeParameterCollection {
    private final EventBus eventBus = new EventBus();
    private JIPipeColumMatching columnMatching = JIPipeColumMatching.PrefixHashUnion;
    private StringQueryExpression customColumns = new StringQueryExpression();
    private OptionalIntegerRange limit = new OptionalIntegerRange(new IntegerRange("0-9"), false);
    private JIPipeAnnotationMergeStrategy annotationMergeStrategy = JIPipeAnnotationMergeStrategy.Merge;
    private JIPipeAnnotationMatchingMethod annotationMatchingMethod = JIPipeAnnotationMatchingMethod.ExactMatch;
    private DefaultExpressionParameter customAnnotationMatching = new DefaultExpressionParameter("exact_match_results");

    public JIPipeIteratingMissingDataGeneratorDataBatchGenerationSettings() {
    }

    public JIPipeIteratingMissingDataGeneratorDataBatchGenerationSettings(JIPipeIteratingMissingDataGeneratorDataBatchGenerationSettings other) {
        this.columnMatching = other.columnMatching;
        this.customColumns = new StringQueryExpression(other.customColumns);
        this.limit = new OptionalIntegerRange(other.limit);
        this.annotationMergeStrategy = other.annotationMergeStrategy;
        this.annotationMatchingMethod = other.annotationMatchingMethod;
        this.customAnnotationMatching = new DefaultExpressionParameter(other.customAnnotationMatching);
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "Annotation matching method", description = "Allows to customize when two annotation sets are considered as equal. " +
            "By default, non-empty annotation values should match exactly. You can also use a custom expression, instead.")
    @JIPipeParameter(value = "annotation-matching-method", uiOrder = 1999)
    public JIPipeAnnotationMatchingMethod getAnnotationMatchingMethod() {
        return annotationMatchingMethod;
    }

    @JIPipeParameter("annotation-matching-method")
    public void setAnnotationMatchingMethod(JIPipeAnnotationMatchingMethod annotationMatchingMethod) {
        this.annotationMatchingMethod = annotationMatchingMethod;
        triggerParameterUIChange();
    }

    @JIPipeDocumentation(name = "Custom annotation matching method", description = "Expression used to compare two annotation sets.")
    @ExpressionParameterSettings(variableSource = JIPipeCustomAnnotationMatchingExpressionVariables.class)
    @JIPipeParameter(value = "custom-annotation-matching", uiOrder = 2100)
    public DefaultExpressionParameter getCustomAnnotationMatching() {
        return customAnnotationMatching;
    }

    @JIPipeParameter("custom-annotation-matching")
    public void setCustomAnnotationMatching(DefaultExpressionParameter customAnnotationMatching) {
        this.customAnnotationMatching = customAnnotationMatching;
    }

    @JIPipeDocumentation(name = "Grouping method", description = "Algorithms with multiple inputs require to match the incoming data " +
            "to data sets. This allows you to determine how interesting data annotation columns are extracted from the incoming data. " +
            "Union matches using the union of annotation columns. Intersection intersects the sets of available columns. You can also" +
            " customize which columns should be included or excluded.")
    @JIPipeParameter(value = "column-matching", uiOrder = 999)
    public JIPipeColumMatching getColumnMatching() {
        return columnMatching;
    }

    @JIPipeParameter("column-matching")
    public void setColumnMatching(JIPipeColumMatching columnMatching) {
        boolean needsTriggerStructureChange = columnMatching == JIPipeColumMatching.Custom || this.columnMatching == JIPipeColumMatching.Custom;
        this.columnMatching = columnMatching;
        if(needsTriggerStructureChange)
            triggerParameterUIChange();
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if(access.getSource() == this && "custom-matched-columns-expression".equals(access.getKey())) {
            if(getColumnMatching() != JIPipeColumMatching.Custom)
                return false;
        }
        if(access.getSource() == this && "custom-annotation-matching".equals(access.getKey())) {
            if(getAnnotationMatchingMethod() != JIPipeAnnotationMatchingMethod.CustomExpression)
                return false;
        }
        return JIPipeParameterCollection.super.isParameterUIVisible(tree, access);
    }

    @JIPipeDocumentation(name = "Custom grouping columns", description = "Only used if 'Grouping method' is set to 'Custom'. " +
            "Determines which annotation columns are referred to group data sets. " + StringQueryExpression.DOCUMENTATION_DESCRIPTION)
    @JIPipeParameter(value = "custom-matched-columns-expression", uiOrder = 999)
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/annotation.png")
    public StringQueryExpression getCustomColumns() {
        if (customColumns == null)
            customColumns = new StringQueryExpression();
        return customColumns;
    }

    @JIPipeParameter(value = "custom-matched-columns-expression")
    public void setCustomColumns(StringQueryExpression customColumns) {
        this.customColumns = customColumns;
    }

    @JIPipeDocumentation(name = "Limit", description = "Limits which data batches are generated. The first index is zero.\n" + IntegerRange.DOCUMENTATION_DESCRIPTION)
    @JIPipeParameter(value = "limit")
    public OptionalIntegerRange getLimit() {
        return limit;
    }

    @JIPipeParameter("limit")
    public void setLimit(OptionalIntegerRange limit) {
        this.limit = limit;
    }

    @JIPipeDocumentation(name = "Merge same annotation values", description = "Determines which strategy is applied if data sets that " +
            "define different values for the same annotation columns are encountered.")
    @JIPipeParameter("annotation-merge-strategy")
    public JIPipeAnnotationMergeStrategy getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeAnnotationMergeStrategy annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }
}