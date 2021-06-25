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
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.FilesystemExtensionSettings;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.extensions.parameters.primitives.FilePathParameterSettings;
import org.hkijena.jipipe.ui.components.PathEditor;
import org.hkijena.jipipe.utils.PathUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Provides an input file
 */
@JIPipeDocumentation(name = "Path", description = "Converts the path parameter into path data.")
@JIPipeOutputSlot(value = PathData.class, slotName = "Path", autoCreate = true)
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
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
    public void run(JIPipeProgressInfo progressInfo) {
        getFirstOutputSlot().addData(new PathData(path), progressInfo);
    }

    /**
     * @return The file name
     */
    @JIPipeParameter("path")
    @JIPipeDocumentation(name = "Path")
    @FilePathParameterSettings(ioMode = PathEditor.IOMode.Open, pathMode = PathEditor.PathMode.FilesAndDirectories)
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
        FilesystemExtensionSettings settings = FilesystemExtensionSettings.getInstance();
        if (settings != null && settings.isAutoLabelOutputWithFileName()) {
            String name = path != null ? path.getFileName().toString() : "";
            if (!Objects.equals(getFirstOutputSlot().getInfo().getCustomName(), name)) {
                getFirstOutputSlot().getInfo().setCustomName(name);
                getEventBus().post(new JIPipeGraph.NodeSlotsChangedEvent(this));
            }
        }
    }

    @JIPipeDocumentation(name = "Needs to exist", description = "If true, the selected path needs to exist.")
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
    public void reportValidity(JIPipeIssueReport report) {
        if (needsToExist && (path == null || !Files.exists(getAbsolutePath())))
            report.reportIsInvalid("Input path does not exist!",
                    "The file '" + getAbsolutePath() + "' does not exist.",
                    "Please provide a valid input path.",
                    this);
    }

    @Override
    public void setProjectWorkDirectory(Path projectWorkDirectory) {
        super.setProjectWorkDirectory(projectWorkDirectory);

        if (path != null) {
            // Make absolute
            if (!path.isAbsolute()) {
                if (currentWorkingDirectory != null) {
                    setPath(currentWorkingDirectory.resolve(path));
                } else if (projectWorkDirectory != null) {
                    setPath(projectWorkDirectory.resolve(path));
                }
            }
            // Make relative if already absolute and workDirectory != null
            FilesystemExtensionSettings settings = FilesystemExtensionSettings.getInstance();
            if (settings == null || settings.isRelativizePaths()) {
                if (path.isAbsolute()) {
                    if (projectWorkDirectory != null && path.startsWith(projectWorkDirectory)) {
                        setPath(projectWorkDirectory.relativize(path));
                    }
                }
            }
        }

        currentWorkingDirectory = projectWorkDirectory;
    }
}
