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
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class PythonEnvironmentFromVirtualEnvSetupTool implements JIPipeEnvironmentSetupTool {
    @Override
    public boolean configure(JIPipeDesktopWorkbench workbench, Component parent, JIPipeEnvironment environment) {

        // At this point we know it's a PythonEnvironment
        PythonEnvironment pythonEnvironment = (PythonEnvironment) environment;

            // No virtual environments found, let user select manually
            Path selectedVirtualEnvPath = JIPipeDesktop.openDirectory(parent, workbench,
                    JIPipeFileChooserApplicationSettings.LastDirectoryKey.External,
                    getName(),
                    null);

            if (selectedVirtualEnvPath == null) {
                return false; // User cancelled
            }

            // Validate the selected virtual environment
            if (!isValidVirtualEnvironment(selectedVirtualEnvPath)) {
                JOptionPane.showMessageDialog(parent,
                        "The selected directory does not appear to be a valid Python virtual environment.\n" +
                        "Please select a directory containing a 'pyvenv.cfg' file or the appropriate Python executable.",
                        getName(), JOptionPane.ERROR_MESSAGE);
                return false;
            }
            
            // Configure the Python environment
            configureVirtualEnvironment(pythonEnvironment, selectedVirtualEnvPath);
            
            JOptionPane.showMessageDialog(parent,
                    "Python virtual environment configured successfully!\n" +
                    "Virtual environment directory: " + selectedVirtualEnvPath + "\n" +
                    "You can modify the arguments in the configuration if needed.",
                    getName(), JOptionPane.INFORMATION_MESSAGE);
            
            return true;
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
