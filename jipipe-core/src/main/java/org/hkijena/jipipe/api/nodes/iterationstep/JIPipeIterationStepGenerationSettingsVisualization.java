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

package org.hkijena.jipipe.api.nodes.iterationstep;

import org.hkijena.jipipe.api.nodes.JIPipeIterationStepTextAnnotationColumMatching;
import org.hkijena.jipipe.plugins.expressions.StringQueryExpression;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerRange;
import org.hkijena.jipipe.utils.HashUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.*;

public class JIPipeIterationStepGenerationSettingsVisualization {
    private boolean showVisualization;
    private float fillColorHue;
    private String iconInput;
    private String iconCenter;
    private String iconOutput;
    private List<ReportEntry> reportEntries = new ArrayList<>();

    public JIPipeIterationStepGenerationSettingsVisualization() {
    }

    public List<ReportEntry> getReportEntries() {
        return reportEntries;
    }

    public void setReportEntries(List<ReportEntry> reportEntries) {
        this.reportEntries = reportEntries;
    }

    public boolean isShowVisualization() {
        return showVisualization;
    }

    public void setShowVisualization(boolean showVisualization) {
        this.showVisualization = showVisualization;
    }

    public float getFillColorHue() {
        return fillColorHue;
    }

    public void setFillColorHue(float fillColorHue) {
        this.fillColorHue = fillColorHue;
    }

    public String getIconInput() {
        return iconInput;
    }

    public void setIconInput(String iconInput) {
        this.iconInput = iconInput;
    }

    public String getIconCenter() {
        return iconCenter;
    }

    public void setIconCenter(String iconCenter) {
        this.iconCenter = iconCenter;
    }

    public String getIconOutput() {
        return iconOutput;
    }

    public void setIconOutput(String iconOutput) {
        this.iconOutput = iconOutput;
    }

    public record ReportEntry(String key, String name, String message) {
    }

    public static final class Builder {

        private final Map<String, String> trackedValues = new HashMap<>();
        private boolean showVisualization;
        private boolean isFiltering;
        private boolean isSkipIncomplete;
        private JIPipeIterationStepTextAnnotationColumMatching columMatching = JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion;
        private final JIPipeIterationStepGenerationSettingsVisualization result = new JIPipeIterationStepGenerationSettingsVisualization();


        public Builder() {
        }

        public Builder addOptionalParameter(String name, String key, boolean enabled, Object value) {
            trackedValues.put(key, StringUtils.nullToEmpty(value));
            if (enabled) {
                showVisualization = true;
                result.reportEntries.add(new ReportEntry(key, name, "Optional parameter is enabled (value set to " + value + ")"));
                tryExtractSpecialParameter(key, enabled, value);
            }
            return this;
        }

        public <T> Builder addParameter(String name, String key, T value, T defaultValue) {
            trackedValues.put(key, StringUtils.nullToEmpty(value));
            if (!Objects.equals(value, defaultValue)) {
                result.reportEntries.add(new ReportEntry(key, name, "Current value '" + value + "' different from default value '" + defaultValue + "'"));
                showVisualization = true;
            }
            tryExtractSpecialParameter(key, true, value);
            return this;
        }

        private <T> void tryExtractSpecialParameter(String key, boolean enabled, T value) {
            switch (key) {
                case "column-matching" -> {
                    if (value instanceof JIPipeIterationStepTextAnnotationColumMatching) {
                        columMatching = (JIPipeIterationStepTextAnnotationColumMatching) value;
                    }
                }
                case "skip-incomplete" -> {
                    if (value instanceof Boolean) {
                        isSkipIncomplete = (Boolean) value;
                    }
                }
                case "limit" -> {
                    isFiltering = enabled;
                }
            }

        }

        public Builder addStandardParameters(JIPipeIterationStepTextAnnotationColumMatching columMatching, StringQueryExpression customColumns, OptionalIntegerRange limit, boolean skipIncompleteDataSets) {
            return addParameter("Grouping method", "column-matching", columMatching, JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion)
                    .addOptionalParameter("Custom grouping columns", "custom-column-matching", customColumns != null && !StringUtils.isNullOrEmpty(customColumns.getExpression()) && columMatching == JIPipeIterationStepTextAnnotationColumMatching.Custom, customColumns != null ? customColumns.getExpression() : "")
                    .addOptionalParameter("Limit", "limit", limit != null && limit.isEnabled(), limit != null ? limit.toString() : "")
                    .addParameter("Skip incomplete data sets", "skip-incomplete", skipIncompleteDataSets, false);
        }

        public JIPipeIterationStepGenerationSettingsVisualization build() {
            result.showVisualization = showVisualization;

            // Top icon
            result.iconInput = switch (columMatching) {
                case PrefixHashUnion -> "actions/hashtag.png";
                case Custom -> "actions/insert-math-expression.png";
                case MergeAll -> "actions/data-flow-iteration-steps-n.png";
                default -> "actions/configure.png";
            };

            // Center icon
            if(StringUtils.isNullOrEmpty(result.iconCenter)) {
                if (isFiltering) {
                    result.iconCenter = "nodeui/data-flow-filter-16x32.png";
                } else if (columMatching == JIPipeIterationStepTextAnnotationColumMatching.MergeAll) {
                    result.iconCenter = "nodeui/data-flow-merge-16x32.png";
                } else {
                    result.iconCenter = "nodeui/data-flow-iterate-16x32.png";
                }
            }

            // Bottom icon
            if(columMatching == JIPipeIterationStepTextAnnotationColumMatching.MergeAll) {
                result.iconOutput = "actions/data-flow-iteration-steps-single.png";
            }
            else if(isSkipIncomplete) {
                result.iconOutput = "actions/view-filter.png";
            }
            else {
                result.iconOutput = "actions/data-flow-iteration-steps-m.png";
            }

            // Calculate hue
            result.fillColorHue = HashUtils.toUnitFloat(trackedValues);

            return result;
        }

        public Builder setCenterIcon(String icon) {
            result.iconCenter =  icon;
            return this;
        }
    }
}
