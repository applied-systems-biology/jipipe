package org.hkijena.acaq5.extensions.filesystem.api.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.FolderData;
import org.hkijena.acaq5.extensions.parameters.collections.PathFilterListParameter;

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
@ACAQOrganization(menuPath = "List", algorithmCategory = ACAQAlgorithmCategory.FileSystem)

// Algorithm flow
@AlgorithmInputSlot(value = FolderData.class, slotName = "Folders", autoCreate = true)
@AlgorithmOutputSlot(value = FolderData.class, slotName = "Subfolders", autoCreate = true)

// Traits
public class ListSubfolders extends ACAQIteratingAlgorithm {

    private PathFilterListParameter filters = new PathFilterListParameter();

    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public ListSubfolders(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public ListSubfolders(ListSubfolders other) {
        super(other);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        FolderData inputFolder = dataInterface.getInputData("Folders", FolderData.class);
        try {
            for (Path path : Files.list(inputFolder.getFolderPath()).filter(Files::isDirectory).collect(Collectors.toList())) {
                if (filters.isEmpty() || filters.stream().anyMatch(f -> f.test(path))) {
                    dataInterface.addOutputData("Subfolders", new FolderData(path));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Filters").report(filters);
    }

    @ACAQDocumentation(name = "Filters", description = "You can optionally filter the result folders. " +
            "The filters are connected via a logical OR operation. An empty list disables filtering")
    @ACAQParameter("filters")
    public PathFilterListParameter getFilters() {
        return filters;
    }

    @ACAQParameter("filters")
    public void setFilters(PathFilterListParameter filters) {
        this.filters = filters;
    }
}
