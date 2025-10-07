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

package org.hkijena.jipipe.desktop.commons.components.filechoosernative.linux;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.desktop.commons.components.filechoosernative.ModernNativeFileChooser;
import org.hkijena.jipipe.desktop.commons.components.filechoosernative.ModernNativeFileChooserImplementation;
import org.hkijena.jipipe.desktop.commons.components.filechoosernative.ModernNativeFileChooserResponse;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.ProcessUtils;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class ModernNativeFileChooserLinux implements ModernNativeFileChooserImplementation {

    // Cached paths for Linux tools
    private static Path cachedKDialogPath;
    private static Path cachedZenityPath;

    private final ModernNativeFileChooser fileChooser;

    public ModernNativeFileChooserLinux(ModernNativeFileChooser fileChooser) {
        this.fileChooser = fileChooser;
    }

    private Path findKDialog() {
        if (cachedKDialogPath != null) {
            return cachedKDialogPath;
        }

        String kDialogPath = StringUtils.nullToEmpty(ProcessUtils.queryFast(Paths.get("/usr/bin/which"), new JIPipeProgressInfo(), "kdialog")).trim();
        if (!StringUtils.isNullOrEmpty(kDialogPath)) {
            cachedKDialogPath = Paths.get(kDialogPath);
            return cachedKDialogPath;
        }
        return null;
    }

    private Path findZenity() {
        if (cachedZenityPath != null) {
            return cachedZenityPath;
        }

        String zenityPath = StringUtils.nullToEmpty(ProcessUtils.queryFast(Paths.get("/usr/bin/which"), new JIPipeProgressInfo(), "zenity")).trim();
        if (!StringUtils.isNullOrEmpty(zenityPath)) {
            cachedZenityPath = Paths.get(zenityPath);
            return cachedZenityPath;
        }
        return null;
    }


    private ModernNativeFileChooserResponse showKDEFolderBrowser(Window parent, PathIOMode action) {
        Path kDialogPath = findKDialog();
        if (kDialogPath != null) {
            try {
                // Build the kdialog command
                ArrayList<String> command = new ArrayList<>();
                command.add(kDialogPath.toString());
                command.add("--getexistingdirectory");

                // Add title if available
                if (!StringUtils.isNullOrEmpty(dialogTitle)) {
                    command.add("--title");
                    command.add(dialogTitle);
                }

                // Add current directory if available
                if (currentDirectory != null && currentDirectory.exists()) {
                    command.add("--initial");
                    command.add(currentDirectory.getAbsolutePath());
                }

                // Execute the command
                String result = StringUtils.nullToEmpty(ProcessUtils.queryFast(kDialogPath,
                        new JIPipeProgressInfo(), command.toArray(new String[0]))).trim();

                // Check if user cancelled (kdialog returns non-zero exit code)
                if (result.isEmpty()) {
                    return ModernNativeFileChooserResponse.Cancelled;
                }

                // Validate the selected directory
                File selectedDir = new File(result);
                if (selectedDir.exists() && selectedDir.isDirectory()) {
                    selectedFiles = new File[]{selectedDir};
                    currentDirectory = selectedDir.getParentFile() != null ?
                            selectedDir.getParentFile() : selectedDir;
                    return ModernNativeFileChooserResponse.OK;
                } else {
                    return ModernNativeFileChooserResponse.Error;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return ModernNativeFileChooserResponse.Error;
            }
        }
        return ModernNativeFileChooserResponse.Error;
    }

    private ModernNativeFileChooserResponse showKDEFileChooser(Window parent, PathIOMode action) {
        Path kDialogPath = findKDialog();
        if (kDialogPath != null) {
            try {
                // Build the kdialog command
                ArrayList<String> command = new ArrayList<>();
                command.add(kDialogPath.toString());

                // Use different command based on action type
                if (action == PathIOMode.Open) {
                    command.add("--getopenfilename");
                } else if (action == PathIOMode.Save) {
                    command.add("--getsavefilename");
                }

                // Add current directory as first argument (startDir)
                if (currentDirectory != null && currentDirectory.exists()) {
                    command.add(currentDirectory.getAbsolutePath());
                } else {
                    command.add(".");
                }

                // Add file filter as second argument (name or mimetype filter)
                if (filters != null && !filters.isEmpty()) {
                    String filterPattern = buildKDEFilterPattern();
                    if (!StringUtils.isNullOrEmpty(filterPattern)) {
                        command.add(filterPattern);
                    } else {
                        command.add("*");
                    }
                } else {
                    command.add("*");
                }

                // Add title if available
                if (!StringUtils.isNullOrEmpty(dialogTitle)) {
                    command.add("--title");
                    command.add(dialogTitle);
                }

                // Add multiple selection support (only for open action)
                if (multiSelectionEnabled && action == PathIOMode.Open) {
                    command.add("--multiple");
                }

                // Execute the command
                String result = StringUtils.nullToEmpty(ProcessUtils.queryFast(kDialogPath, false,
                        JIPipeProgressInfo.STDOUT, command.toArray(new String[0]))).trim();

                // Check if user cancelled (kdialog returns empty string when cancelled)
                if (result.isEmpty()) {
                    return ModernNativeFileChooserResponse.Cancelled;
                }

                // Parse the selected files
                File[] selectedFiles = parseKDEFileSelection(result);
                if (selectedFiles != null && selectedFiles.length > 0) {
                    this.selectedFiles = selectedFiles;
                    // Update current directory to the directory of the first selected file
                    if (selectedFiles[0] != null) {
                        File firstFileDir = selectedFiles[0].getParentFile();
                        if (firstFileDir != null) {
                            this.currentDirectory = firstFileDir;
                        }
                    }
                    return ModernNativeFileChooserResponse.OK;
                } else {
                    return ModernNativeFileChooserResponse.Error;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return ModernNativeFileChooserResponse.Error;
            }
        }
        return ModernNativeFileChooserResponse.Error;
    }

    /**
     * Builds a KDE filter pattern from the internal filters
     * KDialog format: "Filter Name (*.ext1 *.ext2)"
     *
     * @return The filter pattern string
     */
    private String buildKDEFilterPattern() {
        if (filters == null || filters.isEmpty()) {
            return "";
        }

        // Use the first filter for now
        String[] filterSpec = filters.get(0);
        if (filterSpec.length < 2) {
            return "";
        }

        StringBuilder pattern = new StringBuilder(filterSpec[0]); // Filter name
        pattern.append(" (");

        for (int i = 1; i < filterSpec.length; i++) {
            if (i > 1) {
                pattern.append(" ");
            }
            pattern.append("*").append(filterSpec[i]);
        }

        pattern.append(")");
        return pattern.toString();
    }

    /**
     * Builds a Zenity filter pattern from the internal filters
     * Zenity format: "*.ext1 *.ext2"
     *
     * @return The filter pattern string
     */
    private String buildZenityFilterPattern() {
        if (filters == null || filters.isEmpty()) {
            return "";
        }

        // Use the first filter for now
        String[] filterSpec = filters.get(0);
        if (filterSpec.length < 2) {
            return "";
        }

        StringBuilder pattern = new StringBuilder();

        for (int i = 1; i < filterSpec.length; i++) {
            if (i > 1) {
                pattern.append(" ");
            }
            pattern.append("*").append(filterSpec[i]);
        }

        return pattern.toString();
    }

    private ModernNativeFileChooserResponse showGtkFolderBrowser(Window parent, Action action) {
        Path zenityPath = findZenity();
        if (zenityPath != null) {
            try {
                // Build the zenity command
                ArrayList<String> command = new ArrayList<>();
                command.add(zenityPath.toString());
                command.add("--file-selection");
                command.add("--directory");

                // Add title if available
                if (!StringUtils.isNullOrEmpty(dialogTitle)) {
                    command.add("--title");
                    command.add(dialogTitle);
                }

                // Add current directory if available
                if (currentDirectory != null && currentDirectory.exists()) {
                    command.add("--filename");
                    command.add(currentDirectory.getAbsolutePath());
                }

                // Execute the command
                String result = StringUtils.nullToEmpty(ProcessUtils.queryFast(zenityPath,
                        JIPipeProgressInfo.STDOUT, command.toArray(new String[0]))).trim();

                // Check if user cancelled (zenity returns empty string when cancelled)
                if (result.isEmpty()) {
                    return ModernNativeFileChooserResponse.Cancelled;
                }

                // Validate the selected directory
                File selectedDir = new File(result);
                if (selectedDir.exists() && selectedDir.isDirectory()) {
                    selectedFiles = new File[]{selectedDir};
                    currentDirectory = selectedDir.getParentFile() != null ?
                            selectedDir.getParentFile() : selectedDir;
                    return ModernNativeFileChooserResponse.OK;
                } else {
                    return ModernNativeFileChooserResponse.Error;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return ModernNativeFileChooserResponse.Error;
            }
        }
        return ModernNativeFileChooserResponse.Error;
    }

    private ModernNativeFileChooserResponse showGtkFileChooser(Window parent, PathIOMode action) {
        Path zenityPath = findZenity();
        if (zenityPath != null) {
            try {
                // Build the zenity command
                ArrayList<String> command = new ArrayList<>();
                command.add(zenityPath.toString());
                command.add("--file-selection");

                // Add save flag for save action
                if (action == PathIOMode.Save) {
                    command.add("--save");
                }

                // Add title if available
                if (!StringUtils.isNullOrEmpty(dialogTitle)) {
                    command.add("--title");
                    command.add(dialogTitle);
                }

                // Add current directory if available
                if (currentDirectory != null && currentDirectory.exists()) {
                    command.add("--filename");
                    command.add(currentDirectory.getAbsolutePath());
                }

                // Add file filter if available
                if (filters != null && !filters.isEmpty()) {
                    String filterPattern = buildZenityFilterPattern();
                    if (!StringUtils.isNullOrEmpty(filterPattern)) {
                        command.add("--file-filter");
                        command.add(filterPattern);
                    }
                }

                // Add multiple selection support (only for open action)
                if (multiSelectionEnabled && action == PathIOMode.Open) {
                    command.add("--multiple");
                }

                // Execute the command
                String result = StringUtils.nullToEmpty(ProcessUtils.queryFast(zenityPath,
                        JIPipeProgressInfo.STDOUT, command.toArray(new String[0]))).trim();

                // Check if user cancelled (zenity returns empty string when cancelled)
                if (result.isEmpty()) {
                    return ModernNativeFileChooserResponse.Cancelled;
                }

                // Parse the selected files
                File[] selectedFiles = parseZenityFileSelection(result);
                if (selectedFiles != null && selectedFiles.length > 0) {
                    this.selectedFiles = selectedFiles;
                    // Update current directory to the directory of the first selected file
                    if (selectedFiles[0] != null) {
                        File firstFileDir = selectedFiles[0].getParentFile();
                        if (firstFileDir != null) {
                            this.currentDirectory = firstFileDir;
                        }
                    }
                    return ModernNativeFileChooserResponse.OK;
                } else {
                    return ModernNativeFileChooserResponse.Error;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return ModernNativeFileChooserResponse.Error;
            }
        }
        return ModernNativeFileChooserResponse.Error;
    }

    @Override
    public ModernNativeFileChooserResponse showFileChooser(Window parent, Action action) {
        return null;
    }

    @Override
    public ModernNativeFileChooserResponse showFolderBrowser(Window parent) {
        return null;
    }

    /**
     * Parses the output from Zenity's --file-selection command
     * Zenity returns file paths separated by newlines for multiple selections
     *
     * @param zenityOutput The output from Zenity
     * @return Array of selected files, or null if parsing failed
     */
    private File[] parseZenityFileSelection(String zenityOutput) {
        if (StringUtils.isNullOrEmpty(zenityOutput)) {
            return null;
        }

        // Zenity returns files separated by newlines for multiple selections
        String[] filePaths = zenityOutput.split("\\r?\\n");

        ArrayList<File> files = new ArrayList<>();
        for (String filePath : filePaths) {
            if (!StringUtils.isNullOrEmpty(filePath)) {
                File file = new File(filePath.trim());
                if (file.exists()) {
                    files.add(file);
                }
            }
        }

        return files.toArray(new File[0]);
    }

    /**
     * Parses the output from KDialog's --getopenfilename command
     * KDialog returns file paths separated by spaces, with spaces in paths escaped
     *
     * @param kdialogOutput The output from KDialog
     * @return Array of selected files, or null if parsing failed
     */
    private File[] parseKDEFileSelection(String kdialogOutput) {
        if (StringUtils.isNullOrEmpty(kdialogOutput)) {
            return null;
        }

        // KDialog returns files separated by spaces, with spaces in paths escaped
        // We need to split on spaces that are not preceded by a backslash
        String[] filePaths = kdialogOutput.split("(?<!\\\\)\\s+");

        ArrayList<File> files = new ArrayList<>();
        for (String filePath : filePaths) {
            if (!StringUtils.isNullOrEmpty(filePath)) {
                // Remove backslash escaping
                filePath = filePath.replace("\\ ", " ");
                File file = new File(filePath);
                if (file.exists()) {
                    files.add(file);
                }
            }
        }

        return files.toArray(new File[0]);
    }
}
