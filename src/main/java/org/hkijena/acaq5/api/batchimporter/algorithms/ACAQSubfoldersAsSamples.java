package org.hkijena.acaq5.api.batchimporter.algorithms;

import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.batchimporter.dataslots.ACAQFilesDataSlot;
import org.hkijena.acaq5.api.batchimporter.dataslots.ACAQFoldersDataSlot;
import org.hkijena.acaq5.api.batchimporter.dataypes.ACAQFolderData;
import org.hkijena.acaq5.api.batchimporter.dataypes.ACAQFoldersData;
import org.hkijena.acaq5.api.batchimporter.traits.ProjectSampleTrait;
import org.hkijena.acaq5.api.traits.AddsTrait;
import org.hkijena.acaq5.api.traits.AutoTransferTraits;
import org.hkijena.acaq5.api.traits.BadForTrait;

@ACAQDocumentation(name = "Subfolders as samples", description = "Lists all subfolders of the input folder, and marks them as samples")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.FileSystem, visibility = ACAQAlgorithmVisibility.BatchImporter)

// Algorithm flow
@AlgorithmInputSlot(value = ACAQFoldersDataSlot.class, slotName = "Folder", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQFoldersDataSlot.class, slotName = "Sample folders", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQFilesDataSlot.class, slotName = "Sample files", autoCreate = true)

// Traits
@AutoTransferTraits
@AddsTrait(ProjectSampleTrait.class)
@BadForTrait(ProjectSampleTrait.class)

public class ACAQSubfoldersAsSamples extends ACAQAlgorithm {

    public ACAQSubfoldersAsSamples(ACAQAlgorithmDeclaration declaration) {
        super(declaration, null, null);
    }

    public ACAQSubfoldersAsSamples(ACAQSubfoldersAsSamples other) {
        super(other);
    }

    @Override
    public void run() {
        ACAQFoldersData data = (ACAQFoldersData) getInputSlots().get(0).getData();

        ACAQListSubfolders listSubfoldersOperation = (ACAQListSubfolders) ACAQRegistryService.getInstance().getAlgorithmRegistry()
                .getDefaultDeclarationFor(ACAQListSubfolders.class).newInstance();
        listSubfoldersOperation.setInputData(data);
        listSubfoldersOperation.run();

        ACAQListFiles listFilesOperation = (ACAQListFiles) ACAQRegistryService.getInstance().getAlgorithmRegistry()
                .getDefaultDeclarationFor(ACAQListFiles.class).newInstance();
        listFilesOperation.setInputData(listSubfoldersOperation.getOutputData());
        listFilesOperation.run();

        for (ACAQFolderData folder : listSubfoldersOperation.getOutputData().getFolders()) {
            folder.annotate(ProjectSampleTrait.FILESYSTEM_ANNOTATION_SAMPLE, folder.getFolderPath().getFileName().toString());
        }

        getSlots().get("Sample folders").setData(listSubfoldersOperation.getOutputData());
        getSlots().get("Sample files").setData(listFilesOperation.getOutputData());
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}
