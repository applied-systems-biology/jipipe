package org.hkijena.acaq5.extensions.filesystem.api.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.FileData;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.FolderData;

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
@AlgorithmInputSlot(value = FolderData.class, slotName = "Folders", autoCreate = true)
@AlgorithmOutputSlot(value = FileData.class, slotName = "Files", autoCreate = true)

// Traits
public class ListFiles extends ACAQIteratingAlgorithm {

    /**
     * Creates new instance
     *
     * @param declaration The declaration
     */
    public ListFiles(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public ListFiles(ListFiles other) {
        super(other);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        FolderData inputFolder = dataInterface.getInputData("Folders");
        try {
            for (Path file : Files.list(inputFolder.getFolderPath()).filter(Files::isRegularFile).collect(Collectors.toList())) {
                dataInterface.addOutputData("Files", new FileData(file));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}
