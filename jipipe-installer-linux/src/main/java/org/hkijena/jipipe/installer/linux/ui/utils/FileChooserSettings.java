/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.installer.linux.ui.utils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Window;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Settings concerning file dialogs
 */
public class FileChooserSettings {

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

    /**
     * Lets the user choose a file
     *
     * @param parent           parent component
     * @param title            dialog title
     * @param extensionFilters optional extension filters. the first one is chosen automatically
     * @return selected file or null if dialog was cancelled
     */
    public static Path openFile(Component parent, String title, FileNameExtensionFilter... extensionFilters) {
        JFileChooser fileChooser = new JFileChooser();
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
            return path;
        } else {
            return null;
        }
    }

    /**
     * Lets the user choose a file
     *
     * @param parent           parent component
     * @param title            dialog title
     * @param extensionFilters extension filters. the first one is chosen automatically
     * @return selected file or null if dialog was cancelled
     */
    public static Path saveFile(Component parent, String title, FileNameExtensionFilter... extensionFilters) {
        
        Path currentPath = Paths.get("");
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
            if (extensionFilters.length > 0 && fileChooser.getFileFilter() instanceof FileNameExtensionFilter) {
                FileNameExtensionFilter fileNameExtensionFilter = (FileNameExtensionFilter) fileChooser.getFileFilter();
                boolean found = false;
                for (String extension : fileNameExtensionFilter.getExtensions()) {
                    if (path.toString().toLowerCase().endsWith(extension)) {
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
    }

    /**
     * Lets the user choose a file or directory
     *
     * @param parent parent component
     * @param title  dialog title
     * @return selected file or null if dialog was cancelled
     */
    public static Path openPath(Component parent, String title) {
        
        Path currentPath = Paths.get("");
        JFileChooser fileChooser = new JFileChooser(currentPath.toFile());
        fileChooser.setDialogTitle(title);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            Path path = fileChooser.getSelectedFile().toPath();
            
            return path;
        } else {
            return null;
        }
    }

    /**
     * Lets the user choose a file or directory
     *
     * @param parent parent component
     * @param title  dialog title
     * @return selected file or null if dialog was cancelled
     */
    public static Path savePath(Component parent, String title) {
        
        Path currentPath = Paths.get("");
        JFileChooser fileChooser = new JFileChooser(currentPath.toFile());
        fileChooser.setDialogTitle(title);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            Path path = fileChooser.getSelectedFile().toPath();
            
            return path;
        } else {
            return null;
        }
    }

    /**
     * Lets the user choose a directory
     *
     * @param parent parent component
     * @param title  dialog title
     * @return selected directory or null if dialog was cancelled
     */
    public static Path openDirectory(Component parent, String title) {
        
        Path currentPath = Paths.get("");
        JFileChooser fileChooser = new JFileChooser(currentPath.toFile());
        fileChooser.setDialogTitle(title);
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            Path path = fileChooser.getSelectedFile().toPath();
            
            return path;
        } else {
            return null;
        }
    }

    /**
     * Lets the user choose a directory
     *
     * @param parent parent component
     * @param title  dialog title
     * @return selected directory or null if dialog was cancelled
     */
    public static Path saveDirectory(Component parent, String title) {
        
        Path currentPath = Paths.get("");
        JFileChooser fileChooser = new JFileChooser(currentPath.toFile());
        fileChooser.setDialogTitle(title);
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            Path path = fileChooser.getSelectedFile().toPath();
            
            return path;
        } else {
            return null;
        }
    }

    /**
     * Lets the user choose multiple files
     *
     * @param parent parent component
     * @param title  dialog title
     * @return selected list of files. Is empty if dialog was cancelled.
     */
    public static List<Path> openFiles(Component parent, String title) {
        
        Path currentPath = Paths.get("");
        JFileChooser fileChooser = new JFileChooser(currentPath.toFile());
        fileChooser.setDialogTitle(title);
        fileChooser.setMultiSelectionEnabled(true);
        if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            return Arrays.stream(fileChooser.getSelectedFiles()).map(File::toPath).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Lets the user choose multiple directories
     *
     * @param parent parent component
     * @param title  dialog title
     * @return selected list of files. Is empty if dialog was cancelled.
     */
    public static List<Path> openDirectories(Component parent, String title) {
        
        Path currentPath = Paths.get("");
        JFileChooser fileChooser = new JFileChooser(currentPath.toFile());
        fileChooser.setDialogTitle(title);
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            return Arrays.stream(fileChooser.getSelectedFiles()).map(File::toPath).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Lets the user choose multiple files or directories
     *
     * @param parent parent component
     * @param title  dialog title
     * @return selected list of files. Is empty if dialog was cancelled.
     */
    public static List<Path> openPaths(Component parent, String title) {
        
        Path currentPath = Paths.get("");
        JFileChooser fileChooser = new JFileChooser(currentPath.toFile());
        fileChooser.setDialogTitle(title);
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            return Arrays.stream(fileChooser.getSelectedFiles()).map(File::toPath).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Generic open/save method for single paths
     *
     * @param parent   parent component
     * @param title    dialog title
     * @param ioMode   whether to load or save
     * @param pathMode which types of paths are returned
     * @return selected path of provided pathMode or null if dialog was cancelled
     */
    public static Path selectSingle(Component parent, String title, PathEditor.IOMode ioMode, PathEditor.PathMode pathMode) {
        Path selected;
        if (ioMode == PathEditor.IOMode.Open) {
            switch (pathMode) {
                case FilesOnly:
                    selected = FileChooserSettings.openFile(parent, title);
                    break;
                case DirectoriesOnly:
                    selected = FileChooserSettings.openDirectory(parent, title);
                    break;
                case FilesAndDirectories:
                    selected = FileChooserSettings.openPath(parent, title);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported: " + pathMode);
            }
        } else {
            switch (pathMode) {
                case FilesOnly:
                    selected = FileChooserSettings.saveFile(parent, title);
                    break;
                case DirectoriesOnly:
                    selected = FileChooserSettings.saveDirectory(parent, title);
                    break;
                case FilesAndDirectories:
                    selected = FileChooserSettings.savePath(parent, title);
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
     * @param parent   parent component
     * @param title    dialog title
     * @param ioMode   whether to load or save
     * @param pathMode which types of paths are returned
     * @return selected paths of provided pathMode or empty list if dialog was cancelled
     */
    public static List<Path> selectMulti(Component parent, String title, PathEditor.IOMode ioMode, PathEditor.PathMode pathMode) {
        List<Path> selected;
        if (ioMode == PathEditor.IOMode.Open) {
            switch (pathMode) {
                case FilesOnly:
                    selected = FileChooserSettings.openFiles(parent, title);
                    break;
                case DirectoriesOnly:
                    selected = FileChooserSettings.openDirectories(parent, title);
                    break;
                case FilesAndDirectories:
                    selected = FileChooserSettings.openPaths(parent, title);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported: " + pathMode);
            }
            return selected;
        } else {
            Path saveSelection;
            switch (pathMode) {
                case FilesOnly:
                    saveSelection = FileChooserSettings.saveFile(parent, title);
                    break;
                case DirectoriesOnly:
                    saveSelection = FileChooserSettings.saveDirectory(parent, title);
                    break;
                case FilesAndDirectories:
                    saveSelection = FileChooserSettings.savePath(parent, title);
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
}
