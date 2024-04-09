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

package org.hkijena.jipipe.api.data.thumbnails;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.LabelAsJIPipeHidden;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.utils.JIPipeSerializedJsonObjectData;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

@SetJIPipeDocumentation(name = "Text and icon thumbnail", description = "Text and icon thumbnail data (used internally)")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains a single *.txt file that stores the current string.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/string-data.schema.json")
@LabelAsJIPipeHidden
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
    public void display(String displayName, JIPipeDesktopWorkbench desktopWorkbench, JIPipeDataSource source) {

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
        } catch (NullPointerException e) {
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
