package org.hkijena.jipipe.plugins.artifacts;

public enum JIPipeArtifactAccelerationPreference {
    CPU("CPU (max compatibility, slowest)", ""),
    CUDA("NVidia GPU (CUDA) ", "cu"),
    ROCM("AMD GPU (ROCm)", "rocm");


    private final String label;
    private final String prefix;

    JIPipeArtifactAccelerationPreference(String label, String prefix) {
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
