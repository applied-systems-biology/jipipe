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
import org.hkijena.jipipe.api.JIPipeProgressInfo;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CUDAUtils {

    public static Path getNvidiaSmiPath() {
        Path nvidiaSmiPath = null;

        if (SystemUtils.IS_OS_WINDOWS) {
            // Common paths for Windows
            nvidiaSmiPath = PathUtils.findAnyOf(
                    Paths.get("C:\\Program Files\\NVIDIA Corporation\\NVSMI\\nvidia-smi.exe"),
                    Paths.get("C:\\Windows\\System32\\nvidia-smi.exe")
            );
        } else if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC) {
            // Common paths for Linux/macOS
            nvidiaSmiPath = PathUtils.findAnyOf(
                    Paths.get("/usr/bin/nvidia-smi"),
                    Paths.get("/usr/local/bin/nvidia-smi"),
                    Paths.get("/bin/nvidia-smi")
            );
        }

        // If nvidia-smi is in the PATH, fallback to calling it directly
        if (nvidiaSmiPath == null) {
            Path fallbackPath = Paths.get("nvidia-smi");
            if (PathUtils.isExecutable(fallbackPath)) {
                return fallbackPath;
            }
        }

        return nvidiaSmiPath;
    }

    public static boolean hasCudaSupport() {
        return getNvidiaSmiPath() != null;
    }

    /**
     * Get the maximum CUDA version supported by the installed driver.
     */
    public static int getMaximumCudaVersion() {
        Path nvidiaSmiPath = getNvidiaSmiPath();
        if (nvidiaSmiPath == null) {
            return 0;
        }

        try {
            String output = ProcessUtils.queryFast(nvidiaSmiPath, new JIPipeProgressInfo());
            if(output == null) {
                return 0;
            }
            return (int) StringUtils.tryParseDouble(parseMaxCudaVersion(output), 0);
        } catch (Exception e) {
           return 0;
        }
    }

    /**
     * Get the minimum CUDA version supported based on GPU compute capability.
     */
    public static int getMinimumCudaVersion() {
        Path nvidiaSmiPath = getNvidiaSmiPath();
        if (nvidiaSmiPath == null) {
            return 0;
        }

        try {
            String output = ProcessUtils.queryFast(nvidiaSmiPath, new JIPipeProgressInfo(), "--query-gpu=compute_cap", "--format=csv,noheader");
            String[] computeCaps = output.split("\\r?\\n");
            double minCudaVersion = Double.MAX_VALUE;

            for (String computeCap : computeCaps) {
                double capability = Double.parseDouble(computeCap.trim());
                double minVersion = computeCapabilityToCudaVersion(capability);
                minCudaVersion = Math.min(minCudaVersion, minVersion);
            }

            return (int) StringUtils.tryParseDouble(String.format("%.1f", minCudaVersion), 0) * 10;
        } catch (Exception e) {
            return 0;
        }
    }

    private static String parseMaxCudaVersion(String nvidiaSmiOutput) {
        String[] lines = nvidiaSmiOutput.split("\\r?\\n");
        for (String line : lines) {
            if (line.contains("CUDA Version")) {
                return line.split(":")[1].trim();
            }
        }
        return "Unknown";
    }


    /**
     * Maps compute capability to the minimum CUDA version.
     */
    private static double computeCapabilityToCudaVersion(double computeCapability) {
        if (computeCapability >= 9.0) {
            return 11.3;
        } else if (computeCapability >= 8.0) {
            return 11.0;
        } else if (computeCapability >= 7.0) {
            return 10.0;
        } else if (computeCapability >= 6.0) {
            return 8.0;
        } else if (computeCapability >= 5.0) {
            return 6.5;
        } else {
            return 5.0;
        }
    }


}
