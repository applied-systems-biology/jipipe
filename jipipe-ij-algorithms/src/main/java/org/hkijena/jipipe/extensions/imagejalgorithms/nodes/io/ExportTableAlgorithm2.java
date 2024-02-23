package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.io;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DataExportExpressionParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.PathUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

@SetJIPipeDocumentation(name = "Export table", description = "Exports a results table to CSV/XLSX")
@AddJIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = FileData.class, slotName = "Exported file", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class, menuPath = "Tables")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "File\nSave")
public class ExportTableAlgorithm2 extends JIPipeIteratingAlgorithm {

    private DataExportExpressionParameter filePath = new DataExportExpressionParameter();
    private ExportTableAlgorithm.FileFormat fileFormat = ExportTableAlgorithm.FileFormat.CSV;

    public ExportTableAlgorithm2(JIPipeNodeInfo info) {
        super(info);
    }

    public ExportTableAlgorithm2(ExportTableAlgorithm2 other) {
        super(other);
        this.fileFormat = other.fileFormat;
        this.filePath = new DataExportExpressionParameter(other.filePath);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData inputData = iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);

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

        Path outputFile;
        switch (fileFormat) {
            case CSV: {
                outputFile = PathUtils.ensureExtension(outputPath, ".csv");
                inputData.saveAsCSV(outputFile);
            }
            break;
            case XLSX: {
                outputFile = PathUtils.ensureExtension(outputPath, ".xlsx");
                inputData.saveAsXLSX(outputFile);
            }
            break;
            default:
                throw new UnsupportedOperationException();
        }

        iterationStep.addOutputData(getFirstOutputSlot(), new FileData(outputFile), progressInfo);
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

    @SetJIPipeDocumentation(name = "File format", description = "The format of the exported file")
    @JIPipeParameter("file-format")
    public ExportTableAlgorithm.FileFormat getFileFormat() {
        return fileFormat;
    }

    @JIPipeParameter("file-format")
    public void setFileFormat(ExportTableAlgorithm.FileFormat fileFormat) {
        this.fileFormat = fileFormat;
    }

}
