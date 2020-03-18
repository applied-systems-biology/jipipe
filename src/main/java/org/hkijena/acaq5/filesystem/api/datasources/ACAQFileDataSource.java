package org.hkijena.acaq5.filesystem.api.datasources;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extension.ui.parametereditors.FilePathParameterSettings;
import org.hkijena.acaq5.filesystem.api.dataypes.ACAQFileData;
import org.hkijena.acaq5.ui.components.FileSelection;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Provides an input file
 */
@ACAQDocumentation(name = "File")
@AlgorithmOutputSlot(value = ACAQFileData.class, slotName = "Filename", autoCreate = true)
@AlgorithmMetadata(category = ACAQAlgorithmCategory.DataSource)
public class ACAQFileDataSource extends ACAQAlgorithm {

    private Path fileName;

    public ACAQFileDataSource(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ACAQFileDataSource(ACAQFileDataSource other) {
        super(other);
        this.fileName = other.fileName;
    }

    @Override
    public void run() {
        getFirstOutputSlot().addData(new ACAQFileData(fileName));
    }

    @ACAQParameter("file-name")
    @ACAQDocumentation(name = "File name")
    @FilePathParameterSettings(ioMode = FileSelection.IOMode.Open, pathMode = FileSelection.PathMode.FilesOnly)
    public Path getFileName() {
        return fileName;
    }

    @ACAQParameter("file-name")
    public void setFileName(Path fileName) {
        this.fileName = fileName;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (fileName == null || !Files.isRegularFile(fileName))
            report.reportIsInvalid("Input file does not exist! Please provide a valid input file.");
    }
}
