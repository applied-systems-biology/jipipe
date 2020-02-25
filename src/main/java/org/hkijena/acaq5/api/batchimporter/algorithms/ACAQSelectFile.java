package org.hkijena.acaq5.api.batchimporter.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.batchimporter.dataslots.ACAQFileDataSlot;
import org.hkijena.acaq5.api.batchimporter.dataslots.ACAQFilesDataSlot;
import org.hkijena.acaq5.api.batchimporter.dataypes.ACAQFileData;
import org.hkijena.acaq5.api.batchimporter.dataypes.ACAQFilesData;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.utils.PathFilter;

import java.nio.file.Path;
import java.util.List;

@ACAQDocumentation(name = "Select file", description = "Selects any file that has a name that matches the filter")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Converter)

// Algorithm flow
@AlgorithmInputSlot(value = ACAQFilesDataSlot.class, slotName = "Files", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQFileDataSlot.class, slotName = "File", autoCreate = true)
public class ACAQSelectFile extends ACAQSimpleAlgorithm<ACAQFilesData, ACAQFileData> {

    private PathFilter filter = new PathFilter();

    public ACAQSelectFile(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ACAQSelectFile(ACAQSelectFile other) {
        super(other);
    }

    @Override
    public void run() {
        List<Path> fileNames = getInputData().getFileNames();
        setOutputData(new ACAQFileData(fileNames.stream().filter(filter).findFirst().orElse(null)));
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }

    @ACAQParameter("filter")
    @ACAQDocumentation(name = "Filter")
    public PathFilter getFilter() {
        return filter;
    }

    @ACAQParameter("filter")
    public void setFilter(PathFilter filter) {
        this.filter = filter;
    }
}
