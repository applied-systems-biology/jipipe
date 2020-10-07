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
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.FileSystemNodeTypeCategory;
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
@JIPipeOrganization(menuPath = "Filter", nodeTypeCategory = FileSystemNodeTypeCategory.class)


@JIPipeInputSlot(value = PathData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = PathData.class, slotName = "Output", autoCreate = true, inheritedSlot = "Input")

// Traits
public class FilterPaths extends JIPipeSimpleIteratingAlgorithm {

    //    private PathFilter filter = new PathFilter();
    private PathPredicate.List filters = new PathPredicate.List();
    private boolean filterOnlyNames = true;
    private boolean invert = false;
    private boolean outputFiles = true;
    private boolean outputFolders = true;
    private boolean outputNonExisting = true;

    /**
     * Instantiates the algorithm
     *
     * @param info Algorithm info
     */
    public FilterPaths(JIPipeNodeInfo info) {
        super(info);
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
        this.outputFiles = other.outputFiles;
        this.outputFolders = other.outputFolders;
        this.outputNonExisting = other.outputNonExisting;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        PathData inputData = dataBatch.getInputData(getFirstInputSlot(), PathData.class);
        JIPipeDataSlot firstOutputSlot = getFirstOutputSlot();
        Path inputPath = inputData.getPath();
        if (!canOutput(inputPath))
            return;
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
                        dataBatch.addOutputData(firstOutputSlot, inputData);
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
                    dataBatch.addOutputData(firstOutputSlot, inputData);
            }
        } else {
            dataBatch.addOutputData(firstOutputSlot, inputData);
        }
    }

    private boolean canOutput(Path data) {
        if (Files.isDirectory(data)) {
            return outputFolders;
        } else if (Files.exists(data)) {
            return outputFiles;
        } else {
            return outputNonExisting;
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

    @JIPipeDocumentation(name = "Output files", description = "If enabled, existing files are put into the output.")
    @JIPipeParameter("output-files")
    public boolean isOutputFiles() {
        return outputFiles;
    }

    @JIPipeParameter("output-files")
    public void setOutputFiles(boolean outputFiles) {
        this.outputFiles = outputFiles;
    }

    @JIPipeDocumentation(name = "Output folders", description = "If enabled, existing folders are put into the output.")
    @JIPipeParameter("output-folders")
    public boolean isOutputFolders() {
        return outputFolders;
    }

    @JIPipeParameter("output-folders")
    public void setOutputFolders(boolean outputFolders) {
        this.outputFolders = outputFolders;
    }

    @JIPipeDocumentation(name = "Output non-existing paths", description = "If enabled, non-existing paths are put into the output.")
    @JIPipeParameter("output-non-existing")
    public boolean isOutputNonExisting() {
        return outputNonExisting;
    }

    @JIPipeParameter("output-non-existing")
    public void setOutputNonExisting(boolean outputNonExisting) {
        this.outputNonExisting = outputNonExisting;
    }
}
