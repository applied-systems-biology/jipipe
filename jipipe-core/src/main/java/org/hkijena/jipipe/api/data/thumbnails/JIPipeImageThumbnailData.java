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

import ij.IJ;
import ij.ImagePlus;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.LabelAsJIPipeHeavyData;
import org.hkijena.jipipe.api.LabelAsJIPipeHidden;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.Objects;

@SetJIPipeDocumentation(name = "Image thumbnail", description = "Image thumbnail data (used internally)")
@LabelAsJIPipeHeavyData
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains one image file with one of following extensions: *.tif, *.tiff, *.png, *.jpeg, *.jpg, *.png. " +
        "We recommend the usage of TIFF.", jsonSchemaURL = "https://jipipe.org/schemas/datatypes/imageplus-data.schema.json")
@LabelAsJIPipeHidden
public class JIPipeImageThumbnailData implements JIPipeThumbnailData {

    private final ImagePlus image;

    public JIPipeImageThumbnailData(ImagePlus image) {
        this.image = Objects.requireNonNull(image);
    }

    public JIPipeImageThumbnailData(ImageProcessor processor) {
        this.image = new ImagePlus("thumbnail", processor);
    }

    public JIPipeImageThumbnailData(Image image) {
        this.image = new ImagePlus("thumbnail", new ColorProcessor(Objects.requireNonNull(image)));
    }

    public JIPipeImageThumbnailData(JIPipeImageThumbnailData other) {
        this.image = other.image;
    }

    public static JIPipeImageThumbnailData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        Path targetFile = PathUtils.findFileByExtensionIn(storage.getFileSystemPath(), ".tif", ".tiff", ".png", ".jpg", ".jpeg", ".bmp");
        progressInfo.log("ImageJ import " + targetFile);
        ImagePlus outputImage = null;
        if (targetFile != null) {
            outputImage = IJ.openImage(targetFile.toString());
        }
        if (outputImage == null) {
            outputImage = IJ.createImage("empty", 16, 16, 1, 24);
        }
        return new JIPipeImageThumbnailData(outputImage);
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        Path outputPath = PathUtils.ensureExtension(storage.getFileSystemPath().resolve(name), ".tif", ".tiff");
        IJ.saveAsTiff(image, outputPath.toString());
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new JIPipeImageThumbnailData(this);
    }

    @Override
    public Component renderToComponent(int width, int height) {
        if (image.getWidth() * image.getWidth() <= 0) {
            return new JLabel("Zero size!", UIUtils.getIconFromResources("emblems/vcs-conflicting.png"), JLabel.LEFT);
        }
        return new JLabel(new ImageIcon(image.getBufferedImage()));
    }

    @Override
    public boolean hasSize() {
        return true;
    }

    @Override
    public int getWidth() {
        return image.getWidth();
    }

    @Override
    public int getHeight() {
        return image.getHeight();
    }

    public ImagePlus getImage() {
        return image;
    }
}
