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
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.parameters.primitives.FilePathParameterSettings;
import org.hkijena.jipipe.ui.components.PathEditor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Provides an input file
 */
@JIPipeDocumentation(name = "File", description = "Converts the path parameter into file data.")
@JIPipeOutputSlot(value = FileData.class, slotName = "Filename", autoCreate = true)
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class FileDataSource extends JIPipeAlgorithm {

    private Path currentWorkingDirectory;
    private Path fileName;
    private boolean needsToExist = true;

    /**
     * Initializes the algorithm
     *
     * @param info The algorithm info
     */
    public FileDataSource(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public FileDataSource(FileDataSource other) {
        super(other);
        this.fileName = other.fileName;
        this.currentWorkingDirectory = other.currentWorkingDirectory;
        this.needsToExist = other.needsToExist;
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {
        getFirstOutputSlot().addData(new FileData(fileName), progressInfo);
    }

    /**
     * @return The file name
     */
    @JIPipeParameter("file-name")
    @JIPipeDocumentation(name = "File name")
    @FilePathParameterSettings(ioMode = PathEditor.IOMode.Open, pathMode = PathEditor.PathMode.FilesOnly)
    public Path getFileName() {
        return fileName;
    }

    /**
     * Sets the file name
     *
     * @param fileName The file name
     */
    @JIPipeParameter("file-name")
    public void setFileName(Path fileName) {
        this.fileName = fileName;
        FilesystemExtensionSettings settings = FilesystemExtensionSettings.getInstance();
        if(settings != null && settings.isAutoLabelOutputWithFileName()) {
            String name = fileName != null ? fileName.getFileName().toString() : "";
            if(!Objects.equals(getFirstOutputSlot().getInfo().getCustomName(), name)) {
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
     * @return The file name as absolute path
     */
    public Path getAbsoluteFileName() {
        if (fileName == null)
            return null;
        else if (currentWorkingDirectory != null)
            return currentWorkingDirectory.resolve(fileName);
        else
            return fileName;
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        if (needsToExist && (fileName == null || !Files.isRegularFile(getAbsoluteFileName())))
            report.reportIsInvalid("Input file does not exist!",
                    "The file '" + getAbsoluteFileName() + "' does not exist.",
                    "Please provide a valid input file.",
                    this);
    }

    @Override
    public void setWorkDirectory(Path workDirectory) {
        super.setWorkDirectory(workDirectory);

        if (fileName != null) {
            // Make absolute
            if (!fileName.isAbsolute()) {
                if (currentWorkingDirectory != null) {
                    setFileName(currentWorkingDirectory.resolve(fileName));
                } else if (workDirectory != null) {
                    setFileName(workDirectory.resolve(fileName));
                }
            }
            // Make relative if already absolute and workDirectory != null
            FilesystemExtensionSettings settings = FilesystemExtensionSettings.getInstance();
            if(settings == null || settings.isRelativizePaths()) {
                if (fileName.isAbsolute()) {
                    if (workDirectory != null && fileName.startsWith(workDirectory)) {
                        setFileName(workDirectory.relativize(fileName));
                    }
                }
            }
        }

        currentWorkingDirectory = workDirectory;
    }
}
