package org.hkijena.jipipe.api.data;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;

import javax.swing.*;
import java.awt.Component;
import java.nio.file.Path;

@JIPipeDocumentation(name = "Merged data annotations", description = "A table of data")
@JIPipeDataStorageDocumentation("Stores a data table in the standard JIPipe format (data-table.json plus numeric slot folders)")
public class JIPipeMergedDataAnnotationsData extends JIPipeDataTableData {
    public JIPipeMergedDataAnnotationsData(JIPipeDataSlot dataSlot) {
        super(dataSlot);
    }

    @Override
    public Component preview(int width, int height) {
        return new JLabel(toString());
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
}
