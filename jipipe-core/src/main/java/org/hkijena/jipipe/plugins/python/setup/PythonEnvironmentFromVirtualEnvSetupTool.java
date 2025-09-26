package org.hkijena.jipipe.plugins.python.setup;

import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.environments.JIPipeEnvironmentSetupTool;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.plugins.python.PythonEnvironment;
import org.hkijena.jipipe.plugins.python.PythonEnvironmentType;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionEvaluator;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.settings.application.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringQueryExpressionAndStringPairParameter;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PythonEnvironmentFromVirtualEnvSetupTool implements JIPipeEnvironmentSetupTool {
    @Override
    public boolean configure(JIPipeDesktopWorkbench workbench, Component parent, JIPipeEnvironment environment) {

        // At this point we know it's a PythonEnvironment
        PythonEnvironment pythonEnvironment = (PythonEnvironment) environment;

        // Try to auto-detect virtual environments
        List<Path> detectedVirtualEnvPaths = detectVirtualEnvironments();
        
        if (detectedVirtualEnvPaths.isEmpty()) {
            // No virtual environments found, let user select manually
            Path selectedVirtualEnvPath = JIPipeDesktop.openDirectory(parent, workbench,
                    JIPipeFileChooserApplicationSettings.LastDirectoryKey.External,
                    "Select Python virtual environment directory",
                    null);
            
            if (selectedVirtualEnvPath == null) {
                return false; // User cancelled
            }
            
            // Validate the selected virtual environment
            if (!isValidVirtualEnvironment(selectedVirtualEnvPath)) {
                JOptionPane.showMessageDialog(parent,
                        "The selected directory does not appear to be a valid Python virtual environment.\n" +
                        "Please select a directory containing a 'pyvenv.cfg' file or the appropriate Python executable.",
                        "Invalid Virtual Environment", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            
            // Configure the Python environment
            configureVirtualEnvironment(pythonEnvironment, selectedVirtualEnvPath);
            
            JOptionPane.showMessageDialog(parent,
                    "Python virtual environment configured successfully!\n" +
                    "Virtual environment directory: " + selectedVirtualEnvPath + "\n" +
                    "Environment type: Virtual Environment\n" +
                    "You can modify the arguments in the configuration if needed.",
                    "Python Configuration", JOptionPane.INFORMATION_MESSAGE);
            
            return true;
        } else {
            // Found virtual environments, offer selection to user
            String[] options = new String[detectedVirtualEnvPaths.size() + 1];
            for (int i = 0; i < detectedVirtualEnvPaths.size(); i++) {
                options[i] = detectedVirtualEnvPaths.get(i).toString();
            }
            options[detectedVirtualEnvPaths.size()] = "Select manually...";
            
            String selection = (String) JOptionPane.showInputDialog(parent,
                    "The following Python virtual environments were found on your system:\n\n" +
                    "Please select which one to use, or choose 'Select manually...' to pick a different location.",
                    "Select Python virtual environment",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);
            
            if (selection == null) {
                return false; // User cancelled
            }
            
            Path selectedVirtualEnvPath;
            if (selection.equals("Select manually...")) {
                // Manual selection
                selectedVirtualEnvPath = JIPipeDesktop.openDirectory(parent, workbench,
                        JIPipeFileChooserApplicationSettings.LastDirectoryKey.External,
                        "Select Python virtual environment directory",
                        null);
                
                if (selectedVirtualEnvPath == null) {
                    return false; // User cancelled
                }
                
                // Validate the selected virtual environment
                if (!isValidVirtualEnvironment(selectedVirtualEnvPath)) {
                    JOptionPane.showMessageDialog(parent,
                            "The selected directory does not appear to be a valid Python virtual environment.\n" +
                            "Please select a directory containing a 'pyvenv.cfg' file or the appropriate Python executable.",
                            "Invalid Virtual Environment", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            } else {
                // Use detected path
                selectedVirtualEnvPath = Paths.get(selection);
            }
            
            // Configure the Python environment
            configureVirtualEnvironment(pythonEnvironment, selectedVirtualEnvPath);
            
            JOptionPane.showMessageDialog(parent,
                    "Python virtual environment configured successfully!\n" +
                    "Virtual environment directory: " + selectedVirtualEnvPath + "\n" +
                    "Environment type: Virtual Environment\n" +
                    "You can modify the arguments in the configuration if needed.",
                    "Python Configuration", JOptionPane.INFORMATION_MESSAGE);
            
            return true;
        }
    }

    /**
     * Detects Python virtual environments on the system based on the operating system
     * @return List of detected virtual environment paths
     */
    private List<Path> detectVirtualEnvironments() {
        List<Path> result = new ArrayList<>();
        
        if (SystemUtils.IS_OS_WINDOWS) {
            // Windows: Check common virtual environment locations
            String[] commonPaths = {
                System.getenv("USERPROFILE") + "\\Environments",
                System.getenv("USERPROFILE") + "\\Anaconda3\\envs",
                System.getenv("USERPROFILE") + "\\Miniconda3\\envs",
                System.getenv("LOCALAPPDATA") + "\\Continuum\\anaconda3\\envs",
                System.getenv("LOCALAPPDATA") + "\\Continuum\\miniconda3\\envs"
            };
            
            for (String path : commonPaths) {
                try {
                    Path envPath = Paths.get(path).toAbsolutePath().normalize();
                    if (Files.isDirectory(envPath)) {
                        // List all subdirectories as potential virtual environments
                        try (var dirs = Files.list(envPath)) {
                            dirs.filter(Files::isDirectory)
                                .filter(this::isValidVirtualEnvironment)
                                .forEach(result::add);
                        }
                    }
                } catch (Exception e) {
                    // Ignore invalid paths
                }
            }
            
            // Also check common project directories
            String[] projectPaths = {
                System.getenv("USERPROFILE") + "\\Documents\\PythonProjects",
                System.getenv("USERPROFILE") + "\\Projects",
                System.getenv("USERPROFILE") + "\\dev"
            };
            
            for (String path : projectPaths) {
                try {
                    Path projectPath = Paths.get(path).toAbsolutePath().normalize();
                    if (Files.isDirectory(projectPath)) {
                        // Recursively search for virtual environments
                        searchForVirtualEnvironments(projectPath, result);
                    }
                } catch (Exception e) {
                    // Ignore invalid paths
                }
            }
        } else if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC_OSX) {
            // Linux/macOS: Check common virtual environment locations
            String[] commonPaths = {
                "~/Environments",
                "~/anaconda3/envs",
                "~/miniconda3/envs",
                "~/opt/anaconda3/envs",
                "~/opt/miniconda3/envs",
                "/opt/anaconda3/envs",
                "/opt/miniconda3/envs",
                "/usr/local/anaconda3/envs",
                "/usr/local/miniconda3/envs",
                "~/venvs",
                "~/python-envs",
                "~/Documents/PythonProjects",
                "~/Projects",
                "~/dev"
            };
            
            for (String path : commonPaths) {
                try {
                    Path envPath = Paths.get(path).toAbsolutePath().normalize();
                    if (Files.isDirectory(envPath)) {
                        // List all subdirectories as potential virtual environments
                        try (var dirs = Files.list(envPath)) {
                            dirs.filter(Files::isDirectory)
                                .filter(this::isValidVirtualEnvironment)
                                .forEach(result::add);
                        }
                    }
                } catch (Exception e) {
                    // Ignore invalid paths
                }
            }
            
            // Also search in common project directories
            String[] projectPaths = {
                "~/Documents/PythonProjects",
                "~/Projects",
                "~/dev"
            };
            
            for (String path : projectPaths) {
                try {
                    Path projectPath = Paths.get(path).toAbsolutePath().normalize();
                    if (Files.isDirectory(projectPath)) {
                        // Recursively search for virtual environments
                        searchForVirtualEnvironments(projectPath, result);
                    }
                } catch (Exception e) {
                    // Ignore invalid paths
                }
            }
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

    /**
     * Recursively searches for virtual environments in the given directory
     * @param directory The directory to search in
     * @param result List to add found virtual environments to
     */
    private void searchForVirtualEnvironments(Path directory, List<Path> result) {
        try {
            // Check if this directory is a virtual environment
            if (isValidVirtualEnvironment(directory)) {
                result.add(directory);
                return;
            }
            
            // Search subdirectories
            try (var dirs = Files.list(directory)) {
                dirs.filter(Files::isDirectory)
                   .forEach(subDir -> searchForVirtualEnvironments(subDir, result));
            }
        } catch (Exception e) {
            // Ignore invalid paths
        }
    }

    /**
     * Validates if the given path is a valid Python virtual environment
     * @param path Path to validate
     * @return true if it's a valid virtual environment, false otherwise
     */
    private boolean isValidVirtualEnvironment(Path path) {
        try {
            // Check if pyvenv.cfg exists (standard virtual environment marker)
            Path pyvenvCfg = path.resolve("pyvenv.cfg");
            if (Files.isRegularFile(pyvenvCfg)) {
                return true;
            }
            
            // Check for virtual environment executables
            if (SystemUtils.IS_OS_WINDOWS) {
                Path pythonExe = path.resolve("Scripts").resolve("python.exe");
                if (Files.isRegularFile(pythonExe)) {
                    return true;
                }
            } else {
                Path pythonBin = path.resolve("bin").resolve("python");
                if (Files.isRegularFile(pythonBin)) {
                    return true;
                }
                
                // Also check for python3
                Path python3Bin = path.resolve("bin").resolve("python3");
                if (Files.isRegularFile(python3Bin)) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Configures the Python environment for the selected virtual environment
     * @param pythonEnvironment The Python environment to configure
     * @param virtualEnvPath Path to the virtual environment directory
     */
    private void configureVirtualEnvironment(PythonEnvironment pythonEnvironment, Path virtualEnvPath) {
        // Set environment type
        pythonEnvironment.setType(PythonEnvironmentType.VirtualEnvironment);
        
        // Set Python executable path based on platform
        Path pythonExecutable;
        if (SystemUtils.IS_OS_WINDOWS) {
            pythonExecutable = virtualEnvPath.resolve("Scripts").resolve("python.exe");
        } else {
            pythonExecutable = virtualEnvPath.resolve("bin").resolve("python");
        }
        pythonEnvironment.setExecutablePath(pythonExecutable);
        
        // Set default arguments for virtual environment
        pythonEnvironment.setArguments(new JIPipeExpressionParameter("ARRAY(\"-u\", script_file)"));
        
        // Configure environment variables
        pythonEnvironment.getEnvironmentVariables().clear();
        
        if (SystemUtils.IS_OS_WINDOWS) {
            // Add Scripts directory to PATH
            Path scriptsPath = virtualEnvPath.resolve("Scripts");
            pythonEnvironment.getEnvironmentVariables().add(new StringQueryExpressionAndStringPairParameter(
                    "\"" + JIPipeExpressionEvaluator.escapeString(scriptsPath.toString()) + ";\"" + " + Path",
                    "Path"
            ));
            
            // Set VIRTUAL_ENV
            pythonEnvironment.getEnvironmentVariables().add(new StringQueryExpressionAndStringPairParameter(
                    "\"" + JIPipeExpressionEvaluator.escapeString(virtualEnvPath.toString()) + "\"",
                    "VIRTUAL_ENV"
            ));
        } else {
            // Add bin directory to PATH
            Path binPath = virtualEnvPath.resolve("bin");
            pythonEnvironment.getEnvironmentVariables().add(new StringQueryExpressionAndStringPairParameter(
                    "\"" + JIPipeExpressionEvaluator.escapeString(binPath.toString()) + ":\"" + " + PATH",
                    "PATH"
            ));
            
            // Set VIRTUAL_ENV
            pythonEnvironment.getEnvironmentVariables().add(new StringQueryExpressionAndStringPairParameter(
                    "\"" + JIPipeExpressionEvaluator.escapeString(virtualEnvPath.toString()) + "\"",
                    "VIRTUAL_ENV"
            ));
        }
    }

    @Override
    public String getName() {
        return "Import existing Python (venv)";
    }

    @Override
    public String getDescription() {
        return "Configures this service connector to utilize an existing Python virtual environment.";
    }

    @Override
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("apps/python.png");
    }

    @Override
    public boolean accepts(Class<? extends JIPipeEnvironment> environmentClass) {
        return PythonEnvironment.class.isAssignableFrom(environmentClass);
    }
}
