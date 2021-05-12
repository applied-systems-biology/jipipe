package org.hkijena.jipipe.extensions.cellpose.parameters;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.cellpose.CellPoseModel;

public class ModelParameters implements JIPipeParameterCollection {
    private final EventBus eventBus = new EventBus();

    private boolean enableGPU = true;
    private CellPoseModel model = CellPoseModel.Cytoplasm;

    public ModelParameters() {
    }

    public ModelParameters(ModelParameters other) {
        this.enableGPU = other.enableGPU;
        this.model = other.model;
    }

    @JIPipeDocumentation(name = "With GPU", description = "Utilize a GPU if available. Please note that you need to setup Cellpose " +
            "to allow usage of your GPU. Also ensure that enough memory is available.")
    @JIPipeParameter("enable-gpu")
    public boolean isEnableGPU() {
        return enableGPU;
    }

    @JIPipeParameter("enable-gpu")
    public void setEnableGPU(boolean enableGPU) {
        this.enableGPU = enableGPU;
    }

    @JIPipeDocumentation(name = "Model", description = "The model type that should be used.")
    @JIPipeParameter("model")
    public CellPoseModel getModel() {
        return model;
    }

    @JIPipeParameter("model")
    public void setModel(CellPoseModel model) {
        this.model = model;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }
}
