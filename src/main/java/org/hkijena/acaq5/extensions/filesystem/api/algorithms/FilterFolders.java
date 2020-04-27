package org.hkijena.acaq5.extensions.filesystem.api.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.PathFilter;
import org.hkijena.acaq5.api.parameters.PathFilterCollection;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.FolderData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that filters folders
 */
@ACAQDocumentation(name = "Filter folders", description = "Filters the input folders by their name")
@ACAQOrganization(menuPath = "Filter", algorithmCategory = ACAQAlgorithmCategory.FileSystem)

// Algorithm flow
@AlgorithmInputSlot(value = FolderData.class, slotName = "Folders", autoCreate = true)
@AlgorithmOutputSlot(value = FolderData.class, slotName = "Filtered folders", autoCreate = true)

// Traits
public class FilterFolders extends ACAQIteratingAlgorithm {

    private PathFilterCollection filters = new PathFilterCollection();

    /**
     * Initializes the algorithm
     *
     * @param declaration Algorithm declaration
     */
    public FilterFolders(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
        filters.addNewInstance();
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public FilterFolders(FilterFolders other) {
        super(other);
        this.filters.clear();
        for (PathFilter filter : other.filters) {
            this.filters.add(new PathFilter(filter));
        }
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        FolderData inputData = dataInterface.getInputData("Folders", FolderData.class);
        ACAQDataSlot firstOutputSlot = getFirstOutputSlot();
        if (!filters.isEmpty()) {
            for (PathFilter filter : filters) {
                if (filter.test(inputData.getFolderPath())) {
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
    public PathFilterCollection getFilters() {
        return filters;
    }

    @ACAQParameter("filters")
    public void setFilters(PathFilterCollection filters) {
        this.filters = filters;
        getEventBus().post(new ParameterChangedEvent(this, "filters"));
    }
}
