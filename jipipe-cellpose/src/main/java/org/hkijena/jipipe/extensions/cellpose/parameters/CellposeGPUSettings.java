package org.hkijena.jipipe.extensions.cellpose.parameters;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalIntegerParameter;

public class CellposeGPUSettings extends AbstractJIPipeParameterCollection {
    private boolean enableGPU = true;
    private OptionalIntegerParameter gpuDevice = new OptionalIntegerParameter(false, 0);

    public CellposeGPUSettings() {
    }

    public CellposeGPUSettings(CellposeGPUSettings other) {
        this.enableGPU = other.enableGPU;
        this.gpuDevice = new OptionalIntegerParameter(other.gpuDevice);
    }

    @SetJIPipeDocumentation(name = "GPU device", description = "Which GPU device to use")
    @JIPipeParameter("gpu-device")
    public OptionalIntegerParameter getGpuDevice() {
        return gpuDevice;
    }

    @JIPipeParameter("gpu-device")
    public void setGpuDevice(OptionalIntegerParameter gpuDevice) {
        this.gpuDevice = gpuDevice;
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
