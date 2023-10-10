package org.hkijena.jipipe.extensions.ij3d.nodes.roi3d;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeSingleDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DataExportExpressionParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.utils.PathUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

@JIPipeDocumentation(name = "Export 3D ROI", description = "Exports a 3D ROI list into one or multiple ROI files")
@JIPipeInputSlot(value = ROI3DListData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = FileData.class, slotName = "Exported file", autoCreate = true)
@JIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class, menuPath = "ROI")
public class ExportROI3DAlgorithm2 extends JIPipeIteratingAlgorithm {

    private DataExportExpressionParameter filePath = new DataExportExpressionParameter();

    public ExportROI3DAlgorithm2(JIPipeNodeInfo info) {
        super(info);
    }

    public ExportROI3DAlgorithm2(ExportROI3DAlgorithm2 other) {
        super(other);
        this.filePath = new DataExportExpressionParameter(other.filePath);
    }

    @Override
    protected void runIteration(JIPipeSingleDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROI3DListData inputData = dataBatch.getInputData(getFirstInputSlot(), ROI3DListData.class, progressInfo);

        Map<String, Path> projectDataDirs;
        if (getRuntimeProject() != null) {
            projectDataDirs = getRuntimeProject().getDirectoryMap();
        } else {
            projectDataDirs = Collections.emptyMap();
        }
        Path outputPath = filePath.generatePath(getFirstOutputSlot().getSlotStoragePath(),
                getProjectDirectory(),
                projectDataDirs,
                inputData.toString(),
                dataBatch.getInputRow(getFirstInputSlot()),
                new ArrayList<>(dataBatch.getMergedTextAnnotations().values()));


        PathUtils.ensureParentDirectoriesExist(outputPath);
        ROI3DListData rois = dataBatch.getInputData(getFirstInputSlot(), ROI3DListData.class, progressInfo);

        rois.save(outputPath);
        dataBatch.addOutputData(getFirstOutputSlot(), new FileData(outputPath), progressInfo);
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
