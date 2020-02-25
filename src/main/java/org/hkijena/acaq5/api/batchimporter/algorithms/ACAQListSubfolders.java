package org.hkijena.acaq5.api.batchimporter.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.batchimporter.dataslots.ACAQFolderDataSlot;
import org.hkijena.acaq5.api.batchimporter.dataslots.ACAQFoldersDataSlot;
import org.hkijena.acaq5.api.batchimporter.dataypes.ACAQFolderData;
import org.hkijena.acaq5.api.batchimporter.dataypes.ACAQFolderListData;
import org.hkijena.acaq5.api.batchimporter.dataypes.ACAQFoldersData;
import org.hkijena.acaq5.api.traits.AutoTransferTraits;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@ACAQDocumentation(name = "List subfolders", description = "Lists all subfolders")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.FileSystem)

// Algorithm flow
@AlgorithmInputSlot(value = ACAQFoldersDataSlot.class, slotName = "Folders", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQFoldersDataSlot.class, slotName = "Subfolders", autoCreate = true)

// Traits
@AutoTransferTraits
public class ACAQListSubfolders extends ACAQSimpleAlgorithm<ACAQFoldersData, ACAQFoldersData> {

    public ACAQListSubfolders(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ACAQListSubfolders(ACAQListSubfolders other) {
        super(other);
    }

    @Override
    public void run() {
        ACAQFoldersData inputFolders = getInputData();
        List<ACAQFolderData> result = new ArrayList<>();
        try {
            for (ACAQFolderData inputFolder : inputFolders.getFolders()) {
                for (Path path : Files.list(inputFolder.getFolderPath()).filter(Files::isDirectory).collect(Collectors.toList())) {
                    result.add(new ACAQFolderData(inputFolder, path));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        setOutputData(new ACAQFolderListData(result));
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}
