package org.hkijena.jipipe.api.data;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemReadDataStorage;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;

import javax.swing.*;
import java.nio.file.Path;

/**
 * A {@link JIPipeDataImportOperation} that wraps around a {@link JIPipeDataDisplayOperation}
 */
public class JIPipeDataDisplayWrapperImportOperation implements JIPipeDataImportOperation {
    private final JIPipeDataDisplayOperation displayOperation;

    public JIPipeDataDisplayWrapperImportOperation(JIPipeDataDisplayOperation displayOperation) {
        this.displayOperation = displayOperation;
    }

    public JIPipeDataDisplayOperation getDisplayOperation() {
        return displayOperation;
    }

    @Override
    public JIPipeData show(JIPipeDataSlot slot, JIPipeDataTableMetadataRow row, String dataAnnotationName, Path rowStorageFolder, String compartmentName, String algorithmName, String displayName, JIPipeWorkbench workbench, JIPipeProgressInfo progressInfo) {
        ImportDataRun run = new ImportDataRun(rowStorageFolder, slot.getAcceptedDataType(), row);
        run.onFinished(event -> {
            JIPipeData data = run.getOutputTable().getData(0, slot.getAcceptedDataType(), progressInfo);
            JIPipeDataTableDataSource dataSource = new JIPipeDataTableDataSource(run.getOutputTable(), 0);
            data.display(displayName, workbench, dataSource);
        });
        JIPipeRunnerQueue.getInstance().enqueue(run);
        return null;
    }

    @Override
    public String getId() {
        return "display:" + displayOperation.getId();
    }

    @Override
    public String getName() {
        return displayOperation.getName();
    }

    @Override
    public String getDescription() {
        return displayOperation.getDescription();
    }

    @Override
    public int getOrder() {
        return displayOperation.getOrder();
    }

    @Override
    public Icon getIcon() {
        return displayOperation.getIcon();
    }

    public static class ImportDataRun implements JIPipeRunnable {

        private final Path rowStorageFolder;
        private final Class<? extends JIPipeData> dataType;
        private final JIPipeDataTableMetadataRow metadataRow;
        private JIPipeDataTable outputTable;
        private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

        public ImportDataRun(Path rowStorageFolder, Class<? extends JIPipeData> dataType, JIPipeDataTableMetadataRow metadataRow) {
            this.rowStorageFolder = rowStorageFolder;
            this.dataType = dataType;
            this.outputTable = new JIPipeDataTable(dataType);
            this.metadataRow = metadataRow;
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
            return "Import data for display";
        }

        @Override
        public void run() {
            try {
                JIPipeData data = JIPipe.importData(new JIPipeFileSystemReadDataStorage(progressInfo, rowStorageFolder), dataType, progressInfo);
                outputTable.addData(data, metadataRow.getTextAnnotations(), JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
            } finally {
                outputTable = null;
            }
        }

        public JIPipeDataTable getOutputTable() {
            return outputTable;
        }
    }
}
