package org.hkijena.acaq5.api.filesystem.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.filesystem.dataslots.ACAQFilesDataSlot;
import org.hkijena.acaq5.api.filesystem.dataslots.ACAQFoldersDataSlot;
import org.hkijena.acaq5.api.filesystem.dataypes.*;
import org.hkijena.acaq5.api.traits.AutoTransferTraits;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@ACAQDocumentation(name = "List files", description = "Lists all files in the input folder")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.FileSystem)

// Algorithm flow
@AlgorithmInputSlot(value = ACAQFoldersDataSlot.class, slotName = "Folders", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQFilesDataSlot.class, slotName = "Files", autoCreate = true)

// Traits
@AutoTransferTraits
public class ACAQListFiles extends ACAQSimpleAlgorithm<ACAQFoldersData, ACAQFilesData> {

    public ACAQListFiles(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ACAQListFiles(ACAQListFiles other) {
        super(other);
    }

    @Override
    public void run() {
        ACAQFoldersData inputData = getInputData();
        List<ACAQFileData> result = new ArrayList<>();

        try {
            for(ACAQFolderData folder : inputData.getFolders()) {
                for(Path file : Files.list(folder.getFolderPath()).filter(Files::isRegularFile).collect(Collectors.toList())) {
                    result.add(new ACAQFileData(folder, file));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        setOutputData(new ACAQFileListData(result));
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}
