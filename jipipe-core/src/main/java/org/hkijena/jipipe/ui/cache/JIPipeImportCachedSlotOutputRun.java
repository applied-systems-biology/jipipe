package org.hkijena.jipipe.ui.cache;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.*;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemReadStorage;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JIPipeImportCachedSlotOutputRun implements JIPipeRunnable {

    private final JIPipeProject project;
    private final JIPipeGraphNode graphNode;
    private final Path inputFolder;
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

    public JIPipeImportCachedSlotOutputRun(JIPipeProject project, JIPipeGraphNode graphNode, Path inputFolder) {

        this.project = project;
        this.graphNode = graphNode;
        this.inputFolder = inputFolder;
    }

    @Override
    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }

    @Override
    public void setProgressInfo(JIPipeProgressInfo progressInfo) {
        this.progressInfo = progressInfo;
    }

    @Override
    public String getTaskLabel() {
        return null;
    }

    @Override
    public void run() {
        Map<String, JIPipeDataSlot> loadedSlotMap = new HashMap<>();
        for (int i = 0; i < graphNode.getOutputSlots().size(); i++) {
            JIPipeProgressInfo slotProgressInfo = this.progressInfo.resolveAndLog("Output slot", i, graphNode.getOutputSlots().size());
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
            JIPipeDataSlot tempSlot = new JIPipeDataSlot(outputSlot.getInfo(), graphNode);
            importIntoTempSlot(tempSlot, slotFolder, slotProgressInfo);

            loadedSlotMap.put(outputSlot.getName(), tempSlot);
        }

        // Push into cache
        JIPipeProjectCacheQuery query = new JIPipeProjectCacheQuery(project);
        JIPipeProjectCacheState state = query.getCachedId(graphNode);
        for (int i = 0; i < graphNode.getOutputSlots().size(); i++) {
            JIPipeProgressInfo slotProgressInfo = this.progressInfo.resolveAndLog("Storing into cache", i, graphNode.getOutputSlots().size());
            JIPipeDataSlot outputSlot = graphNode.getOutputSlots().get(i);
            JIPipeDataSlot tempSlot = loadedSlotMap.getOrDefault(outputSlot.getName(), null);
            if (tempSlot == null)
                continue;
            slotProgressInfo.log("Slot '" + outputSlot.getName() + "'");
            project.getCache().store(graphNode, state, tempSlot, slotProgressInfo);
        }
    }

    private void importIntoTempSlot(JIPipeDataSlot tempSlot, Path dataFolder, JIPipeProgressInfo slotProgressInfo) {
        slotProgressInfo.log("Importing from " + dataFolder);
        if (!Files.exists(dataFolder.resolve("data-table.json"))) {
            slotProgressInfo.log("Error: data-table.json missing");
            throw new UserFriendlyRuntimeException("Missing data-table.json!",
                    "Wrong input folder!",
                    "Import cache into algorithm " + graphNode.getName(),
                    "You tried to import data from a JIPipe output slot folder located at " + dataFolder + ". JIPipe has a very specific format to store such folders. The directory seems to not conform to this format.",
                    "Check if the folder contains many numeric subfolders and a data-table.json file.");
        }
        JIPipeDataTableMetadata exportedDataTable = JIPipeDataTableMetadata.loadFromJson(dataFolder.resolve("data-table.json"));
        Class<? extends JIPipeData> dataType = JIPipe.getDataTypes().getById(exportedDataTable.getAcceptedDataTypeId());
        if (dataType == null) {
            slotProgressInfo.log("Error: Unknown data type id " + exportedDataTable.getAcceptedDataTypeId());
            throw new UserFriendlyRuntimeException("Unknown data type id: " + exportedDataTable.getAcceptedDataTypeId(),
                    "Unknown data type",
                    "Import cache into algorithm " + graphNode.getName(),
                    "You tried to import data from a JIPipe output slot folder located at " + dataFolder + ". The data contained in this folder is identified by a type id '" + exportedDataTable.getAcceptedDataTypeId() + "', but it could not be found.",
                    "Check if you installed the necessary plugins and extensions.");
        }

        for (JIPipeDataTableMetadataRow row : exportedDataTable.getRowList()) {
            slotProgressInfo.log("Importing data row " + row.getIndex());
            Path storageFolder = dataFolder.resolve("" + row.getIndex());
            List<JIPipeTextAnnotation> annotationList = row.getTextAnnotations();
            JIPipeDataInfo trueDataType = exportedDataTable.getDataTypeOf(row.getIndex());
            JIPipeData data = JIPipe.importData(new JIPipeFileSystemReadStorage(storageFolder), trueDataType.getDataClass(), progressInfo);
            tempSlot.addData(data, annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting, slotProgressInfo);

            for (JIPipeExportedDataAnnotation dataAnnotation : row.getDataAnnotations()) {
                try {
                    JIPipeDataInfo dataAnnotationDataTypeInfo = JIPipeDataInfo.getInstance(dataAnnotation.getTrueDataType());
                    JIPipeData dataAnnotationData = JIPipe.importData(new JIPipeFileSystemReadStorage(dataFolder.resolve(dataAnnotation.getRowStorageFolder())),
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
