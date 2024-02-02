package org.hkijena.jipipe.api.data.thumbnails;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHeavyData;
import org.hkijena.jipipe.api.JIPipeHidden;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import javax.swing.*;
import java.awt.*;

@JIPipeDocumentation(name = "Image thumbnail", description = "Image thumbnail data (used internally)")
@JIPipeHeavyData
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains one image file with one of following extensions: *.tif, *.tiff, *.png, *.jpeg, *.jpg, *.png. " +
        "We recommend the usage of TIFF.", jsonSchemaURL = "https://jipipe.org/schemas/datatypes/imageplus-data.schema.json")
@JIPipeHidden
public class JIPipeImageThumbnailData implements JIPipeThumbnailData {

    private final ImagePlus image;

    public JIPipeImageThumbnailData(ImagePlus image) {
        this.image = image;
    }

    public JIPipeImageThumbnailData(JIPipeImageThumbnailData other) {
        this.image = other.image;
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {

    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new JIPipeImageThumbnailData(this);
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {

    }

    @Override
    public Component renderToComponent(int width, int height) {
        return new JLabel(new ImageIcon(image.getBufferedImage()));
    }

    public ImagePlus getImage() {
        return image;
    }
}
