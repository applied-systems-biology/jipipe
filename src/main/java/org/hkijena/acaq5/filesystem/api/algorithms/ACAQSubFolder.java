package org.hkijena.acaq5.filesystem.api.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.traits.AutoTransferTraits;
import org.hkijena.acaq5.filesystem.api.dataypes.ACAQFolderData;

@ACAQDocumentation(name = "Subfolders", description = "Goes to the specified subfolder")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.FileSystem)

// Algorithm flow
@AlgorithmInputSlot(value = ACAQFolderData.class, slotName = "Folders", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQFolderData.class, slotName = "Subfolders", autoCreate = true)

// Traits
@AutoTransferTraits
public class ACAQSubFolder extends ACAQIteratingAlgorithm {

    private String subFolder;

    public ACAQSubFolder(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ACAQSubFolder(ACAQSubFolder other) {
        super(other);
        this.subFolder = other.subFolder;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface) {
        ACAQFolderData inputFolder = dataInterface.getInputData("Folders");
        dataInterface.addOutputData("Subfolders", new ACAQFolderData(inputFolder.getFolderPath().resolve(subFolder)));
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (subFolder == null || subFolder.isEmpty())
            report.forCategory("Subfolder name").reportIsInvalid("The subfolder name is empty! Please enter a name.");
    }

    @ACAQParameter("subfolder")
    @ACAQDocumentation(name = "Subfolder name")
    public String getSubFolder() {
        return subFolder;
    }

    @ACAQParameter("subfolder")
    public void setSubFolder(String subFolder) {
        this.subFolder = subFolder;
    }
}
