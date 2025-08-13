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

import org.hkijena.jipipe.api.nodes.JIPipeColumMatching;
import org.hkijena.jipipe.plugins.expressions.StringQueryExpression;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerRange;

public class JIPipeIterationStepGenerationSettingsVisualization {
    private boolean showVisualization;
    private float fillColorHue;
    private String iconInput;
    private String iconCenter;
    private String iconOutput;

    public JIPipeIterationStepGenerationSettingsVisualization() {
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


    public static final class Builder {
        public Builder() {
        }

        public Builder addOptionalParameter(String key, boolean enabled, Object value) {
            return this;
        }

        public <T> Builder addParameter(String key, T value, T defaultValue) {
            return this;
        }

        public Builder addStandardParameters(JIPipeColumMatching value, StringQueryExpression customColumns, OptionalIntegerRange limit, boolean skipIncompleteDataSets) {
            return this;
        }

        public JIPipeIterationStepGenerationSettingsVisualization build() {
            JIPipeIterationStepGenerationSettingsVisualization result = new JIPipeIterationStepGenerationSettingsVisualization();
            return result;
        }

        public Builder setCenterIcon(String icon) {
            return this;
        }
    }
}
