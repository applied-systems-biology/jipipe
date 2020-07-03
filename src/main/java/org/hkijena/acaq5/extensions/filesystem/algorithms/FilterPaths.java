/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.acaq5.extensions.filesystem.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDataInterface;
import org.hkijena.acaq5.api.algorithm.ACAQSimpleIteratingAlgorithm;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.filesystem.dataypes.PathData;
import org.hkijena.acaq5.extensions.parameters.predicates.PathPredicate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Filters input files
 */
@ACAQDocumentation(name = "Filter paths", description = "Filters the paths (files/folders) by their name or absolute path")
@ACAQOrganization(menuPath = "Filter", algorithmCategory = ACAQAlgorithmCategory.FileSystem)

// Algorithm flow
@AlgorithmInputSlot(value = PathData.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = PathData.class, slotName = "Output", autoCreate = true, inheritedSlot = "Input")

// Traits
public class FilterPaths extends ACAQSimpleIteratingAlgorithm {

    //    private PathFilter filter = new PathFilter();
    private PathPredicate.List filters = new PathPredicate.List();
    private boolean filterOnlyNames = true;
    private boolean invert = false;

    /**
     * Instantiates the algorithm
     *
     * @param declaration Algorithm declaration
     */
    public FilterPaths(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
        filters.addNewInstance();
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public FilterPaths(FilterPaths other) {
        super(other);
        this.filters.clear();
        for (PathPredicate filter : other.filters) {
            this.filters.add(new PathPredicate(filter));
        }
        this.filterOnlyNames = other.filterOnlyNames;
        this.invert = other.invert;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        PathData inputData = dataInterface.getInputData(getFirstInputSlot(), PathData.class);
        ACAQDataSlot firstOutputSlot = getFirstOutputSlot();
        Path inputPath = inputData.getPath();
        if (filterOnlyNames)
            inputPath = inputPath.getFileName();
        else {
            if (Files.exists(inputPath)) {
                inputPath = inputPath.toAbsolutePath();
            }
        }
        if (!filters.isEmpty()) {
            if (!invert) {
                for (PathPredicate filter : filters) {
                    if (filter.test(inputPath)) {
                        dataInterface.addOutputData(firstOutputSlot, inputData);
                        break;
                    }
                }
            } else {
                boolean canPass = true;
                for (PathPredicate filter : filters) {
                    if (filter.test(inputPath)) {
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
    public PathPredicate.List getFilters() {
        return filters;
    }

    @ACAQParameter("filters")
    public void setFilters(PathPredicate.List filters) {
        this.filters = filters;

    }

    @ACAQDocumentation(name = "Filter only names", description = "If enabled, the filter is only applied for the path name (file or directory name). If disabled, the filter is " +
            "applied for the absolute path. For non-existing paths it cannot bne guaranteed that the absolute path is tested.")
    @ACAQParameter("only-names")
    public boolean isFilterOnlyNames() {
        return filterOnlyNames;
    }

    @ACAQParameter("only-names")
    public void setFilterOnlyNames(boolean filterOnlyNames) {
        this.filterOnlyNames = filterOnlyNames;

    }

    @ACAQParameter("invert")
    @ACAQDocumentation(name = "Invert filter", description = "If true, the filter is inverted")
    public boolean isInvert() {
        return invert;
    }

    @ACAQParameter("invert")
    public void setInvert(boolean invert) {
        this.invert = invert;

    }
}
