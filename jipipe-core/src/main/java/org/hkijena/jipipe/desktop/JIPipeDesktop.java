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
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopAdvancedFileChooser;
import org.hkijena.jipipe.desktop.commons.components.filechoosernext.JIPipeDesktopFileChooserNext;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
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
        Path currentPath = instance.getLastDirectoryBy(workbench, key);
        if (instance.getFileChooserType() == JIPipeFileChooserApplicationSettings.FileChooserType.Native) {
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
        } else if (instance.getFileChooserType() == JIPipeFileChooserApplicationSettings.FileChooserType.Standard) {
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
        } else if (instance.getFileChooserType() == JIPipeFileChooserApplicationSettings.FileChooserType.AdvancedLegacy) {
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
        } else {
            return JIPipeDesktopFileChooserNext.showDialogSingle(parent,
                    workbench,
                    title,
                    description, currentPath,
                    PathIOMode.Open,
                    PathType.FilesOnly,
                    extensionFilters);
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
        Path currentPath = instance.getLastDirectoryBy(workbench, key);
        if (instance.getFileChooserType() == JIPipeFileChooserApplicationSettings.FileChooserType.Native) {
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
        } else if (instance.getFileChooserType() == JIPipeFileChooserApplicationSettings.FileChooserType.Standard) {
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
        } else if (instance.getFileChooserType() == JIPipeFileChooserApplicationSettings.FileChooserType.AdvancedLegacy) {
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
        } else {
            return JIPipeDesktopFileChooserNext.showDialogSingle(parent,
                    workbench,
                    title,
                    description, currentPath,
                    PathIOMode.Save,
                    PathType.FilesOnly,
                    extensionFilters);
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
        Path currentPath = instance.getLastDirectoryBy(workbench, key);
        if (instance.getFileChooserType() == JIPipeFileChooserApplicationSettings.FileChooserType.Standard) {
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
        } else if (instance.getFileChooserType() == JIPipeFileChooserApplicationSettings.FileChooserType.AdvancedLegacy) {
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
        } else {
            return JIPipeDesktopFileChooserNext.showDialogSingle(parent,
                    workbench,
                    title,
                    description, currentPath,
                    PathIOMode.Open,
                    PathType.FilesAndDirectories,
                    extensionFilters);
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
        Path currentPath = instance.getLastDirectoryBy(workbench, key);
        if (instance.getFileChooserType() == JIPipeFileChooserApplicationSettings.FileChooserType.Standard) {
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
        } else if (instance.getFileChooserType() == JIPipeFileChooserApplicationSettings.FileChooserType.AdvancedLegacy) {
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
        } else {
            return JIPipeDesktopFileChooserNext.showDialogSingle(parent,
                    workbench,
                    title,
                    description, currentPath,
                    PathIOMode.Save,
                    PathType.FilesAndDirectories,
                    extensionFilters);
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
        Path currentPath = instance.getLastDirectoryBy(workbench, key);
        if (instance.getFileChooserType() == JIPipeFileChooserApplicationSettings.FileChooserType.Standard) {
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
        } else if (instance.getFileChooserType() == JIPipeFileChooserApplicationSettings.FileChooserType.AdvancedLegacy) {
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
        } else {
            return JIPipeDesktopFileChooserNext.showDialogSingle(parent,
                    workbench,
                    title,
                    description, currentPath,
                    PathIOMode.Open,
                    PathType.DirectoriesOnly);
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
        Path currentPath = instance.getLastDirectoryBy(workbench, key);
        if (instance.getFileChooserType() == JIPipeFileChooserApplicationSettings.FileChooserType.Standard) {
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
        } else if (instance.getFileChooserType() == JIPipeFileChooserApplicationSettings.FileChooserType.AdvancedLegacy) {
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
        } else {
            return JIPipeDesktopFileChooserNext.showDialogSingle(parent,
                    workbench,
                    title,
                    description, currentPath,
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
        Path currentPath = instance.getLastDirectoryBy(workbench, key);
        if (instance.getFileChooserType() == JIPipeFileChooserApplicationSettings.FileChooserType.Native) {
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
        } else if (instance.getFileChooserType() == JIPipeFileChooserApplicationSettings.FileChooserType.Standard) {
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
        } else if (instance.getFileChooserType() == JIPipeFileChooserApplicationSettings.FileChooserType.AdvancedLegacy) {
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
        } else {
            return JIPipeDesktopFileChooserNext.showDialog(parent,
                    workbench,
                    title,
                    description, currentPath,
                    PathIOMode.Open,
                    PathType.FilesOnly,
                    true,
                    extensionFilters);
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
        Path currentPath = instance.getLastDirectoryBy(workbench, key);
        if (instance.getFileChooserType() == JIPipeFileChooserApplicationSettings.FileChooserType.Standard) {
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
        } else if (instance.getFileChooserType() == JIPipeFileChooserApplicationSettings.FileChooserType.AdvancedLegacy) {
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
        } else {
            return JIPipeDesktopFileChooserNext.showDialog(parent,
                    workbench,
                    title,
                    description, currentPath,
                    PathIOMode.Open,
                    PathType.DirectoriesOnly,
                    true);
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
        Path currentPath = instance.getLastDirectoryBy(workbench, key);
        if (instance.getFileChooserType() == JIPipeFileChooserApplicationSettings.FileChooserType.Standard) {
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
        } else if (instance.getFileChooserType() == JIPipeFileChooserApplicationSettings.FileChooserType.AdvancedLegacy) {
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
        } else {
            return JIPipeDesktopFileChooserNext.showDialog(parent,
                    workbench,
                    title,
                    description, currentPath,
                    PathIOMode.Open,
                    PathType.FilesAndDirectories,
                    true,
                    extensionFilters);
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
            switch (pathMode) {
                case FilesOnly:
                    selected = openFile(parent, workbench, key, title, description, extensionFilters);
                    break;
                case DirectoriesOnly:
                    selected = openDirectory(parent, workbench, key, title, description);
                    break;
                case FilesAndDirectories:
                    selected = openPath(parent, workbench, key, title, description);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported: " + pathMode);
            }
        } else {
            switch (pathMode) {
                case FilesOnly:
                    selected = saveFile(parent, workbench, key, title, description, extensionFilters);
                    break;
                case DirectoriesOnly:
                    selected = saveDirectory(parent, workbench, key, title, description);
                    break;
                case FilesAndDirectories:
                    selected = savePath(parent, workbench, key, title, description);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported: " + pathMode);
            }
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
            switch (pathMode) {
                case FilesOnly:
                    selected = openFiles(parent, workbench, key, title, description);
                    break;
                case DirectoriesOnly:
                    selected = openDirectories(parent, workbench, key, title, description);
                    break;
                case FilesAndDirectories:
                    selected = openPaths(parent, workbench, key, title, description);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported: " + pathMode);
            }
            return selected;
        } else {
            Path saveSelection;
            switch (pathMode) {
                case FilesOnly:
                    saveSelection = saveFile(parent, workbench, key, title, description);
                    break;
                case DirectoriesOnly:
                    saveSelection = saveDirectory(parent, workbench, key, title, description);
                    break;
                case FilesAndDirectories:
                    saveSelection = savePath(parent, workbench, key, title, description);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported: " + pathMode);
            }
            if (saveSelection != null) {
                return Arrays.asList(saveSelection);
            } else {
                return Collections.emptyList();
            }
        }
    }

    private static FileDialog createFileDialog(Component parent, String title, int mode) {
        Window windowAncestor = SwingUtilities.getWindowAncestor(parent);
        if (windowAncestor instanceof Frame) {
            return new FileDialog((Frame) parent, title, mode);
        } else if (windowAncestor instanceof Dialog) {
            return new FileDialog((Dialog) parent, title, mode);
        } else {
            throw new UnsupportedOperationException("Unknown window type: " + windowAncestor);
        }
    }
}
