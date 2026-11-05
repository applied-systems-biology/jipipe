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

package org.hkijena.jipipe.plugins.filesystem.algorithms.local;


import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeZIPReadDataStorage;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.data.TableDataFunction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.filesystem.dataypes.FileData;

import org.hkijena.jipipe.utils.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SetJIPipeDocumentation(name = "Import data table (archive)", description = "Imports a data table from a ZIP archive. The ZIP file should contain a data-table.json file in its root, or in the folder given by the 'Internal path' parameter")
@ConfigureJIPipeNode(function = TableDataFunction.class)
@AddJIPipeInputSlot(value = FileData.class, name = "Archive", create = true)
@AddJIPipeOutputSlot(value = JIPipeData.class, name = "Data", create = true)
public class ImportDataTableArchive extends JIPipeSimpleIteratingAlgorithm {

    private String internalPath = "";

    private boolean ignoreInputTextAnnotations = false;
    private boolean ignoreInputDataAnnotations = false;
    private boolean ignoreImportedTextAnnotations = false;
    private boolean ignoreImportedDataAnnotations = false;
    private JIPipeTextAnnotationMergeMode textAnnotationMergeMode = JIPipeTextAnnotationMergeMode.Merge;
    private JIPipeDataAnnotationMergeMode dataAnnotationMergeMode = JIPipeDataAnnotationMergeMode.OverwriteExisting;

    public ImportDataTableArchive(JIPipeNodeInfo info) {
        super(info);
    }

    public ImportDataTableArchive(ImportDataTableArchive other) {
        super(other);
        this.internalPath = other.internalPath;
        this.ignoreInputTextAnnotations = other.ignoreInputTextAnnotations;
        this.ignoreInputDataAnnotations = other.ignoreInputDataAnnotations;
        this.ignoreImportedTextAnnotations = other.ignoreImportedTextAnnotations;
        this.ignoreImportedDataAnnotations = other.ignoreImportedDataAnnotations;
        this.textAnnotationMergeMode = other.textAnnotationMergeMode;
        this.dataAnnotationMergeMode = other.dataAnnotationMergeMode;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Path archiveFile = iterationStep.getInputData("Archive", FileData.class, progressInfo).toPath();
        try (JIPipeZIPReadDataStorage storage = new JIPipeZIPReadDataStorage(progressInfo, archiveFile)) {
            JIPipeReadDataStorage tableStorage;
            if (StringUtils.isNullOrEmpty(internalPath)) {
                tableStorage = storage;
            } else {
                // Normalize the user-provided path so that it matches the
                // internal (Unix-style) entry names of the archive
                String sanitized = internalPath.replace('\\', '/');
                while (sanitized.startsWith("/")) {
                    sanitized = sanitized.substring(1);
                }
                tableStorage = storage.resolve(Paths.get(sanitized).normalize());
            }
            if (!tableStorage.exists(Paths.get("data-table.json"))) {
                throw createMissingDataTableException(storage, tableStorage, archiveFile);
            }
            JIPipeDataTable dataTable = JIPipeDataTable.importData(tableStorage, progressInfo.resolve("Import"));
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a validation exception for the case that no 'data-table.json'
     * was found at the location given by the internal path parameter.
     * The error lists folders of the archive that contain candidate data
     * tables, so users can adapt the internal path themselves.
     *
     * @param storage      the root storage of the archive
     * @param tableStorage the storage where the data table was expected
     * @param archiveFile  the archive file
     * @return the exception
     */
    private JIPipeValidationRuntimeException createMissingDataTableException(JIPipeZIPReadDataStorage storage, JIPipeReadDataStorage tableStorage, Path archiveFile) {
        // Collect archive folders (other than the expected one) that contain
        // a data-table.json file, so the error can point users to them
        List<String> candidateFolders = new ArrayList<>();
        for (Path entry : storage.list()) {
            if (entry.getNameCount() >= 2 && entry.getFileName().toString().equals("data-table.json") && storage.isFile(entry)) {
                String folder = entry.getParent().toString();
                if (!candidateFolders.contains(folder)) {
                    candidateFolders.add(folder);
                }
            }
        }
        String expectedLocation;
        if (StringUtils.isNullOrEmpty(internalPath)) {
            expectedLocation = "its root";
        } else {
            expectedLocation = "the folder '" + internalPath + "'";
        }
        StringBuilder explanation = new StringBuilder();
        explanation.append("The ZIP archive '").append(archiveFile).append("' does not contain a 'data-table.json' file at ").append(expectedLocation).append(".");
        if (!candidateFolders.isEmpty()) {
            explanation.append(" Data tables were found in the following folders of the archive: ");
            for (int i = 0; i < candidateFolders.size(); i++) {
                if (i > 0) {
                    explanation.append(", ");
                }
                explanation.append("'").append(candidateFolders.get(i)).append("'");
            }
            explanation.append(".");
        }
        return new JIPipeValidationRuntimeException(new FileNotFoundException("No 'data-table.json' at " + tableStorage.getInternalPath()),
                "Data table archive contains no 'data-table.json'",
                explanation.toString(),
                "If the data table is located inside a subfolder of the archive, set the parameter 'Internal path' of this node to that folder (for example 'wrapped'). " +
                        "If the parameter is already set, verify that the folder contains a 'data-table.json' file.");
    }

    @SetJIPipeDocumentation(name = "Internal path", description = "The folder inside the ZIP file that contains the data table (the folder that contains the 'data-table.json' file). Leave empty to use the root of the ZIP file. Set this parameter if the data table is located inside a subfolder, for example if the archive wraps the data table into a directory")
    @JIPipeParameter("internal-path")
    public String getInternalPath() {
        return internalPath;
    }

    @JIPipeParameter("internal-path")
    public void setInternalPath(String internalPath) {
        this.internalPath = internalPath;
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
