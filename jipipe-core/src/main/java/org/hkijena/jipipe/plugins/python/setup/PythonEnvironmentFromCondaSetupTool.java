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
import org.hkijena.jipipe.plugins.python.utils.CondaUtils;
import org.hkijena.jipipe.plugins.settings.application.JIPipeFileChooserApplicationSettings;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PythonEnvironmentFromCondaSetupTool implements JIPipeEnvironmentSetupTool {
    @Override
    public boolean configure(JIPipeDesktopWorkbench workbench, Component parent, JIPipeEnvironment environment) {

        // At this point we know it's a PythonEnvironment
        PythonEnvironment pythonEnvironment = (PythonEnvironment) environment;

        // Step 1: Find conda executables on the system
        List<Path> detectedCondaPaths = CondaUtils.detectCondaExecutablesFromCommonLocations();
        
        // Step 2: Ask the user if they are happy with the detected conda or want to select it manually
        Path selectedCondaPath = selectCondaExecutable(parent, workbench, detectedCondaPaths);
        if (selectedCondaPath == null) {
            return false; // User cancelled
        }
        
        // Set conda executable path and type
        pythonEnvironment.setExecutablePath(selectedCondaPath);
        pythonEnvironment.setType(PythonEnvironmentType.Conda);
        pythonEnvironment.setLoadFromArtifact(false);
        
        // Step 3: Query the conda environments using the selected conda executable
        List<String> environments = listCondaEnvironments(selectedCondaPath);
        
        // Step 4: Let the user select from the available environments
        String selectedEnvironment = selectCondaEnvironment(parent, environments);
        if (selectedEnvironment == null) {
            return false; // User cancelled
        }
        
        // Set arguments for selected environment
        pythonEnvironment.setArguments(new JIPipeExpressionParameter(
                String.format("ARRAY(\"run\", \"--no-capture-output\", \"-n\", \"%s\", \"python\", \"-u\", script_file)",
                        selectedEnvironment)));
        
        // Show success message
        JOptionPane.showMessageDialog(parent,
                "Conda environment configured successfully!\n" +
                "Conda executable: " + selectedCondaPath + "\n" +
                "Environment: " + selectedEnvironment,
                "Conda Configuration", JOptionPane.INFORMATION_MESSAGE);
        
        return true;
    }
    
    /**
     * Step 2: Ask the user if they are happy with the detected conda or want to select it manually
     * @param parent Parent component for dialogs
     * @param workbench JIPipe desktop workbench
     * @param detectedCondaPaths List of detected conda executable paths
     * @return Selected conda executable path, or null if user cancelled
     */
    private Path selectCondaExecutable(Component parent, JIPipeDesktopWorkbench workbench, List<Path> detectedCondaPaths) {
        if (detectedCondaPaths.isEmpty()) {
            // No conda found, let user select manually
            Path selectedCondaPath = JIPipeDesktop.openFile(parent, workbench,
                    JIPipeFileChooserApplicationSettings.LastDirectoryKey.External,
                    getName(),
                    null,
                    new FileNameExtensionFilter("Conda executable", "exe", "bat", "sh", "bash"));
            
            if (selectedCondaPath == null) {
                return null; // User cancelled
            }
            
            // Show confirmation dialog
            int result = JOptionPane.showConfirmDialog(parent,
                    "Conda executable set to: " + selectedCondaPath + "\n\n" +
                    "Do you want to continue with this selection?",
                    "Confirm Conda Selection",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            
            return result == JOptionPane.YES_OPTION ? selectedCondaPath : null;
        } else {
            // Found conda executables, offer selection to user
            String[] options = new String[detectedCondaPaths.size() + 1];
            for (int i = 0; i < detectedCondaPaths.size(); i++) {
                options[i] = detectedCondaPaths.get(i).toString();
            }
            options[detectedCondaPaths.size()] = "Select manually...";
            
            String selection = (String) JOptionPane.showInputDialog(parent,
                    "The following conda executables were found on your system:\n\n" +
                    "Please select which one to use, or choose 'Select manually...' to pick a different location.",
                    "Select Conda executable",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);
            
            if (selection == null) {
                return null; // User cancelled
            }
            
            Path selectedCondaPath;
            if (selection.equals("Select manually...")) {
                // Manual selection
                selectedCondaPath = JIPipeDesktop.openFile(parent, workbench,
                        JIPipeFileChooserApplicationSettings.LastDirectoryKey.External,
                        "Select Conda executable",
                        null,
                        new FileNameExtensionFilter("Conda executable", "exe", "bat", "sh", "bash"));
                
                if (selectedCondaPath == null) {
                    return null; // User cancelled
                }
            } else {
                // Use detected path
                selectedCondaPath = Paths.get(selection);
            }
            
            // Show confirmation dialog
            int result = JOptionPane.showConfirmDialog(parent,
                    "Selected conda executable: " + selectedCondaPath + "\n\n" +
                    "Do you want to continue with this selection?",
                    "Confirm Conda Selection",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            
            return result == JOptionPane.YES_OPTION ? selectedCondaPath : null;
        }
    }
    
    /**
     * Step 4: Let the user select from the available environments
     * @param parent Parent component for dialogs
     * @param environments List of available conda environments
     * @return Selected environment name, or null if user cancelled
     */
    private String selectCondaEnvironment(Component parent, List<String> environments) {
        if (environments.isEmpty()) {
            // No environments found, use base
            int result = JOptionPane.showConfirmDialog(parent,
                    "No conda environments found. Would you like to use the 'base' environment?",
                    "No Environments Found",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            
            if (result == JOptionPane.YES_OPTION) {
                return "base";
            } else {
                return null; // User cancelled
            }
        } else {
            // Let user select environment
            String envSelection = (String) JOptionPane.showInputDialog(parent,
                    "The following conda environments are available:\n\n" +
                    "Please select which environment to use:",
                    "Select Conda environment",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    environments.toArray(new String[0]),
                    environments.get(0));
            
            return envSelection;
        }
    }

    /**
     * Lists available conda environments
     * @param condaPath Path to conda executable
     * @return List of environment names
     */
    private List<String> listCondaEnvironments(Path condaPath) {
        List<String> environments = new ArrayList<>();
        
        try {
            // Build command to list environments
            ProcessBuilder processBuilder;
            if (SystemUtils.IS_OS_WINDOWS) {
                processBuilder = new ProcessBuilder(condaPath.toString(), "env", "list");
            } else {
                processBuilder = new ProcessBuilder("bash", "-c", condaPath.toString() + " env list");
            }
            
            // Start process
            Process process = processBuilder.start();
            
            // Read output
            try (BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Skip header and empty lines
                    if (line.startsWith("#") || line.trim().isEmpty()) {
                        continue;
                    }
                    
                    // Parse environment name (format: environment_name path_to_env)
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 1) {
                        String envName = parts[0];
                        // Skip "*" which indicates the current environment
                        if (!envName.equals("*")) {
                            environments.add(envName);
                        }
                    }
                }
            }
            
            // Wait for process to complete
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                // If command fails, try alternative approach
                return getCondaEnvironmentsAlternative(condaPath);
            }
            
        } catch (IOException | InterruptedException e) {
            // If command fails, try alternative approach
            return getCondaEnvironmentsAlternative(condaPath);
        }
        
        return environments;
    }

    /**
     * Alternative method to get conda environments if the main method fails
     * @param condaPath Path to conda executable
     * @return List of environment names
     */
    private List<String> getCondaEnvironmentsAlternative(Path condaPath) {
        List<String> environments = new ArrayList<>();
        
        // Add base environment as fallback
        environments.add("base");
        
        // Try to get environments from conda info
        try {
            ProcessBuilder processBuilder;
            if (SystemUtils.IS_OS_WINDOWS) {
                processBuilder = new ProcessBuilder(condaPath.toString(), "info", "--envs");
            } else {
                processBuilder = new ProcessBuilder("bash", "-c", condaPath.toString() + " info --envs");
            }
            
            Process process = processBuilder.start();
            
            try (BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Skip header and empty lines
                    if (line.startsWith("#") || line.trim().isEmpty()) {
                        continue;
                    }
                    
                    // Parse environment name (format: * environment_name path_to_env)
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 2) {
                        String envName = parts[1];
                        if (!envName.equals("*") && !envName.equals("base")) {
                            environments.add(envName);
                        }
                    }
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                // If still fails, return just base environment
                return List.of("base");
            }
            
        } catch (IOException | InterruptedException e) {
            // If everything fails, return just base environment
            return List.of("base");
        }
        
        return environments;
    }

    @Override
    public String getName() {
        return "Import existing Python (Conda)";
    }

    @Override
    public String getDescription() {
        return "Configures this service connector to utilize an existing Conda environment.";
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
