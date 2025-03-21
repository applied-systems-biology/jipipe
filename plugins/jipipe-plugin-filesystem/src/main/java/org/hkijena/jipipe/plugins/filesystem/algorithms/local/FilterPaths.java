/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.filesystem.algorithms.local;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.FileSystemNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.PathQueryExpression;
import org.hkijena.jipipe.plugins.filesystem.dataypes.PathData;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Filters input files
 */
@SetJIPipeDocumentation(name = "Filter paths", description = "Filters the paths (files/folders) by their name or absolute path")
@ConfigureJIPipeNode(menuPath = "Filter", nodeTypeCategory = FileSystemNodeTypeCategory.class)


@AddJIPipeInputSlot(value = PathData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = PathData.class, name = "Output", create = true)


public class FilterPaths extends JIPipeSimpleIteratingAlgorithm {

    //    private PathFilter filter = new PathFilter();
    private PathQueryExpression filters = new PathQueryExpression("");
    private boolean outputFiles = true;
    private boolean outputFolders = true;
    private boolean outputNonExisting = true;
    private boolean enableFilter = true;

    /**
     * Instantiates the algorithm
     *
     * @param info Algorithm info
     */
    public FilterPaths(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public FilterPaths(FilterPaths other) {
        super(other);
        this.outputFiles = other.outputFiles;
        this.outputFolders = other.outputFolders;
        this.outputNonExisting = other.outputNonExisting;
        this.filters = new PathQueryExpression(other.filters);
        this.enableFilter = other.enableFilter;
    }

    @SetJIPipeDocumentation(name = "Enable filter", description = "Determines if the filter is enabled")
    @JIPipeParameter(value = "enable-filter", important = true)
    public boolean isEnableFilter() {
        return enableFilter;
    }

    @JIPipeParameter("enable-filter")
    public void setEnableFilter(boolean enableFilter) {
        this.enableFilter = enableFilter;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        // Expression parameters from annotations
        JIPipeExpressionVariablesMap expressionVariables = new JIPipeExpressionVariablesMap(iterationStep);
        for (JIPipeTextAnnotation annotation : iterationStep.getMergedTextAnnotations().values()) {
            expressionVariables.set(annotation.getName(), annotation.getValue());
        }

        PathData inputData = iterationStep.getInputData(getFirstInputSlot(), PathData.class, progressInfo);
        JIPipeOutputDataSlot firstOutputSlot = getFirstOutputSlot();
        Path inputPath = inputData.toPath();
        if (!canOutput(inputPath)) {
            return;
        }
        if (!enableFilter || filters.test(inputPath, expressionVariables)) {
            iterationStep.addOutputData(firstOutputSlot, inputData, progressInfo);
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

    @JIPipeParameter("filters")
    @SetJIPipeDocumentation(name = "Keep path if ...", description = "Filter expression that is used to filter the paths. Click [X] to see all available variables. " +
            "An example for an expression would be '\".tif\" IN name', which would test if there is '.tif' inside the file name. " +
            "Annotations are available as variables.")
    public PathQueryExpression getFilters() {
        return filters;
    }

    @JIPipeParameter("filters")
    public void setFilters(PathQueryExpression filters) {
        this.filters = filters;
    }

    @SetJIPipeDocumentation(name = "Output files", description = "If enabled, existing files are put into the output.")
    @JIPipeParameter("output-files")
    public boolean isOutputFiles() {
        return outputFiles;
    }

    @JIPipeParameter("output-files")
    public void setOutputFiles(boolean outputFiles) {
        this.outputFiles = outputFiles;
    }

    @SetJIPipeDocumentation(name = "Output folders", description = "If enabled, existing folders are put into the output.")
    @JIPipeParameter("output-folders")
    public boolean isOutputFolders() {
        return outputFolders;
    }

    @JIPipeParameter("output-folders")
    public void setOutputFolders(boolean outputFolders) {
        this.outputFolders = outputFolders;
    }

    @SetJIPipeDocumentation(name = "Output non-existing paths", description = "If enabled, non-existing paths are put into the output.")
    @JIPipeParameter("output-non-existing")
    public boolean isOutputNonExisting() {
        return outputNonExisting;
    }

    @JIPipeParameter("output-non-existing")
    public void setOutputNonExisting(boolean outputNonExisting) {
        this.outputNonExisting = outputNonExisting;
    }
}
