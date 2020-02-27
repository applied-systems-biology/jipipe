package org.hkijena.acaq5.api.filesystem.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.filesystem.dataslots.ACAQFilesDataSlot;
import org.hkijena.acaq5.api.filesystem.dataypes.ACAQFileData;
import org.hkijena.acaq5.api.filesystem.dataypes.ACAQFileListData;
import org.hkijena.acaq5.api.filesystem.dataypes.ACAQFilesData;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.traits.AutoTransferTraits;
import org.hkijena.acaq5.utils.PathFilter;

import java.util.ArrayList;
import java.util.List;

@ACAQDocumentation(name = "Filter files", description = "Filters the input files by their name")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.FileSystem)

// Algorithm flow
@AlgorithmInputSlot(value = ACAQFilesDataSlot.class, slotName = "Files", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQFilesDataSlot.class, slotName = "Filtered files", autoCreate = true)

// Traits
@AutoTransferTraits
public class ACAQFilterFiles extends ACAQSimpleAlgorithm<ACAQFilesData, ACAQFilesData> {

    private PathFilter filter = new PathFilter();

    public ACAQFilterFiles(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ACAQFilterFiles(ACAQFilterFiles other) {
        super(other);
    }

    @Override
    public void run() {
        List<ACAQFileData> files = getInputData().getFiles();
        List<ACAQFileData> result = new ArrayList<>();
        for(ACAQFileData folder : files) {
            if(filter.test(folder.getFilePath())) {
                result.add(folder);
            }
        }
        setOutputData(new ACAQFileListData(result));
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
