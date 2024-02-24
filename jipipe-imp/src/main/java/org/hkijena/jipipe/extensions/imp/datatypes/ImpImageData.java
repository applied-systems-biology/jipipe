package org.hkijena.jipipe.extensions.imp.datatypes;

import ij.ImageStack;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.LabelAsJIPipeHeavyData;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.sources.JIPipeDataTableDataSource;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeImageThumbnailData;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeThumbnailData;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.extensions.imagejdatatypes.display.CachedImagePlusDataViewerWindow;
import org.hkijena.jipipe.extensions.imp.utils.ImpImageDataImageViewerCustomLoader;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.BufferedImageUtils;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

@SetJIPipeDocumentation(name = "IMP Image", description = "An image used by the Image Manipulation and Processing toolkit. Unlike ImageJ or OME images, " +
        "images of this type are 2D with an RGBA color space.")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains one image file with one of following extensions: *.tif, *.tiff, *.png, *.jpeg, *.jpg, *.png. " +
        "We recommend the usage of TIFF.", jsonSchemaURL = "https://jipipe.org/schemas/datatypes/imageplus-data.schema.json")
@LabelAsJIPipeHeavyData
public class ImpImageData implements JIPipeData {
    private final BufferedImage image;

    public ImpImageData(BufferedImage image) {
        this.image = image;
    }

    public BufferedImage getImage() {
        return image;
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        String fileName = StringUtils.orElse(name, "image");
        Path outputPath = PathUtils.ensureExtension(storage.getFileSystemPath().resolve(fileName), ".png");
        try {
            ImageIO.write(image, "PNG", outputPath.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new ImpImageData(BufferedImageUtils.copyBufferedImage(image));
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        CachedImagePlusDataViewerWindow window = new CachedImagePlusDataViewerWindow(workbench, JIPipeDataTableDataSource.wrap(this, source), displayName);
        window.setCustomDataLoader(new ImpImageDataImageViewerCustomLoader());
        window.setVisible(true);
        SwingUtilities.invokeLater(window::reloadDisplayedData);
    }

    @Override
    public JIPipeThumbnailData createThumbnail(int width, int height, JIPipeProgressInfo progressInfo) {
        double factorX = 1.0 * width / image.getWidth();
        double factorY = 1.0 * height / image.getHeight();
        double factor = Math.min(factorX, factorY);
        boolean smooth = factor < 0;
        int imageWidth = (int) Math.max(1, image.getWidth() * factor);
        int imageHeight = (int) Math.max(1, image.getHeight() * factor);
        Image scaledInstance = image.getScaledInstance(imageWidth, imageHeight, Image.SCALE_SMOOTH);
        return new JIPipeImageThumbnailData(BufferedImageUtils.convertAlphaToCheckerboard(scaledInstance, 10));
    }

    public static ImpImageData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        Path targetFile = PathUtils.findFileByExtensionIn(storage.getFileSystemPath(), ".tif", ".tiff", ".png", ".jpg", ".jpeg", ".bmp");
        if (targetFile == null) {
            throw new JIPipeValidationRuntimeException(
                    new FileNotFoundException("Unable to find file in location '" + storage + "'"),
                    "Could not find a compatible image file in '" + storage + "'!",
                    "JIPipe needs to load the image from a folder, but it could not find any matching file.",
                    "Please contact the JIPipe developers about this issue.");
        }
        try {
            return new ImpImageData(ImageIO.read(targetFile.toFile()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public BufferedImage getImageWithoutAlpha() {
        return BufferedImageUtils.convertAlphaToCheckerboard(image, 10);
    }

    @Override
    public String toString() {
        return image.getWidth() + " x " + image.getHeight() + " [" + BufferedImageUtils.getColorModelString(image) + "]";
    }
}
