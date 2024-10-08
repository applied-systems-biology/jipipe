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

package org.hkijena.jipipe.plugins.pipelinerender;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerParameter;

public class RenderPipelineRunSettings extends AbstractJIPipeParameterCollection {
    private boolean renderLabel = true;
    private boolean renderShadow = true;
    private int labelFontSize = 40;
    private OptionalIntegerParameter maxEdgeWidth = new OptionalIntegerParameter(false, 80);

    public RenderPipelineRunSettings() {
    }

    public RenderPipelineRunSettings(RenderPipelineRunSettings other) {
        this.renderLabel = other.renderLabel;
        this.renderShadow = other.renderShadow;
        this.labelFontSize = other.labelFontSize;
        this.maxEdgeWidth = new OptionalIntegerParameter(other.maxEdgeWidth);
    }

    @SetJIPipeDocumentation(name = "Draw shadow", description = "If enabled, draw a shadow around each compartment")
    @JIPipeParameter("draw-shadow")
    public boolean isRenderShadow() {
        return renderShadow;
    }

    @JIPipeParameter("draw-shadow")
    public void setRenderShadow(boolean renderShadow) {
        this.renderShadow = renderShadow;
    }

    @SetJIPipeDocumentation(name = "Maximum edge width", description = "The maximum width of edge strokes. If disabled, the width is calculated based on the scale factor.")
    @JIPipeParameter("max-edge-width")
    public OptionalIntegerParameter getMaxEdgeWidth() {
        return maxEdgeWidth;
    }

    @JIPipeParameter("max-edge-width")
    public void setMaxEdgeWidth(OptionalIntegerParameter maxEdgeWidth) {
        this.maxEdgeWidth = maxEdgeWidth;
    }

    @SetJIPipeDocumentation(name = "Draw compartment name", description = "If enabled, render the name of the compartment")
    @JIPipeParameter("render-label")
    public boolean isRenderLabel() {
        return renderLabel;
    }

    @JIPipeParameter("render-label")
    public void setRenderLabel(boolean renderLabel) {
        this.renderLabel = renderLabel;
    }

    @SetJIPipeDocumentation(name = "Compartment name font size", description = "The font size of the rendered compartment name")
    @JIPipeParameter("label-font-size")
    public int getLabelFontSize() {
        return labelFontSize;
    }

    @JIPipeParameter("label-font-size")
    public void setLabelFontSize(int labelFontSize) {
        this.labelFontSize = labelFontSize;
    }
}
