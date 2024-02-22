package org.hkijena.jipipe.api.data.thumbnails;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.LabelAsJIPipeHidden;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import javax.swing.*;
import java.awt.*;

@SetJIPipeDocumentation(name = "Empty thumbnail", description = "Empty thumbnail data (used internally)")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Unknown storage schema (generic data)",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-empty-data.schema.json")
@LabelAsJIPipeHidden
public class JIPipeEmptyThumbnailData implements JIPipeThumbnailData {

    public JIPipeEmptyThumbnailData() {

    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {

    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new JIPipeEmptyThumbnailData();
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {

    }

    @Override
    public Component renderToComponent(int width, int height) {
        return new JLabel("N/A");
    }

    @Override
    public boolean hasSize() {
        return false;
    }

    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    public static JIPipeEmptyThumbnailData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return new JIPipeEmptyThumbnailData();
    }
}
