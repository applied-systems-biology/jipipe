package org.hkijena.jipipe.extensions.imagejdatatypes.util;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;

public class CalibrationParameters extends AbstractJIPipeParameterCollection {
    private ImageJCalibrationMode calibrationMode = ImageJCalibrationMode.AutomaticImageJ;
    private double customMin = 0;
    private double customMax = 1;

    public CalibrationParameters() {
    }

    public CalibrationParameters(CalibrationParameters other) {
        this.calibrationMode = other.calibrationMode;
        this.customMin = other.customMin;
        this.customMax = other.customMax;
    }

    @JIPipeDocumentation(name = "Calibration method", description = "The method to apply for calibration.")
    @JIPipeParameter("calibration-mode")
    public ImageJCalibrationMode getCalibrationMode() {
        return calibrationMode;
    }

    @JIPipeParameter("calibration-mode")
    public void setCalibrationMode(ImageJCalibrationMode calibrationMode) {
        this.calibrationMode = calibrationMode;
    }

    @JIPipeDocumentation(name = "Custom min", description = "Used if 'Calibration' method is set to 'Custom'. Sets custom minimum value.")
    @JIPipeParameter("custom-min")
    public double getCustomMin() {
        return customMin;
    }

    @JIPipeParameter("custom-min")
    public void setCustomMin(double customMin) {
        this.customMin = customMin;
    }

    @JIPipeDocumentation(name = "Custom max", description = "Used if 'Calibration' method is set to 'Custom'. Sets custom maximum value.")
    @JIPipeParameter("custom-max")
    public double getCustomMax() {
        return customMax;
    }

    @JIPipeParameter("custom-max")
    public void setCustomMax(double customMax) {
        this.customMax = customMax;
    }
}
