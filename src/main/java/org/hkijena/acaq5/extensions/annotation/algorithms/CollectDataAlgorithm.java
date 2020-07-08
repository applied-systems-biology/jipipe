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

package org.hkijena.acaq5.extensions.annotation.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.data.ACAQAnnotation;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.filesystem.dataypes.FolderData;
import org.hkijena.acaq5.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.acaq5.utils.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

@ACAQDocumentation(name = "Collect data", description = "Collects all incoming data into one or multiple folders that contain the raw output files. " +
        "The output files are named according to the metadata columns and can be easily processed by humans or third-party scripts.")
@AlgorithmInputSlot(ACAQData.class)
@AlgorithmInputSlot(FolderData.class)
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Miscellaneous)
public class CollectDataAlgorithm extends ACAQAlgorithm {

    private boolean splitByInputSlots = true;
    private boolean ignoreMissingMetadata = false;
    private boolean withMetadataKeys = true;
    private String separatorString = "_";
    private String equalsString = "=";
    private String missingString = "NA";

    public CollectDataAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQDefaultMutableSlotConfiguration.builder()
        .addInputSlot("Input", ACAQData.class)
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
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        if(isPassThrough()) {
            algorithmProgress.accept(subProgress.resolve("Data passed through to output"));
            getFirstOutputSlot().addData(new FolderData(getFirstOutputSlot().getStoragePath()));
            return;
        }
        Path outputPath = getFirstOutputSlot().getStoragePath();
        if(splitByInputSlots) {
            for (ACAQDataSlot inputSlot : getInputSlots()) {
                if(isCancelled.get())
                    return;
                writeToFolder(Collections.singletonList(inputSlot), outputPath.resolve(inputSlot.getName()), subProgress.resolve("Slot '" + inputSlot.getName() + "'"), algorithmProgress, isCancelled);
                getFirstOutputSlot().addData(new FolderData(outputPath.resolve(inputSlot.getName())));
            }
        }
        else {
            writeToFolder(getInputSlots(), outputPath, subProgress, algorithmProgress, isCancelled);
            getFirstOutputSlot().addData(new FolderData(getFirstOutputSlot().getStoragePath()));
        }
    }

    private void writeToFolder(List<ACAQDataSlot> dataSlotList, Path outputPath, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        if(!Files.isDirectory(outputPath)) {
            try {
                Files.createDirectories(outputPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        Set<String> existingMetadata = new HashSet<>();
        for (ACAQDataSlot dataSlot : dataSlotList) {
            for (int row = 0; row < dataSlot.getRowCount(); row++) {
                if(isCancelled.get())
                    return;
                StringBuilder metadataStringBuilder = new StringBuilder();
                for (int col = 0; col < dataSlot.getAnnotationColumns().size() ; col++) {
                    String metadataKey = dataSlot.getAnnotationColumns().get(col);
                    ACAQAnnotation metadataValue;
                    if(ignoreMissingMetadata) {
                        metadataValue  = dataSlot.getAnnotationOr(row, metadataKey, null);
                    }
                    else {
                        metadataValue = dataSlot.getAnnotationOr(row, metadataKey, new ACAQAnnotation(metadataKey, missingString));
                    }

                    if(metadataValue != null) {
                        if(metadataStringBuilder.length() > 0)
                            metadataStringBuilder.append(separatorString);
                        if(withMetadataKeys)
                            metadataStringBuilder.append(metadataKey).append(equalsString);
                        metadataStringBuilder.append(metadataValue.getValue());
                    }
                }

                String metadataString = StringUtils.makeFilesystemCompatible(StringUtils.makeUniqueString(metadataStringBuilder.toString(), separatorString, existingMetadata));
                existingMetadata.add(metadataString);
                ACAQData data = dataSlot.getData(row, ACAQData.class);
                algorithmProgress.accept(subProgress.resolve("Saving " + data + " as " + metadataString + " into " + outputPath));
                data.saveTo(outputPath, metadataString, true);
            }
        }
    }

    @Override
    protected boolean canPassThrough() {
        return true;
    }

    @ACAQDocumentation(name = "Split by input slots", description = "If enabled, a folder for each input slot is created. Otherwise all output is stored within one folder.")
    @ACAQParameter("split-by-input-slots")
    public boolean isSplitByInputSlots() {
        return splitByInputSlots;
    }

    @ACAQParameter("split-by-input-slots")
    public void setSplitByInputSlots(boolean splitByInputSlots) {
        this.splitByInputSlots = splitByInputSlots;
    }

    @ACAQDocumentation(name = "Separator string", description = "String that separates multiple metadata column fields.")
    @ACAQParameter("separator-string")
    @StringParameterSettings(monospace = true)
    public String getSeparatorString() {
        return separatorString;
    }

    @ACAQParameter("separator-string")
    public void setSeparatorString(String separatorString) {
        this.separatorString = separatorString;
    }

    @ACAQDocumentation(name = "Equals string", description = "String that separates the metadata name and value.")
    @ACAQParameter("equals-string")
    @StringParameterSettings(monospace = true)
    public String getEqualsString() {
        return equalsString;
    }

    @ACAQParameter("equals-string")
    public void setEqualsString(String equalsString) {
        this.equalsString = equalsString;
    }

    @ACAQDocumentation(name = "Ignore missing metadata", description = "If enabled, missing metadata does not appear in the generated file names.")
    @ACAQParameter("ignore-missing-metadata")
    public boolean isIgnoreMissingMetadata() {
        return ignoreMissingMetadata;
    }

    @ACAQParameter("ignore-missing-metadata")
    public void setIgnoreMissingMetadata(boolean ignoreMissingMetadata) {
        this.ignoreMissingMetadata = ignoreMissingMetadata;
    }

    @ACAQDocumentation(name = "Missing metadata string", description = "Missing metadata is replaced by this value")
    @ACAQParameter("missing-string")
    @StringParameterSettings(monospace = true)
    public String getMissingString() {
        return missingString;
    }

    @ACAQParameter("missing-string")
    public void setMissingString(String missingString) {
        this.missingString = missingString;
    }

    @ACAQDocumentation(name = "Add metadata keys", description = "If enabled, metadata keys are added to the generated file names. If this is disabled, the values are still written.")
    @ACAQParameter("with-metadata-keys")
    public boolean isWithMetadataKeys() {
        return withMetadataKeys;
    }

    @ACAQParameter("with-metadata-keys")
    public void setWithMetadataKeys(boolean withMetadataKeys) {
        this.withMetadataKeys = withMetadataKeys;
    }
}
