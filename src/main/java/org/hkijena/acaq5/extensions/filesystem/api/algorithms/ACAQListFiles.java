package org.hkijena.acaq5.extensions.filesystem.api.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.ACAQFileData;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.ACAQFolderData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Algorithm that lists files in each folder
 */
@ACAQDocumentation(name = "List files", description = "Lists all files in the input folder")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.FileSystem)
@ACAQOrganization(menuPath = "List")

// Algorithm flow
@AlgorithmInputSlot(value = ACAQFolderData.class, slotName = "Folders", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQFileData.class, slotName = "Files", autoCreate = true)

// Traits
public class ACAQListFiles extends ACAQIteratingAlgorithm {

    /**
     * Creates new instance
     *
     * @param declaration The declaration
     */
    public ACAQListFiles(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public ACAQListFiles(ACAQListFiles other) {
        super(other);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
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
