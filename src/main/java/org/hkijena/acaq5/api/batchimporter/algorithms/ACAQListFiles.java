package org.hkijena.acaq5.api.batchimporter.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.batchimporter.dataslots.ACAQFilesDataSlot;
import org.hkijena.acaq5.api.batchimporter.dataslots.ACAQFolderDataSlot;
import org.hkijena.acaq5.api.batchimporter.dataypes.ACAQFilesData;
import org.hkijena.acaq5.api.batchimporter.dataypes.ACAQFolderData;
import org.hkijena.acaq5.api.traits.AutoTransferTraits;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

@ACAQDocumentation(name = "List files", description = "Lists all files in the input folder")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Converter)

// Algorithm flow
@AlgorithmInputSlot(value = ACAQFolderDataSlot.class, slotName = "Folder", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQFilesDataSlot.class, slotName = "Files", autoCreate = true)

// Traits
@AutoTransferTraits
public class ACAQListFiles extends ACAQSimpleAlgorithm<ACAQFolderData, ACAQFilesData> {

    public ACAQListFiles(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ACAQListFiles(ACAQListFiles other) {
        super(other);
    }

    @Override
    public void run() {
        Path folderPath = getInputData().getFolderPath();
        try {
            setOutputData(new ACAQFilesData(Files.list(folderPath).filter(Files::isRegularFile).collect(Collectors.toList())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}
