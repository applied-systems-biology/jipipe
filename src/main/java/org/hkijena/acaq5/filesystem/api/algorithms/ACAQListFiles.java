package org.hkijena.acaq5.filesystem.api.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.traits.AutoTransferTraits;
import org.hkijena.acaq5.filesystem.api.dataypes.ACAQFileData;
import org.hkijena.acaq5.filesystem.api.dataypes.ACAQFolderData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

@ACAQDocumentation(name = "List files", description = "Lists all files in the input folder")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.FileSystem)

// Algorithm flow
@AlgorithmInputSlot(value = ACAQFolderData.class, slotName = "Folders", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQFileData.class, slotName = "Files", autoCreate = true)

// Traits
@AutoTransferTraits
public class ACAQListFiles extends ACAQIteratingAlgorithm {

    public ACAQListFiles(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ACAQListFiles(ACAQListFiles other) {
        super(other);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface) {
        ACAQFolderData inputFolder = dataInterface.getInputData("Folders");
        try {
            for (Path file : Files.list(inputFolder.getFolderPath()).filter(Files::isRegularFile).collect(Collectors.toList())) {
                dataInterface.addOutputData("Files", new ACAQFileData(file));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}
