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
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.ProcessUtils;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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


    private ModernNativeFileChooserResponse showKDEFolderBrowser() {
        Path kDialogPath = findKDialog();
        File currentDirectory = fileChooser.getCurrentDirectory();
        if (kDialogPath != null) {
            try {
                // Build the kdialog command
                ArrayList<String> args = new ArrayList<>();
                args.add("--getexistingdirectory");

                if (currentDirectory != null && currentDirectory.isDirectory()) {
                    args.add(currentDirectory.getAbsolutePath());
                } else {
                    args.add(PathUtils.getHomeDirectory().toString());
                }

                // Add title if available
                if (!StringUtils.isNullOrEmpty(fileChooser.getDialogTitle())) {
                    args.add("--title");
                    args.add(fileChooser.getDialogTitle());
                }

                // Execute the command
                String result = StringUtils.nullToEmpty(ProcessUtils.queryFast(kDialogPath, false,
                        new JIPipeProgressInfo(), args.toArray(new String[0]))).trim();

                // Check if user cancelled (kdialog returns non-zero exit code)
                if (result.isEmpty()) {
                    return ModernNativeFileChooserResponse.Cancelled;
                }

                // Validate the selected directory
                File selectedDir = new File(result);
                if (selectedDir.exists() && selectedDir.isDirectory()) {
                    fileChooser.setSelectedFiles(new File[]{selectedDir});
                    fileChooser.setCurrentDirectory(selectedDir.getParentFile() != null ?
                            selectedDir.getParentFile() : selectedDir);
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

    private ModernNativeFileChooserResponse showKDEFileChooser(PathIOMode action) {
        Path kDialogPath = findKDialog();
        File currentDirectory = fileChooser.getCurrentDirectory();
        if (kDialogPath != null) {
            try {
                // Build the kdialog command
                ArrayList<String> args = new ArrayList<>();

                // Use different command based on action type
                if (action == PathIOMode.Open) {
                    args.add("--getopenfilename");
                } else if (action == PathIOMode.Save) {
                    args.add("--getsavefilename");
                }

                // Add current directory as first argument (startDir)
                if (currentDirectory != null && currentDirectory.exists()) {
                    args.add(currentDirectory.getAbsolutePath());
                } else {
                    args.add(PathUtils.getHomeDirectory().toString());
                }

                // Add file filter as second argument (name or mimetype filter)
                if (!fileChooser.getFilters().isEmpty()) {
                    StringBuilder builder = new StringBuilder();
                    for (FileNameExtensionFilter filter : fileChooser.getFilters()) {
                        for (String extension : filter.getExtensions()) {
                            builder.append("*.").append(extension).append(" ");
                        }
                    }
                    args.add(builder.toString().trim());
                }

                // Add title if available
                if (!StringUtils.isNullOrEmpty(fileChooser.getDialogTitle())) {
                    args.add("--title");
                    args.add(fileChooser.getDialogTitle());
                }

                // Add multiple selection support (only for open action)
                if (fileChooser.isMultiSelectionEnabled() && action == PathIOMode.Open) {
                    args.add("--multiple");
                    args.add("--separate-output");
                }

                // Execute the command
                String result = StringUtils.nullToEmpty(ProcessUtils.queryFast(kDialogPath, false,
                        JIPipeProgressInfo.STDOUT, args.toArray(new String[0]))).trim();

                // Check if user cancelled (kdialog returns empty string when cancelled)
                if (result.isEmpty()) {
                    return ModernNativeFileChooserResponse.Cancelled;
                }

                // Parse the selected files
                File[] selectedFiles = parseKDEFileSelection(result);
                if (selectedFiles != null && selectedFiles.length > 0) {
                    fileChooser.setSelectedFiles(selectedFiles);
                    // Update current directory to the directory of the first selected file
                    if (selectedFiles[0] != null) {
                        File firstFileDir = selectedFiles[0].getParentFile();
                        if (firstFileDir != null) {
                            fileChooser.setCurrentDirectory(firstFileDir);
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

    private ModernNativeFileChooserResponse showGtkFolderBrowser() {
        Path zenityPath = findZenity();
        File currentDirectory = fileChooser.getCurrentDirectory();
        if (zenityPath != null) {
            try {
                // Build the zenity command
                ArrayList<String> args = new ArrayList<>();

                args.add("--file-selection");
                args.add("--directory");

                // Add title if available
                if (!StringUtils.isNullOrEmpty(fileChooser.getDialogTitle())) {
                    args.add("--title");
                    args.add(fileChooser.getDialogTitle());
                }

                // Add current directory if available
                if (currentDirectory != null && currentDirectory.exists()) {
                    args.add("--filename");
                    args.add(currentDirectory.getAbsolutePath());
                }

                // Execute the command
                String result = StringUtils.nullToEmpty(ProcessUtils.queryFast(zenityPath,
                        JIPipeProgressInfo.STDOUT, args.toArray(new String[0]))).trim();

                // Check if user cancelled (zenity returns empty string when cancelled)
                if (result.isEmpty()) {
                    return ModernNativeFileChooserResponse.Cancelled;
                }

                // Validate the selected directory
                File selectedDir = new File(result);
                if (selectedDir.exists() && selectedDir.isDirectory()) {
                    fileChooser.setSelectedFiles(new File[]{selectedDir});
                    fileChooser.setCurrentDirectory(selectedDir.getParentFile() != null ?
                            selectedDir.getParentFile() : selectedDir);
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

    private ModernNativeFileChooserResponse showGtkFileChooser(PathIOMode action) {
        Path zenityPath = findZenity();
        File currentDirectory = fileChooser.getCurrentDirectory();
        if (zenityPath != null) {
            try {
                // Build the zenity command
                ArrayList<String> args = new ArrayList<>();

                args.add("--file-selection");

                // Add save flag for save action
                if (action == PathIOMode.Save) {
                    args.add("--save");
                }

                // Add title if available
                if (!StringUtils.isNullOrEmpty(fileChooser.getDialogTitle())) {
                    args.add("--title");
                    args.add(fileChooser.getDialogTitle());
                }

                // Add current directory if available
                if (currentDirectory != null && currentDirectory.exists()) {
                    args.add("--filename");
                    args.add(currentDirectory.getAbsolutePath() + "/");
                }

                // Add file filter if available
                for (FileNameExtensionFilter filter : fileChooser.getFilters()) {
                    args.add("--file-filter");
                    args.add(filter.getDescription() + " | " + Arrays.stream(filter.getExtensions()).map(s -> "*." + s).collect(Collectors.joining(" ")));
                }

                // Add multiple selection support (only for open action)
                if (fileChooser.isMultiSelectionEnabled() && action == PathIOMode.Open) {
                    args.add("--multiple");
                    args.add("--separator");
                    args.add("<|SEPARATOR|>");
                }

                // Execute the command
                String result = StringUtils.nullToEmpty(ProcessUtils.queryFast(zenityPath, false,
                        JIPipeProgressInfo.SILENT, args.toArray(new String[0]))).trim();

                // Check if user cancelled (zenity returns empty string when cancelled)
                if (result.isEmpty()) {
                    return ModernNativeFileChooserResponse.Cancelled;
                }

                // Parse the selected files
                File[] selectedFiles = parseZenityFileSelection(result);
                if (selectedFiles != null && selectedFiles.length > 0) {
                    fileChooser.setSelectedFiles(selectedFiles);
                    // Update current directory to the directory of the first selected file
                    if (selectedFiles[0] != null) {
                        File firstFileDir = selectedFiles[0].getParentFile();
                        if (firstFileDir != null) {
                            fileChooser.setCurrentDirectory(firstFileDir);
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
    public ModernNativeFileChooserResponse showFolderBrowser(Window parent) {
        // Try KDialog first for KDE environments, then fall back to Zenity
        Path kDialogPath = findKDialog();
        Path zenityPath = findZenity();

        if (zenityPath != null) {
            // Zenity also supports KDE
            return showGtkFolderBrowser();
        }

        boolean isGtk = GtkDesktopEnvironmentDetector.detect().verdict() == GtkDesktopEnvironmentDetector.Verdict.NO;

        // If KDialog is available, try to use it first
        // Kdialog only if KDE
        if (!isGtk && kDialogPath != null) {
            return showKDEFolderBrowser();
        }

        return ModernNativeFileChooserResponse.Error;
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
        String[] filePaths = zenityOutput.split(Pattern.quote("<|SEPARATOR|>"));

        ArrayList<File> files = new ArrayList<>();
        for (String filePath : filePaths) {
            if (!StringUtils.isNullOrEmpty(filePath)) {
                File file = new File(filePath.trim());
                files.add(file);
            }
        }

        return files.toArray(new File[0]);
    }

    /**
     * Parses the output from KDialog's --getopenfilename command
     * KDialog returns file paths separated by spaces with escaped spaces (old format)
     * or separated by newlines when --separate-output is used (new format)
     *
     * @param kdialogOutput The output from KDialog
     * @return Array of selected files, or null if parsing failed
     */
    private File[] parseKDEFileSelection(String kdialogOutput) {
        if (StringUtils.isNullOrEmpty(kdialogOutput)) {
            return null;
        }

        ArrayList<File> files = new ArrayList<>();

        // New format: each file path is on a separate line
        String[] filePaths = kdialogOutput.split(Pattern.quote("\n"));
        for (String filePath : filePaths) {
            if (!StringUtils.isNullOrEmpty(filePath)) {
                File file = new File(filePath.trim());
                files.add(file);
            }
        }

        return files.toArray(new File[0]);
    }

    @Override
    public ModernNativeFileChooserResponse showFileChooser(Window parent, PathIOMode action) {
        // Try KDialog first for KDE environments, then fall back to Zenity
        Path kDialogPath = findKDialog();
        Path zenityPath = findZenity();

        if (zenityPath != null) {
            // Zenity also supports KDE
            return showGtkFileChooser(action);
        }

        boolean isGtk = GtkDesktopEnvironmentDetector.detect().verdict() == GtkDesktopEnvironmentDetector.Verdict.YES;

        // If KDialog is available, try to use it first
        // Kdialog only if KDE
        if (!isGtk && kDialogPath != null) {
            return showKDEFileChooser(action);
        }

        return ModernNativeFileChooserResponse.Error;
    }
}
