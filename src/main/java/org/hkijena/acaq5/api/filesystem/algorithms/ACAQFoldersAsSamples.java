package org.hkijena.acaq5.api.filesystem.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.filesystem.dataslots.ACAQFoldersDataSlot;
import org.hkijena.acaq5.api.filesystem.dataypes.ACAQFolderData;
import org.hkijena.acaq5.api.filesystem.dataypes.ACAQFoldersData;
import org.hkijena.acaq5.api.filesystem.traits.ProjectSampleTrait;
import org.hkijena.acaq5.api.traits.AddsTrait;
import org.hkijena.acaq5.api.traits.AutoTransferTraits;
import org.hkijena.acaq5.api.traits.BadForTrait;

@ACAQDocumentation(name = "Folders as samples", description = "Makes that each individual folder is seen as sample")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.FileSystem)

// Algorithm flow
@AlgorithmInputSlot(value = ACAQFoldersDataSlot.class, slotName = "Folders", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQFoldersDataSlot.class, slotName = "Sample folders", autoCreate = true)

// Traits
@AutoTransferTraits
@AddsTrait(ProjectSampleTrait.class)
@BadForTrait(ProjectSampleTrait.class)

public class ACAQFoldersAsSamples extends ACAQSimpleAlgorithm<ACAQFoldersData, ACAQFoldersData> {

    public ACAQFoldersAsSamples(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ACAQFoldersAsSamples(ACAQFoldersAsSamples other) {
        super(other);
    }

    @Override
    public void run() {
        ACAQFoldersData data = getInputData();
        for (ACAQFolderData folder : data.getFolders()) {
            folder.annotate(ProjectSampleTrait.FILESYSTEM_ANNOTATION_SAMPLE, folder.getFolderPath().getFileName().toString());
        }
        setOutputData(data);
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}
