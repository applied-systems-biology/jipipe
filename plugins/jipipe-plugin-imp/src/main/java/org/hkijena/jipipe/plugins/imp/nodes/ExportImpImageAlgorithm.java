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

package org.hkijena.jipipe.plugins.imp.nodes;

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
import org.hkijena.jipipe.plugins.imp.datatypes.ImpImageData;
import org.hkijena.jipipe.plugins.imp.datatypes.ImpImageOutputFormat;
import org.hkijena.jipipe.plugins.imp.utils.ImpImageUtils;
import org.hkijena.jipipe.utils.PathType;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

@SetJIPipeDocumentation(name = "Export IMP image", description = "Exports IMP images into a non-JIPipe format (PNG, JPEG, BMP, GIF).")
@AddJIPipeInputSlot(value = ImpImageData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = FileData.class, name = "Exported file", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class, menuPath = "Images")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "File\nSave")
public class ExportImpImageAlgorithm extends JIPipeIteratingAlgorithm {
    private ImpImageOutputFormat fileFormat = ImpImageOutputFormat.PNG;
    private DataExportExpressionParameter filePath = new DataExportExpressionParameter();

    public ExportImpImageAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExportImpImageAlgorithm(ExportImpImageAlgorithm other) {
        super(other);
        this.fileFormat = other.fileFormat;
        this.filePath = new DataExportExpressionParameter(other.filePath);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {

        ImpImageData inputData = iterationStep.getInputData(getFirstInputSlot(), ImpImageData.class, progressInfo);

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

        if (inputData.isHyperstack()) {
            ImpImageUtils.forEachIndexedZCTSlice(inputData, (ip, index) -> {
                Path outputFile = outputPath.getParent().resolve(outputPath.getFileName().toString() + "-z" + index.getZ() + "c" + index.getC() + "t" + index.getT());
                outputFile = PathUtils.ensureExtension(outputFile, fileFormat.getExtension(), fileFormat.getExtensions());
                progressInfo.log("Saving to " + outputFile);

                PathUtils.ensureParentDirectoriesExist(outputFile);
                try {
                    ImageIO.write(ip, fileFormat.getNativeValue(), outputFile.toFile());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                iterationStep.addOutputData(getFirstOutputSlot(), new FileData(outputFile), progressInfo);
            }, progressInfo);
        } else {
            BufferedImage image = inputData.getImage(0);
            Path outputFile = PathUtils.ensureExtension(outputPath, fileFormat.getExtension(), fileFormat.getExtensions());
            progressInfo.log("Saving to " + outputFile);
            PathUtils.ensureParentDirectoriesExist(outputFile);
            try {
                ImageIO.write(image, fileFormat.getNativeValue(), outputFile.toFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            iterationStep.addOutputData(getFirstOutputSlot(), new FileData(outputFile), progressInfo);
        }

    }

    @SetJIPipeDocumentation(name = "File format", description = "The file format that should be used. ")
    @JIPipeParameter(value = "file-format", uiOrder = -20)
    public ImpImageOutputFormat getFileFormat() {
        return fileFormat;
    }

    @JIPipeParameter("file-format")
    public void setFileFormat(ImpImageOutputFormat fileFormat) {
        this.fileFormat = fileFormat;
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

    @AddJIPipeDesktopNodeQuickAction(name = "Configure exported path", description = "Selects where the data should be exported", icon = "actions/document-export.png", buttonIcon = "actions/color-select.png", buttonText = "Select")
    public void selectFilePathDesktopQuickAction(JIPipeDesktopGraphCanvasUI canvasUI) {
        DataExportExpressionParameter result = DataExportExpressionParameter.showPathChooser(canvasUI.getDesktopWorkbench().getWindow(),
                canvasUI.getWorkbench(),
                "Select output file",
                PathType.FilesOnly,
                UIUtils.EXTENSION_FILTER_PNG,
                UIUtils.EXTENSION_FILTER_BMP,
                UIUtils.EXTENSION_FILTER_TIFF,
                UIUtils.EXTENSION_FILTER_AVI,
                UIUtils.EXTENSION_FILTER_JPEG);
        if (result != null) {
            setFilePath(result);
            emitParameterChangedEvent("file-path");

            // Also set the file format automatically
            String expression = result.getExpression();
            if (expression.contains(".png")) {
                setFileFormat(ImpImageOutputFormat.PNG);
                emitParameterChangedEvent("file-format");
                emitParameterUIChangedEvent();
            } else if (expression.contains(".bmp")) {
                setFileFormat(ImpImageOutputFormat.BMP);
                emitParameterChangedEvent("file-format");
                emitParameterUIChangedEvent();
            } else if (expression.contains(".jpg") || expression.contains(".jpeg")) {
                setFileFormat(ImpImageOutputFormat.JPG);
                emitParameterChangedEvent("file-format");
                emitParameterUIChangedEvent();
            } else if (expression.contains(".gif")) {
                setFileFormat(ImpImageOutputFormat.GIF);
                emitParameterChangedEvent("file-format");
                emitParameterUIChangedEvent();
            }
        }
    }
}
