/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.annotation;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.JIPipeDataTableMetadata;
import org.hkijena.jipipe.api.data.JIPipeDataTableMetadataRow;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteDataStorage;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class that exports data based on metadata
 */
public class JIPipeDataByMetadataExporter extends AbstractJIPipeParameterCollection {
    private DefaultExpressionParameter fileNameGenerator = new StringQueryExpression("SUMMARIZE_ANNOTATIONS_MAP(annotations, \"#\")");
    private boolean forceName = true;

    private boolean makeFilesystemCompatible = true;

    public JIPipeDataByMetadataExporter() {
    }

    public JIPipeDataByMetadataExporter(JIPipeDataByMetadataExporter other) {
        this.fileNameGenerator = new StringQueryExpression(other.fileNameGenerator.getExpression());
        this.makeFilesystemCompatible = other.makeFilesystemCompatible;
        this.forceName = other.forceName;
    }

    @JIPipeDocumentation(name = "File name", description = "This expression is used to generate the file names. You have all metadata available as variables. By default, it will summarize all variables (annotations) into a long string. " +
            "If you do not want to customize the file name, you can create you own string based on available annotations. For example, you can insert <code>#Dataset + \"_\" + Threshold</code> to store the data set and a threshold annotation.\n\n" +
            "You can use the following function to automatically generate the name from annotations: <code>SUMMARIZE_ANNOTATIONS_MAP(annotations, \"#\")</code>")
    @JIPipeParameter(value = "file-name", important = true)
    @ExpressionParameterSettings(variableSource = VariableSource.class)
    public DefaultExpressionParameter getFileNameGenerator() {
        return fileNameGenerator;
    }

    @JIPipeParameter("file-name")
    public void setFileNameGenerator(DefaultExpressionParameter fileNameGenerator) {
        this.fileNameGenerator = fileNameGenerator;
    }

    @JIPipeDocumentation(name = "Force name", description = "If true, the output files are forced to use the generated name. Otherwise, internal names are utilized.")
    @JIPipeParameter("force-name")
    public boolean isForceName() {
        return forceName;
    }

    @JIPipeParameter("force-name")
    public void setForceName(boolean forceName) {
        this.forceName = forceName;
    }

    @JIPipeDocumentation(name = "Make compatible to file systems", description = "If enabled, the generated name/path is compatible to common file systems")
    @JIPipeParameter("make-filesystem-compatible")
    public boolean isMakeFilesystemCompatible() {
        return makeFilesystemCompatible;
    }

    @JIPipeParameter("make-filesystem-compatible")
    public void setMakeFilesystemCompatible(boolean makeFilesystemCompatible) {
        this.makeFilesystemCompatible = makeFilesystemCompatible;
    }

