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

package org.hkijena.jipipe.api.nodes.algorithm;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.JIPipeColumMatching;
import org.hkijena.jipipe.api.nodes.JIPipeCustomAnnotationMatchingExpressionVariables;
import org.hkijena.jipipe.api.nodes.JIPipeTextAnnotationMatchingMethod;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationStepGenerationSettings;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.StringQueryExpression;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerRange;
import org.hkijena.jipipe.plugins.parameters.library.primitives.ranges.IntegerRange;
import org.hkijena.jipipe.utils.ResourceUtils;

public class JIPipeMergingAlgorithmIterationStepGenerationSettings extends AbstractJIPipeParameterCollection implements JIPipeIterationStepGenerationSettings {
    private JIPipeColumMatching columnMatching = JIPipeColumMatching.PrefixHashUnion;
    private boolean skipIncompleteDataSets = false;
    private StringQueryExpression customColumns = new StringQueryExpression();
    private JIPipeTextAnnotationMergeMode annotationMergeStrategy = JIPipeTextAnnotationMergeMode.Merge;
    private OptionalIntegerRange limit = new OptionalIntegerRange(new IntegerRange("0-9"), false);
    private JIPipeTextAnnotationMatchingMethod annotationMatchingMethod = JIPipeTextAnnotationMatchingMethod.ExactMatch;
    private JIPipeExpressionParameter customAnnotationMatching = new JIPipeExpressionParameter("exact_match_results");
    private JIPipeDataAnnotationMergeMode dataAnnotationMergeStrategy = JIPipeDataAnnotationMergeMode.MergeTables;
    private boolean forceFlowGraphSolver = false;
    private boolean forceNAIsAny = false;

    public JIPipeMergingAlgorithmIterationStepGenerationSettings() {
    }

    public JIPipeMergingAlgorithmIterationStepGenerationSettings(JIPipeMergingAlgorithmIterationStepGenerationSettings other) {
        this.columnMatching = other.columnMatching;
        this.skipIncompleteDataSets = other.skipIncompleteDataSets;
        this.customColumns = new StringQueryExpression(other.customColumns);
        this.annotationMergeStrategy = other.annotationMergeStrategy;
        this.limit = new OptionalIntegerRange(other.limit);
        this.annotationMatchingMethod = other.annotationMatchingMethod;
        this.customAnnotationMatching = new JIPipeExpressionParameter(other.customAnnotationMatching);
        this.dataAnnotationMergeStrategy = other.dataAnnotationMergeStrategy;
        this.forceFlowGraphSolver = other.forceFlowGraphSolver;
        this.forceNAIsAny = other.forceNAIsAny;
    }

    public JIPipeMergingAlgorithmIterationStepGenerationSettings(JIPipeIteratingAlgorithmIterationStepGenerationSettings other) {
        this.columnMatching = other.getColumnMatching();
        this.skipIncompleteDataSets = other.isSkipIncompleteDataSets();
        this.customColumns = new StringQueryExpression(other.getCustomColumns());
        this.annotationMergeStrategy = other.getAnnotationMergeStrategy();
        this.limit = new OptionalIntegerRange(other.getLimit());
        this.annotationMatchingMethod = other.getAnnotationMatchingMethod();
        this.customAnnotationMatching = new JIPipeExpressionParameter(other.getCustomAnnotationMatching());
        this.dataAnnotationMergeStrategy = other.getDataAnnotationMergeStrategy();
        this.forceFlowGraphSolver = other.isForceFlowGraphSolver();
        this.forceNAIsAny = false;
    }

    @SetJIPipeDocumentation(name = "Force NA is ANY (if available)", description = "If enabled, missing annotations are considered as ANY (and thus merged with other data) even if there is only one input. " +
            "Currently only works for the dictionary solver.")
    @JIPipeParameter("force-na-is-any")
    @JsonGetter("force-na-is-any")
    public boolean isForceNAIsAny() {
        return forceNAIsAny;
    }

    @JIPipeParameter("force-na-is-any")
    @JsonSetter("force-na-is-any")
    public void setForceNAIsAny(boolean forceNAIsAny) {
        this.forceNAIsAny = forceNAIsAny;
    }

    @SetJIPipeDocumentation(name = "Force flow graph solver", description = "If enabled, disable the faster dictionary-based solver. Use this if you experience unexpected behavior.")
    @JIPipeParameter("force-flow-graph-solver")
    @JsonGetter("force-flow-graph-solver")
    public boolean isForceFlowGraphSolver() {
        return forceFlowGraphSolver;
    }

    @JIPipeParameter("force-flow-graph-solver")
    @JsonSetter("force-flow-graph-solver")
    public void setForceFlowGraphSolver(boolean forceFlowGraphSolver) {
        this.forceFlowGraphSolver = forceFlowGraphSolver;
    }

    @SetJIPipeDocumentation(name = "Annotation matching method", description = "Allows to customize when two annotation sets are considered as equal. " +
            "By default, non-empty annotation values should match exactly. You can also use a custom expression, instead.")
    @JIPipeParameter(value = "annotation-matching-method", uiOrder = 1999)
    @JsonGetter("annotation-matching-method")
    public JIPipeTextAnnotationMatchingMethod getAnnotationMatchingMethod() {
        return annotationMatchingMethod;
    }

    @JIPipeParameter("annotation-matching-method")
    @JsonSetter("annotation-matching-method")
    public void setAnnotationMatchingMethod(JIPipeTextAnnotationMatchingMethod annotationMatchingMethod) {
        this.annotationMatchingMethod = annotationMatchingMethod;
        emitParameterUIChangedEvent();
    }

