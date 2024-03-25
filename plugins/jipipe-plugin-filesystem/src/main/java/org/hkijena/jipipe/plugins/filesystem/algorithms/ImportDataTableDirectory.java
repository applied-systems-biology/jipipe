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

package org.hkijena.jipipe.plugins.filesystem.algorithms;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemReadDataStorage;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.plugins.filesystem.dataypes.FolderData;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@SetJIPipeDocumentation(name = "Import data table (directory)", description = "Imports a data table directory. The folder contains a data-table.json file and multiple folders with numeric names.")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeInputSlot(value = FolderData.class, slotName = "Data table folder", create = true)
@AddJIPipeOutputSlot(value = JIPipeData.class, slotName = "Data", create = true)
public class ImportDataTableDirectory extends JIPipeSimpleIteratingAlgorithm {

    private boolean ignoreInputTextAnnotations = false;
    private boolean ignoreInputDataAnnotations = false;
    private boolean ignoreImportedTextAnnotations = false;
    private boolean ignoreImportedDataAnnotations = false;
    private JIPipeTextAnnotationMergeMode textAnnotationMergeMode = JIPipeTextAnnotationMergeMode.Merge;
    private JIPipeDataAnnotationMergeMode dataAnnotationMergeMode = JIPipeDataAnnotationMergeMode.OverwriteExisting;

    public ImportDataTableDirectory(JIPipeNodeInfo info) {
        super(info);
    }

    public ImportDataTableDirectory(ImportDataTableDirectory other) {
        super(other);
        this.ignoreInputTextAnnotations = other.ignoreInputTextAnnotations;
        this.ignoreInputDataAnnotations = other.ignoreInputDataAnnotations;
        this.ignoreImportedTextAnnotations = other.ignoreImportedTextAnnotations;
        this.ignoreImportedDataAnnotations = other.ignoreImportedDataAnnotations;
        this.textAnnotationMergeMode = other.textAnnotationMergeMode;
        this.dataAnnotationMergeMode = other.dataAnnotationMergeMode;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        if (ignoreInputTextAnnotations)
            iterationStep.setMergedTextAnnotations(new HashMap<>());
        if (ignoreInputDataAnnotations)
            iterationStep.setMergedDataAnnotations(new HashMap<>());
        Path dataFolder = iterationStep.getInputData(getFirstInputSlot(), FolderData.class, progressInfo).toPath();
        if (!Files.exists(dataFolder.resolve("data-table.json"))) {
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new GraphNodeValidationReportContext(this),
                    "Missing data-table.json!",
                    "You tried to import data from a JIPipe output slot folder located at " + dataFolder + ". JIPipe has a very specific format to store such folders. The directory seems to not conform to this format.",
                    "Check if the folder contains many numeric subfolders and a data-table.json file."));
        }

        JIPipeDataTable dataTable = JIPipeDataTable.importData(new JIPipeFileSystemReadDataStorage(progressInfo, dataFolder), progressInfo);
        for (int row = 0; row < dataTable.getRowCount(); row++) {
            List<JIPipeTextAnnotation> textAnnotationList = ignoreImportedTextAnnotations ? Collections.emptyList() : dataTable.getTextAnnotations(row);
            List<JIPipeDataAnnotation> dataAnnotationList = ignoreImportedDataAnnotations ? Collections.emptyList() : dataTable.getDataAnnotations(row);
            iterationStep.addOutputData(getFirstOutputSlot(),
                    dataTable.getData(row, JIPipeData.class, progressInfo),
                    textAnnotationList,
                    textAnnotationMergeMode,
                    dataAnnotationList,
                    dataAnnotationMergeMode,
                    progressInfo);
        }
    }

    @SetJIPipeDocumentation(name = "Ignore input text annotations", description = "If enabled, incoming text annotations from the input folder will not be passed to the output.")
    @JIPipeParameter("ignore-input-text-annotations")
    public boolean isIgnoreInputTextAnnotations() {
        return ignoreInputTextAnnotations;
    }

    @JIPipeParameter("ignore-input-text-annotations")
    public void setIgnoreInputTextAnnotations(boolean ignoreInputTextAnnotations) {
        this.ignoreInputTextAnnotations = ignoreInputTextAnnotations;
    }

    @SetJIPipeDocumentation(name = "Ignore input data annotations", description = "If enabled, incoming data annotations from the input folder will not be passed to the output.")
    @JIPipeParameter("ignore-input-data-annotations")
    public boolean isIgnoreInputDataAnnotations() {
        return ignoreInputDataAnnotations;
    }

    @JIPipeParameter("ignore-input-data-annotations")
    public void setIgnoreInputDataAnnotations(boolean ignoreInputDataAnnotations) {
        this.ignoreInputDataAnnotations = ignoreInputDataAnnotations;
    }

    @SetJIPipeDocumentation(name = "Ignore imported text annotations", description = "If enabled, annotations from imported text annotations are ignored.")
    @JIPipeParameter("ignore-imported-text-annotations")
    public boolean isIgnoreImportedTextAnnotations() {
        return ignoreImportedTextAnnotations;
    }

    @JIPipeParameter("ignore-imported-text-annotations")
    public void setIgnoreImportedTextAnnotations(boolean ignoreImportedTextAnnotations) {
        this.ignoreImportedTextAnnotations = ignoreImportedTextAnnotations;
    }

    @SetJIPipeDocumentation(name = "Ignore imported data annotations", description = "If enabled, annotations from imported data annotations are ignored.")
    @JIPipeParameter("ignore-imported-data-annotations")
    public boolean isIgnoreImportedDataAnnotations() {
        return ignoreImportedDataAnnotations;
    }

    @JIPipeParameter("ignore-imported-data-annotations")
    public void setIgnoreImportedDataAnnotations(boolean ignoreImportedDataAnnotations) {
        this.ignoreImportedDataAnnotations = ignoreImportedDataAnnotations;
    }

    @SetJIPipeDocumentation(name = "Merge imported text annotations", description = "Determines what happens when imported data has the same text annotations as the input folder.")
    @JIPipeParameter("text-annotation-merge-mode")
    public JIPipeTextAnnotationMergeMode getTextAnnotationMergeMode() {
        return textAnnotationMergeMode;
    }

    @JIPipeParameter("text-annotation-merge-mode")
    public void setTextAnnotationMergeMode(JIPipeTextAnnotationMergeMode textAnnotationMergeMode) {
        this.textAnnotationMergeMode = textAnnotationMergeMode;
    }

    @SetJIPipeDocumentation(name = "Merge imported data annotations", description = "Determines what happens when imported data has the same data annotations as the input folder.")
    @JIPipeParameter("data-annotation-merge-mode")
    public JIPipeDataAnnotationMergeMode getDataAnnotationMergeMode() {
        return dataAnnotationMergeMode;
    }

    @JIPipeParameter("data-annotation-merge-mode")
    public void setDataAnnotationMergeMode(JIPipeDataAnnotationMergeMode dataAnnotationMergeMode) {
        this.dataAnnotationMergeMode = dataAnnotationMergeMode;
    }
}
