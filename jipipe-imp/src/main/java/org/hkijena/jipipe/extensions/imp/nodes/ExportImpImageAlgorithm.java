package org.hkijena.jipipe.extensions.imp.nodes;

import ij.IJ;
import ij.ImagePlus;
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
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.DataExportExpressionParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.AVICompression;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.HyperstackDimension;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imp.datatypes.ImpImageData;
import org.hkijena.jipipe.extensions.imp.datatypes.ImpImageOutputFormat;
import org.hkijena.jipipe.extensions.imp.utils.ImpImageUtils;
import org.hkijena.jipipe.utils.PathUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

@SetJIPipeDocumentation(name = "Export IMP image", description = "Exports IMP images into a non-JIPipe format (PNG, JPEG, BMP, GIF).")
@AddJIPipeInputSlot(value = ImpImageData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = FileData.class, slotName = "Exported file", create = true)
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

        if(inputData.isHyperstack()) {
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
        }
        else {
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
}
