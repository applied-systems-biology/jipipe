package org.hkijena.jipipe.extensions.pipelinerender;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;

public class RenderPipelineRunSettings implements JIPipeParameterCollection {
    private final EventBus eventBus = new EventBus();
    private boolean renderLabel = true;
    private int labelFontSize = 22;

    @Override
    public EventBus getEventBus() {
        return eventBus;
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
