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
import org.hkijena.jipipe.api.LabelAsJIPipeHidden;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeDataByMetadataExporter;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.filesystem.dataypes.FileData;
import org.hkijena.jipipe.plugins.parameters.library.filesystem.PathParameterSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeDataExporterApplicationSettings;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@SetJIPipeDocumentation(name = "Export table as XLSX", description = "Deprecated. Please use the new node. Exports a results table to XLSX. Merge multiple tables into the same batch to create a multi-sheet table.")
@AddJIPipeInputSlot(value = ResultsTableData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = FileData.class, name = "Exported file", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class, menuPath = "Tables")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "File\nSave")
@Deprecated
@LabelAsJIPipeHidden
public class ExportTableAsXLSXAlgorithm extends JIPipeMergingAlgorithm {
    private final JIPipeDataByMetadataExporter exporter;
    private Path outputDirectory = Paths.get("exported-data");
    private boolean relativeToProjectDir = false;

    private JIPipeExpressionParameter sheetNameExpression = new JIPipeExpressionParameter("SUMMARIZE_ANNOTATIONS_MAP(annotations, \"#\")");

    private JIPipeExpressionParameter orderExpression = new JIPipeExpressionParameter("SORT_ASCENDING(sheet_names)");

    public ExportTableAsXLSXAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.exporter = new JIPipeDataByMetadataExporter(JIPipeDataExporterApplicationSettings.getInstance());
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
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
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
            for (Path path : outputPaths) {
                workbook.write(Files.newOutputStream(path));
                iterationStep.addOutputData(getFirstOutputSlot(), new FileData(path), progressInfo);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


    @SetJIPipeDocumentation(name = "Output relative to project directory", description = "If enabled, outputs will be preferably generated relative to the project directory. " +
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

    @SetJIPipeDocumentation(name = "Output directory", description = "Can be a relative or absolute directory. All collected files will be put into this directory. " +
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

    @SetJIPipeDocumentation(name = "File name generation", description = "Following settings control how the output file names are generated from metadata columns.")
    @JIPipeParameter("exporter")
    public JIPipeDataByMetadataExporter getExporter() {
        return exporter;
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
}
