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

package org.hkijena.jipipe.api.acceleration;

public enum JIPipeHardwareAccelerationMode {
    CPU("CPU (max compatibility, slowest)", ""),
    CUDA("NVidia GPU (CUDA) ", "cu"),
    ROCM("AMD GPU (ROCm)", "rocm");


    private final String label;
    private final String prefix;

    JIPipeHardwareAccelerationMode(String label, String prefix) {
        this.label = label;
        this.prefix = prefix;
    }

    @Override
    public String toString() {
        return label;
    }

    public String getPrefix() {
        return prefix;
    }
}
