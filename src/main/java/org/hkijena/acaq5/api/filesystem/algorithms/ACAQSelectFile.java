package org.hkijena.acaq5.api.filesystem.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.filesystem.dataslots.ACAQFileDataSlot;
import org.hkijena.acaq5.api.filesystem.dataslots.ACAQFilesDataSlot;
import org.hkijena.acaq5.api.filesystem.dataypes.ACAQFileData;
import org.hkijena.acaq5.api.filesystem.dataypes.ACAQFilesData;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.traits.AutoTransferTraits;
import org.hkijena.acaq5.utils.PathFilter;

import java.util.List;

@ACAQDocumentation(name = "Select file", description = "Selects any file that has a name that matches the filter")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.FileSystem)

// Algorithm flow
@AlgorithmInputSlot(value = ACAQFilesDataSlot.class, slotName = "Files", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQFileDataSlot.class, slotName = "File", autoCreate = true)

// Traits
@AutoTransferTraits
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
        List<ACAQFileData> inputFiles = getInputData().getFiles();
        for(ACAQFileData inputFile : inputFiles) {
            if(filter.test(inputFile.getFilePath())) {
                setOutputData(inputFile);
                break;
            }
        }
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
