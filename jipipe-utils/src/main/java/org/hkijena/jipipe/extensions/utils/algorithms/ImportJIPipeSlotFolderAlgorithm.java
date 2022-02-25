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

package org.hkijena.jipipe.extensions.utils.algorithms;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemReadStorage;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.extensions.utils.algorithms.meta.GetJIPipeSlotFolderAlgorithm;
import org.hkijena.jipipe.extensions.utils.datatypes.JIPipeOutputData;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@JIPipeDocumentation(name = "Import JIPipe slot folder", description = "Extracts a slot output folder from a JIPipe output and imports their data. Use the 'Set output slot' button to select the correct parameters.")
@JIPipeInputSlot(value = JIPipeOutputData.class, slotName = "JIPipe output", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Data", autoCreate = true)
@JIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Meta run")
public class ImportJIPipeSlotFolderAlgorithm extends GetJIPipeSlotFolderAlgorithm {

    private boolean ignoreInputTextAnnotations = false;
    private boolean ignoreInputDataAnnotations = false;
    private boolean ignoreImportedTextAnnotations = false;
    private boolean ignoreImportedDataAnnotations = false;
    private JIPipeTextAnnotationMergeMode textAnnotationMergeMode = JIPipeTextAnnotationMergeMode.Merge;
    private JIPipeDataAnnotationMergeMode dataAnnotationMergeMode = JIPipeDataAnnotationMergeMode.OverwriteExisting;

    public ImportJIPipeSlotFolderAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ImportJIPipeSlotFolderAlgorithm(ImportJIPipeSlotFolderAlgorithm other) {
        super(other);
        this.ignoreInputTextAnnotations = other.ignoreInputTextAnnotations;
        this.ignoreInputDataAnnotations = other.ignoreInputDataAnnotations;
        this.ignoreImportedTextAnnotations = other.ignoreImportedTextAnnotations;
        this.ignoreImportedDataAnnotations = other.ignoreImportedDataAnnotations;
        this.textAnnotationMergeMode = other.textAnnotationMergeMode;
        this.dataAnnotationMergeMode = other.dataAnnotationMergeMode;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        JIPipeOutputData outputData = dataBatch.getInputData(getFirstInputSlot(), JIPipeOutputData.class, progressInfo);
        Path dataFolder = outputData.toPath().resolve(getCompartmentId()).resolve(getNodeId()).resolve(getSlotName());
        if (ignoreInputTextAnnotations)
            dataBatch.setMergedTextAnnotations(new HashMap<>());
        if(ignoreInputDataAnnotations)
            dataBatch.setMergedDataAnnotations(new HashMap<>());
        if (!Files.exists(dataFolder.resolve("data-table.json"))) {
            throw new UserFriendlyRuntimeException("Missing data-table.json!",
                    "Wrong input folder!",
                    "Algorithm " + getName(),
                    "You tried to import data from a JIPipe output slot folder located at " + dataFolder + ". JIPipe has a very specific format to store such folders. The directory seems to not conform to this format.",
                    "Check if the folder contains many numeric subfolders and a data-table.json file.");
        }

        JIPipeDataTable dataTable = JIPipeDataTable.importData(new JIPipeFileSystemReadStorage(dataFolder), progressInfo);
        for (int row = 0; row < dataTable.getRowCount(); row++) {
            List<JIPipeTextAnnotation> textAnnotationList = ignoreImportedTextAnnotations ? Collections.emptyList() : dataTable.getTextAnnotations(row);
            List<JIPipeDataAnnotation> dataAnnotationList = ignoreImportedDataAnnotations ? Collections.emptyList() : dataTable.getDataAnnotations(row);
            dataBatch.addOutputData(getFirstOutputSlot(),
                    dataTable.getData(row, JIPipeData.class, progressInfo),
                    textAnnotationList,
                    textAnnotationMergeMode,
                    dataAnnotationList,
                    dataAnnotationMergeMode,
                    progressInfo);
        }
    }

    @JIPipeDocumentation(name = "Ignore input text annotations", description = "If enabled, incoming text annotations from the input folder will not be passed to the output.")
    @JIPipeParameter("ignore-input-text-annotations")
    public boolean isIgnoreInputTextAnnotations() {
        return ignoreInputTextAnnotations;
    }

    @JIPipeParameter("ignore-input-text-annotations")
    public void setIgnoreInputTextAnnotations(boolean ignoreInputTextAnnotations) {
        this.ignoreInputTextAnnotations = ignoreInputTextAnnotations;
    }

    @JIPipeDocumentation(name = "Ignore input data annotations", description = "If enabled, incoming data annotations from the input folder will not be passed to the output.")
    @JIPipeParameter("ignore-input-data-annotations")
    public boolean isIgnoreInputDataAnnotations() {
        return ignoreInputDataAnnotations;
    }

    @JIPipeParameter("ignore-input-data-annotations")
    public void setIgnoreInputDataAnnotations(boolean ignoreInputDataAnnotations) {
        this.ignoreInputDataAnnotations = ignoreInputDataAnnotations;
    }

    @JIPipeDocumentation(name = "Ignore imported text annotations", description = "If enabled, annotations from imported text annotations are ignored.")
    @JIPipeParameter("ignore-imported-text-annotations")
    public boolean isIgnoreImportedTextAnnotations() {
        return ignoreImportedTextAnnotations;
    }

    @JIPipeParameter("ignore-imported-text-annotations")
    public void setIgnoreImportedTextAnnotations(boolean ignoreImportedTextAnnotations) {
        this.ignoreImportedTextAnnotations = ignoreImportedTextAnnotations;
    }

    @JIPipeDocumentation(name = "Ignore imported data annotations", description = "If enabled, annotations from imported data annotations are ignored.")
    @JIPipeParameter("ignore-imported-data-annotations")
    public boolean isIgnoreImportedDataAnnotations() {
        return ignoreImportedDataAnnotations;
    }

    @JIPipeParameter("ignore-imported-data-annotations")
    public void setIgnoreImportedDataAnnotations(boolean ignoreImportedDataAnnotations) {
        this.ignoreImportedDataAnnotations = ignoreImportedDataAnnotations;
    }

    @JIPipeDocumentation(name = "Merge imported text annotations", description = "Determines what happens when imported data has the same text annotations as the input folder.")
    @JIPipeParameter("text-annotation-merge-mode")
    public JIPipeTextAnnotationMergeMode getTextAnnotationMergeMode() {
        return textAnnotationMergeMode;
    }

    @JIPipeParameter("text-annotation-merge-mode")
    public void setTextAnnotationMergeMode(JIPipeTextAnnotationMergeMode textAnnotationMergeMode) {
        this.textAnnotationMergeMode = textAnnotationMergeMode;
    }

    @JIPipeDocumentation(name = "Merge imported data annotations", description = "Determines what happens when imported data has the same data annotations as the input folder.")
    @JIPipeParameter("data-annotation-merge-mode")
    public JIPipeDataAnnotationMergeMode getDataAnnotationMergeMode() {
        return dataAnnotationMergeMode;
    }

    @JIPipeParameter("data-annotation-merge-mode")
    public void setDataAnnotationMergeMode(JIPipeDataAnnotationMergeMode dataAnnotationMergeMode) {
        this.dataAnnotationMergeMode = dataAnnotationMergeMode;
    }
}
