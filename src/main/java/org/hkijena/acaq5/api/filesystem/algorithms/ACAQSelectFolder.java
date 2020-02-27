package org.hkijena.acaq5.api.filesystem.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.filesystem.dataslots.ACAQFolderDataSlot;
import org.hkijena.acaq5.api.filesystem.dataslots.ACAQFoldersDataSlot;
import org.hkijena.acaq5.api.filesystem.dataypes.ACAQFolderData;
import org.hkijena.acaq5.api.filesystem.dataypes.ACAQFoldersData;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.traits.AutoTransferTraits;
import org.hkijena.acaq5.utils.PathFilter;

import java.util.List;

@ACAQDocumentation(name = "Select folder", description = "Selects any folder that has a name that matches the filter")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.FileSystem)

// Algorithm flow
@AlgorithmInputSlot(value = ACAQFoldersDataSlot.class, slotName = "Folders", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQFolderDataSlot.class, slotName = "Folder", autoCreate = true)

// Traits
@AutoTransferTraits
public class ACAQSelectFolder extends ACAQSimpleAlgorithm<ACAQFoldersData, ACAQFolderData> {

    private PathFilter filter = new PathFilter();

    public ACAQSelectFolder(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ACAQSelectFolder(ACAQSelectFolder other) {
        super(other);
    }

    @Override
    public void run() {
        List<ACAQFolderData> inputFolders = getInputData().getFolders();
        for(ACAQFolderData inputFolder : inputFolders) {
            if(filter.test(inputFolder.getFolderPath())) {
                setOutputData(inputFolder);
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
