package org.hkijena.acaq5.api.filesystem.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.filesystem.dataslots.ACAQFoldersDataSlot;
import org.hkijena.acaq5.api.filesystem.dataypes.ACAQFolderData;
import org.hkijena.acaq5.api.filesystem.dataypes.ACAQFolderListData;
import org.hkijena.acaq5.api.filesystem.dataypes.ACAQFoldersData;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.traits.AutoTransferTraits;
import org.hkijena.acaq5.utils.PathFilter;

import java.util.ArrayList;
import java.util.List;

@ACAQDocumentation(name = "Filter folders", description = "Filters the input folders by their name")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.FileSystem)

// Algorithm flow
@AlgorithmInputSlot(value = ACAQFoldersDataSlot.class, slotName = "Folders", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQFoldersDataSlot.class, slotName = "Filtered files", autoCreate = true)

// Traits
@AutoTransferTraits
public class ACAQFilterFolders extends ACAQSimpleAlgorithm<ACAQFoldersData, ACAQFoldersData> {

    private PathFilter filter = new PathFilter();

    public ACAQFilterFolders(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ACAQFilterFolders(ACAQFilterFolders other) {
        super(other);
    }

    @Override
    public void run() {
        List<ACAQFolderData> folders = getInputData().getFolders();
        List<ACAQFolderData> result = new ArrayList<>();
        for(ACAQFolderData folder : folders) {
            if(filter.test(folder.getFolderPath())) {
                result.add(folder);
            }
        }
        setOutputData(new ACAQFolderListData(result));
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
