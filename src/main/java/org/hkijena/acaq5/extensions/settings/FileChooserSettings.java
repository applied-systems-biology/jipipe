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

package org.hkijena.acaq5.extensions.settings;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.api.registries.ACAQSettingsRegistry;
import org.hkijena.acaq5.ui.components.PathEditor;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Settings concerning file dialogs
 */
public class FileChooserSettings implements ACAQParameterCollection {

    public static String ID = "file-chooser";

    /**
     * Path key for project locations
     */
    public static String KEY_PROJECT = "Projects";

    /**
     * Path key for any parameter
     */
    public static String KEY_PARAMETER = "Parameters";

    private EventBus eventBus = new EventBus();
    private boolean useNativeChooser = false;
    private Path lastProjectsDirectory;
    private Path lastParametersDirectory;
    private boolean addFileExtension = true;

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @ACAQDocumentation(name = "Use native file chooser", description = "If enabled, ACAQ5 will use the system-specific file chooser instead of the one provided by Java. " +
            "If you have any issues, disable this setting. Please note that there is no native dialog available for all selectable path types. " +
            "In such cases, ACAQ5 will fall back to the Java file dialogs.")
    @ACAQParameter("use-native-chooser")
    public boolean isUseNativeChooser() {
        return useNativeChooser;
    }

    @ACAQParameter("use-native-chooser")
    public void setUseNativeChooser(boolean useNativeChooser) {
        this.useNativeChooser = useNativeChooser;
    }

    /**
     * Gets the last directory by key
     *
     * @param key the key
     * @return the last path or Paths.get() (home directory)
     */
    public Path getLastDirectoryBy(String key) {
        if (KEY_PROJECT.equals(key))
            return getLastProjectsDirectory();
        else
            return getLastParametersDirectory();
    }

    /**
     * Sets the last directory according to the key
     *
     * @param key           the key
     * @param lastDirectory directory or file
     */
    public void setLastDirectoryBy(String key, Path lastDirectory) {
        if (Files.isRegularFile(lastDirectory))
            lastDirectory = lastDirectory.getParent();
        if (KEY_PROJECT.equals(key))
            setLastProjectsDirectory(lastDirectory);
        else
            setLastParametersDirectory(lastDirectory);
    }

    @ACAQDocumentation(name = "Last projects directory", description = "The file chooser will open in this folder when opening a project.")
    @ACAQParameter("last-projects-directory")
    public Path getLastProjectsDirectory() {
        if (lastProjectsDirectory == null)
            lastProjectsDirectory = Paths.get("").toAbsolutePath();
        return lastProjectsDirectory;
    }

    @ACAQParameter("last-projects-directory")
    public void setLastProjectsDirectory(Path lastProjectsDirectory) {
        this.lastProjectsDirectory = lastProjectsDirectory;

    }

    @ACAQDocumentation(name = "Last directory", description = "The file chooser will open in this folder when changing a parameter.")
    @ACAQParameter("last-parameters-directory")
    public Path getLastParametersDirectory() {
        if (lastParametersDirectory == null)
            lastParametersDirectory = Paths.get("").toAbsolutePath();
        return lastParametersDirectory;
    }

    @ACAQParameter("last-parameters-directory")
    public void setLastParametersDirectory(Path lastParametersDirectory) {
        this.lastParametersDirectory = lastParametersDirectory;

    }

    @ACAQDocumentation(name = "Automatically add file extensions", description = "If enabled, appropriate file extensions are automatically added (e.g. .json for projects) on saving a file if they are not present.")
    @ACAQParameter("add-file-extension")
    public boolean isAddFileExtension() {
        return addFileExtension;
    }

