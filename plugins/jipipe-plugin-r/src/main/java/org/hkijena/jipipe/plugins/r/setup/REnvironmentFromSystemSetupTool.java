package org.hkijena.jipipe.plugins.r.setup;

import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.environments.JIPipeEnvironmentSetupTool;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.plugins.r.REnvironment;
import org.hkijena.jipipe.plugins.settings.application.JIPipeFileChooserApplicationSettings;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class REnvironmentFromSystemSetupTool implements JIPipeEnvironmentSetupTool {
    @Override
    public boolean configure(JIPipeDesktopWorkbench workbench, Component parent, JIPipeEnvironment environment) {
        // At this point we know it's an REnvironment
        REnvironment rEnvironment = (REnvironment) environment;

        // Try to auto-detect R executables
        List<Path> detectedRPaths = detectRExecutables();

        if (detectedRPaths.isEmpty()) {
            // No R found, let user select manually
            Path selectedRPath = JIPipeDesktop.openFile(parent, workbench,
                    JIPipeFileChooserApplicationSettings.LastDirectoryKey.External,
                    "Select R executable",
                    null,
                    new FileNameExtensionFilter("R executable", "R", "exe"));

            if (selectedRPath == null) {
                return false; // User cancelled
            }

            // Set both R and RScript to the selected path (user will need to configure RScript separately if needed)
            rEnvironment.setRExecutablePath(selectedRPath);
            rEnvironment.setRScriptExecutablePath(selectedRPath);

            JOptionPane.showMessageDialog(parent,
                    "R executable set to: " + selectedRPath + "\n" +
                            "Note: You may need to manually configure the RScript executable path if it differs from the R executable.",
                    "R Configuration", JOptionPane.INFORMATION_MESSAGE);

            return true;
        } else {
            // Found R executables, offer selection to user
            String[] options = new String[detectedRPaths.size() + 1];
            for (int i = 0; i < detectedRPaths.size(); i++) {
                options[i] = detectedRPaths.get(i).toString();
            }
            options[detectedRPaths.size()] = "Select manually...";

            String selection = (String) JOptionPane.showInputDialog(parent,
                    "The following R executables were found on your system:\n\n" +
                            "Please select which one to use, or choose 'Select manually...' to pick a different location.",
                    "Select R executable",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);

            if (selection == null) {
                return false; // User cancelled
            }

            if (selection.equals("Select manually...")) {
                // Manual selection
                Path selectedRPath = JIPipeDesktop.openFile(parent, workbench,
                        JIPipeFileChooserApplicationSettings.LastDirectoryKey.External,
                        "Select R executable",
                        null,
                        new FileNameExtensionFilter("R executable", "R", "exe"));

                if (selectedRPath == null) {
                    return false; // User cancelled
                }

                rEnvironment.setRExecutablePath(selectedRPath);
                rEnvironment.setRScriptExecutablePath(selectedRPath);
            } else {
                // Use detected path
                Path selectedPath = Paths.get(selection);
                rEnvironment.setRExecutablePath(selectedPath);

                // Try to find RScript in the same directory
                Path rScriptPath = findRScriptInSameDirectory(selectedPath);
                if (rScriptPath != null) {
                    rEnvironment.setRScriptExecutablePath(rScriptPath);
                } else {
                    rEnvironment.setRScriptExecutablePath(selectedPath);
                }
            }

            rEnvironment.setLoadFromArtifact(false);

            JOptionPane.showMessageDialog(parent,
                    "R environment configured successfully!\n" +
                            "R executable: " + rEnvironment.getRExecutablePath() + "\n" +
                            "RScript executable: " + rEnvironment.getRScriptExecutablePath(),
                    "R Configuration", JOptionPane.INFORMATION_MESSAGE);

            return true;
        }
    }

    /**
     * Detects R executables on the system based on the operating system
     *
     * @return List of detected R executable paths
     */
    private List<Path> detectRExecutables() {
        List<Path> result = new ArrayList<>();

        if (SystemUtils.IS_OS_WINDOWS) {
            // Windows: Check common installation paths
            String[] programFilesPaths = {
                    System.getenv("ProgramFiles"),
                    System.getenv("ProgramFiles(x86)")
            };

            for (String programFiles : programFilesPaths) {
                if (programFiles != null) {
                    Path rPath = Paths.get(programFiles, "R", "R.exe");
                    if (Files.isRegularFile(rPath)) {
                        result.add(rPath);
                    }
                }
            }

            // Also check PATH environment variable
            String pathEnv = System.getenv("PATH");
            if (pathEnv != null) {
                for (String path : pathEnv.split(";")) {
                    Path rPath = Paths.get(path, "R.exe");
                    if (Files.isRegularFile(rPath)) {
                        result.add(rPath);
                    }
                }
            }
        } else if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC_OSX) {
            // Linux/macOS: Check common installation paths and PATH
            String[] commonPaths = {
                    "/usr/local/bin/R",
                    "/usr/bin/R",
                    "/opt/R/bin/R",
                    "/usr/local/bin/Rscript",
                    "/usr/bin/Rscript",
                    "/opt/R/bin/Rscript"
            };

            for (String path : commonPaths) {
                Path rPath = Paths.get(path);
                if (Files.isRegularFile(rPath) && Files.isExecutable(rPath)) {
                    result.add(rPath);
                }
            }

            // Also check PATH environment variable
            String pathEnv = System.getenv("PATH");
            if (pathEnv != null) {
                for (String path : pathEnv.split(":")) {
                    Path rPath = Paths.get(path, "R");
                    if (Files.isRegularFile(rPath) && Files.isExecutable(rPath)) {
                        result.add(rPath);
                    }
                    Path rScriptPath = Paths.get(path, "Rscript");
                    if (Files.isRegularFile(rScriptPath) && Files.isExecutable(rScriptPath)) {
                        result.add(rScriptPath);
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

    /**
     * Attempts to find RScript in the same directory as the R executable
     *
     * @param rPath Path to R executable
     * @return Path to RScript executable or null if not found
     */
    private Path findRScriptInSameDirectory(Path rPath) {
        if (SystemUtils.IS_OS_WINDOWS) {
            Path rScriptPath = rPath.getParent().resolve("Rscript.exe");
            if (Files.isRegularFile(rScriptPath)) {
                return rScriptPath;
            }
        } else {
            Path rScriptPath = rPath.getParent().resolve("Rscript");
            if (Files.isRegularFile(rScriptPath) && Files.isExecutable(rScriptPath)) {
                return rScriptPath;
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return "Import existing R (standalone/system-wide)";
    }

    @Override
    public String getDescription() {
        return "Configures this service connector to utilize an existing standalone or system-wide R.";
    }

    @Override
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("apps/rlogo_icon.png");
    }

    @Override
    public boolean accepts(Class<? extends JIPipeEnvironment> environmentClass) {
        return REnvironment.class.isAssignableFrom(environmentClass);
    }
}
