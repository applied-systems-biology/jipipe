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

package org.hkijena.jipipe.extensions.annotation.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithm;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmDeclaration;
import org.hkijena.jipipe.api.algorithm.AlgorithmInputSlot;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataDeclaration;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.registries.JIPipeDatatypeRegistry;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

@JIPipeDocumentation(name = "Collect data", description = "Collects all incoming data into one or multiple folders that contain the raw output files. " +
        "The output files are named according to the metadata columns and can be easily processed by humans or third-party scripts.")
@AlgorithmInputSlot(JIPipeData.class)
@AlgorithmInputSlot(FolderData.class)
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Miscellaneous)
public class CollectDataAlgorithm extends JIPipeAlgorithm {

    private boolean splitByInputSlots = true;
    private boolean ignoreMissingMetadata = false;
    private boolean withMetadataKeys = true;
    private boolean appendDataTypeAsMetadata = false;
    private boolean appendDataTypeUsesRealDataType = true;
    private String appendDataTypeMetadataKey = "DataType";
    private String separatorString = "_";
    private String equalsString = "=";
    private String missingString = "NA";
    private Path outputDirectory = Paths.get("collected-data");

    public CollectDataAlgorithm(JIPipeAlgorithmDeclaration declaration) {
        super(declaration, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Input", JIPipeData.class)
                .addOutputSlot("Output path", FolderData.class, null)
                .sealOutput()
                .build());
    }

    public CollectDataAlgorithm(CollectDataAlgorithm other) {
        super(other);
        this.splitByInputSlots = other.splitByInputSlots;
        this.separatorString = other.separatorString;
        this.equalsString = other.equalsString;
        this.missingString = other.missingString;
        this.ignoreMissingMetadata = other.ignoreMissingMetadata;
        this.withMetadataKeys = other.withMetadataKeys;
        this.outputDirectory = other.outputDirectory;
        this.appendDataTypeAsMetadata = other.appendDataTypeAsMetadata;
        this.appendDataTypeUsesRealDataType = other.appendDataTypeUsesRealDataType;
        this.appendDataTypeMetadataKey = other.appendDataTypeMetadataKey;
    }

    @Override
    public void run(JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        Path outputPath;
        if (outputDirectory == null || outputDirectory.toString().isEmpty() || !outputDirectory.isAbsolute()) {
            outputPath = getFirstOutputSlot().getStoragePath().resolve(outputDirectory);
        } else {
            outputPath = outputDirectory;
        }
        if (isPassThrough()) {
            algorithmProgress.accept(subProgress.resolve("Data passed through to output"));
            getFirstOutputSlot().addData(new FolderData(outputPath));
            return;
        }

        if (splitByInputSlots) {
            for (JIPipeDataSlot inputSlot : getInputSlots()) {
                if (isCancelled.get())
                    return;
                writeToFolder(Collections.singletonList(inputSlot), outputPath.resolve(inputSlot.getName()), subProgress.resolve("Slot '" + inputSlot.getName() + "'"), algorithmProgress, isCancelled);
                getFirstOutputSlot().addData(new FolderData(outputPath.resolve(inputSlot.getName())));
            }
        } else {
            writeToFolder(getInputSlots(), outputPath, subProgress, algorithmProgress, isCancelled);
            getFirstOutputSlot().addData(new FolderData(outputPath));
        }
    }

    private void writeToFolder(List<JIPipeDataSlot> dataSlotList, Path outputPath, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        if (!Files.isDirectory(outputPath)) {
            try {
                Files.createDirectories(outputPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        Set<String> existingMetadata = new HashSet<>();
        for (JIPipeDataSlot dataSlot : dataSlotList) {
            for (int row = 0; row < dataSlot.getRowCount(); row++) {
                if (isCancelled.get())
                    return;
                StringBuilder metadataStringBuilder = new StringBuilder();
                if(appendDataTypeAsMetadata) {
                    String dataTypeName;
                    if(appendDataTypeUsesRealDataType) {
                        JIPipeData data = dataSlot.getData(row, JIPipeData.class);
                        dataTypeName = JIPipeDataDeclaration.getInstance(data.getClass()).getName();
                    }
                    else {
                        dataTypeName = JIPipeDataDeclaration.getInstance(dataSlot.getAcceptedDataType()).getName();
                    }
                    if (withMetadataKeys && !StringUtils.isNullOrEmpty(appendDataTypeMetadataKey))
                        metadataStringBuilder.append(appendDataTypeMetadataKey).append(equalsString);
                    metadataStringBuilder.append(dataTypeName);
                }
                for (int col = 0; col < dataSlot.getAnnotationColumns().size(); col++) {
                    String metadataKey = dataSlot.getAnnotationColumns().get(col);
                    JIPipeAnnotation metadataValue;
                    if (ignoreMissingMetadata) {
                        metadataValue = dataSlot.getAnnotationOr(row, metadataKey, null);
                    } else {
                        metadataValue = dataSlot.getAnnotationOr(row, metadataKey, new JIPipeAnnotation(metadataKey, missingString));
                    }

                    if (metadataValue != null) {
                        if (metadataStringBuilder.length() > 0)
                            metadataStringBuilder.append(separatorString);
                        if (withMetadataKeys)
                            metadataStringBuilder.append(metadataKey).append(equalsString);
                        metadataStringBuilder.append(metadataValue.getValue());
                    }
                }

                String metadataString = StringUtils.makeFilesystemCompatible(StringUtils.makeUniqueString(metadataStringBuilder.toString(), separatorString, existingMetadata));
                existingMetadata.add(metadataString);
                JIPipeData data = dataSlot.getData(row, JIPipeData.class);
                algorithmProgress.accept(subProgress.resolve("Saving " + data + " as " + metadataString + " into " + outputPath));
                data.saveTo(outputPath, metadataString, true);
            }
        }
    }

    @Override
    protected boolean canPassThrough() {
        return true;
    }

    @JIPipeDocumentation(name = "Split by input slots", description = "If enabled, a folder for each input slot is created. Otherwise all output is stored within one folder.")
    @JIPipeParameter("split-by-input-slots")
    public boolean isSplitByInputSlots() {
        return splitByInputSlots;
    }

    @JIPipeParameter("split-by-input-slots")
    public void setSplitByInputSlots(boolean splitByInputSlots) {
        this.splitByInputSlots = splitByInputSlots;
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

    @JIPipeDocumentation(name = "Output directory", description = "Can be a relative or absolute directory. All collected files will be put into this directory. " +
            "If relative, it is relative to the output slot's output directory that is generated based on the current run's output path.")
    @JIPipeParameter("output-directory")
    public Path getOutputDirectory() {
        return outputDirectory;
    }

    @JIPipeParameter("output-directory")
    public void setOutputDirectory(Path outputDirectory) {
        this.outputDirectory = outputDirectory;
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
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/annotation.png")
    public String getAppendDataTypeMetadataKey() {
        return appendDataTypeMetadataKey;
    }

    @JIPipeParameter("append-data-type-metadata-key")
    public void setAppendDataTypeMetadataKey(String appendDataTypeMetadataKey) {
        this.appendDataTypeMetadataKey = appendDataTypeMetadataKey;
    }
}
