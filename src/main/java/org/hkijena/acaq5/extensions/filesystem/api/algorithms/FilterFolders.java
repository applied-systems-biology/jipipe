package org.hkijena.acaq5.extensions.filesystem.api.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDataInterface;
import org.hkijena.acaq5.api.algorithm.ACAQIteratingAlgorithm;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.FolderData;
import org.hkijena.acaq5.extensions.parameters.collections.PathFilterListParameter;
import org.hkijena.acaq5.extensions.parameters.filters.PathFilter;

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

    private PathFilterListParameter filters = new PathFilterListParameter();
    private boolean filterOnlyFolderNames = true;

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
        this.filterOnlyFolderNames = other.filterOnlyFolderNames;
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
                if (filter.test(inputData.getPath())) {
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

    @ACAQDocumentation(name = "Filter only folder names", description = "If enabled, the filter is only applied for the folder name. If disabled, the filter is " +
            "applied for the absolute path. For non-existing paths it cannot bne guaranteed that the absolute path is tested.")
    @ACAQParameter("only-filenames")
    public boolean isFilterOnlyFolderNames() {
        return filterOnlyFolderNames;
    }

    @ACAQParameter("only-filenames")
    public void setFilterOnlyFolderNames(boolean filterOnlyFolderNames) {
        this.filterOnlyFolderNames = filterOnlyFolderNames;
    }
}
