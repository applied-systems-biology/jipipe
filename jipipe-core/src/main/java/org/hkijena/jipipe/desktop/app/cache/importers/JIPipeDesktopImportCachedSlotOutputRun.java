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

package org.hkijena.jipipe.desktop.app.cache.importers;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.cache.JIPipeLocalProjectMemoryCache;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeExportedDataAnnotation;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableMetadata;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableMetadataRow;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemReadDataStorage;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JIPipeDesktopImportCachedSlotOutputRun extends AbstractJIPipeRunnable {

    private final JIPipeProject project;
    private final JIPipeGraphNode graphNode;
    private final Path inputFolder;

    public JIPipeDesktopImportCachedSlotOutputRun(JIPipeProject project, JIPipeGraphNode graphNode, Path inputFolder) {

        this.project = project;
        this.graphNode = graphNode;
        this.inputFolder = inputFolder;
    }

    @Override
    public String getTaskLabel() {
        return null;
    }

    @Override
    public void run() {
        JIPipeProgressInfo progressInfo = getProgressInfo();
        Map<String, JIPipeDataSlot> loadedSlotMap = new HashMap<>();
        for (int i = 0; i < graphNode.getOutputSlots().size(); i++) {
            JIPipeProgressInfo slotProgressInfo = progressInfo.resolveAndLog("Output slot", i, graphNode.getOutputSlots().size());
            JIPipeDataSlot outputSlot = graphNode.getOutputSlots().get(i);
            slotProgressInfo.log("Slot '" + outputSlot.getName() + "'");
            Path slotFolder = inputFolder.resolve(outputSlot.getName());
            if (!Files.isDirectory(slotFolder)) {
                slotProgressInfo.log("Folder " + slotFolder + " does not exist. Skipping.");
                continue;
            }
            if (!Files.exists(slotFolder.resolve("data-table.json"))) {
                slotProgressInfo.log("Folder " + slotFolder + " does not contain data-table.json. Skipping.");
                continue;
            }
            JIPipeDataSlot tempSlot = outputSlot.getInfo().createInstance(graphNode);
            importIntoTempSlot(tempSlot, slotFolder, slotProgressInfo);

            loadedSlotMap.put(outputSlot.getName(), tempSlot);
        }

        // Push into cache
        JIPipeLocalProjectMemoryCache cache = project.getCache();
        for (int i = 0; i < graphNode.getOutputSlots().size(); i++) {
            JIPipeProgressInfo slotProgressInfo = progressInfo.resolveAndLog("Storing into cache", i, graphNode.getOutputSlots().size());
            JIPipeDataSlot outputSlot = graphNode.getOutputSlots().get(i);
            JIPipeDataSlot tempSlot = loadedSlotMap.getOrDefault(outputSlot.getName(), null);
            if (tempSlot == null)
                continue;
            slotProgressInfo.log("Slot '" + outputSlot.getName() + "'");
            cache.store(graphNode, graphNode.getUUIDInParentGraph(), tempSlot, tempSlot.getName(), slotProgressInfo);
        }
    }

    private void importIntoTempSlot(JIPipeDataSlot tempSlot, Path dataFolder, JIPipeProgressInfo slotProgressInfo) {
        JIPipeProgressInfo progressInfo = getProgressInfo();
        slotProgressInfo.log("Importing from " + dataFolder);
        if (!Files.exists(dataFolder.resolve("data-table.json"))) {
            slotProgressInfo.log("Error: data-table.json missing");
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, new GraphNodeValidationReportContext(graphNode),
                    "Missing data-table.json!",
                    "Wrong input folder!",
                    "You tried to import data from a JIPipe output slot folder located at " + dataFolder + ". JIPipe has a very specific format to store such folders. The directory seems to not conform to this format.",
                    "Check if the folder contains many numeric subfolders and a data-table.json file."));
        }
        JIPipeDataTableMetadata exportedDataTable = JIPipeDataTableMetadata.loadFromJson(dataFolder.resolve("data-table.json"));
        Class<? extends JIPipeData> dataType = JIPipe.getDataTypes().getById(exportedDataTable.getAcceptedDataTypeId());
        if (dataType == null) {
            slotProgressInfo.log("Error: Unknown data type id " + exportedDataTable.getAcceptedDataTypeId());
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, new GraphNodeValidationReportContext(graphNode),
                    "Unknown data type id: " + exportedDataTable.getAcceptedDataTypeId(),
                    "Unknown data type",
                    "You tried to import data from a JIPipe output slot folder located at " + dataFolder + ". The data contained in this folder is identified by a type id '" + exportedDataTable.getAcceptedDataTypeId() + "', but it could not be found.",
                    "Check if you installed the necessary plugins and extensions."));
        }

        for (JIPipeDataTableMetadataRow row : exportedDataTable.getRowList()) {
            slotProgressInfo.log("Importing data row " + row.getIndex());
            Path storageFolder = dataFolder.resolve("" + row.getIndex());
            List<JIPipeTextAnnotation> annotationList = row.getTextAnnotations();
            JIPipeDataInfo trueDataType = exportedDataTable.getDataTypeOf(row.getIndex());
            JIPipeData data = JIPipe.importData(new JIPipeFileSystemReadDataStorage(progressInfo, storageFolder), trueDataType.getDataClass(), progressInfo);
            tempSlot.addData(data, annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting, row.getDataContext(), slotProgressInfo);

            for (JIPipeExportedDataAnnotation dataAnnotation : row.getDataAnnotations()) {
                try {
                    JIPipeDataInfo dataAnnotationDataTypeInfo = JIPipeDataInfo.getInstance(dataAnnotation.getTrueDataType());
                    JIPipeData dataAnnotationData = JIPipe.importData(new JIPipeFileSystemReadDataStorage(progressInfo, dataFolder.resolve(dataAnnotation.getRowStorageFolder())),
                            dataAnnotationDataTypeInfo.getDataClass(),
                            progressInfo);
                    tempSlot.setDataAnnotation(tempSlot.getRowCount() - 1, dataAnnotation.getName(), dataAnnotationData);
                } catch (Exception e) {
                    slotProgressInfo.log("Error: " + e);
                    e.printStackTrace();
                }
            }
        }
    }
}
