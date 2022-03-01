package org.hkijena.jipipe.extensions.pipelinerender;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalIntegerParameter;

public class RenderPipelineRunSettings implements JIPipeParameterCollection {
    private final EventBus eventBus = new EventBus();
    private boolean renderLabel = true;
    private boolean renderShadow = true;
    private int labelFontSize = 40;
    private OptionalIntegerParameter maxEdgeWidth = new OptionalIntegerParameter(false, 80);

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "Draw shadow", description = "If enabled, draw a shadow around each compartment")
    @JIPipeParameter("draw-shadow")
    public boolean isRenderShadow() {
        return renderShadow;
    }

    @JIPipeParameter("draw-shadow")
    public void setRenderShadow(boolean renderShadow) {
        this.renderShadow = renderShadow;
    }

    @JIPipeDocumentation(name = "Maximum edge width", description = "The maximum width of edge strokes. If disabled, the width is calculated based on the scale factor.")
    @JIPipeParameter("max-edge-width")
    public OptionalIntegerParameter getMaxEdgeWidth() {
        return maxEdgeWidth;
    }

    @JIPipeParameter("max-edge-width")
    public void setMaxEdgeWidth(OptionalIntegerParameter maxEdgeWidth) {
        this.maxEdgeWidth = maxEdgeWidth;
    }

    @JIPipeDocumentation(name = "Draw compartment name", description = "If enabled, render the name of the compartment")
    @JIPipeParameter("render-label")
    public boolean isRenderLabel() {
        return renderLabel;
    }

    @JIPipeParameter("render-label")
    public void setRenderLabel(boolean renderLabel) {
        this.renderLabel = renderLabel;
    }

    @JIPipeDocumentation(name = "Compartment name font size", description = "The font size of the rendered compartment name")
    @JIPipeParameter("label-font-size")
    public int getLabelFontSize() {
        return labelFontSize;
    }

    @JIPipeParameter("label-font-size")
    public void setLabelFontSize(int labelFontSize) {
        this.labelFontSize = labelFontSize;
    }
}
