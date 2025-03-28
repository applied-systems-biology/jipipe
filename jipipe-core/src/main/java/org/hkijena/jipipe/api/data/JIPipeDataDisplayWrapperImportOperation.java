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

package org.hkijena.jipipe.api.data;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.browser.JIPipeLocalDataTableBrowser;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableRowInfo;
import org.hkijena.jipipe.api.data.sources.JIPipeDataTableDataSource;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemReadDataStorage;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.api.data.JIPipeDesktopDataDisplayOperation;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewerWindow;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.scijava.Disposable;

import javax.swing.*;
import java.nio.file.Path;

/**
 * A {@link JIPipeLegacyDataImportOperation} that wraps around a {@link JIPipeDesktopDataDisplayOperation}
 */
public class JIPipeDataDisplayWrapperImportOperation implements JIPipeLegacyDataImportOperation {
    private final JIPipeDesktopDataDisplayOperation displayOperation;

    public JIPipeDataDisplayWrapperImportOperation(JIPipeDesktopDataDisplayOperation displayOperation) {
        this.displayOperation = displayOperation;
    }

    public JIPipeDesktopDataDisplayOperation getDisplayOperation() {
        return displayOperation;
    }

    @Override
    public JIPipeData show(JIPipeDataSlot slot, JIPipeDataTableRowInfo row, String dataAnnotationName, Path rowStorageFolder, String compartmentName, String algorithmName, String displayName, JIPipeDesktopWorkbench workbench, JIPipeProgressInfo progressInfo) {
        ImportDataRun run = new ImportDataRun(rowStorageFolder, slot.getAcceptedDataType(), row);
        JIPipeRunnableQueue.getInstance().getFinishedEventEmitter().subscribeLambdaOnce((emitter, event) -> {
            if (event.getRun() == run) {
                JIPipeDataTable outputTable = run.getOutputTable();
                run.setOutputTable(null);
                JIPipeDataTableDataSource source = new JIPipeDataTableDataSource(outputTable, 0);
                JIPipeDesktopDataViewerWindow window = new JIPipeDesktopDataViewerWindow(workbench);
                window.setLocationRelativeTo(workbench.getWindow());
                window.setVisible(true);
                window.browseDataTable(new JIPipeLocalDataTableBrowser(source.getDataTable()), source.getRow(), source.getDataAnnotation(), displayName);
            }
        });
        JIPipeRunnableQueue.getInstance().enqueue(run);
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

    public static class ImportDataRun extends AbstractJIPipeRunnable implements Disposable {

        private final Path rowStorageFolder;
        private final Class<? extends JIPipeData> dataType;
        private final JIPipeDataTableRowInfo metadataRow;
        private JIPipeDataTable outputTable;

        public ImportDataRun(Path rowStorageFolder, Class<? extends JIPipeData> dataType, JIPipeDataTableRowInfo metadataRow) {
            this.rowStorageFolder = rowStorageFolder;
            this.dataType = dataType;
            this.outputTable = new JIPipeDataTable(dataType);
            this.metadataRow = metadataRow;
        }

        @Override
        public String getTaskLabel() {
            return "Import data for display";
        }

        @Override
        public void run() {
            JIPipeProgressInfo progressInfo = getProgressInfo();
            JIPipeData data = JIPipe.importData(new JIPipeFileSystemReadDataStorage(progressInfo, rowStorageFolder), dataType, progressInfo);
            outputTable.addData(data, metadataRow.getTextAnnotations(), JIPipeTextAnnotationMergeMode.OverwriteExisting, metadataRow.getDataContext(), progressInfo);
        }

        public JIPipeDataTable getOutputTable() {
            return outputTable;
        }

        public void setOutputTable(JIPipeDataTable outputTable) {
            this.outputTable = outputTable;
        }
    }
}
