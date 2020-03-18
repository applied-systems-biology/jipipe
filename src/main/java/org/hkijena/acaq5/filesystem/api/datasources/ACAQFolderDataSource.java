package org.hkijena.acaq5.filesystem.api.datasources;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extension.ui.parametereditors.FilePathParameterSettings;
import org.hkijena.acaq5.filesystem.api.dataypes.ACAQFolderData;
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

    public ACAQFolderDataSource(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ACAQFolderDataSource(ACAQFolderDataSource other) {
        super(other);
        this.folderPath = other.folderPath;
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
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (folderPath == null || !Files.isDirectory(folderPath))
            report.reportIsInvalid("Input folder path does not exist! Please provide a valid path.");
    }
}
