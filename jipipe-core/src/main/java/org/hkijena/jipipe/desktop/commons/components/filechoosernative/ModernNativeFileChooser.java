/* This file is part of JnaFileChooser.
 *
 * JnaFileChooser is free software: you can redistribute it and/or modify it
 * under the terms of the new BSD license.
 *
 * JnaFileChooser is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 */
package org.hkijena.jipipe.desktop.commons.components.filechoosernative;

import com.sun.jna.Platform;
import org.hkijena.jipipe.desktop.commons.components.filechoosernative.linux.GtkDesktopEnvironmentDetector;
import org.hkijena.jipipe.desktop.commons.components.filechoosernative.win32.WindowsFileChooser;
import org.hkijena.jipipe.desktop.commons.components.filechoosernative.win32.WindowsFolderBrowser;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.utils.ProcessUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.PathUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

/**
 * JnaFileChooser is a wrapper around the native Windows file chooser
 * and folder browser that falls back to the Swing JFileChooser on platforms
 * other than Windows or if the user chooses a combination of features
 * that are not supported by the native dialogs (for example multiple
 * selection of directories).
 * <p>
 * Example:
 * JnaFileChooser fc = new JnaFileChooser();
 * fc.setFilter("All Files", "*");
 * fc.setFilter("Pictures", "jpg", "jpeg", "gif", "png", "bmp");
 * fc.setMultiSelectionEnabled(true);
 * fc.setMode(JnaFileChooser.Mode.FilesAndDirectories);
 * if (fc.showOpenDialog(parent)) {
 * Files[] selected = fc.getSelectedFiles();
 * // do something with selected
 * }
 *
 * @see JFileChooser, WindowsFileChooser, WindowsFileBrowser
 */
public class ModernNativeFileChooser {
    protected File[] selectedFiles;
    protected File currentDirectory;
    protected ArrayList<String[]> filters;
    protected boolean multiSelectionEnabled;
    protected Mode mode;
    protected String defaultFile;
    protected String dialogTitle;
    protected String openButtonText;
    protected String saveButtonText;
    
    // Cached paths for Linux tools
    private Path cachedKDialogPath;
    private Path cachedZenityPath;

    /**
     * creates a new file chooser with multiselection disabled and mode set
     * to allow file selection only.
     */
    public ModernNativeFileChooser() {
        filters = new ArrayList<>();
        multiSelectionEnabled = false;
        mode = Mode.Files;
        selectedFiles = new File[]{null};

        defaultFile = "";
        dialogTitle = "";
        openButtonText = "";
        saveButtonText = "";
        
        // Initialize cached paths
        cachedKDialogPath = null;
        cachedZenityPath = null;
    }
    /**
     * creates a new file chooser with the specified initial directory
     *
     * @param currentDirectory the initial directory
     */
    public ModernNativeFileChooser(File currentDirectory) {
        this();
        if (currentDirectory != null) {
            this.currentDirectory = currentDirectory.isDirectory() ?
                    currentDirectory : currentDirectory.getParentFile();
        }
    }

    /**
     * creates a new file chooser with the specified initial directory
     *
     * @param currentDirectoryPath the initial directory
     */
    public ModernNativeFileChooser(String currentDirectoryPath) {
        this(currentDirectoryPath != null ?
                new File(currentDirectoryPath) : null);
    }

    /**
     * shows a dialog for opening files
     *
     * @param parent the parent window
     */
    public Response showOpenDialog(Window parent) {
        return showDialog(parent, Action.Open);
    }

    /**
     * shows a dialog for saving files
     *
     * @param parent the parent window
     */
    public Response showSaveDialog(Window parent) {
        return showDialog(parent, Action.Save);
    }

    private Response showDialog(Window parent, Action action) {
        try {
            if (Platform.isWindows()) {
                if (mode == Mode.Files) {
                    return showWindowsFileChooser(parent, action);
                } else if (mode == Mode.Directories) {
                    return showWindowsFolderBrowser(parent);
                }
            } else if (Platform.isMac()) {
                if (mode == Mode.Files) {
                    return showMacFileChooser((Frame) parent, action);
                } else if (mode == Mode.Directories) {
                    return showMacFolderBrowser((Frame) parent);
                }
            }
            else if(Platform.isLinux()) {
                // Try KDialog first for KDE environments, then fall back to Zenity
                Path kDialogPath = findKDialog();
                Path zenityPath = findZenity();
                
                // If KDialog is available, try to use it first
                // Kdialog only if KDE
                if (GtkDesktopEnvironmentDetector.detect().verdict() == GtkDesktopEnvironmentDetector.Verdict.NO && kDialogPath != null) {
                    if(mode == Mode.Files) {
                        Response result = showKDEFileChooser(parent, action);
                        if (result != Response.Error) {
                            return result;
                        }
                    }
                    else if(mode == Mode.Directories) {
                        Response result = showKDEFolderBrowser(parent, action);
                        if (result != Response.Error) {
                            return result;
                        }
                    }
                }
                
                // Fall back to Zenity if KDialog failed or not available
                if (zenityPath != null) {
                    if(mode == Mode.Files) {
                        return showGtkFileChooser(parent, action);
                    }
                    else if(mode == Mode.Directories) {
                        return showGtkFolderBrowser(parent, action);
                    }
                }
            }
        }
        catch (Throwable t) {
            t.printStackTrace();
        }

        // Respond back that the implementation should use JIPipe's fallback
        return Response.Error;
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

    private Response showKDEFolderBrowser(Window parent, Action action) {
        Path kDialogPath = findKDialog();
        if(kDialogPath != null) {
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
                    return Response.Cancelled;
                }
                
                // Validate the selected directory
                File selectedDir = new File(result);
                if (selectedDir.exists() && selectedDir.isDirectory()) {
                    selectedFiles = new File[]{selectedDir};
                    currentDirectory = selectedDir.getParentFile() != null ?
                        selectedDir.getParentFile() : selectedDir;
                    return Response.OK;
                } else {
                    return Response.Error;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return Response.Error;
            }
        }
        return Response.Error;
    }

