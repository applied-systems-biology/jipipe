package org.hkijena.acaq5.api.batchimporter.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.batchimporter.dataslots.ACAQFoldersDataSlot;
import org.hkijena.acaq5.api.batchimporter.dataypes.ACAQFolderData;
import org.hkijena.acaq5.api.batchimporter.dataypes.ACAQFoldersData;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.traits.AutoTransferTraits;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@ACAQDocumentation(name = "Subfolders", description = "Goes to the specified subfolder")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Enhancer)

// Algorithm flow
@AlgorithmInputSlot(value = ACAQFoldersDataSlot.class, slotName = "Folders", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQFoldersDataSlot.class, slotName = "Subfolders", autoCreate = true)

// Traits
@AutoTransferTraits
public class ACAQSubFolder extends ACAQSimpleAlgorithm<ACAQFoldersData, ACAQFoldersData> {

    private String subFolder;

    public ACAQSubFolder(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ACAQSubFolder(ACAQSubFolder other) {
        super(other);
    }

    @Override
    public void run() {
        List<ACAQFolderData> fileNames = getInputData().getFolders();
        setOutputData(new ACAQFoldersData(fileNames.stream().map(p -> p.resolveToFolder(Paths.get(subFolder))).collect(Collectors.toList())));
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }

    @ACAQParameter("subfolder")
    @ACAQDocumentation(name = "Subfolder")
    public String getSubFolder() {
        return subFolder;
    }

    @ACAQParameter("subfolder")
    public void setSubFolder(String subFolder) {
        this.subFolder = subFolder;
    }
}
