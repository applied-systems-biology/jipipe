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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.io;

import ij.gui.Roi;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.desktop.api.nodes.AddJIPipeDesktopNodeQuickAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.plugins.expressions.DataExportExpressionParameter;
import org.hkijena.jipipe.plugins.filesystem.dataypes.FileData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.utils.PathType;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import java.nio.file.Path;
import java.util.*;

@SetJIPipeDocumentation(name = "Export ROI", description = "Exports a ROI list into one or multiple ROI files")
@AddJIPipeInputSlot(value = ROIListData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = FileData.class, name = "Exported file", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class, menuPath = "ROI")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "File\nSave")
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROIListData inputData = iterationStep.getInputData(getFirstInputSlot(), ROIListData.class, progressInfo);

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
                iterationStep.getInputRow(getFirstInputSlot()),
                new ArrayList<>(iterationStep.getMergedTextAnnotations().values()));
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
                iterationStep.addOutputData(getFirstOutputSlot(), new FileData(path), progressInfo);
            }
        } else {
            outputPath = inputData.saveToRoiOrZip(outputPath);
            iterationStep.addOutputData(getFirstOutputSlot(), new FileData(outputPath), progressInfo);
        }
    }

    @SetJIPipeDocumentation(name = "File path", description = "Expression that generates the output file path")
    @JIPipeParameter("file-path")
    public DataExportExpressionParameter getFilePath() {
        return filePath;
    }

    @JIPipeParameter("file-path")
    public void setFilePath(DataExportExpressionParameter filePath) {
        this.filePath = filePath;
    }

    @SetJIPipeDocumentation(name = "Force exporting *.roi files", description = "If true, the exporter will always export *.roi files and if necessary split the ROI list.")
    @JIPipeParameter("export-as-roi-file")
    public boolean isExportAsROIFile() {
        return exportAsROIFile;
    }

    @JIPipeParameter("export-as-roi-file")
    public void setExportAsROIFile(boolean exportAsROIFile) {
        this.exportAsROIFile = exportAsROIFile;
    }

    @AddJIPipeDesktopNodeQuickAction(name = "Configure exported path", description = "Selects where the data should be exported", icon = "actions/document-export.png", buttonIcon = "actions/color-select.png", buttonText = "Select")
    public void selectFilePathDesktopQuickAction(JIPipeDesktopGraphCanvasUI canvasUI) {
        DataExportExpressionParameter result = DataExportExpressionParameter.showPathChooser(canvasUI.getDesktopWorkbench().getWindow(),
                canvasUI.getWorkbench(),
                "Select output file",
                PathType.FilesOnly,
                UIUtils.EXTENSION_FILTER_ROIS);
        if(result != null) {
            setFilePath(result);
            emitParameterChangedEvent("file-path");
        }
    }

}
