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

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.FilesystemExtensionSettings;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
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
 * Provides an input file
 */
@JIPipeDocumentation(name = "File list", description = "Converts each provided path into file data.")
@JIPipeOutputSlot(value = FileData.class, slotName = "Filenames", autoCreate = true)
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class FileListDataSource extends JIPipeAlgorithm {

    private PathList files = new PathList();
    private Path currentWorkingDirectory;

    /**
     * Initializes the algorithm
     *
     * @param info The algorithm info
     */
    public FileListDataSource(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public FileListDataSource(FileListDataSource other) {
        super(other);
        this.files.addAll(other.files);
        this.currentWorkingDirectory = other.currentWorkingDirectory;
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {
        for (Path path : files) {
            getFirstOutputSlot().addData(new FileData(path), progressInfo);
        }
    }

    /**
     * @return The file names
     */
    @JIPipeParameter("file-names")
    @JIPipeDocumentation(name = "Files")
    @PathParameterSettings(ioMode = PathIOMode.Open, pathMode = PathType.FilesOnly)
    @ListParameterSettings(withScrollBar = true)
    public PathList getFiles() {
        return files;
    }

    /**
     * Sets the file names
     *
     * @param files The file names
     */
    @JIPipeParameter("file-names")
    public void setFiles(PathList files) {
        this.files = files;
        PathUtils.normalizeList(files);
        FilesystemExtensionSettings settings = FilesystemExtensionSettings.getInstance();
        if (settings != null && settings.isAutoLabelOutputWithFileName()) {
            String name = files.size() == 1 ? files.get(0).getFileName().toString() : "";
            if (!Objects.equals(getFirstOutputSlot().getInfo().getCustomName(), name)) {
                getFirstOutputSlot().getInfo().setCustomName(name);
                getEventBus().post(new JIPipeGraph.NodeSlotsChangedEvent(this));
            }
        }
    }

    /**
     * @return Absolute file names
     */
    public PathList getAbsoluteFileNames() {
        PathList result = new PathList();
        for (Path fileName : files) {
            if (fileName == null) {
                result.add(null);
            } else if (currentWorkingDirectory != null && !fileName.isAbsolute()) {
                result.add(currentWorkingDirectory.resolve(fileName));
            } else {
                result.add(fileName);
            }
        }
        return result;
    }

    /**
     * @return Relative file names (if available)
     */
    public PathList getRelativeFileNames() {
        PathList result = new PathList();
        for (Path fileName : files) {
            if (fileName == null)
                result.add(null);
            else if (currentWorkingDirectory != null && fileName.isAbsolute() && fileName.startsWith(currentWorkingDirectory)) {
                result.add(currentWorkingDirectory.relativize(fileName));
            } else {
                result.add(fileName);
            }
        }
        return result;
    }

    @Override
    public void archiveTo(JIPipeWriteDataStorage projectStorage, JIPipeWriteDataStorage wrappedExternalStorage, JIPipeProgressInfo progressInfo, Path originalBaseDirectory) {
        PathList relativeFileNames = getRelativeFileNames();
        PathList absoluteFileNames = getAbsoluteFileNames();
        PathList newPaths = new PathList();
        Set<String> externalFileNames = new HashSet<>();

        for (int i = 0; i < relativeFileNames.size(); i++) {
            Path source = absoluteFileNames.get(i);
            if (source == null || !Files.isRegularFile(source)) {
                throw new RuntimeException("File " + relativeFileNames.get(i) + " does not exist!");
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
                        progressInfo.log("Warning: Duplicate file name in external storage (" + externalFileName + "). Creating new UUID sub-storage in " + getAliasIdInParentGraph());
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
                    Files.copy(source, target);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                newPaths.add(target);
            }
        }
        setFiles(newPaths);
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        for (Path fileName : getAbsoluteFileNames()) {
            if (fileName == null) {
                report.reportIsInvalid("Invalid file path!",
                        "An input file does not exist!",
                        "Please provide a valid input file.",
                        this);
            } else if (!Files.isRegularFile(fileName)) {
                report.reportIsInvalid("Invalid file path!",
                        "Input file '" + fileName + "' does not exist!",
                        "Please provide a valid input file.",
                        this);
            }
        }
    }

    @JIPipeDocumentation(name = "Paths to absolute", description = "Converts the stored paths to absolute paths.")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/path.png", iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/path.png")
    public void convertPathsToAbsolute() {
        setParameter("file-names", getAbsoluteFileNames());
    }

    @JIPipeDocumentation(name = "Paths to relative", description = "Converts the stored paths to paths relative to the project directory (if available).")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/path.png", iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/path.png")
    public void convertPathsToRelative() {
        setParameter("file-names", getRelativeFileNames());
    }

    @Override
    public void setBaseDirectory(Path baseDirectory) {
        super.setBaseDirectory(baseDirectory);

        boolean modified = false;
        for (int i = 0; i < files.size(); ++i) {
            Path fileName = files.get(i);
            if (fileName != null) {
                // Make absolute
                if (!fileName.isAbsolute()) {
                    if (currentWorkingDirectory != null) {
                        fileName = currentWorkingDirectory.resolve(fileName);
                        modified = true;
                    } else if (baseDirectory != null) {
                        fileName = baseDirectory.resolve(fileName);
                        modified = true;
                    }
                }
                // Make relative if already absolute and workDirectory != null
                FilesystemExtensionSettings settings = FilesystemExtensionSettings.getInstance();
                if (settings == null || settings.isRelativizePaths()) {
                    if (fileName.isAbsolute()) {
                        if (baseDirectory != null && fileName.startsWith(baseDirectory)) {
                            fileName = baseDirectory.relativize(fileName);
                            modified = true;
                        }
                    }
                }

                if (modified)
                    this.files.set(i, fileName);
            }
        }
        currentWorkingDirectory = baseDirectory;
    }
}
