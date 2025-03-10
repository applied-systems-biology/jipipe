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

package org.hkijena.jipipe.plugins.imp.datatypes;

import com.google.common.collect.ImmutableList;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.LabelAsJIPipeHeavyData;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeImageThumbnailData;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeThumbnailData;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.plugins.imp.utils.ImpImageUtils;
import org.hkijena.jipipe.utils.BufferedImageUtils;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SetJIPipeDocumentation(name = "IMP Image", description = "An image used by the Image Manipulation Pipeline")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains one image file with one of following extensions: *.tif, *.tiff, *.png, *.jpeg, *.jpg, *.png. " +
        "We recommend the usage of TIFF.", jsonSchemaURL = "https://jipipe.org/schemas/datatypes/imageplus-data.schema.json")
@LabelAsJIPipeHeavyData
public class ImpImageData implements JIPipeData {
    private final List<BufferedImage> images;
    private final int width;
    private final int height;
    private final int numSlices;
    private final int numFrames;
    private final int numChannels;

    public ImpImageData(BufferedImage image) {
        this.images = Arrays.asList(image);
        this.numSlices = 1;
        this.numFrames = 1;
        this.numChannels = 1;
        this.width = image.getWidth();
        this.height = image.getHeight();
    }

    public ImpImageData(Map<ImageSliceIndex, BufferedImage> images) {
        this.images = new ArrayList<>();
        this.numSlices = ImpImageUtils.findNSlices(images);
        this.numFrames = ImpImageUtils.findNFrames(images);
        this.numChannels = ImpImageUtils.findNChannels(images);
        this.width = ImpImageUtils.findWidth(images.values());
        this.height = ImpImageUtils.findHeight(images.values());

        for (int i = 0; i < numSlices * numFrames * numChannels; i++) {
            this.images.add(null);
        }
        for (Map.Entry<ImageSliceIndex, BufferedImage> entry : images.entrySet()) {
            this.images.set(entry.getKey().zeroSliceIndexToOneStackIndex(numChannels, numSlices, numFrames) - 1, entry.getValue());
        }

        if (images.size() != numSlices * numFrames * numChannels) {
            throw new IllegalArgumentException("Wrong hyperstack dimensions!");
        }
        if (images.isEmpty()) {
            throw new IllegalArgumentException("Empty image!");
        }
        for (BufferedImage image : this.images) {
            if (image == null) {
                throw new NullPointerException("Not all slice indices are present!");
            }
        }

    }

    public ImpImageData(ImagePlus img) {
        this.images = new ArrayList<>();
        this.numSlices = img.getNSlices();
        this.numFrames = img.getNFrames();
        this.numChannels = img.getNChannels();
        this.width = img.getWidth();
        this.height = img.getHeight();

        for (int i = 0; i < numSlices * numFrames * numChannels; i++) {
            this.images.add(null);
        }

        for (int c = 0; c < numChannels; c++) {
            for (int z = 0; z < numSlices; z++) {
                for (int t = 0; t < numFrames; t++) {
                    int i = ImageJUtils.zeroSliceIndexToOneStackIndex(c, z, t, numChannels, numSlices, numFrames);
                    ImageProcessor processor = img.getStack().getProcessor(i);
                    ImagePlus rendered = ImageJUtils.renderToRGBWithLUTIfNeeded(new ImagePlus("image", processor), new JIPipeProgressInfo());
                    this.images.set(i - 1, rendered.getProcessor().getBufferedImage());
                }
            }
        }
    }

    public ImpImageData(List<BufferedImage> images, int numSlices, int numFrames, int numChannels) {
        this.images = ImmutableList.copyOf(images);
        this.numSlices = numSlices;
        this.numFrames = numFrames;
        this.numChannels = numChannels;
        this.width = ImpImageUtils.findWidth(images);
        this.height = ImpImageUtils.findHeight(images);
        if (images.size() != numSlices * numFrames * numChannels) {
            throw new IllegalArgumentException("Wrong hyperstack dimensions!");
        }
        if (images.isEmpty()) {
            throw new IllegalArgumentException("Empty image!");
        }
    }

    public ImpImageData(ImpImageData other) {
        this.images = new ArrayList<>();
        for (BufferedImage image : other.images) {
            this.images.add(BufferedImageUtils.copyBufferedImage(image));
        }
        this.width = other.width;
        this.height = other.height;
        this.numSlices = other.numSlices;
        this.numFrames = other.numFrames;
        this.numChannels = other.numChannels;
    }

