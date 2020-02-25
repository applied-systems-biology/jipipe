package org.hkijena.acaq5.api.batchimporter.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.batchimporter.dataslots.ACAQFolderDataSlot;
import org.hkijena.acaq5.api.batchimporter.dataslots.ACAQFoldersDataSlot;
import org.hkijena.acaq5.api.batchimporter.dataypes.ACAQFolderData;
import org.hkijena.acaq5.api.batchimporter.dataypes.ACAQFoldersData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

@ACAQDocumentation(name = "List subfolders", description = "Lists all subfolders")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Converter)

// Algorithm flow
@AlgorithmInputSlot(value = ACAQFolderDataSlot.class, slotName = "Folder", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQFoldersDataSlot.class, slotName = "Subfolders", autoCreate = true)
public class ACAQListSubfolders extends ACAQSimpleAlgorithm<ACAQFolderData, ACAQFoldersData> {

    public ACAQListSubfolders(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ACAQListSubfolders(ACAQListSubfolders other) {
        super(other);
    }

    @Override
    public void run() {
        Path folderPath = getInputData().getFolderPath();
        try {
            setOutputData(new ACAQFoldersData(Files.list(folderPath).filter(Files::isDirectory).collect(Collectors.toList())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}
