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
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.context.JIPipeDataContext;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.extensions.filesystem.FilesystemExtensionSettings;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
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
@SetJIPipeDocumentation(name = "Path list", description = "Converts each provided path into path data.")
@AddJIPipeOutputSlot(value = PathData.class, slotName = "Paths", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class PathListDataSource extends JIPipeAlgorithm {

    private PathList paths = new PathList();
    private Path currentWorkingDirectory;

    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public PathListDataSource(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public PathListDataSource(PathListDataSource other) {
        super(other);
        this.paths.addAll(other.paths);
        this.currentWorkingDirectory = other.currentWorkingDirectory;
    }

    @Override
    public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        for (Path folderPath : paths) {
            getFirstOutputSlot().addData(new PathData(folderPath), JIPipeDataContext.create(this), progressInfo);
        }
    }

    /**
     * @return Gets the folder paths
     */
    @JIPipeParameter("paths")
    @SetJIPipeDocumentation(name = "Paths")
    @PathParameterSettings(ioMode = PathIOMode.Open, pathMode = PathType.FilesAndDirectories)
    @ListParameterSettings(withScrollBar = true)
    public PathList getPaths() {
        return paths;
    }

    /**
     * Sets the folder path
     *
     * @param paths Folder paths
     */
    @JIPipeParameter("paths")
    public void setPaths(PathList paths) {
        this.paths = paths;
        PathUtils.normalizeList(paths);
        FilesystemExtensionSettings settings = FilesystemExtensionSettings.getInstance();
        if (settings != null && settings.isAutoLabelOutputWithFileName()) {
            String name = paths.size() == 1 ? paths.get(0).getFileName().toString() : "";
            if (!Objects.equals(getFirstOutputSlot().getInfo().getCustomName(), name)) {
                getFirstOutputSlot().getInfo().setCustomName(name);
                getNodeSlotsChangedEventEmitter().emit(new NodeSlotsChangedEvent(this));
            }
        }
    }

    /**
     * @return Folder paths as absolute paths
     */
    public PathList getAbsolutePaths() {
        PathList result = new PathList();
        for (Path folderPath : paths) {
            if (folderPath == null)
                result.add(null);
            else if (currentWorkingDirectory != null && !folderPath.isAbsolute())
                result.add(currentWorkingDirectory.resolve(folderPath));
            else
                result.add(folderPath);
        }
        return result;
    }

    /**
     * @return Relative paths (if available)
     */
    public PathList getRelativePaths() {
        PathList result = new PathList();
        for (Path path : paths) {
            if (path == null)
                result.add(null);
            else if (currentWorkingDirectory != null && path.isAbsolute() && path.startsWith(currentWorkingDirectory)) {
                result.add(currentWorkingDirectory.relativize(path));
            } else {
                result.add(path);
            }
        }
        return result;
    }

    @Override
    public void archiveTo(JIPipeWriteDataStorage projectStorage, JIPipeWriteDataStorage wrappedExternalStorage, JIPipeProgressInfo progressInfo, Path originalBaseDirectory) {
        PathList relativeFileNames = getRelativePaths();
        PathList absoluteFileNames = getAbsolutePaths();
        PathList newPaths = new PathList();
        Set<String> externalFileNames = new HashSet<>();

        for (int i = 0; i < relativeFileNames.size(); i++) {
            Path source = absoluteFileNames.get(i);
            if (source == null || !Files.exists(source)) {
                throw new RuntimeException("Path " + relativeFileNames.get(i) + " does not exist!");
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
                        progressInfo.log("Warning: Duplicate path name in external storage (" + externalFileName + "). Creating new UUID sub-storage in " + getAliasIdInParentGraph());
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
                    if (Files.isRegularFile(source)) {
                        Files.copy(source, target);
                    } else {
                        FileUtils.copyDirectory(source.toFile(), target.toFile());
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                newPaths.add(target);
            }
        }
        setPaths(newPaths);
    }

    @SetJIPipeDocumentation(name = "Paths to absolute", description = "Converts the stored paths to absolute paths.")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/path.png", iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/path.png")
    public void convertPathsToAbsolute() {
        setParameter("paths", getAbsolutePaths());
    }

    @SetJIPipeDocumentation(name = "Paths to relative", description = "Converts the stored paths to paths relative to the project directory (if available).")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/path.png", iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/path.png")
    public void convertPathsToRelative() {
        setParameter("paths", getRelativePaths());
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        for (Path path : getAbsolutePaths()) {
            if (path == null) {
                report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Warning,
                        reportContext,
                        "Input path not set!",
                        "One of the paths is not set.",
                        "Please provide a valid input path."));
            } else if (!Files.exists(path)) {
                report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Warning,
                        reportContext,
                        "Input path does not exist!",
                        "The path '" + path + "' does not exist.",
                        "Please provide a valid input path."));
            }
        }
    }

    @Override
    public void setBaseDirectory(Path baseDirectory) {
        super.setBaseDirectory(baseDirectory);

        boolean modified = false;
        for (int i = 0; i < paths.size(); ++i) {
            Path folderPath = paths.get(i);
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
                    this.paths.set(i, folderPath);
            }
        }
        currentWorkingDirectory = baseDirectory;
    }
}
