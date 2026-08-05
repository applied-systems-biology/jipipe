package org.hkijena.jipipe.api.system;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SystemResources {
    private String gpuType;
    private long gpuVramTotalMB;
    private long gpuVramUsedMB;
    private long systemRamTotalMB;
    private long systemRamAvailableMB;
    private int cpuCores;
    private String accelerationMode;
    private int cudaMaxVersion;

    public SystemResources() {
    }

    public SystemResources(SystemResources other) {
        this.gpuType = other.gpuType;
        this.gpuVramTotalMB = other.gpuVramTotalMB;
        this.gpuVramUsedMB = other.gpuVramUsedMB;
        this.systemRamTotalMB = other.systemRamTotalMB;
        this.systemRamAvailableMB = other.systemRamAvailableMB;
        this.cpuCores = other.cpuCores;
        this.accelerationMode = other.accelerationMode;
        this.cudaMaxVersion = other.cudaMaxVersion;
    }

    @JsonGetter("gpu-type")
    public String getGpuType() {
        return gpuType;
    }

    @JsonSetter("gpu-type")
    public void setGpuType(String gpuType) {
        this.gpuType = gpuType;
    }

    @JsonGetter("gpu-vram-total-mb")
    public long getGpuVramTotalMB() {
        return gpuVramTotalMB;
    }

    @JsonSetter("gpu-vram-total-mb")
    public void setGpuVramTotalMB(long gpuVramTotalMB) {
        this.gpuVramTotalMB = gpuVramTotalMB;
    }

    @JsonGetter("gpu-vram-used-mb")
    public long getGpuVramUsedMB() {
        return gpuVramUsedMB;
    }

    @JsonSetter("gpu-vram-used-mb")
    public void setGpuVramUsedMB(long gpuVramUsedMB) {
        this.gpuVramUsedMB = gpuVramUsedMB;
    }

    @JsonGetter("system-ram-total-mb")
    public long getSystemRamTotalMB() {
        return systemRamTotalMB;
    }

    @JsonSetter("system-ram-total-mb")
    public void setSystemRamTotalMB(long systemRamTotalMB) {
        this.systemRamTotalMB = systemRamTotalMB;
    }

    @JsonGetter("system-ram-available-mb")
    public long getSystemRamAvailableMB() {
        return systemRamAvailableMB;
    }

    @JsonSetter("system-ram-available-mb")
    public void setSystemRamAvailableMB(long systemRamAvailableMB) {
        this.systemRamAvailableMB = systemRamAvailableMB;
    }

    @JsonGetter("cpu-cores")
    public int getCpuCores() {
        return cpuCores;
    }

    @JsonSetter("cpu-cores")
    public void setCpuCores(int cpuCores) {
        this.cpuCores = cpuCores;
    }

    @JsonGetter("acceleration-mode")
    public String getAccelerationMode() {
        return accelerationMode;
    }

    @JsonSetter("acceleration-mode")
    public void setAccelerationMode(String accelerationMode) {
        this.accelerationMode = accelerationMode;
    }

    @JsonGetter("cuda-max-version")
    public int getCudaMaxVersion() {
        return cudaMaxVersion;
    }

    @JsonSetter("cuda-max-version")
    public void setCudaMaxVersion(int cudaMaxVersion) {
        this.cudaMaxVersion = cudaMaxVersion;
    }

    public long getGpuVramFreeMB() {
        return Math.max(0, gpuVramTotalMB - gpuVramUsedMB);
    }
}