    private Response showKDEFileChooser(Window parent, Action action) {
        Path kDialogPath = findKDialog();
        if(kDialogPath != null) {
            try {
                // Build the kdialog command
                ArrayList<String> command = new ArrayList<>();
                command.add(kDialogPath.toString());
                
                // Use different command based on action type
                if (action == Action.Open) {
                    command.add("--getopenfilename");
                } else if (action == Action.Save) {
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
                if (multiSelectionEnabled && action == Action.Open) {
                    command.add("--multiple");
                }
                
                // Execute the command
                String result = StringUtils.nullToEmpty(ProcessUtils.queryFast(kDialogPath, false,
                    JIPipeProgressInfo.STDOUT, command.toArray(new String[0]))).trim();
                
                // Check if user cancelled (kdialog returns empty string when cancelled)
                if (result.isEmpty()) {
                    return Response.Cancelled;
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
                    return Response.OK;
                } else {
                    return Response.Error;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return Response.Error;
            }
        }
        return Response.Error;
    }

    private Response showGtkFolderBrowser(Window parent, Action action) {
        Path zenityPath = findZenity();
        if(zenityPath != null) {
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
                    return Response.Cancelled;
                }
                
                // Validate the selected directory
                File selectedDir = new File(result);
                if (selectedDir.exists() && selectedDir.isDirectory()) {
                    selectedFiles = new File[]{selectedDir};
                    currentDirectory = selectedDir.getParentFile() != null ?
                        selectedDir.getParentFile() : selectedDir;
                    return Response.OK;
                } else {
                    return Response.Error;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return Response.Error;
            }
        }
        return Response.Error;
    }

    private Response showGtkFileChooser(Window parent, Action action) {
        Path zenityPath = findZenity();
        if(zenityPath != null) {
            try {
                // Build the zenity command
                ArrayList<String> command = new ArrayList<>();
                command.add(zenityPath.toString());
                command.add("--file-selection");
                
                // Add save flag for save action
                if (action == Action.Save) {
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
                if (multiSelectionEnabled && action == Action.Open) {
                    command.add("--multiple");
                }
                
                // Execute the command
                String result = StringUtils.nullToEmpty(ProcessUtils.queryFast(zenityPath,
                    JIPipeProgressInfo.STDOUT, command.toArray(new String[0]))).trim();
                
                // Check if user cancelled (zenity returns empty string when cancelled)
                if (result.isEmpty()) {
                    return Response.Cancelled;
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
                    return Response.OK;
                } else {
                    return Response.Error;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return Response.Error;
            }
        }
        return Response.Error;
    }

    private Response showWindowsFileChooser(Window parent, Action action) {
        final WindowsFileChooser fc = new WindowsFileChooser(currentDirectory);
        fc.setFilters(filters);
        fc.setMultiSelectionEnabled(multiSelectionEnabled);
        if (!defaultFile.isEmpty())
            fc.setDefaultFilename(defaultFile);

        if (!dialogTitle.isEmpty()) {
            fc.setTitle(dialogTitle);
        }

        final Response result = fc.showDialog(parent, action == Action.Open);
        if (result == Response.OK) {
            selectedFiles = multiSelectionEnabled ? fc.getSelectedFiles() : new File[]{fc.getSelectedFile()};
            currentDirectory = fc.getCurrentDirectory();
        }
        return result;
    }

    private Response showWindowsFolderBrowser(Window parent) {
        final WindowsFolderBrowser fb = new WindowsFolderBrowser();
        if (!dialogTitle.isEmpty()) {
            fb.setTitle(dialogTitle);
        }
        final File file = fb.showDialog(parent);
        if (file != null) {
            selectedFiles = new File[]{file};
            currentDirectory = file.getParentFile() != null ?
                    file.getParentFile() : file;
            return Response.OK;
        }

        return Response.Cancelled;
    }

    private Response showMacFileChooser(Frame parent, Action action) {
        int mode = action == Action.Open ? FileDialog.LOAD : FileDialog.SAVE;
        String title = !dialogTitle.isEmpty() ?
                dialogTitle : (action == Action.Open ? "Open" : "Save As");
        FileDialog fd = new FileDialog(parent, title, mode);
        fd.setMultipleMode(multiSelectionEnabled);

        if (!defaultFile.isEmpty())
            fd.setFile(defaultFile);

        if (currentDirectory != null) {
            if (currentDirectory.isDirectory())
                fd.setDirectory(currentDirectory.getAbsolutePath());
            else
                fd.setDirectory(currentDirectory.getParent());
        }

        fd.setFilenameFilter((dir, filename) -> {
            if (filters.isEmpty())
                return true;

            // filterSpec is formatted as [ name, ext1, ext2, ext3, ... ]
            for (String[] filterSpec : filters) {
                for (int i = 1; i < filterSpec.length; i++) {
                    String filter = filterSpec[i].toLowerCase();
                    if (filter.startsWith("*")) {
                        if (filename.toLowerCase().endsWith(filter.substring(1)))
                            return true;
                    } else {
                        if (filename.toLowerCase().endsWith(filter))
                            return true;
                    }
                }
            }
            return false;
        });

        fd.setVisible(true);

        if (fd.getFile() != null) {
            selectedFiles = multiSelectionEnabled ?
                    fd.getFiles() : new File[]{new File(fd.getDirectory(), fd.getFile())};
            currentDirectory = new File(fd.getDirectory());
            return Response.OK;
        }

        return Response.Cancelled;
    }

    private Response showMacFolderBrowser(Frame parent) {
        String title = !dialogTitle.isEmpty() ? dialogTitle : "Open";
        FileDialog fd = new FileDialog(parent, title, FileDialog.LOAD);
        if (!dialogTitle.isEmpty())
            fd.setTitle(dialogTitle);

        try {
            System.setProperty("apple.awt.fileDialogForDirectories", "true");
            fd.setVisible(true);
            if (fd.getFile() != null) {
                selectedFiles = new File[]{new File(fd.getDirectory(), fd.getFile())};
                currentDirectory = new File(fd.getDirectory());
                return Response.OK;
            }
        } finally {
            System.setProperty("apple.awt.fileDialogForDirectories", "false");
        }

        return Response.Cancelled;
    }

    /**
     * add a filter to the user-selectable list of file filters
     *
     * @param name   name of the filter
     * @param filter you must pass at least 1 argument, the arguments are the file
     *               extensions.
     */
    public void addFilter(String name, String... filter) {
        if (filter.length < 1) {
            throw new IllegalArgumentException();
        }
        ArrayList<String> parts = new ArrayList<>();
        parts.add(name);
        Collections.addAll(parts, filter);
        filters.add(parts.toArray(new String[0]));
    }

    public Mode getMode() {
        return mode;
    }

    /**
     * sets the selection mode
     *
     * @param mode the selection mode
     */
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public boolean isMultiSelectionEnabled() {
        return multiSelectionEnabled;
    }

    /**
     * sets whether to enable multiselection
     *
     * @param enabled true to enable multiselection, false to disable it
     */
    public void setMultiSelectionEnabled(boolean enabled) {
        this.multiSelectionEnabled = enabled;
    }

    public void setDefaultFileName(String dfile) {
        this.defaultFile = dfile;
    }

    /**
     * set a title name
     *
     * @param title of dialog
     *
     */
    public void setTitle(String title) {
        this.dialogTitle = title;
    }

    /**
     * set an open button name
     *
     * @param buttonText button text
     *
     */
    public void setOpenButtonText(String buttonText) {
        this.openButtonText = buttonText;
    }

    /**
     * set a save button name
     *
     * @param buttonText button text
     *
     */
    public void setSaveButtonText(String buttonText) {
        this.saveButtonText = buttonText;
    }

    public File[] getSelectedFiles() {
        return selectedFiles;
    }

    public File getSelectedFile() {
        return selectedFiles[0];
    }

    public File getCurrentDirectory() {
        return currentDirectory;
    }

    public void setCurrentDirectory(String currentDirectoryPath) {
        this.currentDirectory = (currentDirectoryPath != null ? new File(currentDirectoryPath) : null);
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

    private enum Action {Open, Save}

    public enum Response {
        OK,
        Cancelled,
        Error
    }

    /**
     * the available selection modes of the dialog
     */
    public enum Mode {
        Files(JFileChooser.FILES_ONLY),
        Directories(JFileChooser.DIRECTORIES_ONLY),
        FilesAndDirectories(JFileChooser.FILES_AND_DIRECTORIES);
        private final int jFileChooserValue;

        Mode(int jfcv) {
            this.jFileChooserValue = jfcv;
        }

        public int getJFileChooserValue() {
            return jFileChooserValue;
        }
    }
}
