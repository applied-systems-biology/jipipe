package org.hkijena.acaq5.extensions.filesystem.api.datasources;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.ACAQFolderData;
import org.hkijena.acaq5.extensions.standardparametereditors.ui.parametereditors.FilePathParameterSettings;
import org.hkijena.acaq5.ui.components.FileSelection;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Provides an input folder
 */
@ACAQDocumentation(name = "Folder")
@AlgorithmOutputSlot(value = ACAQFolderData.class, slotName = "Folder path", autoCreate = true)
@AlgorithmMetadata(category = ACAQAlgorithmCategory.DataSource)
public class ACAQFolderDataSource extends ACAQAlgorithm {

    private Path folderPath;
    private Path currentWorkingDirectory;

    public ACAQFolderDataSource(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ACAQFolderDataSource(ACAQFolderDataSource other) {
        super(other);
        this.folderPath = other.folderPath;
        this.currentWorkingDirectory = other.currentWorkingDirectory;
    }

    @Override
    public void run() {
        getFirstOutputSlot().addData(new ACAQFolderData(folderPath));
    }

    @ACAQParameter("folder-path")
    @ACAQDocumentation(name = "Folder path")
    @FilePathParameterSettings(ioMode = FileSelection.IOMode.Open, pathMode = FileSelection.PathMode.DirectoriesOnly)
    public Path getFolderPath() {
        return folderPath;
    }

    @ACAQParameter("folder-path")
    public void setFolderPath(Path folderPath) {
        this.folderPath = folderPath;
        getEventBus().post(new ParameterChangedEvent(this, "folder-path"));
    }

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
            report.reportIsInvalid("Input folder path does not exist! Please provide a valid path.");
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
