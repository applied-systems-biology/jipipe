package org.hkijena.acaq5.filesystem.api.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.filesystem.api.dataypes.ACAQFolderData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

@ACAQDocumentation(name = "List subfolders", description = "Lists all subfolders")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.FileSystem)

// Algorithm flow
@AlgorithmInputSlot(value = ACAQFolderData.class, slotName = "Folders", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQFolderData.class, slotName = "Subfolders", autoCreate = true)

// Traits
public class ACAQListSubfolders extends ACAQIteratingAlgorithm {

    public ACAQListSubfolders(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ACAQListSubfolders(ACAQListSubfolders other) {
        super(other);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface) {
        ACAQFolderData inputFolder = dataInterface.getInputData("Folders");
        try {
            for (Path path : Files.list(inputFolder.getFolderPath()).filter(Files::isDirectory).collect(Collectors.toList())) {
                dataInterface.addOutputData("Subfolders", new ACAQFolderData(path));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}
