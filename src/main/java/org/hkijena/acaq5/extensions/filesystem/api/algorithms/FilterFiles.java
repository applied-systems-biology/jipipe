package org.hkijena.acaq5.extensions.filesystem.api.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.FileData;
import org.hkijena.acaq5.extensions.parameters.collections.PathFilterListParameter;
import org.hkijena.acaq5.extensions.parameters.filters.PathFilter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Filters input files
 */
@ACAQDocumentation(name = "Filter files", description = "Filters the input files by their name")
@ACAQOrganization(menuPath = "Filter", algorithmCategory = ACAQAlgorithmCategory.FileSystem)

// Algorithm flow
@AlgorithmInputSlot(value = FileData.class, slotName = "Files", autoCreate = true)
@AlgorithmOutputSlot(value = FileData.class, slotName = "Filtered files", autoCreate = true)

// Traits
public class FilterFiles extends ACAQSimpleIteratingAlgorithm {

    //    private PathFilter filter = new PathFilter();
    private PathFilterListParameter filters = new PathFilterListParameter();
    private boolean filterOnlyFileNames = true;

    /**
     * Instantiates the algorithm
     *
     * @param declaration Algorithm declaration
     */
    public FilterFiles(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
        filters.addNewInstance();
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public FilterFiles(FilterFiles other) {
        super(other);
        this.filters.clear();
        for (PathFilter filter : other.filters) {
            this.filters.add(new PathFilter(filter));
        }
        this.filterOnlyFileNames = other.filterOnlyFileNames;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        FileData inputData = dataInterface.getInputData("Files", FileData.class);
        ACAQDataSlot firstOutputSlot = getFirstOutputSlot();
        Path inputPath = inputData.getPath();
        if (filterOnlyFileNames)
            inputPath = inputPath.getFileName();
        else {
            if (Files.exists(inputPath)) {
                inputPath = inputPath.toAbsolutePath();
            }
        }
        if (!filters.isEmpty()) {
            for (PathFilter filter : filters) {
                if (filter.test(inputPath)) {
                    dataInterface.addOutputData(firstOutputSlot, inputData);
                    break;
                }
            }
        } else {
            dataInterface.addOutputData(firstOutputSlot, inputData);
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        for (int i = 0; i < filters.size(); ++i) {
            report.forCategory("Filter").forCategory("Item " + (i + 1)).report(filters.get(i));
        }
    }

    @ACAQParameter("filters")
    @ACAQDocumentation(name = "Filters")
    public PathFilterListParameter getFilters() {
        return filters;
    }

    @ACAQParameter("filters")
    public void setFilters(PathFilterListParameter filters) {
        this.filters = filters;
        getEventBus().post(new ParameterChangedEvent(this, "filters"));
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
