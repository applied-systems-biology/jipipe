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

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteDataStorage;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class that exports data based on metadata
 */
public class JIPipeDataByMetadataExporter implements JIPipeParameterCollection {
    private final EventBus eventBus = new EventBus();
    private boolean ignoreMissingMetadata = false;
    private boolean withMetadataKeys = true;
    private boolean appendDataTypeAsMetadata = false;
    private boolean appendDataTypeUsesRealDataType = true;
    private String appendDataTypeMetadataKey = "DataType";
    private String separatorString = "_";
    private String equalsString = "=";
    private String missingString = "NA";
    private StringQueryExpression metadataKeyFilter = new StringQueryExpression("");
    private int metadataValueLengthLimit = 80;
    private Mode mode = Mode.Manual;
    private StringQueryExpression customName = new StringQueryExpression("SUMMARIZE_VARIABLES()");
    private StringQueryExpression customSubDirectory = new StringQueryExpression("\"\"");

    public JIPipeDataByMetadataExporter() {
    }

    public JIPipeDataByMetadataExporter(JIPipeDataByMetadataExporter other) {
        this.ignoreMissingMetadata = other.ignoreMissingMetadata;
        this.withMetadataKeys = other.withMetadataKeys;
        this.appendDataTypeAsMetadata = other.appendDataTypeAsMetadata;
        this.appendDataTypeUsesRealDataType = other.appendDataTypeUsesRealDataType;
        this.appendDataTypeMetadataKey = other.appendDataTypeMetadataKey;
        this.separatorString = other.separatorString;
        this.equalsString = other.equalsString;
        this.missingString = other.missingString;
        this.metadataKeyFilter = new StringQueryExpression(other.metadataKeyFilter);
        this.metadataValueLengthLimit = other.metadataValueLengthLimit;
        this.customName = new StringQueryExpression(other.customName);
        this.mode = other.mode;
        this.customSubDirectory = new StringQueryExpression(other.customSubDirectory);
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if (access.getKey().equals("mode"))
            return true;
        if (mode == Mode.Automatic) {
            return !access.getKey().equals("custom-name") && !access.getKey().equals("custom-directory");
        } else {
            if (access.getKey().equals("ignore-missing-metadata"))
                return true;
            if (access.getKey().equals("missing-string"))
                return true;
            return access.getKey().equals("custom-name") || access.getKey().equals("custom-directory");
        }
    }

    @JIPipeDocumentation(name = "Custom name", description = "This expression is used to generate the file names. You have all metadata available as variables. By default, it will summarize all variables (annotations) into a long string. " +
            "If you do not want to customize the file name, you can create you own string based on available annotations. For example, you can insert <code>#Dataset + \"_\" + Threshold</code> to store the data set and a threshold annotation.")
    @JIPipeParameter(value = "custom-name", important = true)
    @ExpressionParameterSettings(variableSource = VariableSource.class)
    public StringQueryExpression getCustomName() {
        return customName;
    }

    @JIPipeParameter("custom-name")
    public void setCustomName(StringQueryExpression customName) {
        this.customName = customName;
    }

    @JIPipeDocumentation(name = "Custom sub directory", description = "This expression is used to generate a sub directory for your files. Set it to \"\" to have no sub directory. Use '/' to make paths. You have all metadata available as variables.")
    @JIPipeParameter("custom-directory")
    @ExpressionParameterSettings(variableSource = VariableSource.class)
    public StringQueryExpression getCustomDirectory() {
        return customSubDirectory;
    }

    @JIPipeParameter("custom-directory")
    public void setCustomDirectory(StringQueryExpression customDirectory) {
        this.customSubDirectory = customDirectory;
    }

    @JIPipeDocumentation(name = "Mode", description = "If the mode is set to 'Automatic', the string is automatically " +
            "generated based on various settings. Otherwise, you can use an expression to generate a custom string.")
    @JIPipeParameter("mode")
    public Mode getMode() {
        return mode;
    }

    @JIPipeParameter("mode")
    public void setMode(Mode mode) {
        this.mode = mode;
        triggerParameterUIChange();
    }

    @JIPipeDocumentation(name = "Metadata key filters", description = "Only includes the metadata keys that match the filter. " + StringQueryExpression.DOCUMENTATION_DESCRIPTION)
    @JIPipeParameter("metadata-key-filters")
    public StringQueryExpression getMetadataKeyFilter() {
        return metadataKeyFilter;
    }

    @JIPipeParameter("metadata-key-filters")
    public void setMetadataKeyFilter(StringQueryExpression metadataKeyFilter) {
        this.metadataKeyFilter = metadataKeyFilter;
    }

