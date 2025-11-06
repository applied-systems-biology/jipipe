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

package org.hkijena.jipipe.desktop;

import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.desktop.commons.components.filechoosers.fcadvanced.JIPipeDesktopAdvancedFileChooser;
import org.hkijena.jipipe.desktop.commons.components.filechoosers.fcnative.ModernNativeFileChooser;
import org.hkijena.jipipe.desktop.commons.components.filechoosers.fcnative.ModernNativeFileChooserResponse;
import org.hkijena.jipipe.desktop.commons.components.filechoosers.fcnext.JIPipeDesktopFileChooserNext;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.settings.application.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Utility class for desktop-related JIPipe functions
 */
public class JIPipeDesktop {
    /**
     * *.jip or *.crate.zip file that is loaded when the JIPipe window is ready
     */
    private static Path OPEN_ON_LOAD_FILE;
    /**
     * URL that will be downloaded and opened when the JIPipe window is ready
     */
    private static URL OPEN_ON_LOAD_URL;

    private JIPipeDesktop() {

    }

    /**
     * Lets the user choose a file
     *
     * @param parent           parent component
     * @param workbench        the workbench
     * @param key              location where the dialog is opened
     * @param title            dialog title
     * @param description      optional description (only supported by specific file chooser types)
     * @param extensionFilters optional extension filters. the first one is chosen automatically
     * @return selected file or null if dialog was cancelled
     */
    public static Path openFile(Component parent, JIPipeWorkbench workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey key, String title, HTMLText description, FileNameExtensionFilter... extensionFilters) {
        JIPipeFileChooserApplicationSettings instance = JIPipeFileChooserApplicationSettings.getInstance();
        final JIPipeFileChooserApplicationSettings.FileChooserType fileChooserType = instance.getFileChooserType();
        return openFile(parent, workbench, key, title, description, fileChooserType, extensionFilters);
    }

