package org.hkijena.acaq5.extensions.filesystem.api.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.ACAQFolderData;
import org.hkijena.acaq5.utils.PathFilter;
import org.hkijena.acaq5.utils.PathFilterCollection;

/**
 * Algorithm that filters folders
 */
@ACAQDocumentation(name = "Filter folders", description = "Filters the input folders by their name")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.FileSystem)

// Algorithm flow
@AlgorithmInputSlot(value = ACAQFolderData.class, slotName = "Folders", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQFolderData.class, slotName = "Filtered folders", autoCreate = true)

// Traits
public class ACAQFilterFolders extends ACAQIteratingAlgorithm {

    private PathFilterCollection filters = new PathFilterCollection();

    /**
     * Initializes the algorithm
     *
     * @param declaration Algorithm declaration
     */
    public ACAQFilterFolders(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
        filters.addNewInstance();
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public ACAQFilterFolders(ACAQFilterFolders other) {
        super(other);
        this.filters.clear();
        for (PathFilter filter : other.filters) {
            this.filters.add(new PathFilter(filter));
        }
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface) {
        ACAQFolderData inputData = dataInterface.getInputData("Folders");
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
