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
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.FileSystemNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.PathQueryExpression;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Filters input files
 */
@JIPipeDocumentation(name = "Filter paths", description = "Filters the paths (files/folders) by their name or absolute path")
@JIPipeNode(menuPath = "Filter", nodeTypeCategory = FileSystemNodeTypeCategory.class)


@JIPipeInputSlot(value = PathData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = PathData.class, slotName = "Output", autoCreate = true, inheritedSlot = "Input")


public class FilterPaths extends JIPipeSimpleIteratingAlgorithm {

    //    private PathFilter filter = new PathFilter();
    private PathQueryExpression filters = new PathQueryExpression("");
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
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        // Expression parameters from annotations
        ExpressionVariables expressionVariables = new ExpressionVariables();
        for (JIPipeTextAnnotation annotation : dataBatch.getMergedAnnotations().values()) {
            expressionVariables.set(annotation.getName(), annotation.getValue());
        }

        PathData inputData = dataBatch.getInputData(getFirstInputSlot(), PathData.class, progressInfo);
        JIPipeDataSlot firstOutputSlot = getFirstOutputSlot();
        Path inputPath = inputData.toPath();
        if (!canOutput(inputPath))
            return;
        if (filters.test(inputPath, expressionVariables)) {
            dataBatch.addOutputData(firstOutputSlot, inputData, progressInfo);
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
    @JIPipeDocumentation(name = "Filters", description = "Filter expression that is used to filter the paths. Click [X] to see all available variables. " +
            "An example for an expression would be '\".tif\" IN name', which would test if there is '.tif' inside the file name. " +
            "Annotations are available as variables.")
    public PathQueryExpression getFilters() {
        return filters;
    }

    @JIPipeParameter("filters")
    public void setFilters(PathQueryExpression filters) {
        this.filters = filters;
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

    @JIPipeDocumentation(name = "Filter only *.tif", description = "Sets the filter, so only *.tif files are returned.")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/graduation-cap.png", iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/graduation-cap.png")
    public void setToExample(JIPipeWorkbench parent) {
        if (UIUtils.confirmResetParameters(parent, "Load example")) {
            setFilters(new PathQueryExpression("STRING_MATCHES_GLOB(name, \"*.tif\")"));
            getEventBus().post(new ParameterChangedEvent(this, "filters"));
        }
    }
}
