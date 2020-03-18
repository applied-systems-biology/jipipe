package org.hkijena.acaq5.filesystem.api.datasources;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.CollectionParameter;
import org.hkijena.acaq5.api.parameters.PathCollectionParameter;
import org.hkijena.acaq5.extension.ui.parametereditors.FilePathParameterSettings;
import org.hkijena.acaq5.filesystem.api.dataypes.ACAQFolderData;
import org.hkijena.acaq5.ui.components.FileSelection;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Provides an input folder
 */
@ACAQDocumentation(name = "Folder list")
@AlgorithmOutputSlot(value = ACAQFolderData.class, slotName = "Folder paths", autoCreate = true)
@AlgorithmMetadata(category = ACAQAlgorithmCategory.DataSource)
public class ACAQFolderListDataSource extends ACAQAlgorithm {

    private PathCollectionParameter folderPaths = new PathCollectionParameter();

    public ACAQFolderListDataSource(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ACAQFolderListDataSource(ACAQFolderListDataSource other) {
        super(other);
        this.folderPaths.addAll(other.folderPaths);
    }

    @Override
    public void run() {
        for (Path folderPath : folderPaths) {
            getFirstOutputSlot().addData(new ACAQFolderData(folderPath));
        }
    }

    @ACAQParameter("folder-paths")
    @ACAQDocumentation(name = "Folder paths")
    @FilePathParameterSettings(ioMode = FileSelection.IOMode.Open, pathMode = FileSelection.PathMode.DirectoriesOnly)
    public PathCollectionParameter getFolderPaths() {
        return folderPaths;
    }

    @ACAQParameter("folder-paths")
    public void setFolderPaths(PathCollectionParameter folderPaths) {
        this.folderPaths = folderPaths;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        for (Path folderPath : folderPaths) {
            if(folderPath == null) {
                report.reportIsInvalid("An input folder path does not exist! Please provide a valid path.");
            }
            else if(!Files.isDirectory(folderPath)) {
                report.reportIsInvalid("Input folder '" + folderPath + "' does not exist! Please provide a valid path.");
            }
        }
    }
}
