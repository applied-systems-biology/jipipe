package org.hkijena.acaq5.extensions.filesystem.api.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.FileData;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.FolderData;
import org.hkijena.acaq5.extensions.parameters.collections.PathFilterListParameter;
import org.hkijena.acaq5.extensions.parameters.filters.PathFilter;

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
@ACAQOrganization(menuPath = "List", algorithmCategory = ACAQAlgorithmCategory.FileSystem)

// Algorithm flow
@AlgorithmInputSlot(value = FolderData.class, slotName = "Folders", autoCreate = true)
@AlgorithmOutputSlot(value = FileData.class, slotName = "Files", autoCreate = true)

// Traits
public class ListFiles extends ACAQSimpleIteratingAlgorithm {

    private PathFilterListParameter filters = new PathFilterListParameter();
    private boolean filterOnlyFileNames = true;

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
        this.filterOnlyFileNames = other.filterOnlyFileNames;
        this.filters.clear();
        for (PathFilter filter : other.filters) {
            this.filters.add(new PathFilter(filter));
        }
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        FolderData inputFolder = dataInterface.getInputData("Folders", FolderData.class);
        try {
            for (Path file : Files.list(inputFolder.getPath()).filter(Files::isRegularFile).collect(Collectors.toList())) {
                Path testedFile;
                if (filterOnlyFileNames)
                    testedFile = file.getFileName();
                else
                    testedFile = file;
                if (filters.isEmpty() || filters.stream().anyMatch(f -> f.test(testedFile))) {
                    dataInterface.addOutputData("Files", new FileData(file));
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

    @ACAQDocumentation(name = "Filter only file names", description = "If enabled, the filter is only applied for the file name. If disabled, the filter is " +
            "applied for the absolute path. For non-existing paths it cannot bne guaranteed that the absolute path is tested.")
    @ACAQParameter("only-filenames")
    public boolean isFilterOnlyFileNames() {
        return filterOnlyFileNames;
    }

    @ACAQParameter("only-filenames")
    public void setFilterOnlyFileNames(boolean filterOnlyFileNames) {
        this.filterOnlyFileNames = filterOnlyFileNames;
    }
}
