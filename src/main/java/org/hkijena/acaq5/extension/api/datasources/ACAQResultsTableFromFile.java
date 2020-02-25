package org.hkijena.acaq5.extension.api.datasources;

import ij.measure.ResultsTable;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.AlgorithmMetadata;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQSimpleDataSource;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extension.api.dataslots.ACAQResultsTableDataSlot;
import org.hkijena.acaq5.extension.api.datatypes.ACAQResultsTableData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@ACAQDocumentation(name = "Results table from file")
@AlgorithmOutputSlot(value = ACAQResultsTableDataSlot.class, slotName = "Results table", autoCreate = true)
@AlgorithmMetadata(category = ACAQAlgorithmCategory.DataSource)
public class ACAQResultsTableFromFile extends ACAQSimpleDataSource<ACAQResultsTableData> {

    private Path fileName;

    public ACAQResultsTableFromFile(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ACAQResultsTableFromFile(ACAQResultsTableFromFile other) {
        super(other);
        this.fileName = other.fileName;
    }

    @Override
    public void run() {
        try {
            setOutputData(new ACAQResultsTableData(ResultsTable.open(fileName.toString())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
