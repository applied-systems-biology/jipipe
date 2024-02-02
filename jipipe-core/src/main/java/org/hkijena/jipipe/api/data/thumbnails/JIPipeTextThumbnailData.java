package org.hkijena.jipipe.api.data.thumbnails;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHidden;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import javax.swing.*;
import java.awt.*;

@JIPipeDocumentation(name = "Text thumbnail", description = "Text thumbnail data (used internally)")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains a single *.txt file that stores the current string.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/string-data.schema.json")
@JIPipeHidden
public class JIPipeTextThumbnailData implements JIPipeThumbnailData {

    private final String text;

    public JIPipeTextThumbnailData(String text) {
        this.text = text;
    }

    public JIPipeTextThumbnailData(JIPipeTextThumbnailData other) {
        this.text = other.text;
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {

    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new JIPipeTextThumbnailData(this);
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {

    }

    public String getText() {
        return text;
    }

    @Override
    public Component renderToComponent(int width, int height) {
        return new JLabel(text);
    }
}
