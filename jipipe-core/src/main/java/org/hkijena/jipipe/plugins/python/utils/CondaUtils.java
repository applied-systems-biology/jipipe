package org.hkijena.jipipe.plugins.python.utils;

import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.utils.PathUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CondaUtils {
    public static String[] getCommonCondaPathsLinux() {
        return new String[]{
                // User home directory installations
                PathUtils.getHomeDirectory().resolve("anaconda3/bin/conda").toString(),
                PathUtils.getHomeDirectory().resolve("miniconda3/bin/conda").toString(),
                PathUtils.getHomeDirectory().resolve("miniforge3/bin/conda").toString(),
                PathUtils.getHomeDirectory().resolve("anaconda2/bin/conda").toString(),
                PathUtils.getHomeDirectory().resolve("miniconda2/bin/conda").toString(),
                PathUtils.getHomeDirectory().resolve("miniforge2/bin/conda").toString(),
                PathUtils.getHomeDirectory().resolve("Anaconda3/bin/conda").toString(),  // uppercase
                PathUtils.getHomeDirectory().resolve("Miniconda3/bin/conda").toString(),  // uppercase
                PathUtils.getHomeDirectory().resolve("Miniforge3/bin/conda").toString(),  // uppercase
                PathUtils.getHomeDirectory().resolve("Anaconda2/bin/conda").toString(),  // uppercase
                PathUtils.getHomeDirectory().resolve("Miniconda2/bin/conda").toString(),  // uppercase
                PathUtils.getHomeDirectory().resolve("Miniforge2/bin/conda").toString(),  // uppercase

                // System-wide installations - common locations
                "/opt/anaconda3/bin/conda",
                "/opt/miniconda3/bin/conda",
                "/opt/miniforge3/bin/conda",
                "/opt/anaconda2/bin/conda",
                "/opt/miniconda2/bin/conda",
                "/opt/miniforge2/bin/conda",

                "/usr/local/anaconda3/bin/conda",
                "/usr/local/miniconda3/bin/conda",
                "/usr/local/miniforge3/bin/conda",
                "/usr/local/anaconda2/bin/conda",
                "/usr/local/miniconda2/bin/conda",
                "/usr/local/miniforge2/bin/conda",

                // Additional system locations
                "/opt/conda/bin/conda",
                "/usr/bin/conda",
                "/usr/local/bin/conda",
                "/opt/miniforge/bin/conda",  // Generic miniforge
                "/opt/anaconda/bin/conda",   // Generic anaconda
                "/opt/miniconda/bin/conda",  // Generic miniconda

                // Homebrew locations (if installed via Homebrew)
                "/usr/local/anaconda/bin/conda",
                "/usr/local/miniconda/bin/conda",
                "/opt/homebrew/anaconda/bin/conda",  // Apple Silicon Mac
                "/opt/homebrew/miniconda/bin/conda"  // Apple Silicon Mac
        };
    }

    public static String[] getCommonCondaPathsMacOS() {
        return new String[]{
                // User home directory installations
                PathUtils.getHomeDirectory().resolve("anaconda3/bin/conda").toString(),
                PathUtils.getHomeDirectory().resolve("miniconda3/bin/conda").toString(),
                PathUtils.getHomeDirectory().resolve("miniforge3/bin/conda").toString(),
                PathUtils.getHomeDirectory().resolve("anaconda2/bin/conda").toString(),
                PathUtils.getHomeDirectory().resolve("miniconda2/bin/conda").toString(),
                PathUtils.getHomeDirectory().resolve("miniforge2/bin/conda").toString(),
                PathUtils.getHomeDirectory().resolve("Anaconda3/bin/conda").toString(),  // uppercase
                PathUtils.getHomeDirectory().resolve("Miniconda3/bin/conda").toString(),  // uppercase
                PathUtils.getHomeDirectory().resolve("Miniforge3/bin/conda").toString(),  // uppercase
                PathUtils.getHomeDirectory().resolve("Anaconda2/bin/conda").toString(),  // uppercase
                PathUtils.getHomeDirectory().resolve("Miniconda2/bin/conda").toString(),  // uppercase
                PathUtils.getHomeDirectory().resolve("Miniforge2/bin/conda").toString(),  // uppercase

                // System-wide installations - common locations
                "/opt/anaconda3/bin/conda",
                "/opt/miniconda3/bin/conda",
                "/opt/miniforge3/bin/conda",
                "/opt/anaconda2/bin/conda",
                "/opt/miniconda2/bin/conda",
                "/opt/miniforge2/bin/conda",

                "/usr/local/anaconda3/bin/conda",
                "/usr/local/miniconda3/bin/conda",
                "/usr/local/miniforge3/bin/conda",
                "/usr/local/anaconda2/bin/conda",
                "/usr/local/miniconda2/bin/conda",
                "/usr/local/miniforge2/bin/conda",

                // macOS-specific locations
                "/Applications/anaconda3/bin/conda",
                "/Applications/miniconda3/bin/conda",
                "/Applications/miniforge3/bin/conda",
                "/Applications/anaconda2/bin/conda",
                "/Applications/miniconda2/bin/conda",
                "/Applications/miniforge2/bin/conda",

                // Additional system locations
                "/opt/conda/bin/conda",
                "/usr/bin/conda",
                "/usr/local/bin/conda",
                "/opt/miniforge/bin/conda",  // Generic miniforge
                "/opt/anaconda/bin/conda",   // Generic anaconda
                "/opt/miniconda/bin/conda",  // Generic miniconda

                // Homebrew locations (if installed via Homebrew)
                "/usr/local/anaconda/bin/conda",
                "/usr/local/miniconda/bin/conda",
                "/opt/homebrew/anaconda/bin/conda",  // Apple Silicon Mac
                "/opt/homebrew/miniconda/bin/conda",  // Apple Silicon Mac
                "/opt/homebrew/Caskroom/miniconda/base/bin/conda",  // Homebrew Cask
                "/opt/homebrew/Caskroom/miniforge/base/bin/conda"   // Homebrew Cask
        };
    }

    public static void getCommonCondaPathsWindows(List<Path> result) {
        // Windows: Check common installation paths
        String[] programFilesPaths = {
                System.getenv("ProgramFiles"),
                System.getenv("ProgramFiles(x86)"),
                System.getenv("ProgramFilesW6432")  // Additional Program Files directory
        };

        for (String programFiles : programFilesPaths) {
            if (programFiles != null) {
                // Check Anaconda installations (v2 and v3)
                Path anaconda3Path = Paths.get(programFiles, "Anaconda3", "Scripts", "conda.exe");
                if (Files.isRegularFile(anaconda3Path)) {
                    result.add(anaconda3Path);
                }

                Path anaconda2Path = Paths.get(programFiles, "Anaconda2", "Scripts", "conda.exe");
                if (Files.isRegularFile(anaconda2Path)) {
                    result.add(anaconda2Path);
                }

                // Check Miniconda installations (v2 and v3)
                Path miniconda3Path = Paths.get(programFiles, "Miniconda3", "Scripts", "conda.exe");
                if (Files.isRegularFile(miniconda3Path)) {
                    result.add(miniconda3Path);
                }

                Path miniconda2Path = Paths.get(programFiles, "Miniconda2", "Scripts", "conda.exe");
                if (Files.isRegularFile(miniconda2Path)) {
                    result.add(miniconda2Path);
                }

                // Check Miniforge installations (v2 and v3)
                Path miniforge3Path = Paths.get(programFiles, "Miniforge3", "Scripts", "conda.exe");
                if (Files.isRegularFile(miniforge3Path)) {
                    result.add(miniforge3Path);
                }

                Path miniforge2Path = Paths.get(programFiles, "Miniforge2", "Scripts", "conda.exe");
                if (Files.isRegularFile(miniforge2Path)) {
                    result.add(miniforge2Path);
                }
            }
        }

        // Check user home directory paths for non-Admin installations
        String userProfile = PathUtils.getHomeDirectory().toString();
        if (userProfile != null) {
            // Comprehensive user home directory installations
            String[] userHomePaths = {
                    userProfile,  // %USERPROFILE%
                    userProfile + "\\anaconda3",  // %USERPROFILE%\anaconda3
                    userProfile + "\\miniconda3",  // %USERPROFILE%\miniconda3
                    userProfile + "\\miniforge3",  // %USERPROFILE%\miniforge3
                    userProfile + "\\anaconda2",  // %USERPROFILE%\anaconda2
                    userProfile + "\\miniconda2",  // %USERPROFILE%\miniconda2
                    userProfile + "\\miniforge2",  // %USERPROFILE%\miniforge2
                    userProfile + "\\Anaconda3",  // %USERPROFILE%\Anaconda3 (uppercase)
                    userProfile + "\\Miniconda3",  // %USERPROFILE%\Miniconda3 (uppercase)
                    userProfile + "\\Miniforge3",  // %USERPROFILE%\Miniforge3 (uppercase)
                    userProfile + "\\Anaconda2",  // %USERPROFILE%\Anaconda2 (uppercase)
                    userProfile + "\\Miniconda2",  // %USERPROFILE%\Miniconda2 (uppercase)
                    userProfile + "\\Miniforge2"   // %USERPROFILE%\Miniforge2 (uppercase)
            };

            for (String userPath : userHomePaths) {
                // Check if it's a direct path to conda.exe
                Path condaPath = Paths.get(userPath, "Scripts", "conda.exe");
                if (Files.isRegularFile(condaPath)) {
                    result.add(condaPath);
                }

                // Check if it's a path to the installation directory
                Path installPath = Paths.get(userPath);
                if (Files.isDirectory(installPath) && Files.isDirectory(installPath.resolve("Scripts"))) {
                    Path scriptsCondaPath = installPath.resolve("Scripts").resolve("conda.exe");
                    if (Files.isRegularFile(scriptsCondaPath)) {
                        result.add(scriptsCondaPath);
                    }
                }
            }
        }

        // Also check PATH environment variable
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            for (String path : pathEnv.split(";")) {
                Path condaPath = Paths.get(path, "conda.exe");
                if (Files.isRegularFile(condaPath)) {
                    result.add(condaPath);
                }
            }
        }
    }

    public static void getCommonCondaPathsMacOS(List<Path> result) {
        // macOS: Comprehensive detection of all conda distributions
        String[] macPaths = getCommonCondaPathsMacOS();

        for (String path : macPaths) {
            try {
                Path expandedPath = Paths.get(path).toAbsolutePath().normalize();
                if (Files.isRegularFile(expandedPath) && Files.isExecutable(expandedPath)) {
                    result.add(expandedPath);
                }
            } catch (Exception e) {
                // Ignore invalid paths
            }
        }

        // Also check PATH environment variable
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            for (String path : pathEnv.split(":")) {
                Path condaPath = Paths.get(path, "conda");
                if (Files.isRegularFile(condaPath) && Files.isExecutable(condaPath)) {
                    result.add(condaPath);
                }
            }
        }
    }

    public static void getCommonCondaPathsLinux(List<Path> result) {
        // Linux: Comprehensive detection of all conda distributions
        String[] linuxPaths = getCommonCondaPathsLinux();

        for (String path : linuxPaths) {
            try {
                Path expandedPath = Paths.get(path).toAbsolutePath().normalize();
                if (Files.isRegularFile(expandedPath) && Files.isExecutable(expandedPath)) {
                    result.add(expandedPath);
                }
            } catch (Exception e) {
                // Ignore invalid paths
            }
        }

        // Also check PATH environment variable
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            for (String path : pathEnv.split(":")) {
                Path condaPath = Paths.get(path, "conda");
                if (Files.isRegularFile(condaPath) && Files.isExecutable(condaPath)) {
                    result.add(condaPath);
                }
            }
        }
    }

    /**
     * Detects conda executables on the system based on the operating system.
     * On Windows, checks both system-wide installations (Program Files) and user home directory installations.
     * On Linux and macOS, checks common installation paths and PATH environment variable.
     *
     * @return List of detected conda executable paths
     */
    public static List<Path> detectCondaExecutablesFromCommonLocations() {
        List<Path> result = new ArrayList<>();

        if (SystemUtils.IS_OS_WINDOWS) {
            getCommonCondaPathsWindows(result);


        } else if (SystemUtils.IS_OS_LINUX) {
            getCommonCondaPathsLinux(result);
        } else if (SystemUtils.IS_OS_MAC_OSX) {
            getCommonCondaPathsMacOS(result);
        }

        // Remove duplicates
        List<Path> uniqueResult = new ArrayList<>();
        for (Path path : result) {
            if (!uniqueResult.contains(path)) {
                uniqueResult.add(path);
            }
        }

        return uniqueResult;
    }
}