    @JIPipeDocumentation(name = "Separator string", description = "String that separates multiple metadata column fields.")
    @JIPipeParameter("separator-string")
    @StringParameterSettings(monospace = true)
    public String getSeparatorString() {
        return separatorString;
    }

    @JIPipeParameter("separator-string")
    public void setSeparatorString(String separatorString) {
        this.separatorString = separatorString;
    }

    @JIPipeDocumentation(name = "Equals string", description = "String that separates the metadata name and value.")
    @JIPipeParameter("equals-string")
    @StringParameterSettings(monospace = true)
    public String getEqualsString() {
        return equalsString;
    }

    @JIPipeParameter("equals-string")
    public void setEqualsString(String equalsString) {
        this.equalsString = equalsString;
    }

    @JIPipeDocumentation(name = "Ignore missing metadata", description = "If enabled, missing metadata does not appear in the generated file names.")
    @JIPipeParameter("ignore-missing-metadata")
    public boolean isIgnoreMissingMetadata() {
        return ignoreMissingMetadata;
    }

    @JIPipeParameter("ignore-missing-metadata")
    public void setIgnoreMissingMetadata(boolean ignoreMissingMetadata) {
        this.ignoreMissingMetadata = ignoreMissingMetadata;
    }

    @JIPipeDocumentation(name = "Missing metadata string", description = "Missing metadata is replaced by this value")
    @JIPipeParameter("missing-string")
    @StringParameterSettings(monospace = true)
    public String getMissingString() {
        return missingString;
    }

    @JIPipeParameter("missing-string")
    public void setMissingString(String missingString) {
        this.missingString = missingString;
    }

    @JIPipeDocumentation(name = "Add metadata keys", description = "If enabled, metadata keys are added to the generated file names. If this is disabled, the values are still written.")
    @JIPipeParameter("with-metadata-keys")
    public boolean isWithMetadataKeys() {
        return withMetadataKeys;
    }

    @JIPipeParameter("with-metadata-keys")
    public void setWithMetadataKeys(boolean withMetadataKeys) {
        this.withMetadataKeys = withMetadataKeys;
    }

    @JIPipeDocumentation(name = "Append data type as metadata", description = "If enabled, the input slot data type is added as additional metadata to the file name.")
    @JIPipeParameter("append-data-type-as-metadata")
    public boolean isAppendDataTypeAsMetadata() {
        return appendDataTypeAsMetadata;
    }

    @JIPipeParameter("append-data-type-as-metadata")
    public void setAppendDataTypeAsMetadata(boolean appendDataTypeAsMetadata) {
        this.appendDataTypeAsMetadata = appendDataTypeAsMetadata;
    }

    @JIPipeDocumentation(name = "Append true data type as metadata", description = "If enabled, data type added by 'Append data type as metadata' is the " +
            "true data type, not the slot data type.")
    @JIPipeParameter("append-true-data-type-as-metadata")
    public boolean isAppendDataTypeUsesRealDataType() {
        return appendDataTypeUsesRealDataType;
    }

    @JIPipeParameter("append-true-data-type-as-metadata")
    public void setAppendDataTypeUsesRealDataType(boolean appendDataTypeUsesRealDataType) {
        this.appendDataTypeUsesRealDataType = appendDataTypeUsesRealDataType;
    }

    @JIPipeDocumentation(name = "Append data type as", description = "The metadata key that is used to append the data type if enabled. Can be empty.")
    @JIPipeParameter("append-data-type-metadata-key")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/annotation.png")
    public String getAppendDataTypeMetadataKey() {
        return appendDataTypeMetadataKey;
    }

    @JIPipeParameter("append-data-type-metadata-key")
    public void setAppendDataTypeMetadataKey(String appendDataTypeMetadataKey) {
        this.appendDataTypeMetadataKey = appendDataTypeMetadataKey;
    }

    @JIPipeDocumentation(name = "Value length limit", description = "All values with a length greater than this value are omitted. This is due to limitations of file systems. Generally, a file name cannot be longer than 255 characters. On Windows there are additional constraints on the full path length.")
    @JIPipeParameter("metadata-value-length-limit")
    public int getMetadataValueLengthLimit() {
        return metadataValueLengthLimit;
    }

    @JIPipeParameter("metadata-value-length-limit")
    public void setMetadataValueLengthLimit(int metadataValueLengthLimit) {
        this.metadataValueLengthLimit = metadataValueLengthLimit;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
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
        for (JIPipeDataTable dataSlot : dataTableList) {
            if (progressInfo.isCancelled())
                return;
            writeToFolder(dataSlot, outputPath, progressInfo, existingMetadata);
        }
    }

