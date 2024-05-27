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

package org.hkijena.jipipe.plugins.cellpose.legacy.parameters;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.cellpose.legacy.PretrainedLegacyCellpose2InferenceModel;

@Deprecated
public class CellposeSegmentationModelSettings_Old extends AbstractJIPipeParameterCollection {
    private PretrainedLegacyCellpose2InferenceModel model = PretrainedLegacyCellpose2InferenceModel.Cytoplasm;
    private double meanDiameter = 30;

    private boolean enableGPU = true;

    public CellposeSegmentationModelSettings_Old() {
    }

    public CellposeSegmentationModelSettings_Old(CellposeSegmentationModelSettings_Old other) {
        this.model = other.model;
        this.meanDiameter = other.meanDiameter;
        this.enableGPU = other.enableGPU;
    }

    @SetJIPipeDocumentation(name = "Model", description = "The model type that should be used.")
    @JIPipeParameter("model")
    public PretrainedLegacyCellpose2InferenceModel getModel() {
        return model;
    }

    @JIPipeParameter("model")
    public void setModel(PretrainedLegacyCellpose2InferenceModel model) {
        this.model = model;
        emitParameterUIChangedEvent();
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if (access.getSource() == this && "mean-diameter".equals(access.getKey())) {
            return model == PretrainedLegacyCellpose2InferenceModel.Custom;
        }
        return super.isParameterUIVisible(tree, access);
    }

    @SetJIPipeDocumentation(name = "Mean diameter", description = "Mean diameter of the model. Only necessary if you are using a " +
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

    @SetJIPipeDocumentation(name = "With GPU", description = "Utilize a GPU if available. Please note that you need to setup Cellpose " +
            "to allow usage of your GPU. Also ensure that enough memory is available.")
    @JIPipeParameter("enable-gpu")
    public boolean isEnableGPU() {
        return enableGPU;
    }

    @JIPipeParameter("enable-gpu")
    public void setEnableGPU(boolean enableGPU) {
        this.enableGPU = enableGPU;
    }
}

