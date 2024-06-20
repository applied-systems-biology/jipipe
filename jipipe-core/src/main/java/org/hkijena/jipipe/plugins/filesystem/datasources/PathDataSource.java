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

package org.hkijena.jipipe.plugins.filesystem.datasources;

import org.apache.commons.io.FileUtils;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.context.JIPipeDataContext;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.plugins.filesystem.JIPipeFilesystemPluginApplicationSettings;
import org.hkijena.jipipe.plugins.filesystem.dataypes.PathData;
import org.hkijena.jipipe.plugins.parameters.library.filesystem.PathParameterSettings;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Provides an input file
 */
@SetJIPipeDocumentation(name = "Path", description = "Converts the path parameter into path data.")
@AddJIPipeOutputSlot(value = PathData.class, name = "Path", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class PathDataSource extends JIPipeAlgorithm {

    private Path currentWorkingDirectory;
    private Path path;
    private boolean needsToExist = true;

    /**
     * Initializes the algorithm
     *
     * @param info The algorithm info
     */
    public PathDataSource(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public PathDataSource(PathDataSource other) {
        super(other);
        this.path = other.path;
        this.currentWorkingDirectory = other.currentWorkingDirectory;
        this.needsToExist = other.needsToExist;
    }

    @Override
    public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        getFirstOutputSlot().addData(new PathData(path), JIPipeDataContext.create(this), progressInfo);
    }

    /**
     * @return The file name
     */
    @JIPipeParameter("path")
    @SetJIPipeDocumentation(name = "Path")
    @PathParameterSettings(ioMode = PathIOMode.Open, pathMode = PathType.FilesAndDirectories)
    public Path getPath() {
        return path;
    }

    /**
     * Sets the file name
     *
     * @param path The file name
     */
    @JIPipeParameter("path")
    public void setPath(Path path) {
        this.path = PathUtils.normalize(path);
        JIPipeFilesystemPluginApplicationSettings settings = JIPipeFilesystemPluginApplicationSettings.getInstance();
        if (settings != null && settings.isAutoLabelOutputWithFileName()) {
            String name = path != null ? path.getFileName().toString() : "";
            if (!Objects.equals(getFirstOutputSlot().getInfo().getCustomName(), name)) {
                getFirstOutputSlot().getInfo().setCustomName(name);
                getNodeSlotsChangedEventEmitter().emit(new NodeSlotsChangedEvent(this));
            }
        }
    }

    @SetJIPipeDocumentation(name = "Needs to exist", description = "If true, the selected path needs to exist.")
    @JIPipeParameter("needs-to-exist")
    public boolean isNeedsToExist() {
        return needsToExist;
    }

    @JIPipeParameter("needs-to-exist")
    public void setNeedsToExist(boolean needsToExist) {
        this.needsToExist = needsToExist;
    }

    /**
     * @return The file name as absolute path
     */
    public Path getAbsolutePath() {
        if (path == null)
            return null;
        else if (currentWorkingDirectory != null)
            return currentWorkingDirectory.resolve(path);
        else
            return path;
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        if (needsToExist && (path == null || !Files.exists(getAbsolutePath()))) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Warning,
                    reportContext,
                    "Input path does not exist!",
                    "The path '" + getAbsolutePath() + "' does not exist.",
                    "Please provide a valid input path."));
        }
    }

    @Override
    public void archiveTo(JIPipeWriteDataStorage projectStorage, JIPipeWriteDataStorage wrappedExternalStorage, JIPipeProgressInfo progressInfo, Path originalBaseDirectory) {
        Path source = getAbsolutePath();
        if (source == null || !Files.exists(source)) {
            if (isNeedsToExist()) {
                throw new RuntimeException("Path " + getPath() + " does not exist!");
            }
            progressInfo.log("Unable to archive: " + getPath());
        } else {
            Path target;
            if (source.startsWith(originalBaseDirectory)) {
                // The data is located in the project directory. We can directly copy the file.
                Path relativePath = originalBaseDirectory.relativize(source);
                target = projectStorage.getFileSystemPath().resolve(relativePath);
            } else {
                // The data is located outside the project directory. Needs to be copied into a unique directory.
                target = wrappedExternalStorage.resolve(getAliasIdInParentGraph()).getFileSystemPath().resolve(getPath().getFileName());
            }

            if (Files.exists(target)) {
                progressInfo.log("Not copying " + source + " -> " + target + " (Already exists)");
                return;
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
            setPath(target);
        }
    }

    @Override
    public void setBaseDirectory(Path baseDirectory) {
        super.setBaseDirectory(baseDirectory);

        if (path != null) {
            // Make absolute
            if (!path.isAbsolute()) {
                if (currentWorkingDirectory != null) {
                    setPath(currentWorkingDirectory.resolve(path));
                } else if (baseDirectory != null) {
                    setPath(baseDirectory.resolve(path));
                }
            }
            // Make relative if already absolute and workDirectory != null
            JIPipeFilesystemPluginApplicationSettings settings = JIPipeFilesystemPluginApplicationSettings.getInstance();
            if (settings == null || settings.isRelativizePaths()) {
                if (path.isAbsolute()) {
                    if (baseDirectory != null && path.startsWith(baseDirectory)) {
                        setPath(baseDirectory.relativize(path));
                    }
                }
            }
        }

        currentWorkingDirectory = baseDirectory;
    }
}