    public static ImpImageData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {

        List<Path> targetFiles = PathUtils.findFilesByExtensionIn(storage.getFileSystemPath(), ".tif", ".tiff", ".png", ".jpg", ".jpeg", ".bmp");
        if (targetFiles.isEmpty()) {
            throw new JIPipeValidationRuntimeException(
                    new FileNotFoundException("Unable to find file in location '" + storage + "'"),
                    "Could not find a compatible image file in '" + storage + "'!",
                    "JIPipe needs to load the image from a folder, but it could not find any matching file.",
                    "Please contact the JIPipe developers about this issue.");
        }

        if (targetFiles.size() == 1) {
            try {
                return new ImpImageData(ImageIO.read(targetFiles.get(0).toFile()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            Map<ImageSliceIndex, BufferedImage> imageMap = new HashMap<>();
            Pattern p = Pattern.compile("\\d+");

            for (Path targetFile : targetFiles) {
                String fileName = targetFile.getFileName().toString();
                String[] components = fileName.split("-");
                String lastComponent = components[components.length - 1].split("\\.")[0];

                List<Integer> coordinates = new ArrayList<>();
                Matcher matcher = p.matcher(lastComponent);
                while (matcher.find()) {
                    coordinates.add(Integer.parseInt(matcher.group()));
                }

                try {
                    BufferedImage image = ImageIO.read(targetFile.toFile());
                    imageMap.put(new ImageSliceIndex(coordinates.get(0), coordinates.get(1), coordinates.get(2)), image);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return new ImpImageData(imageMap);
        }

    }

    public List<BufferedImage> getImages() {
        return Collections.unmodifiableList(images);
    }

    public int getNumSlices() {
        return numSlices;
    }

    public int getNumFrames() {
        return numFrames;
    }

    public int getNumChannels() {
        return numChannels;
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        if (images.size() == 1) {
            String fileName = StringUtils.orElse(name, "image");
            Path outputPath = PathUtils.ensureExtension(storage.getFileSystemPath().resolve(fileName), ".png");
            try {
                ImageIO.write(images.get(0), "PNG", outputPath.toFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            ImpImageUtils.forEachIndexedZCTSlice(this, (ip, index) -> {
                String fileName = StringUtils.orElse(name, "image") + "-z" + index.getZ() + "c" + index.getC() + "t" + index.getT();
                Path outputPath = PathUtils.ensureExtension(storage.getFileSystemPath().resolve(fileName), ".png");
                try {
                    ImageIO.write(ip, "PNG", outputPath.toFile());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, progressInfo);
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new ImpImageData(this);
    }

    @Override
    public JIPipeThumbnailData createThumbnail(int width, int height, JIPipeProgressInfo progressInfo) {
        double factorX = 1.0 * width / getWidth();
        double factorY = 1.0 * height / getHeight();
        double factor = Math.min(factorX, factorY);
        boolean smooth = factor < 0;
        int imageWidth = (int) Math.max(1, getWidth() * factor);
        int imageHeight = (int) Math.max(1, getHeight() * factor);
        Image scaledInstance = images.get(0).getScaledInstance(imageWidth, imageHeight, Image.SCALE_SMOOTH);
        return new JIPipeImageThumbnailData(BufferedImageUtils.convertAlphaToCheckerboard(scaledInstance, 10));
    }

    public ImageSliceIndex getSliceIndex(int index) {
        int t = index / (numChannels * numSlices);
        int z = (index / numChannels) % numSlices;
        int c = index % numChannels;
        return new ImageSliceIndex(c, z, t);
    }

    public ImagePlus toImagePlus(boolean createCheckerboard, int checkerboardSize) {
        ImageStack stack = new ImageStack(width, height, numChannels * numFrames * numSlices);
        for (int i = 0; i < images.size(); i++) {
            ColorProcessor processor;
            if (createCheckerboard) {
                processor = new ColorProcessor(BufferedImageUtils.convertAlphaToCheckerboard(images.get(i), checkerboardSize));
            } else {
                processor = new ColorProcessor(images.get(i));
            }
            stack.setProcessor(processor, getSliceIndex(i).zeroSliceIndexToOneStackIndex(numChannels, numSlices, numFrames));
        }
        return new ImagePlus("Image", stack);
    }

    @Override
    public String toString() {
        return getWidth() + " x " + getHeight() + " x " + numSlices + " x " + numChannels + " x " + numFrames + " [" + BufferedImageUtils.getColorModelString(images.get(0)) + "]";
    }

    public BufferedImage getImage(int i) {
        return images.get(i);
    }

    public BufferedImage getImage(ImageSliceIndex index) {
        return images.get(index.zeroSliceIndexToOneStackIndex(numChannels, numSlices, numFrames) - 1);
    }

    public BufferedImage getImage(int c, int z, int t) {
        return getImage(ImageJUtils.zeroSliceIndexToOneStackIndex(c, z, t, numChannels, numSlices, numFrames) - 1);
    }

    public BufferedImage getImageOrExpand(ImageSliceIndex index) {
        return getImageOrExpand(index.getC(), index.getZ(), index.getT());
    }

    public BufferedImage getImageOrExpand(int c, int z, int t) {
        return getImage(ImageJUtils.zeroSliceIndexToOneStackIndex(Math.min(c, numChannels - 1),
                Math.min(z, numSlices - 1),
                Math.min(t, numFrames - 1),
                numChannels,
                numSlices,
                numFrames) - 1);
    }

    public int getSize() {
        return images.size();
    }

    public boolean isHyperstack() {
        return images.size() > 1;
    }

}
