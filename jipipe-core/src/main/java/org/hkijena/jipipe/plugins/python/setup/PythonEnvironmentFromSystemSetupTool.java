package org.hkijena.jipipe.plugins.python.setup;

import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.environments.JIPipeEnvironmentSetupTool;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.plugins.python.PythonEnvironment;
import org.hkijena.jipipe.plugins.python.PythonEnvironmentType;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.settings.application.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.PathUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PythonEnvironmentFromSystemSetupTool implements JIPipeEnvironmentSetupTool {
    @Override
    public boolean configure(JIPipeDesktopWorkbench workbench, Component parent, JIPipeEnvironment environment) {

        // At this point we know it's a PythonEnvironment
        PythonEnvironment pythonEnvironment = (PythonEnvironment) environment;

        // Always try to auto-detect Python executables to show in the selection dialog
        List<Path> detectedPythonPaths = detectPythonExecutables();
        
        // Always show selection dialog to user for better control
        String[] options;
        if (detectedPythonPaths.isEmpty()) {
            // No Python found, only show manual selection option
            options = new String[]{"Select Python executable manually..."};
        } else {
            // Found Python executables, show them as options plus manual selection
            options = new String[detectedPythonPaths.size() + 1];
            for (int i = 0; i < detectedPythonPaths.size(); i++) {
                options[i] = detectedPythonPaths.get(i).toString();
            }
            options[detectedPythonPaths.size()] = "Select Python executable manually...";
        }
        
        String selection = (String) JOptionPane.showInputDialog(parent,
                detectedPythonPaths.isEmpty() ?
                    "No Python executables were found on your system.\n\n" +
                    "Please select a Python executable manually." :
                    "The following Python executables were found on your system:\n\n" +
                    "Please select which one to use, or choose 'Select Python executable manually...' to pick a different location.",
                "Select Python executable",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);
        
        if (selection == null) {
            return false; // User cancelled
        }
        
        Path selectedPythonPath;
        if (selection.equals("Select Python executable manually...")) {
            // Manual selection
            selectedPythonPath = JIPipeDesktop.openFile(parent, workbench,
                    JIPipeFileChooserApplicationSettings.LastDirectoryKey.External,
                    "Select Python executable",
                    null,
                    new FileNameExtensionFilter("Python executable", "exe", "py", "bat"));
            
            if (selectedPythonPath == null) {
                return false; // User cancelled
            }
        } else {
            // Use detected path
            selectedPythonPath = Paths.get(selection);
        }
        
        // Set Python executable path
        pythonEnvironment.setExecutablePath(selectedPythonPath);
        pythonEnvironment.setType(PythonEnvironmentType.System);
        pythonEnvironment.setLoadFromArtifact(false);
        
        // Set default arguments for system Python
        pythonEnvironment.setArguments(new JIPipeExpressionParameter("ARRAY(\"-u\", script_file)"));
        
        JOptionPane.showMessageDialog(parent,
                "Python environment configured successfully!\n" +
                "Python executable: " + selectedPythonPath + "\n" +
                "Environment type: System\n" +
                "You can modify the arguments in the configuration if needed.",
                "Python Configuration", JOptionPane.INFORMATION_MESSAGE);
        
        return true;
    }

    /**
     * Detects Python executables on the system based on the operating system
     * @return List of detected Python executable paths
     */
    private List<Path> detectPythonExecutables() {
        List<Path> result = new ArrayList<>();
        
        if (SystemUtils.IS_OS_WINDOWS) {
            // Windows: Check common installation paths
            String[] programFilesPaths = {
                System.getenv("ProgramFiles"),
                System.getenv("ProgramFiles(x86)")
            };
            
            for (String programFiles : programFilesPaths) {
                if (programFiles != null) {
                    // Check Python 3.x installations
                    Path pythonPath = Paths.get(programFiles, "Python39", "python.exe");
                    if (Files.isRegularFile(pythonPath)) {
                        result.add(pythonPath);
                    }
                    
                    pythonPath = Paths.get(programFiles, "Python310", "python.exe");
                    if (Files.isRegularFile(pythonPath)) {
                        result.add(pythonPath);
                    }
                    
                    pythonPath = Paths.get(programFiles, "Python311", "python.exe");
                    if (Files.isRegularFile(pythonPath)) {
                        result.add(pythonPath);
                    }
                    
                    pythonPath = Paths.get(programFiles, "Python312", "python.exe");
                    if (Files.isRegularFile(pythonPath)) {
                        result.add(pythonPath);
                    }
                    
                    // Check older Python 2.x installations
                    pythonPath = Paths.get(programFiles, "Python27", "python.exe");
                    if (Files.isRegularFile(pythonPath)) {
                        result.add(pythonPath);
                    }
                }
            }
            
            // Also check PATH environment variable
            String pathEnv = System.getenv("PATH");
            if (pathEnv != null) {
                for (String path : pathEnv.split(";")) {
                    Path pythonPath = Paths.get(path, "python.exe");
                    if (Files.isRegularFile(pythonPath)) {
                        result.add(pythonPath);
                    }
                    
                    Path python3Path = Paths.get(path, "python3.exe");
                    if (Files.isRegularFile(python3Path)) {
                        result.add(python3Path);
                    }
                }
            }
        } else if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC_OSX) {
            // Linux/macOS: Check common installation paths and PATH
            String[] commonPaths = {
                "/usr/bin/python3",
                "/usr/local/bin/python3",
                "/opt/homebrew/bin/python3",
                "/usr/bin/python",
                "/usr/local/bin/python",
                PathUtils.getHomeDirectory().resolve("anaconda3/bin/python").toString(),
                PathUtils.getHomeDirectory().resolve("miniconda3/bin/python").toString(),
                "/opt/anaconda3/bin/python",
                "/opt/miniconda3/bin/python",
                "/usr/local/anaconda3/bin/python",
                "/usr/local/miniconda3/bin/python"
            };
            
            for (String path : commonPaths) {
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
                    Path pythonPath = Paths.get(path, "python3");
                    if (Files.isRegularFile(pythonPath) && Files.isExecutable(pythonPath)) {
                        result.add(pythonPath);
                    }
                    
                    Path pythonPath2 = Paths.get(path, "python");
                    if (Files.isRegularFile(pythonPath2) && Files.isExecutable(pythonPath2)) {
                        result.add(pythonPath2);
                    }
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

    @Override
    public String getName() {
        return "Import existing Python (standalone/system-wide)";
    }

    @Override
    public String getDescription() {
        return "Configures this service connector to utilize an existing standalone or system-wide Python. " +
                "For Conda/VEnv use other tools!";
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
