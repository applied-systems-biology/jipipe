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

package org.hkijena.jipipe.extensions.settings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.filesystem.FileChooserBookmarkList;
import org.hkijena.jipipe.ui.components.AdvancedFileChooser;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
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
public class FileChooserSettings extends AbstractJIPipeParameterCollection {

    public static String ID = "file-chooser";
    private FileChooserType fileChooserType = FileChooserType.Advanced;
    private Path lastProjectsDirectory;
    private Path lastParametersDirectory;
    private Path lastDataDirectory;
    private Path lastExternalDirectory;
    private boolean addFileExtension = true;

    private FileChooserBookmarkList bookmarks = new FileChooserBookmarkList();

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
     * @param key              location where the dialog is opened
     * @param title            dialog title
     * @param extensionFilters optional extension filters. the first one is chosen automatically
     * @return selected file or null if dialog was cancelled
     */
    public static Path openFile(Component parent, LastDirectoryKey key, String title, FileNameExtensionFilter... extensionFilters) {
        FileChooserSettings instance = getInstance();
        Path currentPath = instance.getLastDirectoryBy(key);
        if (instance.fileChooserType == FileChooserType.Native) {
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
        } else if (instance.fileChooserType == FileChooserType.Standard) {
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
        } else {
            AdvancedFileChooser fileChooser = new AdvancedFileChooser(currentPath.toFile());
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
        }
    }