    /**
     * Writes data to the specified folder
     *
     * @param dataSlot   the data slot
     * @param outputPath the path where the files will be put
     */
    public void writeToFolder(JIPipeDataTable dataSlot, Path outputPath, JIPipeProgressInfo progressInfo) {
        writeToFolder(dataSlot, outputPath, progressInfo, new HashSet<>());
    }

    /**
     * Writes data to the specified folder
     *
     * @param dataSlot         the data slot
     * @param outputPath       the path where the files will be put
     * @param existingMetadata list of existing entries. used to avoid duplicates.
     */
    public void writeToFolder(JIPipeDataTable dataSlot, Path outputPath, JIPipeProgressInfo progressInfo, Set<String> existingMetadata) {
        for (int row = 0; row < dataSlot.getRowCount(); row++) {
            if (progressInfo.isCancelled())
                return;
            writeToFolder(dataSlot, row, outputPath, progressInfo, existingMetadata);
        }
    }

    /**
     * Writes data to the specified folder
     *
     * @param dataSlot   the data slot
     * @param row        the data row
     * @param outputPath the path where the files will be put
     */
    public void writeToFolder(JIPipeDataTable dataSlot, int row, Path outputPath, JIPipeProgressInfo progressInfo) {
        writeToFolder(dataSlot, row, outputPath, progressInfo, new HashSet<>());
    }

    /**
     * Writes data to the specified folder
     *
     * @param dataSlot         the data slot
     * @param row              the data row
     * @param outputPath       the path where the files will be put
     * @param existingMetadata list of existing entries. used to avoid duplicates
     */
    public void writeToFolder(JIPipeDataTable dataSlot, int row, Path outputPath, JIPipeProgressInfo progressInfo, Set<String> existingMetadata) {
        String metadataString = generateMetadataString(dataSlot, row, existingMetadata);
        outputPath = PathUtils.resolveAndMakeSubDirectory(outputPath, generateSubFolder(dataSlot, row));
        JIPipeData data = dataSlot.getData(row, JIPipeData.class, progressInfo);
        progressInfo.log("Saving " + data + " as " + metadataString + " into " + outputPath);
        data.exportData(new JIPipeFileSystemWriteDataStorage(progressInfo, outputPath), metadataString, true, progressInfo);
    }

    /**
     * Generates a unique string based on metadata for the selected row
     *
     * @param dataSlot         the slot
     * @param row              the row
     * @param existingMetadata existing strings
     * @return the string
     */
    public String generateMetadataString(JIPipeDataTable dataSlot, int row, Set<String> existingMetadata) {
        StringBuilder metadataStringBuilder = new StringBuilder();
        if (mode == Mode.Automatic) {
            if (appendDataTypeAsMetadata) {
                String dataTypeName;
                if (appendDataTypeUsesRealDataType) {
                    Class<? extends JIPipeData> dataClass = dataSlot.getDataClass(row);
                    dataTypeName = JIPipeDataInfo.getInstance(dataClass).getName();
                } else {
                    dataTypeName = JIPipeDataInfo.getInstance(dataSlot.getAcceptedDataType()).getName();
                }
                if (withMetadataKeys && !StringUtils.isNullOrEmpty(appendDataTypeMetadataKey))
                    metadataStringBuilder.append(appendDataTypeMetadataKey).append(equalsString);
                metadataStringBuilder.append(dataTypeName);
            }
            for (int col = 0; col < dataSlot.getAnnotationColumns().size(); col++) {
                String metadataKey = dataSlot.getAnnotationColumns().get(col);
                if (!metadataKeyFilter.test(metadataKey))
                    continue;
                JIPipeTextAnnotation metadataValue;
                if (ignoreMissingMetadata) {
                    metadataValue = dataSlot.getTextAnnotationOr(row, metadataKey, null);
                } else {
                    metadataValue = dataSlot.getTextAnnotationOr(row, metadataKey, new JIPipeTextAnnotation(metadataKey, missingString));
                }
                if (metadataValue != null) {
                    if (metadataValue.getValue().length() > metadataValueLengthLimit)
                        continue;
                    if (metadataStringBuilder.length() > 0)
                        metadataStringBuilder.append(separatorString);
                    if (withMetadataKeys)
                        metadataStringBuilder.append(metadataKey).append(equalsString);
                    metadataStringBuilder.append(metadataValue.getValue());
                }
            }
        } else {
            ExpressionVariables parameters = new ExpressionVariables();
            for (int col = 0; col < dataSlot.getAnnotationColumns().size(); col++) {
                String metadataKey = dataSlot.getAnnotationColumns().get(col);
                JIPipeTextAnnotation metadataValue = dataSlot.getTextAnnotationOr(row, metadataKey, null);
                if (metadataValue == null && ignoreMissingMetadata)
                    continue;
                String value = metadataValue != null ? metadataValue.getValue() : missingString;
                parameters.put(metadataKey, value);
            }
            parameters.set("data_string", dataSlot.getVirtualData(row).getStringRepresentation());
            parameters.set("data_type", JIPipe.getDataTypes().getIdOf(dataSlot.getDataClass(row)));
            parameters.set("row", row + "");

            String newName = StringUtils.nullToEmpty(customName.generate(parameters));
            metadataStringBuilder.append(newName);
        }

        if (metadataStringBuilder.length() == 0) {
            metadataStringBuilder.append("unnamed");
        }
        String metadataString = StringUtils.makeFilesystemCompatible(metadataStringBuilder.toString());
        metadataString = StringUtils.makeUniqueString(metadataString, separatorString, existingMetadata);
        existingMetadata.add(metadataString);
        return metadataString;
    }

