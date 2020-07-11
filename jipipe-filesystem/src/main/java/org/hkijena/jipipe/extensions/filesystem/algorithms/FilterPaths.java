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

package org.hkijena.jipipe.extensions.filesystem.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.extensions.parameters.predicates.PathPredicate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Filters input files
 */
@JIPipeDocumentation(name = "Filter paths", description = "Filters the paths (files/folders) by their name or absolute path")
@JIPipeOrganization(menuPath = "Filter", algorithmCategory = JIPipeAlgorithmCategory.FileSystem)

// Algorithm flow
@AlgorithmInputSlot(value = PathData.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = PathData.class, slotName = "Output", autoCreate = true, inheritedSlot = "Input")

// Traits
public class FilterPaths extends JIPipeSimpleIteratingAlgorithm {

    //    private PathFilter filter = new PathFilter();
    private PathPredicate.List filters = new PathPredicate.List();
    private boolean filterOnlyNames = true;
    private boolean invert = false;

    /**
     * Instantiates the algorithm
     *
     * @param declaration Algorithm declaration
     */
    public FilterPaths(JIPipeAlgorithmDeclaration declaration) {
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
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        PathData inputData = dataInterface.getInputData(getFirstInputSlot(), PathData.class);
        JIPipeDataSlot firstOutputSlot = getFirstOutputSlot();
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
    public void reportValidity(JIPipeValidityReport report) {
        for (int i = 0; i < filters.size(); ++i) {
            report.forCategory("Filter").forCategory("Item " + (i + 1)).report(filters.get(i));
        }
    }

    @JIPipeParameter("filters")
    @JIPipeDocumentation(name = "Filters")
    public PathPredicate.List getFilters() {
        return filters;
    }

    @JIPipeParameter("filters")
    public void setFilters(PathPredicate.List filters) {
        this.filters = filters;

    }

    @JIPipeDocumentation(name = "Filter only names", description = "If enabled, the filter is only applied for the path name (file or directory name). If disabled, the filter is " +
            "applied for the absolute path. For non-existing paths it cannot bne guaranteed that the absolute path is tested.")
    @JIPipeParameter("only-names")
    public boolean isFilterOnlyNames() {
        return filterOnlyNames;
    }

    @JIPipeParameter("only-names")
    public void setFilterOnlyNames(boolean filterOnlyNames) {
        this.filterOnlyNames = filterOnlyNames;

    }

    @JIPipeParameter("invert")
    @JIPipeDocumentation(name = "Invert filter", description = "If true, the filter is inverted")
    public boolean isInvert() {
        return invert;
    }

    @JIPipeParameter("invert")
    public void setInvert(boolean invert) {
        this.invert = invert;

    }
}
