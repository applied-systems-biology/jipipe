package org.hkijena.acaq5.extensions.filesystem.api.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.ACAQFolderData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Algorithms that lists the sub folders for each input folder
 */
@ACAQDocumentation(name = "List subfolders", description = "Lists all subfolders")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.FileSystem)
@ACAQOrganization(menuPath = "List")

// Algorithm flow
@AlgorithmInputSlot(value = ACAQFolderData.class, slotName = "Folders", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQFolderData.class, slotName = "Subfolders", autoCreate = true)

// Traits
public class ACAQListSubfolders extends ACAQIteratingAlgorithm {

    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public ACAQListSubfolders(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public ACAQListSubfolders(ACAQListSubfolders other) {
        super(other);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
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
