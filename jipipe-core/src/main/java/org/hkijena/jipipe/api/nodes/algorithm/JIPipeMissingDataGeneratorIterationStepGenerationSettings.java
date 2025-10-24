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
import org.hkijena.jipipe.api.nodes.JIPipeIterationStepTextAnnotationColumMatching;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationStepGenerationSettings;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationStepGenerationSettingsVisualization;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationStepSolverPreference;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.plugins.expressions.StringQueryExpression;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerRange;
import org.hkijena.jipipe.plugins.parameters.library.primitives.ranges.IntegerRange;
import org.hkijena.jipipe.utils.VersionUtils;

/**
 * Groups iteration step generation settings
 */
public class JIPipeMissingDataGeneratorIterationStepGenerationSettings extends AbstractJIPipeParameterCollection implements JIPipeIterationStepGenerationSettings {
    private JIPipeIterationStepTextAnnotationColumMatching dataSetMatching = JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion;
    private StringQueryExpression customColumns = new StringQueryExpression();
    private OptionalIntegerRange limit = new OptionalIntegerRange(new IntegerRange("0-9"), false);
    private boolean allowMerging = false;
    private JIPipeTextAnnotationMergeMode annotationMergeStrategy = JIPipeTextAnnotationMergeMode.Merge;
    private JIPipeDataAnnotationMergeMode dataAnnotationMergeStrategy = JIPipeDataAnnotationMergeMode.MergeTables;
    private JIPipeIterationStepSolverPreference solverPreference = JIPipeIterationStepSolverPreference.Auto;

    public JIPipeMissingDataGeneratorIterationStepGenerationSettings() {
    }

    public JIPipeMissingDataGeneratorIterationStepGenerationSettings(JIPipeMissingDataGeneratorIterationStepGenerationSettings other) {
        this.dataSetMatching = other.dataSetMatching;
        this.customColumns = new StringQueryExpression(other.customColumns);
        this.limit = new OptionalIntegerRange(other.limit);
        this.allowMerging = other.allowMerging;
        this.annotationMergeStrategy = other.annotationMergeStrategy;
        this.dataAnnotationMergeStrategy = other.dataAnnotationMergeStrategy;
        this.solverPreference =  other.solverPreference;

    }

    @Override
    public void applyProjectUpgrade(String fromVersion, JIPipeValidationReportContext context, JIPipeValidationReport report) {
        super.applyProjectUpgrade(fromVersion, context, report);

        if (VersionUtils.isOlderThanOrEqual(fromVersion, "5.3.0")) {
            solverPreference = JIPipeIterationStepSolverPreference.Legacy;
        }
    }

    @SetJIPipeDocumentation(name = "Solver", description = "Allows to override the iteration step solver")
    @JIPipeParameter(value = "solver-preference", pinned = true)
    @JsonGetter("solver-preference")
    public JIPipeIterationStepSolverPreference getSolverPreference() {
        return solverPreference;
    }

    @JIPipeParameter("solver-preference")
    @JsonSetter("solver-preference")
    public void setSolverPreference(JIPipeIterationStepSolverPreference solverPreference) {
        this.solverPreference = solverPreference;
    }

    @SetJIPipeDocumentation(name = "Grouping method", description = "Algorithms with multiple inputs require to match the incoming data " +
            "to data sets. This allows you to determine how interesting data annotation columns are extracted from the incoming data. " +
            "Union matches using the union of annotation columns. Intersection intersects the sets of available columns. You can also" +
            " customize which columns should be included or excluded.")
    @JIPipeParameter(value = "column-matching", uiOrder = -100, important = true, pinned = true)
    public JIPipeIterationStepTextAnnotationColumMatching getDataSetMatching() {
        return dataSetMatching;
    }

    @JIPipeParameter("column-matching")
    public void setDataSetMatching(JIPipeIterationStepTextAnnotationColumMatching dataSetMatching) {
        this.dataSetMatching = dataSetMatching;

    }

    @SetJIPipeDocumentation(name = "Custom grouping columns", description = "Only used if 'Grouping method' is set to 'Custom'. " +
            "Determines which annotation columns are referred to group data sets. ")
    @JIPipeParameter(value = "custom-matched-columns-expression", uiOrder = 999, pinned = true)
    @StringParameterSettings(monospace = true, icon = "data-types/annotation.png")
    public StringQueryExpression getCustomColumns() {
        if (customColumns == null)
            customColumns = new StringQueryExpression();
        return customColumns;
    }

    @JIPipeParameter(value = "custom-matched-columns-expression")
    public void setCustomColumns(StringQueryExpression customColumns) {
        this.customColumns = customColumns;
    }

    @SetJIPipeDocumentation(name = "Limit", description = "Limits which iteration steps are generated. The first index is zero.")
    @JIPipeParameter(value = "limit")
    public OptionalIntegerRange getLimit() {
        return limit;
    }

    @JIPipeParameter("limit")
    public void setLimit(OptionalIntegerRange limit) {
        this.limit = limit;
    }

    @SetJIPipeDocumentation(name = "Allow merging", description = "If enabled, there can be multiple rows per iteration step for any slot. " +
            "Otherwise, only one will be present at most.")
    @JIPipeParameter("allow-merging")
    public boolean isAllowMerging() {
        return allowMerging;
    }

    @JIPipeParameter("allow-merging")
    public void setAllowMerging(boolean allowMerging) {
        this.allowMerging = allowMerging;
    }

    @SetJIPipeDocumentation(name = "Merge same annotation values", description = "Determines which strategy is applied if data sets that " +
            "define different values for the same annotation columns are encountered.")
    @JIPipeParameter("annotation-merge-strategy")
    public JIPipeTextAnnotationMergeMode getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }

    @SetJIPipeDocumentation(name = "Merge same data annotation values", description = "Determines which strategy is applied if different values for the same data annotation columns are encountered.")
    @JIPipeParameter("data-annotation-merge-strategy")
    public JIPipeDataAnnotationMergeMode getDataAnnotationMergeStrategy() {
        return dataAnnotationMergeStrategy;
    }

    @JIPipeParameter("data-annotation-merge-strategy")
    public void setDataAnnotationMergeStrategy(JIPipeDataAnnotationMergeMode dataAnnotationMergeStrategy) {
        this.dataAnnotationMergeStrategy = dataAnnotationMergeStrategy;
    }

    @Override
    public JIPipeIterationStepGenerationSettingsVisualization createVisualization() {
        return new JIPipeIterationStepGenerationSettingsVisualization.Builder()
                .addStandardParameters(getDataSetMatching(), getCustomColumns(), getLimit(), false)
                .addParameter("Merge same annotation values", "annotation-merge-strategy", getAnnotationMergeStrategy(), JIPipeTextAnnotationMergeMode.Merge)
                .addParameter("Merge same data annotation values", "data-annotation-merge-strategy", getDataAnnotationMergeStrategy(), JIPipeDataAnnotationMergeMode.MergeTables)
                .build();
    }
}
