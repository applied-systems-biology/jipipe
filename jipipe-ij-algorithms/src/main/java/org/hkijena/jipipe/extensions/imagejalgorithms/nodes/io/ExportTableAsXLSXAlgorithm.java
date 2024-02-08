package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.io;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHidden;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataByMetadataExporter;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.parameters.library.filesystem.PathParameterSettings;
import org.hkijena.jipipe.extensions.settings.DataExporterSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@JIPipeDocumentation(name = "Export table as XLSX", description = "Deprecated. Please use the new node. Exports a results table to XLSX. Merge multiple tables into the same batch to create a multi-sheet table.")
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = FileData.class, slotName = "Exported file", autoCreate = true)
@JIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class, menuPath = "Tables")
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "File\nSave")
@Deprecated
@JIPipeHidden
public class ExportTableAsXLSXAlgorithm extends JIPipeMergingAlgorithm {
    private final JIPipeDataByMetadataExporter exporter;
    private Path outputDirectory = Paths.get("exported-data");
    private boolean relativeToProjectDir = false;

    private JIPipeExpressionParameter sheetNameExpression = new JIPipeExpressionParameter("SUMMARIZE_ANNOTATIONS_MAP(annotations, \"#\")");

    private JIPipeExpressionParameter orderExpression = new JIPipeExpressionParameter("SORT_ASCENDING(sheet_names)");

    public ExportTableAsXLSXAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.exporter = new JIPipeDataByMetadataExporter(DataExporterSettings.getInstance());
        registerSubParameter(exporter);
    }

    public ExportTableAsXLSXAlgorithm(ExportTableAsXLSXAlgorithm other) {
        super(other);
        this.exporter = new JIPipeDataByMetadataExporter(other.exporter);
        this.outputDirectory = other.outputDirectory;
        this.relativeToProjectDir = other.relativeToProjectDir;
        this.orderExpression = new JIPipeExpressionParameter(other.orderExpression);
        this.sheetNameExpression = new JIPipeExpressionParameter(other.sheetNameExpression);
        registerSubParameter(exporter);
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        final Path outputPath;
        if (outputDirectory == null || outputDirectory.toString().isEmpty() || !outputDirectory.isAbsolute()) {
            if (relativeToProjectDir && getProjectDirectory() != null) {
                outputPath = getProjectDirectory().resolve(StringUtils.nullToEmpty(outputDirectory));
            } else {
                outputPath = getFirstOutputSlot().getSlotStoragePath().resolve(StringUtils.nullToEmpty(outputDirectory));
            }
        } else {
            outputPath = outputDirectory;
        }

        // Generate output paths
        Set<Path> outputPaths = new HashSet<>();
        for (int row : iterationStep.getInputRows(getFirstInputSlot())) {
            // Generate the path
            Path generatedPath = exporter.generatePath(getFirstInputSlot(), row, new HashSet<>());
            Path rowPath;
            // If absolute -> use the path, otherwise use output directory
            if (generatedPath.isAbsolute()) {
                rowPath = generatedPath;
            } else {
                rowPath = outputPath.resolve(generatedPath);
            }
            PathUtils.ensureParentDirectoriesExist(rowPath);
            rowPath = PathUtils.ensureExtension(rowPath, ".xlsx");
            outputPaths.add(rowPath);
        }

        // Generate excel workbook
        try (Workbook workbook = new XSSFWorkbook()) {

            // Collect the sheets
            Map<String, ResultsTableData> sheets = new HashMap<>();
            for (int row : iterationStep.getInputRows(getFirstInputSlot())) {
                ResultsTableData tableData = getFirstInputSlot().getData(row, ResultsTableData.class, progressInfo);

                JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
                variables.putAnnotations(getFirstInputSlot().getTextAnnotationMap(row));
                variables.set("annotations", JIPipeTextAnnotation.annotationListToMap(getFirstInputSlot().getTextAnnotations(row), JIPipeTextAnnotationMergeMode.OverwriteExisting));

                String sheetName = sheetNameExpression.evaluateToString(variables);
                sheetName = ResultsTableData.createXLSXSheetName(sheetName, sheets.keySet());

                sheets.put(sheetName, tableData);
            }

            List<String> sortedSheets = new ArrayList<>();
            {
                JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
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
            for (Path path : outputPaths) {
                workbook.write(Files.newOutputStream(path));
                iterationStep.addOutputData(getFirstOutputSlot(), new FileData(path), progressInfo);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


    @JIPipeDocumentation(name = "Output relative to project directory", description = "If enabled, outputs will be preferably generated relative to the project directory. " +
            "Otherwise, JIPipe will store the results in an automatically generated directory. " +
            "Has no effect if an absolute path is provided.")
    @JIPipeParameter("relative-to-project-dir")
    public boolean isRelativeToProjectDir() {
        return relativeToProjectDir;
    }

    @JIPipeParameter("relative-to-project-dir")
    public void setRelativeToProjectDir(boolean relativeToProjectDir) {
        this.relativeToProjectDir = relativeToProjectDir;
    }

    @JIPipeDocumentation(name = "Output directory", description = "Can be a relative or absolute directory. All collected files will be put into this directory. " +
            "If relative, it is relative to the output slot's output directory that is generated based on the current run's output path.")
    @JIPipeParameter("output-directory")
    @PathParameterSettings(ioMode = PathIOMode.Open, pathMode = PathType.DirectoriesOnly)
    public Path getOutputDirectory() {
        return outputDirectory;
    }

    @JIPipeParameter("output-directory")
    public void setOutputDirectory(Path outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    @JIPipeDocumentation(name = "File name generation", description = "Following settings control how the output file names are generated from metadata columns.")
    @JIPipeParameter("exporter")
    public JIPipeDataByMetadataExporter getExporter() {
        return exporter;
    }

    @JIPipeDocumentation(name = "Order function", description = "Expression that should return an ordered list of workbook sheets as array of strings. " +
            "If a name is missing, the sheet is placed at the end of the list. If a string is returned, the sheet with the name is set as the first sheet.")
    @JIPipeParameter("order-expression")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(name = "Sheet names", description = "Array if sheet names", key = "sheet_names")
    @JIPipeExpressionParameterVariable(name = "Annotations", description = "Map of annotation names to values", key = "annotations")
    public JIPipeExpressionParameter getOrderExpression() {
        return orderExpression;
    }

    @JIPipeParameter("order-expression")
    public void setOrderExpression(JIPipeExpressionParameter orderExpression) {
        this.orderExpression = orderExpression;
    }

    @JIPipeDocumentation(name = "Sheet name function", description = "Expression that determines the name of the sheet. Please note that there are certain restrictions on the naming of sheets that are automatically enforced by JIPipe (see https://poi.apache.org/apidocs/dev/org/apache/poi/ss/usermodel/Workbook.html#createSheet--).")
    @JIPipeParameter("sheet-name-expression")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(name = "Annotations", description = "Map of annotation names to values", key = "annotations")
    public JIPipeExpressionParameter getSheetNameExpression() {
        return sheetNameExpression;
    }

    @JIPipeParameter("sheet-name-expression")
    public void setSheetNameExpression(JIPipeExpressionParameter sheetNameExpression) {
        this.sheetNameExpression = sheetNameExpression;
    }
}
