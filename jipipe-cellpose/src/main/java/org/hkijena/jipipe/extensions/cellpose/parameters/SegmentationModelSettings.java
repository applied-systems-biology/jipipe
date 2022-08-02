package org.hkijena.jipipe.extensions.cellpose.parameters;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.cellpose.CellposeModel;

public class SegmentationModelSettings implements JIPipeParameterCollection {
    private final EventBus eventBus = new EventBus();
    private CellposeModel model = CellposeModel.Cytoplasm;
    private double meanDiameter = 30;

    public SegmentationModelSettings() {
    }

    public SegmentationModelSettings(SegmentationModelSettings other) {
        this.model = other.model;
        this.meanDiameter = other.meanDiameter;
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

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }
}

