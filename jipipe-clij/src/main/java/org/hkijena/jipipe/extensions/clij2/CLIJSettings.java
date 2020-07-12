package org.hkijena.jipipe.extensions.clij2;

import com.google.common.eventbus.EventBus;
import ij.IJ;
import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.converters.CLIJConverterService;
import net.haesleinhuepf.clij.macro.CLIJHandler;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeRunSettings;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.registries.JIPipeSettingsRegistry;
import org.hkijena.jipipe.ui.settings.JIPipeProjectSettingsUI;
import org.scijava.Context;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

/**
 * Settings for CLIJ
 */
public class CLIJSettings implements JIPipeParameterCollection {

    public static String ID = "org.hkijena.jipipe:clij2-integration";

    private final EventBus eventBus = new EventBus();
    private int device = 0;
    private boolean initialized = false;

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

    public static CLIJSettings getInstance() {
        return JIPipeSettingsRegistry.getInstance().getSettings(ID, CLIJSettings.class);
    }

    /**
     * Initializes CLIJ based on the settings
     * @param context SciJava context
     * @param force if a re-initialization should be applied
     */
    public static void initializeCLIJ(Context context, boolean force) {
        if(!force && getInstance().initialized)
            return;
        getInstance().initialized = false;
        ArrayList<String> deviceList;
        try {
            deviceList = CLIJ.getAvailableDeviceNames();
        }
        catch(Exception e) {
            throw new UserFriendlyRuntimeException(e,
                    "Could not get list of available graphics cards!",
                    "CLIJ2 initialization",
                    "There was an error during the detection of installed graphics cards. This is often caused by " +
                            "old drivers or missing software.",
                    "Please check if you have OpenCL installed and a modern graphics card that can make use of it. " +
                            "Try updating your graphics driver. Try Installing 'ocl-icd-opencl-dev' if you are on Ubuntu, as this package provides some mandatory library.");
        }
        if(deviceList.isEmpty()) {
            throw new UserFriendlyRuntimeException("No graphics card device available!",
                    "No compatible graphics card detected!",
                    "CLIJ2 initialization",
                    "CLIJ could not detect a compatible graphics card. You cannot use any of the CLIJ2 functions.",
                    "Please check if you have OpenCL installed and a modern graphics card that can make use of it. " +
                            "Try updating your graphics driver. Try Installing 'ocl-icd-opencl-dev' if you are on Ubuntu.");
        }

        int deviceId = Math.max(0, Math.min(deviceList.size() - 1, getInstance().device));
        String device = deviceList.get(deviceId);
        System.out.println("CLIJ2 will be initialized with device '" + device + "'");

        CLIJ clij = CLIJ.getInstance(device);
        CLIJHandler.automaticOutputVariableNaming = true;

        CLIJConverterService clijConverterService = context.getService(CLIJConverterService.class);
        clijConverterService.setCLIJ(clij);
        clij.setConverterService(clijConverterService);

        if (clij.getOpenCLVersion() < 1.2) {
            System.err.println("Warning: Your GPU does not support OpenCL 1.2. Some operations may not work precisely. " +
                    "For example: CLIJ does not support linear interpolation; it uses nearest-neighbor interpolation instead. " +
                    "Consider upgrading GPU Driver version or GPU hardware.");
        }

        getInstance().initialized = true;
    }
}
