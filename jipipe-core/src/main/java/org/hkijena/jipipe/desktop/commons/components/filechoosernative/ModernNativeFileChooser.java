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
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.ProcessUtils;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private File[] selectedFiles = new File[] {null};
    protected File currentDirectory;
    protected List<FileNameExtensionFilter> filters = new ArrayList<>();
    protected boolean multiSelectionEnabled = false;
    protected Mode mode = Mode.Files;
    protected String defaultFile = "";
    protected String dialogTitle = "";
    protected String openButtonText = "";
    protected String saveButtonText = "";

    /**
     * creates a new file chooser with multiselection disabled and mode set
     * to allow file selection only.
     */
    public ModernNativeFileChooser() {
    }
    /**
     * creates a new file chooser with the specified initial directory
     *
     * @param currentDirectory the initial directory
     */
    public ModernNativeFileChooser(File currentDirectory) {
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
    public ModernNativeFileChooserResponse showOpenDialog(Window parent) {
        return showDialog(parent, PathIOMode.Open);
    }

    public String getDefaultFile() {
        return defaultFile;
    }

    public String getDialogTitle() {
        return dialogTitle;
    }

    public String getOpenButtonText() {
        return openButtonText;
    }

    public String getSaveButtonText() {
        return saveButtonText;
    }

    public List<FileNameExtensionFilter> getFilters() {
        return filters;
    }

    /**
     * shows a dialog for saving files
     *
     * @param parent the parent window
     */
    public ModernNativeFileChooserResponse showSaveDialog(Window parent) {
        return showDialog(parent, PathIOMode.Save);
    }

    private ModernNativeFileChooserResponse showDialog(Window parent, PathIOMode action) {
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
                        ModernNativeFileChooserResponse result = showKDEFileChooser(parent, action);
                        if (result != ModernNativeFileChooserResponse.Error) {
                            return result;
                        }
                    }
                    else if(mode == Mode.Directories) {
                        ModernNativeFileChooserResponse result = showKDEFolderBrowser(parent, action);
                        if (result != ModernNativeFileChooserResponse.Error) {
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
        return ModernNativeFileChooserResponse.Error;
    }

    public void addFilter(FileNameExtensionFilter filter) {
        filters.add(filter);
    }

    public ArrayList<String[]> getFiltersAsArray() {
        ArrayList<String[]> result = new ArrayList<>();
        for (FileNameExtensionFilter filter : filters) {
            List<String> parts = new ArrayList<>();
            parts.add(filter.getDescription());
            Collections.addAll(parts, filter.getExtensions());
            result.add(parts.toArray(new String[0]));
        }
        return result;
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
