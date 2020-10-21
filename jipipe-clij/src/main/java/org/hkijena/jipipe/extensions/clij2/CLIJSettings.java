package org.hkijena.jipipe.extensions.clij2;

import com.google.common.eventbus.EventBus;
import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.converters.CLIJConverterService;
import net.haesleinhuepf.clij.macro.CLIJHandler;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.JIPipeJavaNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.registries.JIPipeSettingsRegistry;
import org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.DisplayRangeCalibrationAlgorithm;
import org.scijava.Context;
import org.scijava.log.LogService;

import java.util.ArrayList;

/**
 * Settings for CLIJ
 */
public class CLIJSettings implements JIPipeParameterCollection {

    public static String ID = "org.hkijena.jipipe:clij2-integration";

    private final EventBus eventBus = new EventBus();
    private int device = 0;
    private boolean initialized = false;
    private boolean autoCalibrateAfterPulling = true;
    private DisplayRangeCalibrationAlgorithm contrastEnhancer;

    public CLIJSettings() {
        contrastEnhancer = new DisplayRangeCalibrationAlgorithm(new JIPipeJavaNodeInfo("", DisplayRangeCalibrationAlgorithm.class));
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "Graphics card device", description = "Relevant if you have multiple graphics cards available. " +
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

    @JIPipeDocumentation(name = "Auto-calibrate images", description = "Apply auto-calibration after extracting an image from the GPU. " +
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

    @JIPipeDocumentation(name = "Calibration settings", description = "Following settings will be used if you enable auto-calibration:")
    @JIPipeParameter(value = "contrast-enhancer", uiExcludeSubParameters = {"jipipe:data-batch-generation", "jipipe:parameter-slot-algorithm", "duplicate-image"})
    public DisplayRangeCalibrationAlgorithm getContrastEnhancer() {
        return contrastEnhancer;
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
            throw new UserFriendlyRuntimeException(e,
                    "Could not get list of available graphics cards!",
                    "CLIJ2 initialization",
                    "There was an error during the detection of installed graphics cards. This is often caused by " +
                            "old drivers or missing software.",
                    "Please check if you have OpenCL installed and a modern graphics card that can make use of it. " +
                            "Try updating your graphics driver. Try Installing 'ocl-icd-opencl-dev' if you are on Ubuntu, as this package provides some mandatory library.");
        }
        if (deviceList.isEmpty()) {
            throw new UserFriendlyRuntimeException("No graphics card device available!",
                    "No compatible graphics card detected!",
                    "CLIJ2 initialization",
                    "CLIJ could not detect a compatible graphics card. You cannot use any of the CLIJ2 functions.",
                    "Please check if you have OpenCL installed and a modern graphics card that can make use of it. " +
                            "Try updating your graphics driver. Try Installing 'ocl-icd-opencl-dev' if you are on Ubuntu.");
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
}