    @SetJIPipeDocumentation(name = "Custom annotation matching method", description = "Expression used to compare two annotation sets.")
    @JIPipeExpressionParameterSettings(variableSource = JIPipeCustomAnnotationMatchingExpressionVariables.class)
    @JIPipeParameter(value = "custom-annotation-matching", uiOrder = 2100)
    @JsonGetter("custom-annotation-matching")
    public JIPipeExpressionParameter getCustomAnnotationMatching() {
        return customAnnotationMatching;
    }

    @JIPipeParameter("custom-annotation-matching")
    @JsonSetter("custom-annotation-matching")
    public void setCustomAnnotationMatching(JIPipeExpressionParameter customAnnotationMatching) {
        this.customAnnotationMatching = customAnnotationMatching;
    }

    @SetJIPipeDocumentation(name = "Grouping method", description = "Algorithms with multiple inputs require to match the incoming data " +
            "to data sets. This allows you to determine how interesting data annotation columns are extracted from the incoming data. " +
            "Union matches using the union of annotation columns. Intersection intersects the sets of available columns. You can also" +
            " customize which columns should be included or excluded.")
    @JIPipeParameter(value = "column-matching", uiOrder = -100, important = true, pinned = true)
    @JsonGetter("column-matching")
    public JIPipeColumMatching getColumnMatching() {
        return columnMatching;
    }

    @JIPipeParameter("column-matching")
    @JsonSetter("column-matching")
    public void setColumnMatching(JIPipeColumMatching columnMatching) {
        boolean needsTriggerStructureChange = columnMatching == JIPipeColumMatching.Custom || this.columnMatching == JIPipeColumMatching.Custom;
        this.columnMatching = columnMatching;
        if (needsTriggerStructureChange)
            emitParameterUIChangedEvent();
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if (access.getSource() == this && "custom-matched-columns-expression".equals(access.getKey())) {
            if (getColumnMatching() != JIPipeColumMatching.Custom)
                return false;
        }
        if (access.getSource() == this && "custom-annotation-matching".equals(access.getKey())) {
            if (getAnnotationMatchingMethod() != JIPipeTextAnnotationMatchingMethod.CustomExpression)
                return false;
        }
        return JIPipeIterationStepGenerationSettings.super.isParameterUIVisible(tree, access);
    }

    @SetJIPipeDocumentation(name = "Custom grouping columns", description = "Only used if 'Grouping method' is set to 'Custom'. " +
            "Determines which annotation columns are referred to group data sets. ")
    @JIPipeParameter(value = "custom-matched-columns-expression", uiOrder = 999, pinned = true)
    @JsonGetter("custom-matched-columns-expression")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/annotation.png")
    public StringQueryExpression getCustomColumns() {
        if (customColumns == null)
            customColumns = new StringQueryExpression();
        return customColumns;
    }

    @JIPipeParameter(value = "custom-matched-columns-expression")
    @JsonSetter("custom-matched-columns-expression")
    public void setCustomColumns(StringQueryExpression customColumns) {
        this.customColumns = customColumns;
    }

    @SetJIPipeDocumentation(name = "Skip incomplete data sets", description = "If enabled, incomplete data sets are silently skipped. " +
            "Otherwise an error is displayed if such a configuration is detected.")
    @JIPipeParameter(value = "skip-incomplete", pinned = true)
    @JsonGetter("skip-incomplete")
    public boolean isSkipIncompleteDataSets() {
        return skipIncompleteDataSets;
    }

    @JIPipeParameter("skip-incomplete")
    @JsonSetter("skip-incomplete")
    public void setSkipIncompleteDataSets(boolean skipIncompleteDataSets) {
        this.skipIncompleteDataSets = skipIncompleteDataSets;

    }

    @SetJIPipeDocumentation(name = "Merge same annotation values", description = "Determines which strategy is applied if data sets that " +
            "define different values for the same annotation columns are encountered.")
    @JIPipeParameter("annotation-merge-strategy")
    @JsonGetter("annotation-merge-strategy")
    public JIPipeTextAnnotationMergeMode getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merge-strategy")
    @JsonSetter("annotation-merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }

    @SetJIPipeDocumentation(name = "Merge same data annotation values", description = "Determines which strategy is applied if different values for the same data annotation columns are encountered.")
    @JIPipeParameter("data-annotation-merge-strategy")
    @JsonGetter("data-annotation-merge-strategy")
    public JIPipeDataAnnotationMergeMode getDataAnnotationMergeStrategy() {
        return dataAnnotationMergeStrategy;
    }

    @JIPipeParameter("data-annotation-merge-strategy")
    @JsonSetter("data-annotation-merge-strategy")
    public void setDataAnnotationMergeStrategy(JIPipeDataAnnotationMergeMode dataAnnotationMergeStrategy) {
        this.dataAnnotationMergeStrategy = dataAnnotationMergeStrategy;
    }

    @SetJIPipeDocumentation(name = "Limit", description = "Limits which data batches are generated. The first index is zero.")
    @JIPipeParameter(value = "limit")
    @JsonGetter("limit")
    public OptionalIntegerRange getLimit() {
        return limit;
    }

    @JIPipeParameter("limit")
    @JsonSetter("limit")
    public void setLimit(OptionalIntegerRange limit) {
        this.limit = limit;
    }
}
