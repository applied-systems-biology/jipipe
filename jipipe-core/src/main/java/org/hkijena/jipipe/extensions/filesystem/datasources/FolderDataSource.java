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
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.FilesystemExtensionSettings;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.extensions.parameters.primitives.FilePathParameterSettings;
import org.hkijena.jipipe.ui.components.PathEditor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Provides an input folder
 */
@JIPipeDocumentation(name = "Folder", description = "Converts the path parameter into folder data.")
@JIPipeOutputSlot(value = FolderData.class, slotName = "Folder path", autoCreate = true)
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class FolderDataSource extends JIPipeAlgorithm {

    private Path folderPath;
    private Path currentWorkingDirectory;
    private boolean needsToExist = true;

    /**
     * Initializes the algorithm
     *
     * @param info Algorithm info
     */
    public FolderDataSource(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public FolderDataSource(FolderDataSource other) {
        super(other);
        this.folderPath = other.folderPath;
        this.currentWorkingDirectory = other.currentWorkingDirectory;
        this.needsToExist = other.needsToExist;
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {
        getFirstOutputSlot().addData(new FolderData(folderPath), progressInfo);
    }

    /**
     * @return The folder path
     */
    @JIPipeParameter("folder-path")
    @JIPipeDocumentation(name = "Folder path")
    @FilePathParameterSettings(ioMode = PathEditor.IOMode.Open, pathMode = PathEditor.PathMode.DirectoriesOnly)
    public Path getFolderPath() {
        return folderPath;
    }

    /**
     * Sets the folder path
     *
     * @param folderPath The folder path
     */
    @JIPipeParameter("folder-path")
    public void setFolderPath(Path folderPath) {
        this.folderPath = folderPath;
        FilesystemExtensionSettings settings = FilesystemExtensionSettings.getInstance();
        if (settings != null && settings.isAutoLabelOutputWithFileName()) {
            String name = folderPath != null ? folderPath.getFileName().toString() : "";
            if (!Objects.equals(getFirstOutputSlot().getInfo().getCustomName(), name)) {
                getFirstOutputSlot().getInfo().setCustomName(name);
                getEventBus().post(new JIPipeGraph.NodeSlotsChangedEvent(this));
            }
        }
    }

    @JIPipeDocumentation(name = "Needs to exist", description = "If true, the selected file needs to exist.")
    @JIPipeParameter("needs-to-exist")
    public boolean isNeedsToExist() {
        return needsToExist;
    }

    @JIPipeParameter("needs-to-exist")
    public void setNeedsToExist(boolean needsToExist) {
        this.needsToExist = needsToExist;
    }

    /**
     * @return The folder path as absolute path
     */
    public Path getAbsoluteFolderPath() {
        if (folderPath == null)
            return null;
        else if (currentWorkingDirectory != null)
            return currentWorkingDirectory.resolve(folderPath);
        else
            return folderPath;
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        if (needsToExist && (folderPath == null || !Files.isDirectory(getAbsoluteFolderPath())))
            report.reportIsInvalid("Input folder path does not exist!",
                    "The path '" + getAbsoluteFolderPath() + "' does not exist.",
                    "Please provide a valid input file.",
                    this);
    }

    @Override
    public void setWorkDirectory(Path workDirectory) {
        super.setWorkDirectory(workDirectory);

        if (folderPath != null) {
            // Make absolute 
            if (!folderPath.isAbsolute()) {
                if (currentWorkingDirectory != null) {
                    setFolderPath(currentWorkingDirectory.resolve(folderPath));
                } else if (workDirectory != null) {
                    setFolderPath(workDirectory.resolve(folderPath));
                }
            }
            // Make relative if already absolute and workDirectory != null
            FilesystemExtensionSettings settings = FilesystemExtensionSettings.getInstance();
            if (settings == null || settings.isRelativizePaths()) {
                if (folderPath.isAbsolute()) {
                    if (workDirectory != null && folderPath.startsWith(workDirectory)) {
                        setFolderPath(workDirectory.relativize(folderPath));
                    }
                }
            }
        }

        currentWorkingDirectory = workDirectory;
    }
}