    /**
     * Writes data to the specified folder
     *
     * @param dataTableList list of data tables the will be exported
     * @param outputPath    the path where the files will be put
     */
    public void writeToFolder(List<? extends JIPipeDataTable> dataTableList, Path outputPath, JIPipeProgressInfo progressInfo) {
        if (!Files.isDirectory(outputPath)) {
            try {
                Files.createDirectories(outputPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        Set<String> existingMetadata = new HashSet<>();
        for (JIPipeDataTable dataTable : dataTableList) {
            if (progressInfo.isCancelled())
                return;
            writeToFolder(dataTable, outputPath, progressInfo, existingMetadata);
        }
    }

    /**
     * Writes data to the specified folder
     *
     * @param dataTable  the data slot
     * @param outputPath the path where the files will be put
     */
    public void writeToFolder(JIPipeDataTable dataTable, Path outputPath, JIPipeProgressInfo progressInfo) {
        writeToFolder(dataTable, outputPath, progressInfo, new HashSet<>());
    }

    /**
     * Writes data to the specified folder
     *
     * @param dataTable        the data slot
     * @param outputPath       the path where the files will be put
     * @param existingMetadata list of existing entries. used to avoid duplicates.
     */
    public void writeToFolder(JIPipeDataTable dataTable, Path outputPath, JIPipeProgressInfo progressInfo, Set<String> existingMetadata) {
        for (int row = 0; row < dataTable.getRowCount(); row++) {
            if (progressInfo.isCancelled())
                return;
            writeToFolder(dataTable, row, outputPath, progressInfo, existingMetadata);
        }
    }

    /**
     * Writes data to the specified folder
     *
     * @param dataTable  the data slot
     * @param row        the data row
     * @param outputPath the path where the files will be put
     */
    public void writeToFolder(JIPipeDataTable dataTable, int row, Path outputPath, JIPipeProgressInfo progressInfo) {
        writeToFolder(dataTable, row, outputPath, progressInfo, new HashSet<>());
    }

    /**
     * Writes data to the specified folder
     *
     * @param dataTable        the data slot
     * @param row              the data row
     * @param outputPath       the path where the files will be put
     * @param existingMetadata list of existing entries. used to avoid duplicates
     */
    public void writeToFolder(JIPipeDataTable dataTable, int row, Path outputPath, JIPipeProgressInfo progressInfo, Set<String> existingMetadata) {
        Path metadataPath = generatePath(dataTable, row, existingMetadata);
        if (metadataPath.getParent() != null) {
            outputPath = PathUtils.resolveAndMakeSubDirectory(outputPath, metadataPath.getParent());
        }
        String metadataString = metadataPath.getFileName().toString();
        JIPipeData data = dataTable.getData(row, JIPipeData.class, progressInfo);
        progressInfo.log("Saving " + data + " as " + metadataString + " into " + outputPath);
        data.exportData(new JIPipeFileSystemWriteDataStorage(progressInfo, outputPath), metadataString, forceName, progressInfo);
    }

    /**
     * Generates a unique string based on metadata for the selected row
     *
     * @param dataTable        the slot
     * @param row              the row
     * @param existingMetadata existing strings
     * @return the string
     */
    public String generateName(JIPipeDataTable dataTable, int row, Set<String> existingMetadata) {
        ExpressionVariables parameters = new ExpressionVariables();
        for (int col = 0; col < dataTable.getTextAnnotationColumns().size(); col++) {
            String metadataKey = dataTable.getTextAnnotationColumns().get(col);
            JIPipeTextAnnotation metadataValue = dataTable.getTextAnnotationOr(row, metadataKey, null);
            if (metadataValue == null)
                continue;
            String value = metadataValue.getValue();
            parameters.put(metadataKey, value);
        }
        parameters.set("data_string", dataTable.getDataItemStore(row).getStringRepresentation());
        parameters.set("data_type", JIPipe.getDataTypes().getIdOf(dataTable.getDataClass(row)));
        parameters.set("row", row + "");
        parameters.set("annotations", JIPipeTextAnnotation.annotationListToMap(dataTable.getTextAnnotations(row),
                JIPipeTextAnnotationMergeMode.OverwriteExisting));

        String metadataString = StringUtils.nullToEmpty(fileNameGenerator.evaluateToString(parameters));
        if (StringUtils.isNullOrEmpty(metadataString)) {
            metadataString = "unnamed";
        }
        if (makeFilesystemCompatible) {
            metadataString = StringUtils.makeFilesystemCompatible(metadataString);
        }
        metadataString = StringUtils.makeUniqueString(metadataString, "_", existingMetadata);
        existingMetadata.add(metadataString);
        return metadataString;
    }

    public String generateName(JIPipeDataTableMetadata exportedDataTable, int row, Set<String> existingMetadata) {
        JIPipeDataTableMetadataRow dataRow = exportedDataTable.getRowList().get(row);
        ExpressionVariables parameters = new ExpressionVariables();
        for (JIPipeTextAnnotation annotation : dataRow.getTextAnnotations()) {
            parameters.put(annotation.getName(), annotation.getValue());
        }

        String metadataString = StringUtils.nullToEmpty(fileNameGenerator.evaluateToString(parameters));
        if (StringUtils.isNullOrEmpty(metadataString)) {
            metadataString = "unnamed";
        }
        if (makeFilesystemCompatible) {
            metadataString = StringUtils.makeFilesystemCompatible(metadataString);
        }
        metadataString = StringUtils.makeUniqueString(metadataString, "_", existingMetadata);
        existingMetadata.add(metadataString);
        return metadataString;
    }

    /**
     * Generates a unique path based on metadata for the selected row
     *
     * @param dataTable        the slot
     * @param row              the row
     * @param existingMetadata existing strings. must be writable
     * @return the string
     */
    public Path generatePath(JIPipeDataTable dataTable, int row, Set<String> existingMetadata) {
        ExpressionVariables parameters = new ExpressionVariables();
        for (int col = 0; col < dataTable.getTextAnnotationColumns().size(); col++) {
            String metadataKey = dataTable.getTextAnnotationColumns().get(col);
            JIPipeTextAnnotation metadataValue = dataTable.getTextAnnotationOr(row, metadataKey, null);
            if (metadataValue == null)
                continue;
            String value = metadataValue.getValue();
            parameters.put(metadataKey, value);
        }
        parameters.set("data_string", dataTable.getDataItemStore(row).getStringRepresentation());
        parameters.set("data_type", JIPipe.getDataTypes().getIdOf(dataTable.getDataClass(row)));
        parameters.set("row", row + "");
        parameters.set("annotations", JIPipeTextAnnotation.annotationListToMap(dataTable.getTextAnnotations(row),
                JIPipeTextAnnotationMergeMode.OverwriteExisting));

        String metadataString = StringUtils.nullToEmpty(fileNameGenerator.evaluateToString(parameters));
        if (StringUtils.isNullOrEmpty(metadataString)) {
            metadataString = "unnamed";
        }
        {
            metadataString = metadataString.replace('\\', '/');
            List<String> pathComponents = new ArrayList<>();
            for (String s : metadataString.split("/")) {
                if (!StringUtils.isNullOrEmpty(s)) {
                    pathComponents.add(StringUtils.makeFilesystemCompatible(s));
                }
            }
            metadataString = String.join("/", pathComponents);
        }
        metadataString = StringUtils.makeUniqueString(metadataString, "_", existingMetadata);
        existingMetadata.add(metadataString);
        return Paths.get(metadataString).normalize();
    }

    public Path generatePath(JIPipeDataTableMetadata exportedDataTable, int row, Set<String> existingMetadata) {
        JIPipeDataTableMetadataRow dataRow = exportedDataTable.getRowList().get(row);
        ExpressionVariables parameters = new ExpressionVariables();
        for (JIPipeTextAnnotation annotation : dataRow.getTextAnnotations()) {
            parameters.put(annotation.getName(), annotation.getValue());
        }

        String metadataString = StringUtils.nullToEmpty(fileNameGenerator.evaluateToString(parameters));
        if (StringUtils.isNullOrEmpty(metadataString)) {
            metadataString = "unnamed";
        }
        {
            metadataString = metadataString.replace('\\', '/');
            List<String> pathComponents = new ArrayList<>();
            for (String s : metadataString.split("/")) {
                if (!StringUtils.isNullOrEmpty(s)) {
                    pathComponents.add(StringUtils.makeFilesystemCompatible(s));
                }
            }
            metadataString = String.join("/", pathComponents);
        }
        metadataString = StringUtils.makeUniqueString(metadataString, "_", existingMetadata);
        existingMetadata.add(metadataString);
        return Paths.get(metadataString).normalize();
    }

    public static class VariableSource implements ExpressionParameterVariableSource {

        public static final Set<ExpressionParameterVariable> VARIABLES;

        static {
            VARIABLES = new HashSet<>();
            VARIABLES.add(ExpressionParameterVariable.ANNOTATIONS_VARIABLE);
            VARIABLES.add(new ExpressionParameterVariable("Annotations",
                    "Map of annotation names and values",
                    "annotations"));
            VARIABLES.add(new ExpressionParameterVariable("Data string",
                    "The data stored as string",
                    "data_string"));
            VARIABLES.add(new ExpressionParameterVariable("Data type ID",
                    "The data type ID",
                    "data_type"));
            VARIABLES.add(new ExpressionParameterVariable("Row",
                    "The row inside the data table",
                    "row"));
        }

        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }
}
