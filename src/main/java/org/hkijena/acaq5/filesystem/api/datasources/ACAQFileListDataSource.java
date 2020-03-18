package org.hkijena.acaq5.filesystem.api.datasources;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.CollectionParameter;
import org.hkijena.acaq5.api.parameters.PathCollectionParameter;
import org.hkijena.acaq5.extension.ui.parametereditors.FilePathParameterSettings;
import org.hkijena.acaq5.filesystem.api.dataypes.ACAQFileData;
import org.hkijena.acaq5.ui.components.FileSelection;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Provides an input file
 */
@ACAQDocumentation(name = "File list")
@AlgorithmOutputSlot(value = ACAQFileData.class, slotName = "Filenames", autoCreate = true)
@AlgorithmMetadata(category = ACAQAlgorithmCategory.DataSource)
public class ACAQFileListDataSource extends ACAQAlgorithm {

    private PathCollectionParameter fileNames = new PathCollectionParameter();

    public ACAQFileListDataSource(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ACAQFileListDataSource(ACAQFileListDataSource other) {
        super(other);
        this.fileNames.addAll(other.fileNames);
    }

    @Override
    public void run() {
        for (Path path : fileNames) {
            getFirstOutputSlot().addData(new ACAQFileData(path));
        }
    }

    @ACAQParameter("file-names")
    @ACAQDocumentation(name = "File names")
    @FilePathParameterSettings(ioMode = FileSelection.IOMode.Open, pathMode = FileSelection.PathMode.FilesOnly)
    public PathCollectionParameter getFileNames() {
        return fileNames;
    }

    @ACAQParameter("file-names")
    public void setFileNames(PathCollectionParameter fileNames) {
        this.fileNames = fileNames;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        for (Path fileName : fileNames) {
            if(fileName == null) {
                report.reportIsInvalid("An input file does not exist! Please provide a valid input file.");
            }
            else if(!Files.isRegularFile(fileName)) {
                report.reportIsInvalid("Input file '" + fileName + "' does not exist! Please provide a valid input file.");
            }
        }
    }
}
