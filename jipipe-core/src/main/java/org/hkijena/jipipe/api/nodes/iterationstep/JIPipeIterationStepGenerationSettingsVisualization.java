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
        private boolean showVisualization;
        private float fillColorHue;
        private String iconInput;
        private String iconCenter;
        private String iconOutput;

        private Builder() {
        }

        public static Builder aJIPipeIterationStepGenerationSettingsVisualization() {
            return new Builder();
        }

        public Builder withShowVisualization(boolean showVisualization) {
            this.showVisualization = showVisualization;
            return this;
        }

        public Builder withFillColorHue(float fillColorHue) {
            this.fillColorHue = fillColorHue;
            return this;
        }

        public Builder withIconInput(String iconInput) {
            this.iconInput = iconInput;
            return this;
        }

        public Builder withIconCenter(String iconCenter) {
            this.iconCenter = iconCenter;
            return this;
        }

        public Builder withIconOutput(String iconOutput) {
            this.iconOutput = iconOutput;
            return this;
        }

        public JIPipeIterationStepGenerationSettingsVisualization build() {
            JIPipeIterationStepGenerationSettingsVisualization jIPipeIterationStepGenerationSettingsVisualization = new JIPipeIterationStepGenerationSettingsVisualization();
            jIPipeIterationStepGenerationSettingsVisualization.setShowVisualization(showVisualization);
            jIPipeIterationStepGenerationSettingsVisualization.setFillColorHue(fillColorHue);
            jIPipeIterationStepGenerationSettingsVisualization.setIconInput(iconInput);
            jIPipeIterationStepGenerationSettingsVisualization.setIconCenter(iconCenter);
            jIPipeIterationStepGenerationSettingsVisualization.setIconOutput(iconOutput);
            return jIPipeIterationStepGenerationSettingsVisualization;
        }
    }
}
