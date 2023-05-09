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

package org.hkijena.jipipe.extensions.filesystem.datasources;

import org.apache.commons.io.FileUtils;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.FilesystemExtensionSettings;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.extensions.parameters.api.collections.ListParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.filesystem.PathList;
import org.hkijena.jipipe.extensions.parameters.library.filesystem.PathParameterSettings;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Provides an input folder
 */
@JIPipeDocumentation(name = "Folder list", description = "Converts each provided path into folder data.")
@JIPipeOutputSlot(value = FolderData.class, slotName = "Folder paths", autoCreate = true)
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class FolderListDataSource extends JIPipeAlgorithm {

    private PathList folderPaths = new PathList();
    private Path currentWorkingDirectory;

    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public FolderListDataSource(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public FolderListDataSource(FolderListDataSource other) {
        super(other);
        this.folderPaths.addAll(other.folderPaths);
        this.currentWorkingDirectory = other.currentWorkingDirectory;
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {
        for (Path folderPath : folderPaths) {
            getFirstOutputSlot().addData(new FolderData(folderPath), progressInfo);
        }
    }

    /**
     * @return Gets the folder paths
     */
    @JIPipeParameter("folder-paths")
    @JIPipeDocumentation(name = "Folder paths")
    @PathParameterSettings(ioMode = PathIOMode.Open, pathMode = PathType.DirectoriesOnly)
    @ListParameterSettings(withScrollBar = true)
    public PathList getFolderPaths() {
        return folderPaths;
    }

    /**
     * Sets the folder path
     *
     * @param folderPaths Folder paths
     */
    @JIPipeParameter("folder-paths")
    public void setFolderPaths(PathList folderPaths) {
        this.folderPaths = folderPaths;
        PathUtils.normalizeList(folderPaths);
        FilesystemExtensionSettings settings = FilesystemExtensionSettings.getInstance();
        if (settings != null && settings.isAutoLabelOutputWithFileName()) {
            String name = folderPaths.size() == 1 ? folderPaths.get(0).getFileName().toString() : "";
            if (!Objects.equals(getFirstOutputSlot().getInfo().getCustomName(), name)) {
                getFirstOutputSlot().getInfo().setCustomName(name);
                getEventBus().post(new NodeSlotsChangedEvent(this));
            }
        }
    }

    /**
     * @return Folder paths as absolute paths
     */
    public PathList getAbsoluteFolderPaths() {
        PathList result = new PathList();
        for (Path folderPath : folderPaths) {
            if (folderPath == null) {
                result.add(null);
            } else if (currentWorkingDirectory != null && !folderPath.isAbsolute()) {
                result.add(currentWorkingDirectory.resolve(folderPath));
            } else {
                result.add(folderPath);
            }
        }
        return result;
    }

    /**
     * @return Relative paths (if available)
     */
    public PathList getRelativeFolderPaths() {
        PathList result = new PathList();
        for (Path folderPath : folderPaths) {
            if (folderPath == null)
                result.add(null);
            else if (currentWorkingDirectory != null && folderPath.isAbsolute() && folderPath.startsWith(currentWorkingDirectory)) {
                result.add(currentWorkingDirectory.relativize(folderPath));
            } else {
                result.add(folderPath);
            }
        }
        return result;
    }

    @Override
    public void archiveTo(JIPipeWriteDataStorage projectStorage, JIPipeWriteDataStorage wrappedExternalStorage, JIPipeProgressInfo progressInfo, Path originalBaseDirectory) {
        PathList relativeFileNames = getRelativeFolderPaths();
        PathList absoluteFileNames = getAbsoluteFolderPaths();
        PathList newPaths = new PathList();
        Set<String> externalFileNames = new HashSet<>();

        for (int i = 0; i < relativeFileNames.size(); i++) {
            Path source = absoluteFileNames.get(i);
            if (source == null || !Files.isDirectory(source)) {
                throw new RuntimeException("Directory " + relativeFileNames.get(i) + " does not exist!");
            } else {
                Path target;
                if (source.startsWith(originalBaseDirectory)) {
                    // The data is located in the project directory. We can directly copy the file.
                    Path relativePath = originalBaseDirectory.relativize(source);
                    target = projectStorage.getFileSystemPath().resolve(relativePath);
                } else {
                    // The data is located outside the project directory. Needs to be copied into a unique directory.
                    String externalFileName = relativeFileNames.get(i).getFileName().toString();
                    if (!externalFileNames.contains(externalFileName)) {
                        // Not yet in external storage. Add it
                        target = wrappedExternalStorage.resolve(getAliasIdInParentGraph()).getFileSystemPath().resolve(externalFileName);
                        externalFileNames.add(externalFileName);
                    } else {
                        // We need to make a new target dir (UUID)
                        progressInfo.log("Warning: Duplicate directory name in external storage (" + externalFileName + "). Creating new UUID sub-storage in " + getAliasIdInParentGraph());
                        target = wrappedExternalStorage.resolve(getAliasIdInParentGraph()).resolve(UUID.randomUUID().toString()).getFileSystemPath().resolve(externalFileName);
                        externalFileNames.add(externalFileName);
                    }
                }

                if (Files.exists(target)) {
                    progressInfo.log("Not copying " + source + " -> " + target + " (Already exists)");
                    continue;
                }

                progressInfo.log("Copy " + source + " -> " + target);
                try {
                    Files.createDirectories(target.getParent());
                    FileUtils.copyDirectory(source.toFile(), target.toFile());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                newPaths.add(target);
            }
        }
        setFolderPaths(newPaths);
    }

    @JIPipeDocumentation(name = "Paths to absolute", description = "Converts the stored paths to absolute paths.")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/path.png", iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/path.png")
    public void convertPathsToAbsolute() {
        setParameter("folder-paths", getAbsoluteFolderPaths());
    }

    @JIPipeDocumentation(name = "Paths to relative", description = "Converts the stored paths to paths relative to the project directory (if available).")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/path.png", iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/path.png")
    public void convertPathsToRelative() {
        setParameter("folder-paths", getRelativeFolderPaths());
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        for (Path folderPath : getAbsoluteFolderPaths()) {
            if (folderPath == null) {
                report.reportIsInvalid("Invalid folder path!",
                        "An input folder path does not exist!",
                        "Please provide a valid path.",
                        this);
            } else if (!Files.isDirectory(folderPath)) {
                report.reportIsInvalid("Invalid folder path!",
                        "Input folder '" + folderPath + "' does not exist!",
                        "Please provide a valid path.",
                        this);
            }
        }
    }

    @Override
    public void setBaseDirectory(Path baseDirectory) {
        super.setBaseDirectory(baseDirectory);

        boolean modified = false;
        for (int i = 0; i < folderPaths.size(); ++i) {
            Path folderPath = folderPaths.get(i);
            if (folderPath != null) {
                // Make absolute
                if (!folderPath.isAbsolute()) {
                    if (currentWorkingDirectory != null) {
                        folderPath = currentWorkingDirectory.resolve(folderPath);
                        modified = true;
                    } else if (baseDirectory != null) {
                        folderPath = baseDirectory.resolve(folderPath);
                        modified = true;
                    }
                }
                // Make relative if already absolute and workDirectory != null
                FilesystemExtensionSettings settings = FilesystemExtensionSettings.getInstance();
                if (settings == null || settings.isRelativizePaths()) {
                    if (folderPath.isAbsolute()) {
                        if (baseDirectory != null && folderPath.startsWith(baseDirectory)) {
                            folderPath = baseDirectory.relativize(folderPath);
                            modified = true;
                        }
                    }
                }

                if (modified)
                    this.folderPaths.set(i, folderPath);
            }
        }
        currentWorkingDirectory = baseDirectory;
    }
}