    public Path generateSubFolder(JIPipeDataTable dataSlot, int row) {
        if (mode == Mode.Automatic) {
            return Paths.get("");
        } else {
            ExpressionVariables parameters = new ExpressionVariables();
            for (int col = 0; col < dataSlot.getAnnotationColumns().size(); col++) {
                String metadataKey = dataSlot.getAnnotationColumns().get(col);
                JIPipeTextAnnotation metadataValue = dataSlot.getTextAnnotationOr(row, metadataKey, null);
                if (metadataValue == null && ignoreMissingMetadata)
                    continue;
                String value = metadataValue != null ? metadataValue.getValue() : missingString;
                parameters.put(metadataKey, value);
            }
            parameters.set("data_string", dataSlot.getVirtualData(row).getStringRepresentation());
            parameters.set("data_type", JIPipe.getDataTypes().getIdOf(dataSlot.getDataClass(row)));
            parameters.set("row", row + "");

            return Paths.get(StringUtils.nullToEmpty(customSubDirectory.generate(parameters)));
        }
    }

    public String generateMetadataString(JIPipeDataTableMetadata exportedDataTable, int row, Set<String> existingMetadata) {
        JIPipeDataTableMetadataRow dataRow = exportedDataTable.getRowList().get(row);
        StringBuilder metadataStringBuilder = new StringBuilder();
        if (mode == Mode.Automatic) {
            if (appendDataTypeAsMetadata) {
                String dataTypeName;
                if (appendDataTypeUsesRealDataType) {
                    dataTypeName = dataRow.getTrueDataType();
                } else {
                    dataTypeName = exportedDataTable.getAcceptedDataTypeId();
                }
                if (withMetadataKeys && !StringUtils.isNullOrEmpty(appendDataTypeMetadataKey))
                    metadataStringBuilder.append(appendDataTypeMetadataKey).append(equalsString);
                metadataStringBuilder.append(dataTypeName);
            }
            for (JIPipeTextAnnotation metadataValue : dataRow.getTextAnnotations()) {
                if (metadataValue.getValue().length() > metadataValueLengthLimit)
                    continue;
                if (!metadataKeyFilter.test(metadataValue.getName()))
                    continue;
                if (metadataStringBuilder.length() > 0)
                    metadataStringBuilder.append(separatorString);
                if (withMetadataKeys)
                    metadataStringBuilder.append(metadataValue.getName()).append(equalsString);
                metadataStringBuilder.append(metadataValue.getValue());
            }
        } else {
            ExpressionVariables parameters = new ExpressionVariables();
            for (JIPipeTextAnnotation annotation : dataRow.getTextAnnotations()) {
                parameters.put(annotation.getName(), annotation.getValue());
            }

            String newName = StringUtils.nullToEmpty(customName.generate(parameters));
            metadataStringBuilder.append(newName);
        }

        if (metadataStringBuilder.length() == 0) {
            metadataStringBuilder.append("unnamed");
        }
        String metadataString = StringUtils.makeFilesystemCompatible(metadataStringBuilder.toString());
        metadataString = StringUtils.makeUniqueString(metadataString, separatorString, existingMetadata);
        existingMetadata.add(metadataString);
        return metadataString;
    }

    public enum Mode {
        Automatic,
        Manual
    }

    public static class VariableSource implements ExpressionParameterVariableSource {

        public static final Set<ExpressionParameterVariable> VARIABLES;

        static {
            VARIABLES = new HashSet<>();
            VARIABLES.add(new ExpressionParameterVariable("<Annotations>",
                    "Data annotations are available as variables named after their column names (use Update Cache to find the list of annotations)",
                    ""));
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
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }
}
