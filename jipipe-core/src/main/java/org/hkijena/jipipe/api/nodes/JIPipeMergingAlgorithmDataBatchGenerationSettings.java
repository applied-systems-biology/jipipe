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
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.parameters.generators.IntegerRange;
import org.hkijena.jipipe.extensions.parameters.generators.OptionalIntegerRange;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.ResourceUtils;

public class JIPipeMergingAlgorithmDataBatchGenerationSettings implements JIPipeParameterCollection {
    private final EventBus eventBus = new EventBus();
    private JIPipeColumnGrouping dataSetMatching = JIPipeColumnGrouping.PrefixHashUnion;
    private boolean skipIncompleteDataSets = false;
    private StringQueryExpression customColumns = new StringQueryExpression();
    private JIPipeAnnotationMergeStrategy annotationMergeStrategy = JIPipeAnnotationMergeStrategy.Merge;
    private OptionalIntegerRange limit = new OptionalIntegerRange(new IntegerRange("0-9"), false);

    public JIPipeMergingAlgorithmDataBatchGenerationSettings() {
    }

    public JIPipeMergingAlgorithmDataBatchGenerationSettings(JIPipeMergingAlgorithmDataBatchGenerationSettings other) {
        this.dataSetMatching = other.dataSetMatching;
        this.skipIncompleteDataSets = other.skipIncompleteDataSets;
        this.customColumns = new StringQueryExpression(other.customColumns);
        this.annotationMergeStrategy = other.annotationMergeStrategy;
        this.limit = new OptionalIntegerRange(other.limit);
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "Grouping method", description = "Algorithms with multiple inputs require to match the incoming data " +
            "to data sets. This allows you to determine how interesting data annotation columns are extracted from the incoming data. " +
            "Union matches using the union of annotation columns. Intersection intersects the sets of available columns. You can also" +
            " customize which columns should be included or excluded.")
    @JIPipeParameter(value = "column-matching", uiOrder = 999)
    public JIPipeColumnGrouping getDataSetMatching() {
        return dataSetMatching;
    }

    @JIPipeParameter("column-matching")
    public void setDataSetMatching(JIPipeColumnGrouping dataSetMatching) {
        this.dataSetMatching = dataSetMatching;

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

    @JIPipeDocumentation(name = "Skip incomplete data sets", description = "If enabled, incomplete data sets are silently skipped. " +
            "Otherwise an error is displayed if such a configuration is detected.")
    @JIPipeParameter(value = "skip-incomplete")
    public boolean isSkipIncompleteDataSets() {
        return skipIncompleteDataSets;
    }

    @JIPipeParameter("skip-incomplete")
    public void setSkipIncompleteDataSets(boolean skipIncompleteDataSets) {
        this.skipIncompleteDataSets = skipIncompleteDataSets;

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

    @JIPipeDocumentation(name = "Limit", description = "Limits which data batches are generated. The first index is zero.\n" + IntegerRange.DOCUMENTATION_DESCRIPTION)
    @JIPipeParameter(value = "limit")
    public OptionalIntegerRange getLimit() {
        return limit;
    }

    @JIPipeParameter("limit")
    public void setLimit(OptionalIntegerRange limit) {
        this.limit = limit;
    }
}