    /**
     * Lets the user choose a file
     *
     * @param parent           parent component
     * @param workbench        the workbench
     * @param key              location where the dialog is opened
     * @param title            dialog title
     * @param description      optional description (only supported by specific file chooser types)
     * @param extensionFilters optional extension filters. the first one is chosen automatically
     * @return selected file or null if dialog was cancelled
     */
    public static Path openFile(Component parent, JIPipeWorkbench workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey key, String title, HTMLText description, JIPipeFileChooserApplicationSettings.FileChooserType fileChooserType, FileNameExtensionFilter... extensionFilters) {
        JIPipeFileChooserApplicationSettings instance = JIPipeFileChooserApplicationSettings.getInstance();

        Path currentPath = instance.getLastDirectoryBy(workbench, key);
        if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.Native) {
            FileDialog dialog = createFileDialog(parent, title, FileDialog.LOAD);
            dialog.setTitle(title);
            dialog.setDirectory(currentPath.toString());
            dialog.setMultipleMode(false);
            dialog.setVisible(true);
            String fileName = dialog.getFile();
            if (fileName != null) {
                Path path = Paths.get(fileName);
                instance.setLastDirectoryBy(key, path.getParent());
                return path;
            } else {
                return null;
            }
        } else if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.Standard) {
            JFileChooser fileChooser = new JFileChooser(currentPath.toFile());
            fileChooser.setDialogTitle(title);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if (extensionFilters.length > 0) {
                for (FileNameExtensionFilter extensionFilter : extensionFilters) {
                    fileChooser.addChoosableFileFilter(extensionFilter);
                }
                fileChooser.setFileFilter(extensionFilters[0]);
            }
            if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                Path path = fileChooser.getSelectedFile().toPath();
                instance.setLastDirectoryBy(key, path.getParent());
                return path;
            } else {
                return null;
            }
        } else if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.AdvancedLegacy) {
            JIPipeDesktopAdvancedFileChooser fileChooser = new JIPipeDesktopAdvancedFileChooser(currentPath.toFile());
            fileChooser.setDialogTitle(title);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if (extensionFilters.length > 0) {
                for (FileNameExtensionFilter extensionFilter : extensionFilters) {
                    fileChooser.addChoosableFileFilter(extensionFilter);
                }
                fileChooser.setFileFilter(extensionFilters[0]);
            }
            if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                Path path = fileChooser.getSelectedFile().toPath();
                instance.setLastDirectoryBy(key, path.getParent());
                return path;
            } else {
                return null;
            }
        } else if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.ModernNative) {
            ModernNativeFileChooser fileChooser = new ModernNativeFileChooser(currentPath.toFile());
            fileChooser.setTitle(title);
            fileChooser.setMode(PathType.FilesOnly);
            for (FileNameExtensionFilter extensionFilter : extensionFilters) {
                fileChooser.addFilter(extensionFilter);
            }
            fileChooser.setMultiSelectionEnabled(false);
            ModernNativeFileChooserResponse response = fileChooser.showOpenDialog(UIUtils.getWindowOrWindowAncestor(parent));
            return switch (response) {
                case OK -> fileChooser.getSelectedFile().toPath();
                case Cancelled -> null;
                case Error ->
                        openFile(parent, workbench, key, title, description, instance.getSafeFallbackFileChooserType(), extensionFilters);
            };
        } else {
            Path result = JIPipeDesktopFileChooserNext.showDialogSingle(parent,
                    workbench,
                    title,
                    description, currentPath,
                    PathIOMode.Open,
                    PathType.FilesOnly,
                    extensionFilters);
            if (result != null) {
                instance.setLastDirectoryBy(key, result.getParent());
            }
            return result;
        }
    }

    /**
     * Lets the user choose a file
     *
     * @param parent           parent component
     * @param workbench        the workbench
     * @param key              location where the dialog is opened
     * @param title            dialog title
     * @param description      optional description (only supported by specific file chooser types)
     * @param extensionFilters extension filters. the first one is chosen automatically
     * @return selected file or null if dialog was cancelled
     */
    public static Path saveFile(Component parent, JIPipeWorkbench workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey key, String title, HTMLText description, FileNameExtensionFilter... extensionFilters) {
        JIPipeFileChooserApplicationSettings instance = JIPipeFileChooserApplicationSettings.getInstance();
        final JIPipeFileChooserApplicationSettings.FileChooserType fileChooserType = instance.getFileChooserType();
        return saveFile(parent, workbench, key, title, description, fileChooserType, extensionFilters);
    }

    /**
     * Lets the user choose a file
     *
     * @param parent           parent component
     * @param workbench        the workbench
     * @param key              location where the dialog is opened
     * @param title            dialog title
     * @param description      optional description (only supported by specific file chooser types)
     * @param fileChooserType  the file chooser type to use
     * @param extensionFilters extension filters. the first one is chosen automatically
     * @return selected file or null if dialog was cancelled
     */
    public static Path saveFile(Component parent, JIPipeWorkbench workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey key, String title, HTMLText description, JIPipeFileChooserApplicationSettings.FileChooserType fileChooserType, FileNameExtensionFilter... extensionFilters) {
        JIPipeFileChooserApplicationSettings instance = JIPipeFileChooserApplicationSettings.getInstance();
        Path currentPath = instance.getLastDirectoryBy(workbench, key);
        if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.Native) {
            FileDialog dialog = createFileDialog(parent, title, FileDialog.SAVE);
            dialog.setTitle(title);
            dialog.setDirectory(currentPath.toString());
            dialog.setMultipleMode(false);
            dialog.setVisible(true);
            String fileName = dialog.getFile();
            if (fileName != null) {
                Path path = Paths.get(fileName);
                instance.setLastDirectoryBy(key, path.getParent());
                if (JIPipeFileChooserApplicationSettings.getInstance().isAddFileExtension() &&
                        extensionFilters.length > 0) {
                    boolean found = false;
                    outer:
                    for (FileNameExtensionFilter extensionFilter : extensionFilters) {
                        for (String extension : extensionFilter.getExtensions()) {
                            if (path.toString().toLowerCase(Locale.ROOT).endsWith(extension)) {
                                found = true;
                                break outer;
                            }
                        }
                    }
                    if (!found) {
                        String extension = extensionFilters[0].getExtensions()[0];
                        path = path.getParent().resolve(path.getFileName() + "." + extension);
                    }
                }
                return path;
            } else {
                return null;
            }
        } else if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.Standard) {
            JFileChooser fileChooser = new JFileChooser(currentPath.toFile());
            fileChooser.setDialogTitle(title);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if (extensionFilters.length > 0) {
                for (FileNameExtensionFilter extensionFilter : extensionFilters) {
                    fileChooser.addChoosableFileFilter(extensionFilter);
                }
                fileChooser.setFileFilter(extensionFilters[0]);
            }
            if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
                Path path = fileChooser.getSelectedFile().toPath();
                instance.setLastDirectoryBy(key, path.getParent());
                if (JIPipeFileChooserApplicationSettings.getInstance().isAddFileExtension() && extensionFilters.length > 0 && fileChooser.getFileFilter() instanceof FileNameExtensionFilter) {
                    FileNameExtensionFilter fileNameExtensionFilter = (FileNameExtensionFilter) fileChooser.getFileFilter();
                    boolean found = false;
                    for (String extension : fileNameExtensionFilter.getExtensions()) {
                        if (path.toString().toLowerCase(Locale.ROOT).endsWith(extension)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        path = path.getParent().resolve(path.getFileName() + "." + fileNameExtensionFilter.getExtensions()[0]);
                    }
                }
                return path;
            } else {
                return null;
            }
        } else if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.AdvancedLegacy) {
            JIPipeDesktopAdvancedFileChooser fileChooser = new JIPipeDesktopAdvancedFileChooser(currentPath.toFile());
            fileChooser.setDialogTitle(title);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if (extensionFilters.length > 0) {
                for (FileNameExtensionFilter extensionFilter : extensionFilters) {
                    fileChooser.addChoosableFileFilter(extensionFilter);
                }
                fileChooser.setFileFilter(extensionFilters[0]);
            }
            if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
                Path path = fileChooser.getSelectedFile().toPath();
                instance.setLastDirectoryBy(key, path.getParent());
                if (JIPipeFileChooserApplicationSettings.getInstance().isAddFileExtension() && extensionFilters.length > 0 && fileChooser.getFileFilter() instanceof FileNameExtensionFilter) {
                    FileNameExtensionFilter fileNameExtensionFilter = (FileNameExtensionFilter) fileChooser.getFileFilter();
                    boolean found = false;
                    for (String extension : fileNameExtensionFilter.getExtensions()) {
                        if (path.toString().toLowerCase(Locale.ROOT).endsWith(extension)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        path = path.getParent().resolve(path.getFileName() + "." + fileNameExtensionFilter.getExtensions()[0]);
                    }
                }
                return path;
            } else {
                return null;
            }
        } else if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.ModernNative) {
            ModernNativeFileChooser fileChooser = new ModernNativeFileChooser(currentPath.toFile());
            fileChooser.setTitle(title);
            fileChooser.setMode(PathType.FilesOnly);
            for (FileNameExtensionFilter extensionFilter : extensionFilters) {
                fileChooser.addFilter(extensionFilter);
            }
            fileChooser.setMultiSelectionEnabled(false);
            ModernNativeFileChooserResponse response = fileChooser.showSaveDialog(UIUtils.getWindowOrWindowAncestor(parent));
            return switch (response) {
                case OK -> fileChooser.getSelectedFile().toPath();
                case Cancelled -> null;
                case Error ->
                        saveFile(parent, workbench, key, title, description, instance.getSafeFallbackFileChooserType(), extensionFilters);
            };
        } else {
            Path path = JIPipeDesktopFileChooserNext.showDialogSingle(parent,
                    workbench,
                    title,
                    description, currentPath,
                    PathIOMode.Save,
                    PathType.FilesOnly,
                    extensionFilters);
            if (path != null) {
                instance.setLastDirectoryBy(key, path.getParent());
            }
            return path;
        }
    }

    /**
     * Lets the user choose a file or directory
     *
     * @param parent      parent component
     * @param workbench   the workbench
     * @param key         location where the dialog is opened
     * @param title       dialog title
     * @param description optional description (only supported by specific file chooser types)
     * @return selected file or null if dialog was cancelled
     */
    public static Path openPath(Component parent, JIPipeWorkbench workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey key, String title, HTMLText description, FileNameExtensionFilter... extensionFilters) {
        JIPipeFileChooserApplicationSettings instance = JIPipeFileChooserApplicationSettings.getInstance();
        final JIPipeFileChooserApplicationSettings.FileChooserType fileChooserType = instance.getFileChooserType();
        return openPath(parent, workbench, key, title, description, fileChooserType, extensionFilters);
    }

    /**
     * Lets the user choose a file or directory
     *
     * @param parent          parent component
     * @param workbench       the workbench
     * @param key             location where the dialog is opened
     * @param title           dialog title
     * @param description     optional description (only supported by specific file chooser types)
     * @param fileChooserType the file chooser type to use
     * @return selected file or null if dialog was cancelled
     */
    public static Path openPath(Component parent, JIPipeWorkbench workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey key, String title, HTMLText description, JIPipeFileChooserApplicationSettings.FileChooserType fileChooserType, FileNameExtensionFilter... extensionFilters) {
        JIPipeFileChooserApplicationSettings instance = JIPipeFileChooserApplicationSettings.getInstance();
        Path currentPath = instance.getLastDirectoryBy(workbench, key);

        // No OS has native Path selection dialogs, so let the user choose
        if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.ModernNative && instance.isAskOnNativePathSelect()) {
            int option = JOptionPane.showOptionDialog(parent,
                    "Do you want to open a file or a directory?",
                    title,
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new Object[]{"Open file", "Open directory", "Cancel"},
                    "Open file");
            return switch (option) {
                case JOptionPane.YES_OPTION ->
                        openFile(parent, workbench, key, title, description, fileChooserType, extensionFilters);
                case JOptionPane.NO_OPTION -> openDirectory(parent, workbench, key, title, description);
                default -> null;
            };
        }

        if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.Standard) {
            JFileChooser fileChooser = new JFileChooser(currentPath.toFile());
            fileChooser.setDialogTitle(title);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                Path path = fileChooser.getSelectedFile().toPath();
                instance.setLastDirectoryBy(key, path);
                return path;
            } else {
                return null;
            }
        } else if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.AdvancedLegacy) {
            JIPipeDesktopAdvancedFileChooser fileChooser = new JIPipeDesktopAdvancedFileChooser(currentPath.toFile());
            fileChooser.setDialogTitle(title);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                Path path = fileChooser.getSelectedFile().toPath();
                instance.setLastDirectoryBy(key, path);
                return path;
            } else {
                return null;
            }
        } else if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.ModernNative) {
            // No native equivalent for path operations, use fallback
            return openPath(parent, workbench, key, title, description, instance.getSafeFallbackFileChooserType(), extensionFilters);
        } else {
            Path result = JIPipeDesktopFileChooserNext.showDialogSingle(parent,
                    workbench,
                    title,
                    description, currentPath,
                    PathIOMode.Open,
                    PathType.FilesAndDirectories,
                    extensionFilters);
            if (result != null) {
                instance.setLastDirectoryBy(key, result);
            }
            return result;
        }
    }

    /**
     * Lets the user choose a file or directory
     *
     * @param parent      parent component
     * @param workbench   the workbench
     * @param key         location where the dialog is opened
     * @param title       dialog title
     * @param description optional description (only supported by specific file chooser types)
     * @return selected file or null if dialog was cancelled
     */
    public static Path savePath(Component parent, JIPipeWorkbench workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey key, String title, HTMLText description, FileNameExtensionFilter... extensionFilters) {
        JIPipeFileChooserApplicationSettings instance = JIPipeFileChooserApplicationSettings.getInstance();
        final JIPipeFileChooserApplicationSettings.FileChooserType fileChooserType = instance.getFileChooserType();
        return savePath(parent, workbench, key, title, description, fileChooserType, extensionFilters);
    }

    /**
     * Lets the user choose a file or directory
     *
     * @param parent          parent component
     * @param workbench       the workbench
     * @param key             location where the dialog is opened
     * @param title           dialog title
     * @param description     optional description (only supported by specific file chooser types)
     * @param fileChooserType the file chooser type to use
     * @return selected file or null if dialog was cancelled
     */
    public static Path savePath(Component parent, JIPipeWorkbench workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey key, String title, HTMLText description, JIPipeFileChooserApplicationSettings.FileChooserType fileChooserType, FileNameExtensionFilter... extensionFilters) {
        JIPipeFileChooserApplicationSettings instance = JIPipeFileChooserApplicationSettings.getInstance();

        // No OS has native Path selection dialogs, so let the user choose
        if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.ModernNative && instance.isAskOnNativePathSelect()) {
            int option = JOptionPane.showOptionDialog(parent,
                    "Do you want to save a file or a directory?",
                    title,
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new Object[]{"Save file", "Select directory", "Cancel"},
                    "Save file");
            return switch (option) {
                case JOptionPane.YES_OPTION ->
                        saveFile(parent, workbench, key, title, description, fileChooserType, extensionFilters);
                case JOptionPane.NO_OPTION -> saveDirectory(parent, workbench, key, title, description);
                default -> null;
            };
        }

        Path currentPath = instance.getLastDirectoryBy(workbench, key);
        if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.Standard) {
            JFileChooser fileChooser = new JFileChooser(currentPath.toFile());
            fileChooser.setDialogTitle(title);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
                Path path = fileChooser.getSelectedFile().toPath();
                instance.setLastDirectoryBy(key, path);
                return path;
            } else {
                return null;
            }
        } else if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.AdvancedLegacy) {
            JIPipeDesktopAdvancedFileChooser fileChooser = new JIPipeDesktopAdvancedFileChooser(currentPath.toFile());
            fileChooser.setDialogTitle(title);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
                Path path = fileChooser.getSelectedFile().toPath();
                instance.setLastDirectoryBy(key, path);
                return path;
            } else {
                return null;
            }
        } else if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.ModernNative) {
            // No native equivalent for path operations, use fallback
            return savePath(parent, workbench, key, title, description, instance.getSafeFallbackFileChooserType(), extensionFilters);
        } else {
            Path path = JIPipeDesktopFileChooserNext.showDialogSingle(parent,
                    workbench,
                    title,
                    description, currentPath,
                    PathIOMode.Save,
                    PathType.FilesAndDirectories,
                    extensionFilters);
            if (path != null) {
                instance.setLastDirectoryBy(key, path);
            }
            return path;
        }
    }

    /**
     * Lets the user choose a directory
     *
     * @param parent      parent component
     * @param workbench   the workbench
     * @param key         location where the dialog is opened
     * @param title       dialog title
     * @param description optional description (only supported by specific file chooser types)
     * @return selected directory or null if dialog was cancelled
     */
    public static Path openDirectory(Component parent, JIPipeWorkbench workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey key, String title, HTMLText description) {
        JIPipeFileChooserApplicationSettings instance = JIPipeFileChooserApplicationSettings.getInstance();
        final JIPipeFileChooserApplicationSettings.FileChooserType fileChooserType = instance.getFileChooserType();
        return openDirectory(parent, workbench, key, title, description, fileChooserType);
    }

    /**
     * Lets the user choose a directory
     *
     * @param parent          parent component
     * @param workbench       the workbench
     * @param key             location where the dialog is opened
     * @param title           dialog title
     * @param description     optional description (only supported by specific file chooser types)
     * @param fileChooserType the file chooser type to use
     * @return selected directory or null if dialog was cancelled
     */
    public static Path openDirectory(Component parent, JIPipeWorkbench workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey key, String title, HTMLText description, JIPipeFileChooserApplicationSettings.FileChooserType fileChooserType) {
        JIPipeFileChooserApplicationSettings instance = JIPipeFileChooserApplicationSettings.getInstance();
        Path currentPath = instance.getLastDirectoryBy(workbench, key);
        if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.Native) {
            FileDialog dialog = createFileDialog(parent, title, FileDialog.LOAD);
            dialog.setTitle(title);
            dialog.setDirectory(currentPath.toString());
            dialog.setMultipleMode(false);
            dialog.setVisible(true);
            String fileName = dialog.getFile();
            if (fileName != null) {
                Path path = Paths.get(fileName);
                instance.setLastDirectoryBy(key, path);
                return path;
            } else {
                return null;
            }
        } else if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.Standard) {
            JFileChooser fileChooser = new JFileChooser(currentPath.toFile());
            fileChooser.setDialogTitle(title);
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                Path path = fileChooser.getSelectedFile().toPath();
                instance.setLastDirectoryBy(key, path);
                return path;
            } else {
                return null;
            }
        } else if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.AdvancedLegacy) {
            JIPipeDesktopAdvancedFileChooser fileChooser = new JIPipeDesktopAdvancedFileChooser(currentPath.toFile());
            fileChooser.setDialogTitle(title);
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                Path path = fileChooser.getSelectedFile().toPath();
                instance.setLastDirectoryBy(key, path);
                return path;
            } else {
                return null;
            }
        } else if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.ModernNative) {
            ModernNativeFileChooser fileChooser = new ModernNativeFileChooser(currentPath.toFile());
            fileChooser.setTitle(title);
            fileChooser.setMode(PathType.DirectoriesOnly);
            fileChooser.setMultiSelectionEnabled(false);
            ModernNativeFileChooserResponse response = fileChooser.showOpenDialog(UIUtils.getWindowOrWindowAncestor(parent));
            return switch (response) {
                case OK -> fileChooser.getSelectedFile().toPath();
                case Cancelled -> null;
                case Error ->
                        openDirectory(parent, workbench, key, title, description, instance.getSafeFallbackFileChooserType());
            };
        } else {
            Path path = JIPipeDesktopFileChooserNext.showDialogSingle(parent,
                    workbench,
                    title,
                    description, currentPath,
                    PathIOMode.Open,
                    PathType.DirectoriesOnly);
            if (path != null) {
                instance.setLastDirectoryBy(key, path);
            }
            return path;
        }
    }

    /**
     * Lets the user choose a directory
     *
     * @param parent      parent component
     * @param workbench   the workbench
     * @param key         location where the dialog is opened
     * @param title       dialog title
     * @param description optional description (only supported by specific file chooser types)
     * @return selected directory or null if dialog was cancelled
     */
    public static Path saveDirectory(Component parent, JIPipeWorkbench workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey key, String title, HTMLText description) {
        JIPipeFileChooserApplicationSettings instance = JIPipeFileChooserApplicationSettings.getInstance();
        return saveDirectory(parent, workbench, key, title, description, instance.getFileChooserType());
    }

    /**
     * Lets the user choose a directory
     *
     * @param parent      parent component
     * @param workbench   the workbench
     * @param key         location where the dialog is opened
     * @param title       dialog title
     * @param description optional description (only supported by specific file chooser types)
     * @return selected directory or null if dialog was cancelled
     */
    public static Path saveDirectory(Component parent, JIPipeWorkbench workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey key, String title, HTMLText description, JIPipeFileChooserApplicationSettings.FileChooserType fileChooserType) {
        JIPipeFileChooserApplicationSettings instance = JIPipeFileChooserApplicationSettings.getInstance();
        Path currentPath = instance.getLastDirectoryBy(workbench, key);
        if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.Standard) {
            JFileChooser fileChooser = new JFileChooser(currentPath.toFile());
            fileChooser.setDialogTitle(title);
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
                Path path = fileChooser.getSelectedFile().toPath();
                instance.setLastDirectoryBy(key, path);
                return path;
            } else {
                return null;
            }
        } else if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.AdvancedLegacy) {
            JIPipeDesktopAdvancedFileChooser fileChooser = new JIPipeDesktopAdvancedFileChooser(currentPath.toFile());
            fileChooser.setDialogTitle(title);
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
                Path path = fileChooser.getSelectedFile().toPath();
                instance.setLastDirectoryBy(key, path);
                return path;
            } else {
                return null;
            }
        } else if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.ModernNative) {
            return openDirectory(parent, workbench, key, title, description);
        } else {
            Path path = JIPipeDesktopFileChooserNext.showDialogSingle(parent,
                    workbench,
                    title,
                    description, currentPath,
                    PathIOMode.Open,
                    PathType.DirectoriesOnly);
            if (path != null) {
                instance.setLastDirectoryBy(key, path);
            }
            return path;
        }
    }

    /**
     * Lets the user choose a directory
     *
     * @param parent      parent component
     * @param workbench   the workbench
     * @param currentPath starting location/default value
     * @param title       dialog title
     * @param description optional description (only supported by specific file chooser types)
     * @return selected directory or null if dialog was cancelled
     */
    public static Path saveDirectory(Component parent, JIPipeWorkbench workbench, Path currentPath, String title, HTMLText description) {
        JIPipeFileChooserApplicationSettings instance = JIPipeFileChooserApplicationSettings.getInstance();
        return saveDirectory(parent, workbench, currentPath, title, description, instance.getFileChooserType());
    }

    /**
     * Lets the user choose a directory
     *
     * @param parent      parent component
     * @param workbench   the workbench
     * @param currentPath starting location/default value
     * @param title       dialog title
     * @param description optional description (only supported by specific file chooser types)
     * @return selected directory or null if dialog was cancelled
     */
    public static Path saveDirectory(Component parent, JIPipeWorkbench workbench, Path currentPath, String title, HTMLText description, JIPipeFileChooserApplicationSettings.FileChooserType fileChooserType) {
        JIPipeFileChooserApplicationSettings instance = JIPipeFileChooserApplicationSettings.getInstance();
        if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.Standard) {
            JFileChooser fileChooser = new JFileChooser(currentPath.toFile());
            fileChooser.setDialogTitle(title);
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
                return fileChooser.getSelectedFile().toPath();
            } else {
                return null;
            }
        } else if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.AdvancedLegacy) {
            JIPipeDesktopAdvancedFileChooser fileChooser = new JIPipeDesktopAdvancedFileChooser(currentPath.toFile());
            fileChooser.setDialogTitle(title);
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
                return fileChooser.getSelectedFile().toPath();
            } else {
                return null;
            }
        } else if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.ModernNative) {
            // Has no equivalent for "save directory", so use open directory
            ModernNativeFileChooser fileChooser = new ModernNativeFileChooser(currentPath.toFile());
            fileChooser.setTitle(title);
            fileChooser.setMode(PathType.DirectoriesOnly);
            fileChooser.setMultiSelectionEnabled(false);
            ModernNativeFileChooserResponse response = fileChooser.showOpenDialog(UIUtils.getWindowOrWindowAncestor(parent));
            return switch (response) {
                case OK -> fileChooser.getSelectedFile().toPath();
                case Cancelled -> null;
                case Error ->
                        saveDirectory(parent, workbench, currentPath, title, description, instance.getSafeFallbackFileChooserType());
            };
        } else {
            return JIPipeDesktopFileChooserNext.showDialogSingle(parent,
                    workbench,
                    title,
                    description,
                    currentPath,
                    PathIOMode.Open,
                    PathType.DirectoriesOnly);
        }
    }

    /**
     * Lets the user choose multiple files
     *
     * @param parent      parent component
     * @param workbench   the workbench
     * @param key         location where the dialog is opened
     * @param title       dialog title
     * @param description optional description (only supported by specific file chooser types)
     * @return selected list of files. Is empty if dialog was cancelled.
     */
    public static List<Path> openFiles(Component parent, JIPipeWorkbench workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey key, String title, HTMLText description, FileNameExtensionFilter... extensionFilters) {
        JIPipeFileChooserApplicationSettings instance = JIPipeFileChooserApplicationSettings.getInstance();
        return openFiles(parent, workbench, key, title, description, instance.getFileChooserType(), extensionFilters);
    }

    /**
     * Lets the user choose multiple files
     *
     * @param parent      parent component
     * @param workbench   the workbench
     * @param key         location where the dialog is opened
     * @param title       dialog title
     * @param description optional description (only supported by specific file chooser types)
     * @return selected list of files. Is empty if dialog was cancelled.
     */
    public static List<Path> openFiles(Component parent, JIPipeWorkbench workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey key, String title, HTMLText description, JIPipeFileChooserApplicationSettings.FileChooserType fileChooserType, FileNameExtensionFilter... extensionFilters) {
        JIPipeFileChooserApplicationSettings instance = JIPipeFileChooserApplicationSettings.getInstance();
        Path currentPath = instance.getLastDirectoryBy(workbench, key);
        if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.Native) {
            FileDialog dialog = createFileDialog(parent, title, FileDialog.LOAD);
            dialog.setTitle(title);
            dialog.setDirectory(currentPath.toString());
            dialog.setMultipleMode(true);
            dialog.setVisible(true);
            File[] files = dialog.getFiles();
            if (files.length > 0) {
                instance.setLastDirectoryBy(key, files[0].toPath().getParent());
            }
            return Arrays.stream(files).map(File::toPath).collect(Collectors.toList());
        } else if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.Standard) {
            JFileChooser fileChooser = new JFileChooser(currentPath.toFile());
            fileChooser.setDialogTitle(title);
            fileChooser.setMultiSelectionEnabled(true);
            if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                if (fileChooser.getSelectedFile() != null) {
                    instance.setLastDirectoryBy(key, fileChooser.getSelectedFile().toPath().getParent());
                }
                return Arrays.stream(fileChooser.getSelectedFiles()).map(File::toPath).collect(Collectors.toList());
            } else {
                return Collections.emptyList();
            }
        } else if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.AdvancedLegacy) {
            JIPipeDesktopAdvancedFileChooser fileChooser = new JIPipeDesktopAdvancedFileChooser(currentPath.toFile());
            fileChooser.setDialogTitle(title);
            fileChooser.setMultiSelectionEnabled(true);
            if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                if (fileChooser.getSelectedFile() != null) {
                    instance.setLastDirectoryBy(key, fileChooser.getSelectedFile().toPath().getParent());
                }
                return Arrays.stream(fileChooser.getSelectedFiles()).map(File::toPath).collect(Collectors.toList());
            } else {
                return Collections.emptyList();
            }
        } else if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.ModernNative) {
            ModernNativeFileChooser fileChooser = new ModernNativeFileChooser(currentPath.toFile());
            fileChooser.setTitle(title);
            fileChooser.setMode(PathType.FilesOnly);
            for (FileNameExtensionFilter extensionFilter : extensionFilters) {
                fileChooser.addFilter(extensionFilter);
            }
            fileChooser.setMultiSelectionEnabled(true);
            ModernNativeFileChooserResponse response = fileChooser.showOpenDialog(UIUtils.getWindowOrWindowAncestor(parent));
            return switch (response) {
                case OK -> Arrays.stream(fileChooser.getSelectedFiles()).map(File::toPath).collect(Collectors.toList());
                case Cancelled -> null;
                case Error ->
                        openFiles(parent, workbench, key, title, description, instance.getSafeFallbackFileChooserType(), extensionFilters);
            };
        } else {
            List<Path> paths = JIPipeDesktopFileChooserNext.showDialog(parent,
                    workbench,
                    title,
                    description, currentPath,
                    PathIOMode.Open,
                    PathType.FilesOnly,
                    true,
                    extensionFilters);
            if (!paths.isEmpty()) {
                instance.setLastDirectoryBy(key, paths.getFirst().getParent());
            }
            return paths;
        }
    }

    /**
     * Lets the user choose multiple directories
     *
     * @param parent      parent component
     * @param workbench   the workbench
     * @param key         location where the dialog is opened
     * @param title       dialog title
     * @param description optional description (only supported by specific file chooser types)
     * @return selected list of files. Is empty if dialog was cancelled.
     */
    public static List<Path> openDirectories(Component parent, JIPipeWorkbench workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey key, String title, HTMLText description) {
        JIPipeFileChooserApplicationSettings instance = JIPipeFileChooserApplicationSettings.getInstance();
        final JIPipeFileChooserApplicationSettings.FileChooserType fileChooserType = instance.getFileChooserType();
        return openDirectories(parent, workbench, key, title, description, fileChooserType);
    }

    /**
     * Lets the user choose multiple directories
     *
     * @param parent          parent component
     * @param workbench       the workbench
     * @param key             location where the dialog is opened
     * @param title           dialog title
     * @param description     optional description (only supported by specific file chooser types)
     * @param fileChooserType the file chooser type to use
     * @return selected list of files. Is empty if dialog was cancelled.
     */
    public static List<Path> openDirectories(Component parent, JIPipeWorkbench workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey key, String title, HTMLText description, JIPipeFileChooserApplicationSettings.FileChooserType fileChooserType) {
        JIPipeFileChooserApplicationSettings instance = JIPipeFileChooserApplicationSettings.getInstance();
        Path currentPath = instance.getLastDirectoryBy(workbench, key);
        if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.Native) {
            FileDialog dialog = createFileDialog(parent, title, FileDialog.LOAD);
            dialog.setTitle(title);
            dialog.setDirectory(currentPath.toString());
            dialog.setMultipleMode(true);
            dialog.setVisible(true);
            File[] files = dialog.getFiles();
            if (files.length > 0) {
                instance.setLastDirectoryBy(key, files[0].toPath());
            }
            return Arrays.stream(files).map(File::toPath).collect(Collectors.toList());
        } else if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.Standard) {
            JFileChooser fileChooser = new JFileChooser(currentPath.toFile());
            fileChooser.setDialogTitle(title);
            fileChooser.setMultiSelectionEnabled(true);
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                if (fileChooser.getSelectedFile() != null) {
                    instance.setLastDirectoryBy(key, fileChooser.getSelectedFile().toPath());
                }
                return Arrays.stream(fileChooser.getSelectedFiles()).map(File::toPath).collect(Collectors.toList());
            } else {
                return Collections.emptyList();
            }
        } else if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.AdvancedLegacy) {
            JIPipeDesktopAdvancedFileChooser fileChooser = new JIPipeDesktopAdvancedFileChooser(currentPath.toFile());
            fileChooser.setDialogTitle(title);
            fileChooser.setMultiSelectionEnabled(true);
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                if (fileChooser.getSelectedFile() != null) {
                    instance.setLastDirectoryBy(key, fileChooser.getSelectedFile().toPath());
                }
                return Arrays.stream(fileChooser.getSelectedFiles()).map(File::toPath).collect(Collectors.toList());
            } else {
                return Collections.emptyList();
            }
        } else if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.ModernNative) {
            ModernNativeFileChooser fileChooser = new ModernNativeFileChooser(currentPath.toFile());
            fileChooser.setTitle(title);
            fileChooser.setMode(PathType.DirectoriesOnly);
            fileChooser.setMultiSelectionEnabled(true);
            ModernNativeFileChooserResponse response = fileChooser.showOpenDialog(UIUtils.getWindowOrWindowAncestor(parent));
            return switch (response) {
                case OK -> Arrays.stream(fileChooser.getSelectedFiles()).map(File::toPath).collect(Collectors.toList());
                case Cancelled -> Collections.emptyList();
                case Error ->
                        openDirectories(parent, workbench, key, title, description, instance.getSafeFallbackFileChooserType());
            };
        } else {
            List<Path> paths = JIPipeDesktopFileChooserNext.showDialog(parent,
                    workbench,
                    title,
                    description, currentPath,
                    PathIOMode.Open,
                    PathType.DirectoriesOnly,
                    true);
            if (!paths.isEmpty()) {
                instance.setLastDirectoryBy(key, paths.getFirst());
            }
            return paths;
        }
    }

    /**
     * Lets the user choose multiple files or directories
     *
     * @param parent      parent component
     * @param workbench   the workbench
     * @param key         location where the dialog is opened
     * @param title       dialog title
     * @param description optional description (only supported by specific file chooser types)
     * @return selected list of files. Is empty if dialog was cancelled.
     */
    public static List<Path> openPaths(Component parent, JIPipeWorkbench workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey key, String title, HTMLText description, FileNameExtensionFilter... extensionFilters) {
        JIPipeFileChooserApplicationSettings instance = JIPipeFileChooserApplicationSettings.getInstance();
        final JIPipeFileChooserApplicationSettings.FileChooserType fileChooserType = instance.getFileChooserType();
        return openPaths(parent, workbench, key, title, description, fileChooserType, extensionFilters);
    }

    /**
     * Lets the user choose multiple files or directories
     *
     * @param parent          parent component
     * @param workbench       the workbench
     * @param key             location where the dialog is opened
     * @param title           dialog title
     * @param description     optional description (only supported by specific file chooser types)
     * @param fileChooserType the file chooser type to use
     * @return selected list of files. Is empty if dialog was cancelled.
     */
    public static List<Path> openPaths(Component parent, JIPipeWorkbench workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey key, String title, HTMLText description, JIPipeFileChooserApplicationSettings.FileChooserType fileChooserType, FileNameExtensionFilter... extensionFilters) {
        JIPipeFileChooserApplicationSettings instance = JIPipeFileChooserApplicationSettings.getInstance();

        // No OS has native Path selection dialogs, so let the user choose
        if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.ModernNative && instance.isAskOnNativePathSelect()) {
            int option = JOptionPane.showOptionDialog(parent,
                    "Do you want to open a file or a directory?",
                    title,
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new Object[]{"Open file", "Open directory", "Cancel"},
                    "Open file");
            return switch (option) {
                case JOptionPane.YES_OPTION ->
                        openFiles(parent, workbench, key, title, description, fileChooserType, extensionFilters);
                case JOptionPane.NO_OPTION -> openDirectories(parent, workbench, key, title, description);
                default -> null;
            };
        }

        Path currentPath = instance.getLastDirectoryBy(workbench, key);
        if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.Standard) {
            JFileChooser fileChooser = new JFileChooser(currentPath.toFile());
            fileChooser.setDialogTitle(title);
            fileChooser.setMultiSelectionEnabled(true);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                if (fileChooser.getSelectedFile() != null) {
                    instance.setLastDirectoryBy(key, fileChooser.getSelectedFile().toPath());
                }
                return Arrays.stream(fileChooser.getSelectedFiles()).map(File::toPath).collect(Collectors.toList());
            } else {
                return Collections.emptyList();
            }
        } else if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.AdvancedLegacy) {
            JIPipeDesktopAdvancedFileChooser fileChooser = new JIPipeDesktopAdvancedFileChooser(currentPath.toFile());
            fileChooser.setDialogTitle(title);
            fileChooser.setMultiSelectionEnabled(true);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                if (fileChooser.getSelectedFile() != null) {
                    instance.setLastDirectoryBy(key, fileChooser.getSelectedFile().toPath());
                }
                return Arrays.stream(fileChooser.getSelectedFiles()).map(File::toPath).collect(Collectors.toList());
            } else {
                return Collections.emptyList();
            }
        } else if (fileChooserType == JIPipeFileChooserApplicationSettings.FileChooserType.ModernNative) {
            // No native equivalent for path operations, use fallback
            return openPaths(parent, workbench, key, title, description, instance.getSafeFallbackFileChooserType(), extensionFilters);
        } else {
            List<Path> paths = JIPipeDesktopFileChooserNext.showDialog(parent,
                    workbench,
                    title,
                    description, currentPath,
                    PathIOMode.Open,
                    PathType.FilesAndDirectories,
                    true,
                    extensionFilters);
            if (!paths.isEmpty()) {
                instance.setLastDirectoryBy(key, paths.getFirst());
            }
            return paths;
        }
    }

    /**
     * Generic open/save method for single paths
     *
     * @param parent           parent component
     * @param workbench        the workbench
     * @param key              location where the dialog is opened
     * @param title            dialog title
     * @param description      optional description (only supported by specific file chooser types)
     * @param ioMode           whether to load or save
     * @param pathMode         which types of paths are returned
     * @param extensionFilters passed if a file is opened/saved
     * @return selected path of provided pathMode or null if dialog was cancelled
     */
    public static Path selectSingle(Component parent, JIPipeWorkbench workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey key, String title, HTMLText description, PathIOMode ioMode, PathType pathMode, FileNameExtensionFilter... extensionFilters) {
        Path selected;
        if (ioMode == PathIOMode.Open) {
            selected = switch (pathMode) {
                case FilesOnly -> openFile(parent, workbench, key, title, description, extensionFilters);
                case DirectoriesOnly -> openDirectory(parent, workbench, key, title, description);
                case FilesAndDirectories -> openPath(parent, workbench, key, title, description);
                default -> throw new UnsupportedOperationException("Unsupported: " + pathMode);
            };
        } else {
            selected = switch (pathMode) {
                case FilesOnly -> saveFile(parent, workbench, key, title, description, extensionFilters);
                case DirectoriesOnly -> saveDirectory(parent, workbench, key, title, description);
                case FilesAndDirectories -> savePath(parent, workbench, key, title, description);
                default -> throw new UnsupportedOperationException("Unsupported: " + pathMode);
            };
        }
        return selected;
    }

    /**
     * Generic open method for multiple paths.
     * Info: Due to API limitations, only one saved file can be returned.
     *
     * @param parent      parent component
     * @param workbench   the workbench
     * @param key         location where the dialog is opened
     * @param title       dialog title
     * @param description optional description (only supported by specific file chooser types)
     * @param ioMode      whether to load or save
     * @param pathMode    which types of paths are returned
     * @return selected paths of provided pathMode or empty list if dialog was cancelled
     */
    public static List<Path> selectMulti(Component parent, JIPipeWorkbench workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey key, String title, HTMLText description, PathIOMode ioMode, PathType pathMode, FileNameExtensionFilter... extensionFilters) {
        List<Path> selected;
        if (ioMode == PathIOMode.Open) {
            selected = switch (pathMode) {
                case FilesOnly -> openFiles(parent, workbench, key, title, description, extensionFilters);
                case DirectoriesOnly -> openDirectories(parent, workbench, key, title, description);
                case FilesAndDirectories -> openPaths(parent, workbench, key, title, description, extensionFilters);
                default -> throw new UnsupportedOperationException("Unsupported: " + pathMode);
            };
            return selected;
        } else {
            Path saveSelection = switch (pathMode) {
                case FilesOnly -> saveFile(parent, workbench, key, title, description, extensionFilters);
                case DirectoriesOnly -> saveDirectory(parent, workbench, key, title, description);
                case FilesAndDirectories -> savePath(parent, workbench, key, title, description, extensionFilters);
                default -> throw new UnsupportedOperationException("Unsupported: " + pathMode);
            };
            if (saveSelection != null) {
                return Arrays.asList(saveSelection);
            } else {
                return Collections.emptyList();
            }
        }
    }

    private static FileDialog createFileDialog(Component parent, String title, int mode) {
        Window windowAncestor = UIUtils.getWindowOrWindowAncestor(parent);
        if (windowAncestor instanceof Frame) {
            return new FileDialog((Frame) parent, title, mode);
        } else if (windowAncestor instanceof Dialog) {
            return new FileDialog((Dialog) parent, title, mode);
        } else {
            throw new UnsupportedOperationException("Unknown window type: " + windowAncestor);
        }
    }

    /**
     * Registers a path or URL to a *.jip *.crate.zip or https:// URL as next operation to open when the project window is opened the first time
     * @param pathOrUrl path to an existing *.jip or *.crate.zip file or a URL
     */
    public static void tryAddOpenProjectOnLoad(String pathOrUrl) {
        if(StringUtils.isNullOrEmpty(pathOrUrl)) {
            return;
        }
        try {
            Path path = Paths.get(pathOrUrl);
            String fileName = path.getFileName().toString();
            if(Files.isRegularFile(path) && (fileName.endsWith(".jip") || fileName.endsWith(".crate.zip"))) {
                OPEN_ON_LOAD_FILE = path;
            }
        } catch (Exception ignored) {
        }
        try {
            URL url = new URL(pathOrUrl);
            OPEN_ON_LOAD_URL = url;
        }
        catch (Exception ignored) {
        }
    }

    public static void doOpenProjectOnLoad(JIPipeDesktopProjectWindow window) {
        if(OPEN_ON_LOAD_URL != null) {
            URL url = OPEN_ON_LOAD_URL;
            OPEN_ON_LOAD_URL = null;
            OPEN_ON_LOAD_FILE = null;
            window.importURL(url.toString(), true);
        }
        else if(OPEN_ON_LOAD_FILE != null) {
            Path file = OPEN_ON_LOAD_FILE;
            OPEN_ON_LOAD_FILE = null;
            OPEN_ON_LOAD_URL = null;
            if(Files.isRegularFile(file)) {
                String fileName = file.getFileName().toString();
                if(fileName.endsWith(".jip")) {
                    window.openProject(file, true);
                }
                else if(fileName.endsWith(".crate.zip")) {
                    window.importROCrate(file, true, true, true);
                }
                else if(ArchiveUtils.isZipFile(file)) {
                    // Magic bytes check for zip
                    window.importROCrate(file, true, true, true);
                }
                else {
                    // If not a ZIP, try to open as JSON
                    window.openProject(file, true);
                }
            }
        }
    }
}