    @ACAQParameter("add-file-extension")
    public void setAddFileExtension(boolean addFileExtension) {
        this.addFileExtension = addFileExtension;
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

    /**
     * Lets the user choose a file
     *
     * @param parent parent component
     * @param key    location where the dialog is opened
     * @param title  dialog title
     * @return selected file or null if dialog was cancelled
     */
    public static Path openFile(Component parent, String key, String title) {
        FileChooserSettings instance = getInstance();
        Path currentPath = instance.getLastDirectoryBy(key);
        if (instance.useNativeChooser) {
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
        } else {
            JFileChooser fileChooser = new JFileChooser(currentPath.toFile());
            fileChooser.setDialogTitle(title);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                Path path = fileChooser.getSelectedFile().toPath();
                instance.setLastDirectoryBy(key, path.getParent());
                return path;
            } else {
                return null;
            }
        }
    }

    /**
     * Lets the user choose a file
     *
     * @param parent    parent component
     * @param key       location where the dialog is opened
     * @param title     dialog title
     * @param extension The extension to add. Includes a period. Example: ".json"
     * @return selected file or null if dialog was cancelled
     */
    public static Path saveFile(Component parent, String key, String title, String extension) {
        FileChooserSettings instance = getInstance();
        Path currentPath = instance.getLastDirectoryBy(key);
        if (instance.useNativeChooser) {
            FileDialog dialog = createFileDialog(parent, title, FileDialog.SAVE);
            dialog.setTitle(title);
            dialog.setDirectory(currentPath.toString());
            dialog.setMultipleMode(false);
            dialog.setVisible(true);
            String fileName = dialog.getFile();
            if (fileName != null) {
                Path path = Paths.get(fileName);
                instance.setLastDirectoryBy(key, path.getParent());
                if (getInstance().isAddFileExtension() &&
                        extension != null &&
                        !path.toString().toLowerCase().endsWith(extension.toLowerCase())) {
                    path = path.getParent().resolve(path.getFileName() + extension);
                }
                return path;
            } else {
                return null;
            }
        } else {
            JFileChooser fileChooser = new JFileChooser(currentPath.toFile());
            fileChooser.setDialogTitle(title);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
                Path path = fileChooser.getSelectedFile().toPath();
                instance.setLastDirectoryBy(key, path.getParent());
                if (getInstance().isAddFileExtension() &&
                        extension != null &&
                        !path.toString().toLowerCase().endsWith(extension.toLowerCase())) {
                    path = path.getParent().resolve(path.getFileName() + extension);
                }
                return path;
            } else {
                return null;
            }
        }
    }

    /**
     * Lets the user choose a file or directory
     *
     * @param parent parent component
     * @param key    location where the dialog is opened
     * @param title  dialog title
     * @return selected file or null if dialog was cancelled
     */
    public static Path openPath(Component parent, String key, String title) {
        FileChooserSettings instance = getInstance();
        Path currentPath = instance.getLastDirectoryBy(key);
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
    }

    /**
     * Lets the user choose a file or directory
     *
     * @param parent parent component
     * @param key    location where the dialog is opened
     * @param title  dialog title
     * @return selected file or null if dialog was cancelled
     */
    public static Path savePath(Component parent, String key, String title) {
        FileChooserSettings instance = getInstance();
        Path currentPath = instance.getLastDirectoryBy(key);
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
    }

    /**
     * Lets the user choose a directory
     *
     * @param parent parent component
     * @param key    location where the dialog is opened
     * @param title  dialog title
     * @return selected directory or null if dialog was cancelled
     */
    public static Path openDirectory(Component parent, String key, String title) {
        FileChooserSettings instance = getInstance();
        Path currentPath = instance.getLastDirectoryBy(key);
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
    }

    /**
     * Lets the user choose a directory
     *
     * @param parent parent component
     * @param key    location where the dialog is opened
     * @param title  dialog title
     * @return selected directory or null if dialog was cancelled
     */
    public static Path saveDirectory(Component parent, String key, String title) {
        FileChooserSettings instance = getInstance();
        Path currentPath = instance.getLastDirectoryBy(key);
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
    }

    /**
     * Lets the user choose multiple files
     *
     * @param parent parent component
     * @param key    location where the dialog is opened
     * @param title  dialog title
     * @return selected list of files. Is empty if dialog was cancelled.
     */
    public static List<Path> openFiles(Component parent, String key, String title) {
        FileChooserSettings instance = getInstance();
        Path currentPath = instance.getLastDirectoryBy(key);
        if (instance.useNativeChooser) {
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
        } else {
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
        }
    }

    /**
     * Lets the user choose multiple directories
     *
     * @param parent parent component
     * @param key    location where the dialog is opened
     * @param title  dialog title
     * @return selected list of files. Is empty if dialog was cancelled.
     */
    public static List<Path> openDirectories(Component parent, String key, String title) {
        FileChooserSettings instance = getInstance();
        Path currentPath = instance.getLastDirectoryBy(key);
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
    }

    /**
     * Lets the user choose multiple files or directories
     *
     * @param parent parent component
     * @param key    location where the dialog is opened
     * @param title  dialog title
     * @return selected list of files. Is empty if dialog was cancelled.
     */
    public static List<Path> openPaths(Component parent, String key, String title) {
        FileChooserSettings instance = getInstance();
        Path currentPath = instance.getLastDirectoryBy(key);
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
    }

    /**
     * Generic open/save method for single paths
     *
     * @param parent   parent component
     * @param key      location where the dialog is opened
     * @param title    dialog title
     * @param ioMode   whether to load or save
     * @param pathMode which types of paths are returned
     * @return selected path of provided pathMode or null if dialog was cancelled
     */
    public static Path selectSingle(Component parent, String key, String title, PathEditor.IOMode ioMode, PathEditor.PathMode pathMode) {
        Path selected;
        if (ioMode == PathEditor.IOMode.Open) {
            switch (pathMode) {
                case FilesOnly:
                    selected = FileChooserSettings.openFile(parent, key, title);
                    break;
                case DirectoriesOnly:
                    selected = FileChooserSettings.openDirectory(parent, key, title);
                    break;
                case FilesAndDirectories:
                    selected = FileChooserSettings.openPath(parent, key, title);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported: " + pathMode);
            }
        } else {
            switch (pathMode) {
                case FilesOnly:
                    selected = FileChooserSettings.saveFile(parent, key, title, null);
                    break;
                case DirectoriesOnly:
                    selected = FileChooserSettings.saveDirectory(parent, key, title);
                    break;
                case FilesAndDirectories:
                    selected = FileChooserSettings.savePath(parent, key, title);
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
     * @param key      location where the dialog is opened
     * @param title    dialog title
     * @param ioMode   whether to load or save
     * @param pathMode which types of paths are returned
     * @return selected paths of provided pathMode or empty list if dialog was cancelled
     */
    public static List<Path> selectMulti(Component parent, String key, String title, PathEditor.IOMode ioMode, PathEditor.PathMode pathMode) {
        List<Path> selected;
        if (ioMode == PathEditor.IOMode.Open) {
            switch (pathMode) {
                case FilesOnly:
                    selected = FileChooserSettings.openFiles(parent, key, title);
                    break;
                case DirectoriesOnly:
                    selected = FileChooserSettings.openDirectories(parent, key, title);
                    break;
                case FilesAndDirectories:
                    selected = FileChooserSettings.openPaths(parent, key, title);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported: " + pathMode);
            }
            return selected;
        } else {
            Path saveSelection;
            switch (pathMode) {
                case FilesOnly:
                    saveSelection = FileChooserSettings.saveFile(parent, key, title, null);
                    break;
                case DirectoriesOnly:
                    saveSelection = FileChooserSettings.saveDirectory(parent, key, title);
                    break;
                case FilesAndDirectories:
                    saveSelection = FileChooserSettings.savePath(parent, key, title);
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

    public static FileChooserSettings getInstance() {
        return ACAQSettingsRegistry.getInstance().getSettings(ID, FileChooserSettings.class);
    }

}
