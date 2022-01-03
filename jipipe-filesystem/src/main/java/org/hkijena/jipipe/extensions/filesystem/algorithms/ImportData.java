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

package org.hkijena.jipipe.extensions.filesystem.algorithms;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeExportedDataTable;
import org.hkijena.jipipe.api.data.JIPipeExportedDataTableRow;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@JIPipeDocumentation(name = "Import data from slot", description = "Imports data from a slot folder back into JIPipe. The folder contains a data-table.json file and multiple folders with numeric names.")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeInputSlot(value = FolderData.class, slotName = "Slot folder", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Slot data", autoCreate = true)
public class ImportData extends JIPipeSimpleIteratingAlgorithm {

    private boolean ignoreInputAnnotations = false;
    private boolean ignoreImportedDataAnnotations = false;
    private JIPipeAnnotationMergeStrategy annotationMergeStrategy = JIPipeAnnotationMergeStrategy.Merge;

    public ImportData(JIPipeNodeInfo info) {
        super(info);
    }

    public ImportData(ImportData other) {
        super(other);
        this.ignoreInputAnnotations = other.ignoreInputAnnotations;
        this.ignoreImportedDataAnnotations = other.ignoreImportedDataAnnotations;
        this.annotationMergeStrategy = other.annotationMergeStrategy;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        if (ignoreInputAnnotations)
            dataBatch.setMergedAnnotations(new HashMap<>());
        Path dataFolder = dataBatch.getInputData(getFirstInputSlot(), FolderData.class, progressInfo).toPath();
        if (!Files.exists(dataFolder.resolve("data-table.json"))) {
            throw new UserFriendlyRuntimeException("Missing data-table.json!",
                    "Wrong input folder!",
                    "Algorithm " + getName(),
                    "You tried to import data from a JIPipe output slot folder located at " + dataFolder + ". JIPipe has a very specific format to store such folders. The directory seems to not conform to this format.",
                    "Check if the folder contains many numeric subfolders and a data-table.json file.");
        }
        JIPipeExportedDataTable exportedDataTable = JIPipeExportedDataTable.loadFromJson(dataFolder.resolve("data-table.json"));
        Class<? extends JIPipeData> dataType = JIPipe.getDataTypes().getById(exportedDataTable.getAcceptedDataTypeId());
        if (dataType == null) {
            throw new UserFriendlyRuntimeException("Unknown data type id: " + exportedDataTable.getAcceptedDataTypeId(),
                    "Unknown data type",
                    "Algorithm " + getName(),
                    "You tried to import data from a JIPipe output slot folder located at " + dataFolder + ". The data contained in this folder is identified by a type id '" + exportedDataTable.getAcceptedDataTypeId() + "', but it could not be found.",
                    "Check if you installed the necessary plugins and extensions.");
        }

        for (JIPipeExportedDataTableRow row : exportedDataTable.getRowList()) {
            progressInfo.log("Importing data row " + row.getIndex());
            Path storageFolder = dataFolder.resolve("" + row.getIndex());
            List<JIPipeAnnotation> annotationList = ignoreImportedDataAnnotations ? Collections.emptyList() : row.getAnnotations();
            JIPipeDataInfo trueDataType = exportedDataTable.getDataTypeOf(row.getIndex());
            JIPipeData data = JIPipe.importData(storageFolder, trueDataType.getDataClass());
            dataBatch.addOutputData(getFirstOutputSlot(), data, annotationList, annotationMergeStrategy, progressInfo);
        }
    }

    @JIPipeDocumentation(name = "Ignore input annotations", description = "If enabled, incoming annotations from the input folder will not be passed to the output.")
    @JIPipeParameter("ignore-input-annotations")
    public boolean isIgnoreInputAnnotations() {
        return ignoreInputAnnotations;
    }

    @JIPipeParameter("ignore-input-annotations")
    public void setIgnoreInputAnnotations(boolean ignoreInputAnnotations) {
        this.ignoreInputAnnotations = ignoreInputAnnotations;
    }

    @JIPipeDocumentation(name = "Ignore imported annotations", description = "If enabled, annotations from imported data are ignored.")
    @JIPipeParameter("ignore-imported-annotations")
    public boolean isIgnoreImportedDataAnnotations() {
        return ignoreImportedDataAnnotations;
    }

    @JIPipeParameter("ignore-imported-annotations")
    public void setIgnoreImportedDataAnnotations(boolean ignoreImportedDataAnnotations) {
        this.ignoreImportedDataAnnotations = ignoreImportedDataAnnotations;
    }

    @JIPipeDocumentation(name = "Merge imported annotations", description = "Determines what happens when imported data has the same annotations as the input folder.")
    @JIPipeParameter("annotation-merge-strategy")
    public JIPipeAnnotationMergeStrategy getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeAnnotationMergeStrategy annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }
}
