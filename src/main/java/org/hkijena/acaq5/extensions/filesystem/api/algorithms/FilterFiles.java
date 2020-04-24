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
import org.hkijena.acaq5.utils.PathFilter;
import org.hkijena.acaq5.utils.PathFilterCollection;

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
public class FilterFiles extends ACAQIteratingAlgorithm {

    //    private PathFilter filter = new PathFilter();
    private PathFilterCollection filters = new PathFilterCollection();

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
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        FileData inputData = dataInterface.getInputData("Files", FileData.class);
        ACAQDataSlot firstOutputSlot = getFirstOutputSlot();
        if (!filters.isEmpty()) {
            for (PathFilter filter : filters) {
                if (filter.test(inputData.getFilePath())) {
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

//    /**
//     * @return The filter
//     */
//    @ACAQParameter("filter")
//    @ACAQDocumentation(name = "Filter")
//    public PathFilter getFilter() {
//        return null;
//    }
//
//    /**
//     * Sets the filter.
//     * Cannot be null.
//     *
//     * @param filter The filter
//     */
//    @ACAQParameter("filter")
//    public void setFilter(PathFilter filter) {
//        this.filters.add(filter);
//        getEventBus().post(new ParameterChangedEvent(this, "filter"));
//    }

    @ACAQParameter("filters")
    @ACAQDocumentation(name = "Filters")
    public PathFilterCollection getFilters() {
        return filters;
    }

    @ACAQParameter("filters")
    public void setFilters(PathFilterCollection filters) {
        this.filters = filters;
        getEventBus().post(new ParameterChangedEvent(this, "filters"));
    }
}