    /**
     * Lets the user choose a file
     *
     * @param parent           parent component
     * @param key              location where the dialog is opened
     * @param title            dialog title
     * @param extensionFilters extension filters. the first one is chosen automatically
     * @return selected file or null if dialog was cancelled
     */
    public static Path saveFile(Component parent, LastDirectoryKey key, String title, FileNameExtensionFilter... extensionFilters) {
        FileChooserSettings instance = getInstance();
        Path currentPath = instance.getLastDirectoryBy(key);
        if (instance.fileChooserType == FileChooserType.Native) {
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
                        extensionFilters.length > 0) {
                    boolean found = false;
                    outer:
                    for (FileNameExtensionFilter extensionFilter : extensionFilters) {
                        for (String extension : extensionFilter.getExtensions()) {
                            if (path.toString().toLowerCase().endsWith(extension)) {
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
        } else if (instance.fileChooserType == FileChooserType.Standard) {
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
                if (getInstance().isAddFileExtension() && extensionFilters.length > 0 && fileChooser.getFileFilter() instanceof FileNameExtensionFilter) {
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
        } else {
            AdvancedFileChooser fileChooser = new AdvancedFileChooser(currentPath.toFile());
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
                if (getInstance().isAddFileExtension() && extensionFilters.length > 0 && fileChooser.getFileFilter() instanceof FileNameExtensionFilter) {
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
    }

    /**
     * Lets the user choose a file or directory
     *
     * @param parent parent component
     * @param key    location where the dialog is opened
     * @param title  dialog title
     * @return selected file or null if dialog was cancelled
     */
    public static Path openPath(Component parent, LastDirectoryKey key, String title) {
        FileChooserSettings instance = getInstance();
        Path currentPath = instance.getLastDirectoryBy(key);
        if (instance.getFileChooserType() == FileChooserType.Standard) {
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
        } else {
            AdvancedFileChooser fileChooser = new AdvancedFileChooser(currentPath.toFile());
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
    }

    /**
     * Lets the user choose a file or directory
     *
     * @param parent parent component
     * @param key    location where the dialog is opened
     * @param title  dialog title
     * @return selected file or null if dialog was cancelled
     */
    public static Path savePath(Component parent, LastDirectoryKey key, String title) {
        FileChooserSettings instance = getInstance();
        Path currentPath = instance.getLastDirectoryBy(key);
        if (instance.getFileChooserType() == FileChooserType.Standard) {
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
        } else {
            AdvancedFileChooser fileChooser = new AdvancedFileChooser(currentPath.toFile());
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
    }

    /**
     * Lets the user choose a directory
     *
     * @param parent parent component
     * @param key    location where the dialog is opened
     * @param title  dialog title
     * @return selected directory or null if dialog was cancelled
     */
    public static Path openDirectory(Component parent, LastDirectoryKey key, String title) {
        FileChooserSettings instance = getInstance();
        Path currentPath = instance.getLastDirectoryBy(key);
        if (instance.getFileChooserType() == FileChooserType.Standard) {
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
        } else {
            AdvancedFileChooser fileChooser = new AdvancedFileChooser(currentPath.toFile());
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
    }

    /**
     * Lets the user choose a directory
     *
     * @param parent parent component
     * @param key    location where the dialog is opened
     * @param title  dialog title
     * @return selected directory or null if dialog was cancelled
     */
    public static Path saveDirectory(Component parent, LastDirectoryKey key, String title) {
        FileChooserSettings instance = getInstance();
        Path currentPath = instance.getLastDirectoryBy(key);
        if (instance.getFileChooserType() == FileChooserType.Standard) {
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
        } else {
            AdvancedFileChooser fileChooser = new AdvancedFileChooser(currentPath.toFile());
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
    }

    /**
     * Lets the user choose multiple files
     *
     * @param parent parent component
     * @param key    location where the dialog is opened
     * @param title  dialog title
     * @return selected list of files. Is empty if dialog was cancelled.
     */
    public static List<Path> openFiles(Component parent, LastDirectoryKey key, String title) {
        FileChooserSettings instance = getInstance();
        Path currentPath = instance.getLastDirectoryBy(key);
        if (instance.getFileChooserType() == FileChooserType.Native) {
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
        } else if (instance.getFileChooserType() == FileChooserType.Standard) {
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
        } else {
            AdvancedFileChooser fileChooser = new AdvancedFileChooser(currentPath.toFile());
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
    public static List<Path> openDirectories(Component parent, LastDirectoryKey key, String title) {
        FileChooserSettings instance = getInstance();
        Path currentPath = instance.getLastDirectoryBy(key);
        if (instance.getFileChooserType() == FileChooserType.Standard) {
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
        } else {
            AdvancedFileChooser fileChooser = new AdvancedFileChooser(currentPath.toFile());
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
    }

    /**
     * Lets the user choose multiple files or directories
     *
     * @param parent parent component
     * @param key    location where the dialog is opened
     * @param title  dialog title
     * @return selected list of files. Is empty if dialog was cancelled.
     */
    public static List<Path> openPaths(Component parent, LastDirectoryKey key, String title) {
        FileChooserSettings instance = getInstance();
        Path currentPath = instance.getLastDirectoryBy(key);
        if (instance.getFileChooserType() == FileChooserType.Standard) {
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
        } else {
            AdvancedFileChooser fileChooser = new AdvancedFileChooser(currentPath.toFile());
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
    }

    /**
     * Generic open/save method for single paths
     *
     * @param parent           parent component
     * @param key              location where the dialog is opened
     * @param title            dialog title
     * @param ioMode           whether to load or save
     * @param pathMode         which types of paths are returned
     * @param extensionFilters passed if a file is opened/saved
     * @return selected path of provided pathMode or null if dialog was cancelled
     */
    public static Path selectSingle(Component parent, LastDirectoryKey key, String title, PathIOMode ioMode, PathType pathMode, FileNameExtensionFilter... extensionFilters) {
        Path selected;
        if (ioMode == PathIOMode.Open) {
            switch (pathMode) {
                case FilesOnly:
                    selected = FileChooserSettings.openFile(parent, key, title, extensionFilters);
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
                    selected = FileChooserSettings.saveFile(parent, key, title, extensionFilters);
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
    public static List<Path> selectMulti(Component parent, LastDirectoryKey key, String title, PathIOMode ioMode, PathType pathMode) {
        List<Path> selected;
        if (ioMode == PathIOMode.Open) {
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
                    saveSelection = FileChooserSettings.saveFile(parent, key, title);
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
        return JIPipe.getSettings().getSettings(ID, FileChooserSettings.class);
    }

    @JIPipeDocumentation(name = "Bookmarks", description = "Bookmarks used by the advanced file dialog")
    @JIPipeParameter("bookmarks-v2")
    public FileChooserBookmarkList getBookmarks() {
        return bookmarks;
    }

    @JIPipeParameter("bookmarks-v2")
    public void setBookmarks(FileChooserBookmarkList bookmarks) {
        this.bookmarks = bookmarks;
    }

    @JIPipeDocumentation(name = "File dialog design", description = "Determines which file dialog type is used within JIPipe. " +
            "<ul>" +
            "<li>Advanced: A dialog that extends Java's standard dialog by modern features.</li>" +
            "<li>Standard: The standard platform-independent file dialog provided by Java.</li>" +
            "<li>Native: Use the operating system's native dialog (GTK on Linux). Can cause issues depending on the operating system.</li>" +
            "</ul>")
    @JIPipeParameter("file-chooser-type")
    public FileChooserType getFileChooserType() {
        return fileChooserType;
    }

    @JIPipeParameter("file-chooser-type")
    public void setFileChooserType(FileChooserType fileChooserType) {
        this.fileChooserType = fileChooserType;
    }

    /**
     * Gets the last directory by key
     *
     * @param key the key
     * @return the last path or Paths.get() (home directory)
     */
    public Path getLastDirectoryBy(LastDirectoryKey key) {
        switch (key) {
            case Data:
                return getLastDataDirectory();
            case External:
                return getLastExternalDirectory();
            case Projects:
                return getLastProjectsDirectory();
            default:
                return getLastParametersDirectory();
        }
    }

    /**
     * Sets the last directory according to the key
     *
     * @param key           the key
     * @param lastDirectory directory or file
     */
    public void setLastDirectoryBy(LastDirectoryKey key, Path lastDirectory) {
        if (Files.isRegularFile(lastDirectory))
            lastDirectory = lastDirectory.getParent();
        switch (key) {
            case Projects:
                setLastProjectsDirectory(lastDirectory);
                break;
            case External:
                setLastExternalDirectory(lastDirectory);
                break;
            case Data:
                setLastDataDirectory(lastDirectory);
                break;
            default:
                setLastParametersDirectory(lastDirectory);
                break;
        }
        JIPipe.getSettings().save();
    }

    @JIPipeDocumentation(name = "Last external directory", description = "The file chooser will open in this folder when selecting external utilities.")
    @JIPipeParameter("last-external-directory")
    public Path getLastExternalDirectory() {
        if (lastExternalDirectory == null)
            lastExternalDirectory = Paths.get("").toAbsolutePath();
        return lastExternalDirectory;
    }

    @JIPipeParameter("last-external-directory")
    public void setLastExternalDirectory(Path lastExternalDirectory) {
        this.lastExternalDirectory = lastExternalDirectory;
    }

    @JIPipeDocumentation(name = "Last projects directory", description = "The file chooser will open in this folder when opening a project.")
    @JIPipeParameter("last-projects-directory")
    public Path getLastProjectsDirectory() {
        if (lastProjectsDirectory == null)
            lastProjectsDirectory = Paths.get("").toAbsolutePath();
        return lastProjectsDirectory;
    }

    @JIPipeParameter("last-projects-directory")
    public void setLastProjectsDirectory(Path lastProjectsDirectory) {
        if (!Files.isDirectory(lastProjectsDirectory)) {
            lastProjectsDirectory = lastProjectsDirectory.getParent();
        }
        this.lastProjectsDirectory = lastProjectsDirectory;

    }

    @JIPipeDocumentation(name = "Last directory", description = "The file chooser will open in this folder when changing a parameter.")
    @JIPipeParameter("last-parameters-directory")
    public Path getLastParametersDirectory() {
        if (lastParametersDirectory == null)
            lastParametersDirectory = Paths.get("").toAbsolutePath();
        return lastParametersDirectory;
    }

    @JIPipeParameter("last-parameters-directory")
    public void setLastParametersDirectory(Path lastParametersDirectory) {
        if (!Files.isDirectory(lastParametersDirectory)) {
            lastParametersDirectory = lastParametersDirectory.getParent();
        }
        this.lastParametersDirectory = lastParametersDirectory;

    }

    @JIPipeDocumentation(name = "Last data directory", description = "The file chooser will open in this folder when asking for data.")
    @JIPipeParameter("last-data-directory")
    public Path getLastDataDirectory() {
        if (lastDataDirectory == null)
            lastDataDirectory = Paths.get("").toAbsolutePath();
        return lastDataDirectory;
    }

    @JIPipeParameter("last-data-directory")
    public void setLastDataDirectory(Path lastDataDirectory) {
        if (!Files.isDirectory(lastDataDirectory)) {
            lastDataDirectory = lastDataDirectory.getParent();
        }
        this.lastDataDirectory = lastDataDirectory;

    }

    @JIPipeDocumentation(name = "Automatically add file extensions", description = "If enabled, appropriate file extensions are automatically added (e.g. .json for projects) on saving a file if they are not present.")
    @JIPipeParameter("add-file-extension")
    public boolean isAddFileExtension() {
        return addFileExtension;
    }

    @JIPipeParameter("add-file-extension")
    public void setAddFileExtension(boolean addFileExtension) {
        this.addFileExtension = addFileExtension;
    }

    public enum FileChooserType {
        Advanced,
        Standard,
        Native
    }

    public enum LastDirectoryKey {
        Projects,
        Data,
        Parameters,
        External
    }

}
