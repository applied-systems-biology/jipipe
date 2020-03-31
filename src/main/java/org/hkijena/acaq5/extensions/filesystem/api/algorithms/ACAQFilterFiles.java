package org.hkijena.acaq5.extensions.filesystem.api.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.ACAQFileData;
import org.hkijena.acaq5.utils.PathFilter;

/**
 * Filters input files
 */
@ACAQDocumentation(name = "Filter files", description = "Filters the input files by their name")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.FileSystem)

// Algorithm flow
@AlgorithmInputSlot(value = ACAQFileData.class, slotName = "Files", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQFileData.class, slotName = "Filtered files", autoCreate = true)

// Traits
public class ACAQFilterFiles extends ACAQIteratingAlgorithm {

    private PathFilter filter = new PathFilter();

    /**
     * Instantiates the algorithm
     * @param declaration Algorithm declaration
     */
    public ACAQFilterFiles(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Copies the algorithm
     * @param other The original
     */
    public ACAQFilterFiles(ACAQFilterFiles other) {
        super(other);
        this.filter = new PathFilter(other.filter);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface) {
        ACAQFileData inputData = dataInterface.getInputData("Files");
        if (filter.test(inputData.getFilePath())) {
            dataInterface.addOutputData("Filtered files", inputData);
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
     * Sets the filter.
     * Cannot be null.
     * @param filter The filter
     */
    @ACAQParameter("filter")
    public void setFilter(PathFilter filter) {
        this.filter = filter;
        getEventBus().post(new ParameterChangedEvent(this, "filter"));
    }
}
