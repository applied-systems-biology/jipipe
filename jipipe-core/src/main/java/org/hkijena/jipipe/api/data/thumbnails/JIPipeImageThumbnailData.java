package org.hkijena.jipipe.api.data.thumbnails;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.LabelAsJIPipeHeavyData;
import org.hkijena.jipipe.api.LabelAsJIPipeHidden;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.PathUtils;

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
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {

    }

    @Override
    public Component renderToComponent(int width, int height) {
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

    public static JIPipeImageThumbnailData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        Path targetFile = PathUtils.findFileByExtensionIn(storage.getFileSystemPath(), ".tif", ".tiff", ".png", ".jpg", ".jpeg", ".bmp");
        progressInfo.log("ImageJ import " + targetFile);
        ImagePlus outputImage = null;
        if(targetFile != null) {
            outputImage = IJ.openImage(targetFile.toString());
        }
        if(outputImage == null) {
            outputImage = IJ.createImage("empty", 16,16,1,24);
        }
        return new JIPipeImageThumbnailData(outputImage);
    }
}
