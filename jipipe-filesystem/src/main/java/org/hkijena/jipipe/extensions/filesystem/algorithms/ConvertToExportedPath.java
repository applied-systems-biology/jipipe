package org.hkijena.jipipe.extensions.filesystem.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DataExportExpressionParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

@JIPipeDocumentation(name = "Get export path", description = "Generates an export path for the input data and returns it as JIPipe path data")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = PathData.class, slotName = "Output", autoCreate = true)
@JIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class)
public class ConvertToExportedPath extends JIPipeSimpleIteratingAlgorithm {

    private DataExportExpressionParameter filePath = new DataExportExpressionParameter();

    public ConvertToExportedPath(JIPipeNodeInfo info) {
        super(info);
    }

    public ConvertToExportedPath(ConvertToExportedPath other) {
        super(other);
        this.filePath = new DataExportExpressionParameter(other.filePath);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        JIPipeData data = iterationStep.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo);

        Map<String, Path> projectDataDirs;
        if (getRuntimeProject() != null) {
            projectDataDirs = getRuntimeProject().getDirectoryMap();
        } else {
            projectDataDirs = Collections.emptyMap();
        }
        Path outputPath = filePath.generatePath(getFirstOutputSlot().getSlotStoragePath(),
                getProjectDirectory(),
                projectDataDirs,
                data.toString(),
                iterationStep.getInputRow(getFirstInputSlot()),
                new ArrayList<>(iterationStep.getMergedTextAnnotations().values()));

        iterationStep.addOutputData(getFirstOutputSlot(), new PathData(outputPath), progressInfo);
    }

    @JIPipeDocumentation(name = "File path", description = "Expression that generates the output file path")
    @JIPipeParameter("file-path")
    public DataExportExpressionParameter getFilePath() {
        return filePath;
    }

    @JIPipeParameter("file-path")
    public void setFilePath(DataExportExpressionParameter filePath) {
        this.filePath = filePath;
    }
}
