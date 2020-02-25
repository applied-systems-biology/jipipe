package org.hkijena.acaq5.api.batchimporter.datasources;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.AlgorithmMetadata;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.batchimporter.dataslots.ACAQFileDataSlot;
import org.hkijena.acaq5.api.batchimporter.dataypes.ACAQFileData;
import org.hkijena.acaq5.api.batchimporter.dataypes.ACAQFolderData;
import org.hkijena.acaq5.api.data.ACAQSimpleDataSource;
import org.hkijena.acaq5.api.parameters.ACAQParameter;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Provides an input file
 */
@ACAQDocumentation(name = "File")
@AlgorithmOutputSlot(value = ACAQFileDataSlot.class, slotName = "Filename", autoCreate = true)
@AlgorithmMetadata(category = ACAQAlgorithmCategory.DataSource)
public class ACAQFileDataSource extends ACAQSimpleDataSource<ACAQFileData> {

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
        setOutputData(new ACAQFileData(new ACAQFolderData(null, fileName.getParent()), fileName));
    }

    @ACAQParameter("file-name")
    public void setFileName(Path fileName) {
        this.fileName = fileName;
    }

    @ACAQParameter("file-name")
    @ACAQDocumentation(name = "File name")
    public Path getFileName() {
        return fileName;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if(fileName == null ||!Files.isRegularFile(fileName))
            report.reportIsInvalid("Input file does not exist! Please provide a valid input file.");
    }
}
