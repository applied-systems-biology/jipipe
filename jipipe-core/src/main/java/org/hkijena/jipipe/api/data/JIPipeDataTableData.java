package org.hkijena.jipipe.api.data;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.cache.JIPipeCacheDataSlotTableUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.nio.file.Path;

/**
 * {@link JIPipeData} that stores a {@link JIPipeDataSlot} (data table)
 */
@JIPipeDocumentation(name = "Data table", description = "A table of data")
@JIPipeDataStorageDocumentation("Stores a data table in the standard JIPipe format (data-table.json plus numeric slot folders)")
public class JIPipeDataTableData implements JIPipeData {

    private final JIPipeDataSlot dataSlot;

    public JIPipeDataTableData(JIPipeDataSlot dataSlot) {
        this.dataSlot = dataSlot;
    }

    /**
     * Imports this data from the path
     *
     * @param storagePath the storage path
     * @return the data
     */
    public static JIPipeDataTableData importFrom(Path storagePath) {
        JIPipeDataSlot slot = JIPipeDataSlot.loadFromStoragePath(storagePath, new JIPipeProgressInfo());
        return new JIPipeDataTableData(slot);
    }

    @Override
    public void saveTo(Path storageFilePath, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        if (forceName) {
            dataSlot.save(storageFilePath.resolve(name), null, progressInfo);
        } else {
            dataSlot.save(storageFilePath, null, progressInfo);
        }
    }

    @Override
    public JIPipeData duplicate() {
        JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
        JIPipeDataSlot copySlot = new JIPipeDataSlot(dataSlot.getInfo(), dataSlot.getNode());
        for (int row = 0; row < dataSlot.getRowCount(); row++) {
            JIPipeData copy = dataSlot.getData(row, JIPipeData.class, progressInfo).duplicate();
            copySlot.addData(copy, dataSlot.getAnnotations(row), JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
        }
        return new JIPipeDataTableData(copySlot);
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        JIPipeCacheDataSlotTableUI tableUI = new JIPipeCacheDataSlotTableUI(workbench, dataSlot);
        JFrame frame = new JFrame(displayName);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
        frame.setContentPane(tableUI);
        frame.pack();
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(workbench.getWindow());
        frame.setVisible(true);
    }

    @Override
    public String toString() {
        return dataSlot.getRowCount() + " rows of " + JIPipeData.getNameOf(dataSlot.getAcceptedDataType());
    }

    /**
     * Returns the stored data slot
     *
     * @return the data slot
     */
    public JIPipeDataSlot getDataSlot() {
        return dataSlot;
    }
}
