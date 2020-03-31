package org.hkijena.acaq5.extensions.filesystem.api.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.ACAQFolderData;
import org.hkijena.acaq5.utils.PathFilter;

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

    private PathFilter filter = new PathFilter();

    /**
     * Initializes the algorithm
     * @param declaration Algorithm declaration
     */
    public ACAQFilterFolders(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Copies the algorithm
     * @param other The original
     */
    public ACAQFilterFolders(ACAQFilterFolders other) {
        super(other);
        this.filter = new PathFilter(other.filter);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface) {
        ACAQFolderData inputData = dataInterface.getInputData("Folders");
        if (filter.test(inputData.getFolderPath())) {
            dataInterface.addOutputData("Filtered folders", inputData);
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Filter").report(filter);
    }

    /**
     * @return The filter
     */
    @ACAQParameter("filter")
    @ACAQDocumentation(name = "Filter")
    public PathFilter getFilter() {
        return filter;
    }

    /**
     * Sets the filter
     * @param filter The filter
     */
    @ACAQParameter("filter")
    public void setFilter(PathFilter filter) {
        this.filter = filter;
        getEventBus().post(new ParameterChangedEvent(this, "filter"));
    }
}
