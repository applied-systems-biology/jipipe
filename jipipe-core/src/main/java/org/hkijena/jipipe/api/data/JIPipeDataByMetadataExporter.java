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

package org.hkijena.jipipe.api.data;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
     * @param dataSlotList      list of data slots the will be exported
     * @param outputPath        the path where the files will be put
     */
    public void writeToFolder(List<JIPipeDataSlot> dataSlotList, Path outputPath, JIPipeProgressInfo progress) {
        if (!Files.isDirectory(outputPath)) {
            try {
                Files.createDirectories(outputPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        Set<String> existingMetadata = new HashSet<>();
        for (JIPipeDataSlot dataSlot : dataSlotList) {
            if (progress.isCancelled().get())
                return;
            writeToFolder(dataSlot, outputPath, progress, existingMetadata);
        }
    }

    /**
     * Writes data to the specified folder
     *
     * @param dataSlot          the data slot
     * @param outputPath        the path where the files will be put
     */
    public void writeToFolder(JIPipeDataSlot dataSlot, Path outputPath, JIPipeProgressInfo progress) {
        writeToFolder(dataSlot, outputPath, progress, new HashSet<>());
    }

    /**
     * Writes data to the specified folder
     *
     * @param dataSlot          the data slot
     * @param outputPath        the path where the files will be put
     * @param existingMetadata  list of existing entries. used to avoid duplicates.
     */
    public void writeToFolder(JIPipeDataSlot dataSlot, Path outputPath, JIPipeProgressInfo progress, Set<String> existingMetadata) {
        for (int row = 0; row < dataSlot.getRowCount(); row++) {
            if (progress.isCancelled().get())
                return;
            writeToFolder(dataSlot, row, outputPath, progress, existingMetadata);
        }
    }

    /**
     * Writes data to the specified folder
     *
     * @param dataSlot          the data slot
     * @param row               the data row
     * @param outputPath        the path where the files will be put
     */
    public void writeToFolder(JIPipeDataSlot dataSlot, int row, Path outputPath, JIPipeProgressInfo progress) {
        writeToFolder(dataSlot, row, outputPath, progress, new HashSet<>());
    }

    /**
     * Writes data to the specified folder
     *
     * @param dataSlot          the data slot
     * @param row               the data row
     * @param outputPath        the path where the files will be put
     * @param existingMetadata  list of existing entries. used to avoid duplicates
     */
    public void writeToFolder(JIPipeDataSlot dataSlot, int row, Path outputPath, JIPipeProgressInfo progress, Set<String> existingMetadata) {
        String metadataString = generateMetadataString(dataSlot, row, existingMetadata);
        JIPipeData data = dataSlot.getData(row, JIPipeData.class);
        progress.log("Saving " + data + " as " + metadataString + " into " + outputPath);
        data.saveTo(outputPath, metadataString, true);
    }

    /**
     * Generates a unique string based on metadata for the selected row
     *
     * @param dataSlot         the slot
     * @param row              the row
     * @param existingMetadata existing strings
     * @return the string
     */
    @Nullable
    public String generateMetadataString(JIPipeDataSlot dataSlot, int row, Set<String> existingMetadata) {
        StringBuilder metadataStringBuilder = new StringBuilder();
        if (appendDataTypeAsMetadata) {
            String dataTypeName;
            if (appendDataTypeUsesRealDataType) {
                JIPipeData data = dataSlot.getData(row, JIPipeData.class);
                dataTypeName = JIPipeDataInfo.getInstance(data.getClass()).getName();
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
            JIPipeAnnotation metadataValue;
            if (ignoreMissingMetadata) {
                metadataValue = dataSlot.getAnnotationOr(row, metadataKey, null);
            } else {
                metadataValue = dataSlot.getAnnotationOr(row, metadataKey, new JIPipeAnnotation(metadataKey, missingString));
            }
            if (metadataValue != null) {
                if(metadataValue.getValue().length() > metadataValueLengthLimit)
                    continue;
                if (metadataStringBuilder.length() > 0)
                    metadataStringBuilder.append(separatorString);
                if (withMetadataKeys)
                    metadataStringBuilder.append(metadataKey).append(equalsString);
                metadataStringBuilder.append(metadataValue.getValue());
            }
        }

        if(metadataStringBuilder.length() == 0) {
            metadataStringBuilder.append("unnamed");
        }
        String metadataString = StringUtils.makeFilesystemCompatible(metadataStringBuilder.toString());
        metadataString = StringUtils.makeUniqueString(metadataString, separatorString, existingMetadata);
        existingMetadata.add(metadataString);
        return metadataString;
    }

    public String generateMetadataString(JIPipeExportedDataTable exportedDataTable, int row, Set<String> existingMetadata) {
        JIPipeExportedDataTable.Row dataRow = exportedDataTable.getRowList().get(row);
        StringBuilder metadataStringBuilder = new StringBuilder();
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
        for (JIPipeAnnotation metadataValue : dataRow.getAnnotations()) {
            if(metadataValue.getValue().length() > metadataValueLengthLimit)
                continue;
            if (!metadataKeyFilter.test(metadataValue.getName()))
                continue;
            if (metadataStringBuilder.length() > 0)
                metadataStringBuilder.append(separatorString);
            if (withMetadataKeys)
                metadataStringBuilder.append(metadataValue.getName()).append(equalsString);
            metadataStringBuilder.append(metadataValue.getValue());
        }

        if(metadataStringBuilder.length() == 0) {
            metadataStringBuilder.append("unnamed");
        }
        String metadataString = StringUtils.makeFilesystemCompatible(metadataStringBuilder.toString());
        metadataString = StringUtils.makeUniqueString(metadataString, separatorString, existingMetadata);
        existingMetadata.add(metadataString);
        return metadataString;
    }
}
