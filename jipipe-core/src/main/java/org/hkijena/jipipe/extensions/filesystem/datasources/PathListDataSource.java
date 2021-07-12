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
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.FilesystemExtensionSettings;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.extensions.parameters.primitives.PathParameterSettings;
import org.hkijena.jipipe.extensions.parameters.primitives.PathList;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;
import org.hkijena.jipipe.utils.PathUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Provides an input folder
 */
@JIPipeDocumentation(name = "Path list", description = "Converts each provided path into path data.")
@JIPipeOutputSlot(value = PathData.class, slotName = "Paths", autoCreate = true)
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
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
    public void run(JIPipeProgressInfo progressInfo) {
        for (Path folderPath : paths) {
            getFirstOutputSlot().addData(new PathData(folderPath), progressInfo);
        }
    }

    /**
     * @return Gets the folder paths
     */
    @JIPipeParameter("paths")
    @JIPipeDocumentation(name = "Paths")
    @PathParameterSettings(ioMode = PathIOMode.Open, pathMode = PathType.FilesAndDirectories)
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
                getEventBus().post(new JIPipeGraph.NodeSlotsChangedEvent(this));
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
            else if (currentWorkingDirectory != null)
                result.add(currentWorkingDirectory.resolve(folderPath));
            else
                result.add(folderPath);
        }
        return result;
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        for (Path folderPath : getAbsolutePaths()) {
            if (folderPath == null) {
                report.reportIsInvalid("Invalid path!",
                        "An input folder path does not exist!",
                        "Please provide a valid path.",
                        this);
            } else if (!Files.exists(folderPath)) {
                report.reportIsInvalid("Invalid path!",
                        "Input path '" + folderPath + "' does not exist!",
                        "Please provide a valid path.",
                        this);
            }
        }
    }

    @Override
    public void setProjectWorkDirectory(Path projectWorkDirectory) {
        super.setProjectWorkDirectory(projectWorkDirectory);

        boolean modified = false;
        for (int i = 0; i < paths.size(); ++i) {
            Path folderPath = paths.get(i);
            if (folderPath != null) {
                // Make absolute
                if (!folderPath.isAbsolute()) {
                    if (currentWorkingDirectory != null) {
                        folderPath = currentWorkingDirectory.resolve(folderPath);
                        modified = true;
                    } else if (projectWorkDirectory != null) {
                        folderPath = projectWorkDirectory.resolve(folderPath);
                        modified = true;
                    }
                }
                // Make relative if already absolute and workDirectory != null
                FilesystemExtensionSettings settings = FilesystemExtensionSettings.getInstance();
                if (settings == null || settings.isRelativizePaths()) {
                    if (folderPath.isAbsolute()) {
                        if (projectWorkDirectory != null && folderPath.startsWith(projectWorkDirectory)) {
                            folderPath = projectWorkDirectory.relativize(folderPath);
                            modified = true;
                        }
                    }
                }

                if (modified)
                    this.paths.set(i, folderPath);
            }
        }
        currentWorkingDirectory = projectWorkDirectory;
    }
}
