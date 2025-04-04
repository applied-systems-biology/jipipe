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

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.desktop.api.nodes.AddJIPipeDesktopNodeQuickAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.DataExportExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.filesystem.dataypes.FileData;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@SetJIPipeDocumentation(name = "Export table as multi-sheet XLSX", description = "Exports a results table to XLSX. Merge multiple tables into the same batch to create a multi-sheet table.")
@AddJIPipeInputSlot(value = ResultsTableData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = FileData.class, name = "Exported file", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class, menuPath = "Tables")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "File\nSave")
public class ExportTableAsXLSXAlgorithm2 extends JIPipeMergingAlgorithm {

    private JIPipeExpressionParameter sheetNameExpression = new JIPipeExpressionParameter("SUMMARIZE_ANNOTATIONS_MAP(annotations, \"#\")");
    private JIPipeExpressionParameter orderExpression = new JIPipeExpressionParameter("SORT_ASCENDING(sheet_names)");
    private DataExportExpressionParameter filePath = new DataExportExpressionParameter();

    public ExportTableAsXLSXAlgorithm2(JIPipeNodeInfo info) {
        super(info);
    }

    public ExportTableAsXLSXAlgorithm2(ExportTableAsXLSXAlgorithm2 other) {
        super(other);
        this.orderExpression = new JIPipeExpressionParameter(other.orderExpression);
        this.sheetNameExpression = new JIPipeExpressionParameter(other.sheetNameExpression);
        this.filePath = new DataExportExpressionParameter(other.filePath);
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {

        Map<String, Path> projectDataDirs;
        if (getRuntimeProject() != null) {
            projectDataDirs = getRuntimeProject().getDirectoryMap();
        } else {
            projectDataDirs = Collections.emptyMap();
        }
        Path outputPath = filePath.generatePath(getFirstOutputSlot().getSlotStoragePath(),
                getProjectDirectory(),
                projectDataDirs,
                null,
                -1,
                new ArrayList<>(iterationStep.getMergedTextAnnotations().values()));
        outputPath = PathUtils.ensureExtension(outputPath, ".xlsx");
        PathUtils.ensureParentDirectoriesExist(outputPath);

        // Generate excel workbook
        try (Workbook workbook = new XSSFWorkbook()) {

            // Collect the sheets
            Map<String, ResultsTableData> sheets = new HashMap<>();
            for (int row : iterationStep.getInputRows(getFirstInputSlot())) {
                ResultsTableData tableData = getFirstInputSlot().getData(row, ResultsTableData.class, progressInfo);

                JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);
                variables.putAnnotations(getFirstInputSlot().getTextAnnotationMap(row));
                variables.set("annotations", JIPipeTextAnnotation.annotationListToMap(getFirstInputSlot().getTextAnnotations(row), JIPipeTextAnnotationMergeMode.OverwriteExisting));

                String sheetName = sheetNameExpression.evaluateToString(variables);
                sheetName = ResultsTableData.createXLSXSheetName(sheetName, sheets.keySet());

                sheets.put(sheetName, tableData);
            }

            List<String> sortedSheets = new ArrayList<>();
            {
                JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);
                variables.putAnnotations(iterationStep.getMergedTextAnnotations());
                variables.set("annotations", JIPipeTextAnnotation.annotationListToMap(iterationStep.getMergedTextAnnotations().values(), JIPipeTextAnnotationMergeMode.OverwriteExisting));
                variables.set("sheet_names", new ArrayList<>(sheets.keySet()));
                Object result = orderExpression.evaluate(variables);
                if (result instanceof String) {
                    sortedSheets.add((String) result);
                } else if (result instanceof Collection) {
                    for (Object o : (Collection) result) {
                        sortedSheets.add(StringUtils.nullToEmpty(o));
                    }
                }
                sheets.keySet().stream().sorted(NaturalOrderComparator.INSTANCE).forEach(name -> {
                    if (!sortedSheets.contains(name))
                        sortedSheets.add(name);
                });
            }

            // Generate sheets
            for (String sheetName : sortedSheets) {
                ResultsTableData tableData = sheets.getOrDefault(sheetName, null);
                if (tableData == null)
                    continue;
                Sheet sheet = workbook.createSheet(sheetName);
                tableData.saveToXLSXSheet(sheet);
            }

            // Save outputs
            workbook.write(Files.newOutputStream(outputPath));
            iterationStep.addOutputData(getFirstOutputSlot(), new FileData(outputPath), progressInfo);

        } catch (IOException e) {
            throw new RuntimeException(e);
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

    @SetJIPipeDocumentation(name = "Order function", description = "Expression that should return an ordered list of workbook sheets as array of strings. " +
            "If a name is missing, the sheet is placed at the end of the list. If a string is returned, the sheet with the name is set as the first sheet.")
    @JIPipeParameter("order-expression")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(name = "Sheet names", description = "Array if sheet names", key = "sheet_names")
    @AddJIPipeExpressionParameterVariable(name = "Annotations", description = "Map of annotation names to values", key = "annotations")
    public JIPipeExpressionParameter getOrderExpression() {
        return orderExpression;
    }

    @JIPipeParameter("order-expression")
    public void setOrderExpression(JIPipeExpressionParameter orderExpression) {
        this.orderExpression = orderExpression;
    }

    @SetJIPipeDocumentation(name = "Sheet name function", description = "Expression that determines the name of the sheet. Please note that there are certain restrictions on the naming of sheets that are automatically enforced by JIPipe (see https://poi.apache.org/apidocs/dev/org/apache/poi/ss/usermodel/Workbook.html#createSheet--).")
    @JIPipeParameter("sheet-name-expression")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(name = "Annotations", description = "Map of annotation names to values", key = "annotations")
    public JIPipeExpressionParameter getSheetNameExpression() {
        return sheetNameExpression;
    }

    @JIPipeParameter("sheet-name-expression")
    public void setSheetNameExpression(JIPipeExpressionParameter sheetNameExpression) {
        this.sheetNameExpression = sheetNameExpression;
    }

    @AddJIPipeDesktopNodeQuickAction(name = "Configure exported path", description = "Selects where the data should be exported", icon = "actions/document-export.png", buttonIcon = "actions/color-select.png", buttonText = "Select")
    public void selectFilePathDesktopQuickAction(JIPipeDesktopGraphCanvasUI canvasUI) {
        DataExportExpressionParameter result = DataExportExpressionParameter.showPathChooser(canvasUI.getDesktopWorkbench().getWindow(),
                canvasUI.getWorkbench(),
                "Select output file",
                PathType.FilesOnly,
                UIUtils.EXTENSION_FILTER_XLSX);
        if (result != null) {
            setFilePath(result);
            emitParameterChangedEvent("file-path");
        }
    }
}
