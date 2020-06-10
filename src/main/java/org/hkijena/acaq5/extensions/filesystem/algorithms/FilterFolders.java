package org.hkijena.acaq5.extensions.filesystem.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.filesystem.dataypes.FolderData;
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
public class FilterFolders extends ACAQSimpleIteratingAlgorithm {

    private PathFilterListParameter filters = new PathFilterListParameter();
    private boolean filterOnlyFolderNames = true;
    private boolean invert = false;

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
        this.invert = other.invert;
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
            if (!invert) {
                for (PathFilter filter : filters) {
                    if (filter.test(inputData.getPath())) {
                        dataInterface.addOutputData(firstOutputSlot, inputData);
                        break;
                    }
                }
            } else {
                boolean canPass = true;
                for (PathFilter filter : filters) {
                    if (filter.test(inputData.getPath())) {
                        canPass = false;
                        break;
                    }
                }
                if (canPass)
                    dataInterface.addOutputData(firstOutputSlot, inputData);
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

    @ACAQParameter("invert")
    @ACAQDocumentation(name = "Invert filter", description = "If true, the filter is inverted")
    public boolean isInvert() {
        return invert;
    }

    @ACAQParameter("invert")
    public void setInvert(boolean invert) {
        this.invert = invert;
        getEventBus().post(new ParameterChangedEvent(this, "invert"));
    }
}
