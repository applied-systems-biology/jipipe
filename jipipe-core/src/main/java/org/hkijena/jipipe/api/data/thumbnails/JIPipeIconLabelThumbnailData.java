package org.hkijena.jipipe.api.data.thumbnails;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.base.Charsets;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHidden;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.api.data.utils.JIPipeSerializedJsonObjectData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@JIPipeDocumentation(name = "Text and icon thumbnail", description = "Text and icon thumbnail data (used internally)")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains a single *.txt file that stores the current string.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/string-data.schema.json")
@JIPipeHidden
public class JIPipeIconLabelThumbnailData extends JIPipeSerializedJsonObjectData implements JIPipeThumbnailData {

    private String text;
    private String icon;

    public JIPipeIconLabelThumbnailData(String text, String icon) {
        this.text = text;
        this.icon = icon;
    }

    public JIPipeIconLabelThumbnailData(JIPipeIconLabelThumbnailData other) {
        this.text = other.text;
        this.icon = other.icon;
    }

    public static JIPipeIconLabelThumbnailData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
      return JIPipeSerializedJsonObjectData.importData(storage, JIPipeIconLabelThumbnailData.class);
    }


    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new JIPipeIconLabelThumbnailData(this);
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {

    }

    @JsonGetter("text")
    public String getText() {
        return text;
    }

    @JsonSetter("text")
    public void setText(String text) {
        this.text = text;
    }

    @JsonGetter("icon")
    public String getIcon() {
        return icon;
    }

    @JsonSetter("icon")
    public void setIcon(String icon) {
        this.icon = icon;
    }

    @Override
    public Component renderToComponent(int width, int height) {
        JLabel label = new JLabel(text);
        try {
            ImageIcon iconFromResources = UIUtils.getIconFromResources(icon);
            label.setIcon(iconFromResources);
        }
        catch (NullPointerException e) {
            e.printStackTrace();
        }
        return label;
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
}
