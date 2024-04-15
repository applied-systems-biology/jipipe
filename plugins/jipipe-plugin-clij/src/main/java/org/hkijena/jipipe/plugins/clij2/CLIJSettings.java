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

package org.hkijena.jipipe.plugins.clij2;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.converters.CLIJConverterService;
import net.haesleinhuepf.clij.macro.CLIJHandler;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;
import org.scijava.Context;
import org.scijava.log.LogService;

import java.util.ArrayList;

/**
 * Settings for CLIJ
 */
public class CLIJSettings extends AbstractJIPipeParameterCollection {

    public static String ID = "org.hkijena.jipipe:clij2-integration";
    private int device = 0;
    private boolean initialized = false;
    private boolean autoCalibrateAfterPulling = true;
    private ContrastEnhancerSettings contrastEnhancerSettings = new ContrastEnhancerSettings();

    public CLIJSettings() {
    }

    public static CLIJSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, CLIJSettings.class);
    }

    /**
     * Initializes CLIJ based on the settings
     *
     * @param context SciJava context
     * @param force   if a re-initialization should be applied
     */
    public static void initializeCLIJ(Context context, boolean force) {
        LogService logService = context.getService(LogService.class);
        if (!force && getInstance().initialized)
            return;
        getInstance().initialized = false;
        ArrayList<String> deviceList;
        try {
            deviceList = CLIJ.getAvailableDeviceNames();
        } catch (Exception e) {
            throw new JIPipeValidationRuntimeException(e,
                    "Could not get list of available graphics cards!",
                    "There was an error during the detection of installed graphics cards. This is often caused by " +
                            "old drivers or missing software.",
                    "Please check if you have OpenCL installed and a modern graphics card that can make use of it. " +
                            "Try updating your graphics driver. Try Installing 'ocl-icd-opencl-dev' if you are on Ubuntu, as this package provides some mandatory library.");
        }
        if (deviceList.isEmpty()) {
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, new UnspecifiedValidationReportContext(),
                    "No graphics card device available!",
                    "CLIJ could not detect a compatible graphics card. You cannot use any of the CLIJ2 functions.",
                    "Please check if you have OpenCL installed and a modern graphics card that can make use of it. " +
                            "Try updating your graphics driver. Try Installing 'ocl-icd-opencl-dev' if you are on Ubuntu."));
        }

        int deviceId = Math.max(0, Math.min(deviceList.size() - 1, getInstance().device));
        String device = deviceList.get(deviceId);
        logService.info("CLIJ2 will be initialized with device '" + device + "'");

        CLIJ clij = CLIJ.getInstance(device);
        CLIJHandler.automaticOutputVariableNaming = true;

        CLIJConverterService clijConverterService = context.getService(CLIJConverterService.class);
        clijConverterService.setCLIJ(clij);
        clij.setConverterService(clijConverterService);

        if (clij.getOpenCLVersion() < 1.2) {
            logService.warn("Warning: Your GPU does not support OpenCL 1.2. Some operations may not work precisely. " +
                    "For example: CLIJ does not support linear interpolation; it uses nearest-neighbor interpolation instead. " +
                    "Consider upgrading GPU Driver version or GPU hardware.");
        }

        getInstance().initialized = true;
    }

    @SetJIPipeDocumentation(name = "Graphics card device", description = "Relevant if you have multiple graphics cards available. " +
            "The first device is zero. " +
            "This determines which graphics card should be used. Changing this setting might require a ImageJ restart to take effect.")
    @JIPipeParameter("device")
    public int getDevice() {
        return device;
    }

    @JIPipeParameter("device")
    public void setDevice(int device) {
        this.device = device;
    }

    @SetJIPipeDocumentation(name = "Auto-calibrate images", description = "Apply auto-calibration after extracting an image from the GPU. " +
            "This is helpful if you see only black or white output images. Calibration does not modify the contained data, but only " +
            "how the image is displayed in ImageJ.")
    @JIPipeParameter("auto-calibrate-after-pull")
    public boolean isAutoCalibrateAfterPulling() {
        return autoCalibrateAfterPulling;
    }

    @JIPipeParameter("auto-calibrate-after-pull")
    public void setAutoCalibrateAfterPulling(boolean autoCalibrateAfterPulling) {
        this.autoCalibrateAfterPulling = autoCalibrateAfterPulling;
    }

    @SetJIPipeDocumentation(name = "Calibration settings", description = "Following settings will be used if you enable auto-calibration:")
    @JIPipeParameter(value = "contrast-enhancer")
    public ContrastEnhancerSettings getContrastEnhancerSettings() {
        return contrastEnhancerSettings;
    }

    public static class ContrastEnhancerSettings extends AbstractJIPipeParameterCollection {
        private ImageJCalibrationMode calibrationMode = ImageJCalibrationMode.AutomaticImageJ;
        private double customMin = 0;
        private double customMax = 1;
        private boolean duplicateImage = true;
        private boolean applyToAllPlanes = true;

        @SetJIPipeDocumentation(name = "Calibration method", description = "The method to apply for calibration.")
        @JIPipeParameter("calibration-mode")
        public ImageJCalibrationMode getCalibrationMode() {
            return calibrationMode;
        }

        @JIPipeParameter("calibration-mode")
        public void setCalibrationMode(ImageJCalibrationMode calibrationMode) {
            this.calibrationMode = calibrationMode;
        }

        @SetJIPipeDocumentation(name = "Custom min", description = "Used if 'Calibration' method is set to 'Custom'. Sets custom minimum value.")
        @JIPipeParameter("custom-min")
        public double getCustomMin() {
            return customMin;
        }

        @JIPipeParameter("custom-min")
        public void setCustomMin(double customMin) {
            this.customMin = customMin;
        }

        @SetJIPipeDocumentation(name = "Custom max", description = "Used if 'Calibration' method is set to 'Custom'. Sets custom maximum value.")
        @JIPipeParameter("custom-max")
        public double getCustomMax() {
            return customMax;
        }

        @JIPipeParameter("custom-max")
        public void setCustomMax(double customMax) {
            this.customMax = customMax;
        }

        @SetJIPipeDocumentation(name = "Duplicate image", description = "As the calibration does not change any image data, you can disable creating a duplicate.")
        @JIPipeParameter("duplicate-image")
        public boolean isDuplicateImage() {
            return duplicateImage;
        }

        @JIPipeParameter("duplicate-image")
        public void setDuplicateImage(boolean duplicateImage) {
            this.duplicateImage = duplicateImage;
        }

        @SetJIPipeDocumentation(name = "Apply to all planes", description = "If enabled, all image planes are recalibrated, not only the one of the current plane.")
        @JIPipeParameter("apply-to-all-planes")
        public boolean isApplyToAllPlanes() {
            return applyToAllPlanes;
        }

        @JIPipeParameter("apply-to-all-planes")
        public void setApplyToAllPlanes(boolean applyToAllPlanes) {
            this.applyToAllPlanes = applyToAllPlanes;
        }
    }
}
