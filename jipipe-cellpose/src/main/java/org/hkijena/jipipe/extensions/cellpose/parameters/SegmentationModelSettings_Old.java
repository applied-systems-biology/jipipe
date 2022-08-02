package org.hkijena.jipipe.extensions.cellpose.parameters;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.cellpose.CellposeModel;

@Deprecated
public class SegmentationModelSettings_Old implements JIPipeParameterCollection {
    private final EventBus eventBus = new EventBus();
    private CellposeModel model = CellposeModel.Cytoplasm;
    private double meanDiameter = 30;

    private boolean enableGPU = true;

    public SegmentationModelSettings_Old() {
    }

    public SegmentationModelSettings_Old(SegmentationModelSettings_Old other) {
        this.model = other.model;
        this.meanDiameter = other.meanDiameter;
        this.enableGPU = other.enableGPU;
    }

    @JIPipeDocumentation(name = "Model", description = "The model type that should be used.")
    @JIPipeParameter("model")
    public CellposeModel getModel() {
        return model;
    }

    @JIPipeParameter("model")
    public void setModel(CellposeModel model) {
        this.model = model;
        triggerParameterUIChange();
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if (access.getSource() == this && "mean-diameter".equals(access.getKey())) {
            return model == CellposeModel.Custom;
        }
        return JIPipeParameterCollection.super.isParameterUIVisible(tree, access);
    }

    @JIPipeDocumentation(name = "Mean diameter", description = "Mean diameter of the model. Only necessary if you are using a " +
            "custom model. If you retrained a pretrained model, set following values:" +
            "<ul>" +
            "<li>Model based on Cytoplasm: Set to 30.0</li>" +
            "<li>Model based on Nuclei: Set to 17.0</li>" +
            "</ul>")
    @JIPipeParameter("mean-diameter")
    public double getMeanDiameter() {
        return meanDiameter;
    }

    @JIPipeParameter("mean-diameter")
    public void setMeanDiameter(double meanDiameter) {
        this.meanDiameter = meanDiameter;
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

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }
}

