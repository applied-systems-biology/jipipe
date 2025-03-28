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

package org.hkijena.jipipe.plugins.cellpose.parameters.cp2;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerParameter;

public class Cellpose2GPUSettings extends AbstractJIPipeParameterCollection {
    private boolean enableGPU = true;
    private OptionalIntegerParameter gpuDevice = new OptionalIntegerParameter(false, 0);

    public Cellpose2GPUSettings() {
    }

    public Cellpose2GPUSettings(Cellpose2GPUSettings other) {
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
