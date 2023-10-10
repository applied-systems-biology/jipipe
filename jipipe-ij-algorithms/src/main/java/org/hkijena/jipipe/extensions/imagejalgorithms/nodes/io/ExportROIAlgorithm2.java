package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.io;

import ij.gui.Roi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.utils.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DataExportExpressionParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Path;
import java.util.*;

@JIPipeDocumentation(name = "Export ROI", description = "Exports a ROI list into one or multiple ROI files")
@JIPipeInputSlot(value = ROIListData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = FileData.class, slotName = "Exported file", autoCreate = true)
@JIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class, menuPath = "ROI")
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "File\nSave")
public class ExportROIAlgorithm2 extends JIPipeIteratingAlgorithm {
    private boolean exportAsROIFile = false;
    private DataExportExpressionParameter filePath = new DataExportExpressionParameter();

    public ExportROIAlgorithm2(JIPipeNodeInfo info) {
        super(info);
    }

    public ExportROIAlgorithm2(ExportROIAlgorithm2 other) {
        super(other);
        this.exportAsROIFile = other.exportAsROIFile;
        this.filePath = new DataExportExpressionParameter(other.filePath);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROIListData inputData = dataBatch.getInputData(getFirstInputSlot(), ROIListData.class, progressInfo);

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

        if (exportAsROIFile && inputData.size() > 1) {
            Set<String> existing = new HashSet<>();
            final String baseName = outputPath.getFileName().toString();
            for (Roi roi : inputData) {
                String roiName;
                if (!StringUtils.isNullOrEmpty(roi.getName())) {
                    roiName = StringUtils.makeUniqueString(baseName + "_" + roi.getName(), "_", existing);
                } else {
                    roiName = StringUtils.makeUniqueString(baseName, "_", existing);
                }
                Path path = PathUtils.ensureExtension(outputPath.getParent().resolve(roiName), ".roi");
                ROIListData.saveSingleRoi(roi, path);
                dataBatch.addOutputData(getFirstOutputSlot(), new FileData(path), progressInfo);
            }
        } else {
            outputPath = inputData.saveToRoiOrZip(outputPath);
            dataBatch.addOutputData(getFirstOutputSlot(), new FileData(outputPath), progressInfo);
        }
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

    @JIPipeDocumentation(name = "Force exporting *.roi files", description = "If true, the exporter will always export *.roi files and if necessary split the ROI list.")
    @JIPipeParameter("export-as-roi-file")
    public boolean isExportAsROIFile() {
        return exportAsROIFile;
    }

    @JIPipeParameter("export-as-roi-file")
    public void setExportAsROIFile(boolean exportAsROIFile) {
        this.exportAsROIFile = exportAsROIFile;
    }

}
