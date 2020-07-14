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
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.JIPipeOutputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithm;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.extensions.parameters.primitives.FilePathParameterSettings;
import org.hkijena.jipipe.ui.components.PathEditor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Provides an input folder
 */
@JIPipeDocumentation(name = "Folder")
@JIPipeOutputSlot(value = FolderData.class, slotName = "Folder path", autoCreate = true)
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.DataSource)
public class FolderDataSource extends JIPipeAlgorithm {

    private Path folderPath;
    private Path currentWorkingDirectory;

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
    }

    @Override
    public void run(JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        getFirstOutputSlot().addData(new FolderData(folderPath));
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
        if (folderPath == null || !Files.isDirectory(getAbsoluteFolderPath()))
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
            if (folderPath.isAbsolute()) {
                if (workDirectory != null && folderPath.startsWith(workDirectory)) {
                    setFolderPath(workDirectory.relativize(folderPath));
                }
            }
        }

        currentWorkingDirectory = workDirectory;
    }
}
