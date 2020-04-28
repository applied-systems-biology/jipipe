package org.hkijena.acaq5.extensions.filesystem.api.datasources;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.FolderData;
import org.hkijena.acaq5.extensions.standardparametereditors.editors.FilePathParameterSettings;
import org.hkijena.acaq5.ui.components.FileSelection;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Provides an input folder
 */
@ACAQDocumentation(name = "Folder")
@AlgorithmOutputSlot(value = FolderData.class, slotName = "Folder path", autoCreate = true)
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.DataSource)
public class FolderDataSource extends ACAQAlgorithm {

    private Path folderPath;
    private Path currentWorkingDirectory;

    /**
     * Initializes the algorithm
     *
     * @param declaration Algorithm declaration
     */
    public FolderDataSource(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
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
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        getFirstOutputSlot().addData(new FolderData(folderPath));
    }

    /**
     * @return The folder path
     */
    @ACAQParameter("folder-path")
    @ACAQDocumentation(name = "Folder path")
    @FilePathParameterSettings(ioMode = FileSelection.IOMode.Open, pathMode = FileSelection.PathMode.DirectoriesOnly)
    public Path getFolderPath() {
        return folderPath;
    }

    /**
     * Sets the folder path
     *
     * @param folderPath The folder path
     */
    @ACAQParameter("folder-path")
    public void setFolderPath(Path folderPath) {
        this.folderPath = folderPath;
        getEventBus().post(new ParameterChangedEvent(this, "folder-path"));
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
    public void reportValidity(ACAQValidityReport report) {
        if (folderPath == null || !Files.isDirectory(getAbsoluteFolderPath()))
            report.reportIsInvalid("Input folder path does not exist!",
                    "The path '" + getAbsoluteFolderPath() + "' does not exist.",
                    "Please provide a valid input file.");
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
