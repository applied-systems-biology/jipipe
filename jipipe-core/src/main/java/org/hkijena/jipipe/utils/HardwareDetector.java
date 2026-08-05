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

package org.hkijena.jipipe.utils;

import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.api.system.SystemResources;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class HardwareDetector {

    public static SystemResources detect() {
        SystemResources resources = new SystemResources();
        resources.setCpuCores(Runtime.getRuntime().availableProcessors());

        detectSystemRam(resources);
        detectGpu(resources);

        if (resources.getAccelerationMode() == null) {
            resources.setAccelerationMode("CPU");
        }

        return resources;
    }

    private static void detectSystemRam(SystemResources resources) {
        if (SystemUtils.IS_OS_LINUX) {
            detectSystemRamLinux(resources);
        } else if (SystemUtils.IS_OS_MAC) {
            detectSystemRamMac(resources);
        } else if (SystemUtils.IS_OS_WINDOWS) {
            detectSystemRamWindows(resources);
        }
    }

    private static void detectSystemRamLinux(SystemResources resources) {
        try {
            List<String> meminfo = readProcessOutput("cat", "/proc/meminfo");
            long memTotalKB = 0;
            long memAvailableKB = 0;
            for (String line : meminfo) {
                if (line.startsWith("MemTotal:")) {
                    memTotalKB = parseMemInfoLine(line);
                } else if (line.startsWith("MemAvailable:")) {
                    memAvailableKB = parseMemInfoLine(line);
                }
            }
            resources.setSystemRamTotalMB(memTotalKB / 1024);
            resources.setSystemRamAvailableMB(memAvailableKB / 1024);
        } catch (Exception e) {
        }
    }

    private static void detectSystemRamMac(SystemResources resources) {
        try {
            List<String> output = readProcessOutput("sysctl", "-n", "hw.memsize");
            if (!output.isEmpty()) {
                long totalBytes = Long.parseLong(output.get(0).trim());
                resources.setSystemRamTotalMB(totalBytes / (1024 * 1024));
            }
            List<String> vmStat = readProcessOutput("vm_stat");
            long pageSize = 4096;
            long freePages = 0;
            for (String line : vmStat) {
                if (line.contains("free:") || line.contains("inactive:")) {
                    String numStr = line.replaceAll(".*:\\s*", "").replaceAll("\\.", "").trim();
                    freePages += Long.parseLong(numStr);
                }
            }
            resources.setSystemRamAvailableMB((freePages * pageSize) / (1024 * 1024));
        } catch (Exception e) {
        }
    }

    private static void detectSystemRamWindows(SystemResources resources) {
        try {
            List<String> output = readProcessOutput("wmic", "OS", "get", "TotalVisibleMemorySize,FreePhysicalMemory");
            for (String line : output) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.contains("TotalVisible")) continue;
                String[] parts = trimmed.split("\\s+");
                if (parts.length >= 2) {
                    resources.setSystemRamTotalMB(Long.parseLong(parts[0]) / 1024);
                    resources.setSystemRamAvailableMB(Long.parseLong(parts[1]) / 1024);
                }
            }
        } catch (Exception e) {
        }
    }

    private static void detectGpu(SystemResources resources) {
        if (detectNvidiaGpu(resources)) return;
        detectVulkanGpu(resources);
    }

    private static boolean detectNvidiaGpu(SystemResources resources) {
        Path nvidiaSmiPath = CUDAUtils.getNvidiaSmiPath();
        if (nvidiaSmiPath == null) return false;
        try {
            List<String> output = readProcessOutput(nvidiaSmiPath.toString(),
                    "--query-gpu=name,memory.total,memory.used",
                    "--format=csv,noheader,nounits");
            if (output.isEmpty()) return false;
            String line = output.get(0).trim();
            String[] parts = line.split(",");
            if (parts.length >= 3) {
                resources.setGpuType(parts[0].trim());
                resources.setGpuVramTotalMB(Long.parseLong(parts[1].trim()));
                resources.setGpuVramUsedMB(Long.parseLong(parts[2].trim()));
                resources.setAccelerationMode("CUDA");
                resources.setCudaMaxVersion(CUDAUtils.getMaximumCudaVersion());
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    private static void detectVulkanGpu(SystemResources resources) {
        try {
            List<String> output = readProcessOutput("vulkaninfo", "--summary");
            for (String line : output) {
                if (line.contains("deviceName") || line.contains("Device name")) {
                    String name = line.split("=")[1].trim();
                    if (!name.isEmpty() && !name.equals("llvmpipe")) {
                        resources.setGpuType(name);
                        resources.setAccelerationMode("Vulkan");
                        return;
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    private static long parseMemInfoLine(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length >= 2) {
            return Long.parseLong(parts[1]);
        }
        return 0;
    }

    private static List<String> readProcessOutput(String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
            if (!process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return new ArrayList<>();
            }
            return lines;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
