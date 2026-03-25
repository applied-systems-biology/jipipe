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

package org.hkijena.jipipe.plugins.settings.application;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.plugins.parameters.library.filesystem.FileChooserBookmarkList;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Settings concerning file dialogs
 */
public class JIPipeFileChooserApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {

    public static String ID = "org.hkijena.jipipe:file-chooser";
    private FileChooserType fileChooserType = FileChooserType.ModernNative;
    private FileChooserType fallbackFileChooserType = FileChooserType.Advanced;
    private boolean askOnNativePathSelect = true;
    private Path lastProjectsDirectory;
    private Path lastParametersDirectory;
    private Path lastDataDirectory;
    private Path lastExternalDirectory;
    private boolean addFileExtension = true;
    private boolean useLegacyDataExportPathChooser = false;

    private FileChooserBookmarkList bookmarks = new FileChooserBookmarkList();

    public static JIPipeFileChooserApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, JIPipeFileChooserApplicationSettings.class);
    }

    public static String getID() {
        return ID;
    }

    public static void setID(String ID) {
        JIPipeFileChooserApplicationSettings.ID = ID;
    }

    @SetJIPipeDocumentation(name = "Use legacy data exporter path chooser", description = "If enabled, use the older JIPipe 5.x data exporter path chooser")
    @JIPipeParameter("use-legacy-data-export-path-chooser")
    public boolean isUseLegacyDataExportPathChooser() {
        return useLegacyDataExportPathChooser;
    }

    @JIPipeParameter("use-legacy-data-export-path-chooser")
    public void setUseLegacyDataExportPathChooser(boolean useLegacyDataExportPathChooser) {
        this.useLegacyDataExportPathChooser = useLegacyDataExportPathChooser;
    }

    @SetJIPipeDocumentation(name = "OS dialogs: ask on path select", description = "If the 'Prefer OS dialogs' option is selected and a path (i.e. file or directory) is opened/saved," +
            " ask about the path type instead of falling back to the non-native file dialog.")
    @JIPipeParameter("ask-on-native-path-select")
    public boolean isAskOnNativePathSelect() {
        return askOnNativePathSelect;
    }

    @JIPipeParameter("ask-on-native-path-select")
    public void setAskOnNativePathSelect(boolean askOnNativePathSelect) {
        this.askOnNativePathSelect = askOnNativePathSelect;
    }

    @SetJIPipeDocumentation(name = "Bookmarks", description = "Bookmarks used by the advanced file dialog")
    @JIPipeParameter("bookmarks-v2")
    public FileChooserBookmarkList getBookmarks() {
        return bookmarks;
    }

    @JIPipeParameter("bookmarks-v2")
    public void setBookmarks(FileChooserBookmarkList bookmarks) {
        this.bookmarks = bookmarks;
    }

    @SetJIPipeDocumentation(name = "File dialog design", description = "Determines which file dialog type is used within JIPipe. " +
            "<ul>" +
            "<li>ModernNative: Attempts to use native Windows/macOS/Linux dialogs. Otherwise falls back to the fallback chooser.</li>" +
            "<li>Advanced: A dialog that extends Java's standard dialog by modern features.</li>" +
            "<li>Standard: The standard platform-independent file dialog provided by Java.</li>" +
            "<li>Native: Use the operating system's native dialog (GTK on Linux). Can cause issues depending on the operating system.</li>" +
            "</ul>")
    @JIPipeParameter("file-chooser-type-v2")
    public FileChooserType getFileChooserType() {
        return fileChooserType;
    }

    @JIPipeParameter("file-chooser-type-v2")
    public void setFileChooserType(FileChooserType fileChooserType) {
        this.fileChooserType = fileChooserType;
    }

    /**
     * Gets the last directory by key
     *
     * @param workbench the workbench (can be null)
     * @param key       the key
     * @return the last path or Paths.get() (home directory)
     */
    public Path getLastDirectoryBy(JIPipeWorkbench workbench, LastDirectoryKey key) {
        Path result = switch (key) {
            case Data -> getLastDataDirectory();
            case External -> getLastExternalDirectory();
            case Projects -> getLastProjectsDirectory();
            default -> getLastParametersDirectory();
        };
        if (workbench != null && workbench.getProject() != null && workbench.getProject().getWorkDirectory() != null && result.equals(Paths.get("").toAbsolutePath())) {
            result = workbench.getProject().getWorkDirectory();
        }
        if (result.toString().isEmpty() || result.equals(Paths.get("").toAbsolutePath())) {
            // Go to user.home
            result = PathUtils.getHomeDirectory();
        }
        if (Files.exists(result) && !Files.isDirectory(result)) {
            try {
                result = Paths.get(result.toString()).getParent();
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    /**
     * Sets the last directory according to the key
     *
     * @param key           the key
     * @param lastDirectory directory or file
     */
    public void setLastDirectoryBy(LastDirectoryKey key, Path lastDirectory) {
        if (Files.exists(lastDirectory) && !Files.isDirectory(lastDirectory)) {
            try {
                lastDirectory = Paths.get(lastDirectory.toString()).getParent();
            } catch (Exception ignored) {
            }
        }
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
        JIPipe.autoSaveSettings();
    }

    @SetJIPipeDocumentation(name = "Last external directory", description = "The file chooser will open in this folder when selecting external utilities.")
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

    @SetJIPipeDocumentation(name = "Last projects directory", description = "The file chooser will open in this folder when opening a project.")
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

    @SetJIPipeDocumentation(name = "Last directory", description = "The file chooser will open in this folder when changing a parameter.")
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

    @SetJIPipeDocumentation(name = "Last data directory", description = "The file chooser will open in this folder when asking for data.")
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

    @SetJIPipeDocumentation(name = "Automatically add file extensions", description = "If enabled, appropriate file extensions are automatically added (e.g. .json for projects) on saving a file if they are not present.")
    @JIPipeParameter("add-file-extension")
    public boolean isAddFileExtension() {
        return addFileExtension;
    }

    @JIPipeParameter("add-file-extension")
    public void setAddFileExtension(boolean addFileExtension) {
        this.addFileExtension = addFileExtension;
    }

    @Override
    public JIPipeDefaultApplicationSettingsSheetCategory getDefaultCategory() {
        return JIPipeDefaultApplicationSettingsSheetCategory.UI;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("actions/quickopen-file.png");
    }

    @Override
    public String getName() {
        return "File chooser";
    }

    @Override
    public String getDescription() {
        return "Allows to change the open/save dialogs";
    }

    @SetJIPipeDocumentation(name = "File dialog design (fallback)", description = "Determines which file dialog type is used within JIPipe if the first choice fails (only ModernNative). " +
            "<ul>" +
            "<li>ModernNative: Not supported and will act like 'Advanced'</li>" +
            "<li>Advanced: A dialog that extends Java's standard dialog by modern features.</li>" +
            "<li>Standard: The standard platform-independent file dialog provided by Java.</li>" +
            "<li>Native: Use the operating system's native dialog (GTK on Linux). Can cause issues depending on the operating system.</li>" +
            "</ul>")
    @JIPipeParameter("file-chooser-type-fallback")
    public FileChooserType getFallbackFileChooserType() {
        return fallbackFileChooserType;
    }

    @JIPipeParameter("file-chooser-type-fallback")
    public void setFallbackFileChooserType(FileChooserType fallbackFileChooserType) {
        this.fallbackFileChooserType = fallbackFileChooserType;
    }

    public FileChooserType getSafeFallbackFileChooserType() {
        return fallbackFileChooserType != FileChooserType.ModernNative ? fallbackFileChooserType : FileChooserType.Advanced;
    }

    public enum FileChooserType {
        ModernNative("Prefer OS dialogs (recommended)"),
        Advanced("JIPipe"),
        AdvancedLegacy("JIPipe (legacy)"),
        Standard("Java Swing"),
        Native("Java Native");

        private final String name;

        FileChooserType(String name) {
            this.name = name.replace("OS", StringUtils.getOSName());
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum LastDirectoryKey {
        Projects,
        Data,
        Parameters,
        External
    }

}
