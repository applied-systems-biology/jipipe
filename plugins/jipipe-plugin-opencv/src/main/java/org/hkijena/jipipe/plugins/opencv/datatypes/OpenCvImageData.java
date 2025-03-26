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

package org.hkijena.jipipe.plugins.opencv.datatypes;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_imgcodecs;
import org.bytedeco.javacpp.opencv_imgproc;
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
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;
import org.hkijena.jipipe.plugins.opencv.utils.OpenCvImageUtils;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SetJIPipeDocumentation(name = "OpenCV Image", description = "An OpenCV image")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains one image file with one of following extensions: *.tif, *.tiff, *.png, *.jpeg, *.jpg, *.png. " +
        "We recommend the usage of TIFF.", jsonSchemaURL = "https://jipipe.org/schemas/datatypes/imageplus-data.schema.json")
@LabelAsJIPipeHeavyData
public class OpenCvImageData implements JIPipeData {
    private final opencv_core.MatVector images;
    private final int width;
    private final int height;
    private final int numSlices;
    private final int numFrames;
    private final int numChannels;

    public OpenCvImageData(opencv_core.Mat image) {
        this.images = new opencv_core.MatVector();
        this.images.push_back(image);
        this.numSlices = 1;
        this.numFrames = 1;
        this.numChannels = 1;
        this.width = image.cols();
        this.height = image.rows();
    }

    public OpenCvImageData(Map<ImageSliceIndex, opencv_core.Mat> images) {
        this.images = new opencv_core.MatVector();
        this.numSlices = OpenCvImageUtils.findNSlices(images);
        this.numFrames = OpenCvImageUtils.findNFrames(images);
        this.numChannels = OpenCvImageUtils.findNChannels(images);
        this.width = OpenCvImageUtils.findWidth(images.values());
        this.height = OpenCvImageUtils.findHeight(images.values());

        this.images.resize(numSlices * numFrames * numChannels);
        for (Map.Entry<ImageSliceIndex, opencv_core.Mat> entry : images.entrySet()) {
            this.images.put(entry.getKey().zeroSliceIndexToOneStackIndex(numChannels, numSlices, numFrames) - 1, entry.getValue());
        }

        if (images.size() != numSlices * numFrames * numChannels) {
            throw new IllegalArgumentException("Wrong hyperstack dimensions!");
        }
        if (images.isEmpty()) {
            throw new IllegalArgumentException("Empty image!");
        }
        for (long i = 0; i < this.images.size(); i++) {
            opencv_core.Mat image = this.images.get(i);
            if (image == null) {
                throw new NullPointerException("Not all slice indices are present!");
            }
        }
    }

    public OpenCvImageData(ImagePlus img) {
        this.images = new opencv_core.MatVector();
        this.numSlices = img.getNSlices();
        this.numFrames = img.getNFrames();
        this.numChannels = img.getNChannels();
        this.width = img.getWidth();
        this.height = img.getHeight();
        this.images.resize(numSlices * numFrames * numChannels);

        for (int c = 0; c < numChannels; c++) {
            for (int z = 0; z < numSlices; z++) {
                for (int t = 0; t < numFrames; t++) {
                    int i = ImageJUtils.zeroSliceIndexToOneStackIndex(c, z, t, numChannels, numSlices, numFrames);
                    ImageProcessor processor = img.getStack().getProcessor(i);
                    opencv_core.Mat mat = OpenCvImageUtils.toMat(processor);
                    this.images.put(i - 1, mat);
                }
            }
        }
    }

    public OpenCvImageData(opencv_core.MatVector images, int numSlices, int numFrames, int numChannels) {
        this.images = new opencv_core.MatVector();
        this.numSlices = numSlices;
        this.numFrames = numFrames;
        this.numChannels = numChannels;
        this.width = OpenCvImageUtils.findWidth(images);
        this.height = OpenCvImageUtils.findHeight(images);
        for (long i = 0; i < this.images.size(); i++) {
            opencv_core.Mat image = images.get(i);
            this.images.push_back(image);
        }
        if (images.size() != (long) numSlices * numFrames * numChannels) {
            throw new IllegalArgumentException("Wrong hyperstack dimensions!");
        }
        if (images.empty()) {
            throw new IllegalArgumentException("Empty image!");
        }
    }

    public OpenCvImageData(OpenCvImageData other) {
        this.images = new opencv_core.MatVector();
        for (long i = 0; i < this.images.size(); i++) {
            opencv_core.Mat image = images.get(i).clone();
            this.images.push_back(image);
        }
        this.width = other.width;
        this.height = other.height;
        this.numSlices = other.numSlices;
        this.numFrames = other.numFrames;
        this.numChannels = other.numChannels;
    }

    public static OpenCvImageData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {

        List<Path> targetFiles = PathUtils.findFilesByExtensionIn(storage.getFileSystemPath(), ".tif", ".tiff", ".png", ".jpg", ".jpeg", ".bmp");
        if (targetFiles.isEmpty()) {
            throw new JIPipeValidationRuntimeException(
                    new FileNotFoundException("Unable to find file in location '" + storage + "'"),
                    "Could not find a compatible image file in '" + storage + "'!",
                    "JIPipe needs to load the image from a folder, but it could not find any matching file.",
                    "Please contact the JIPipe developers about this issue.");
        }

        if (targetFiles.size() == 1) {
            ImagePlus imagePlus = IJ.openImage(targetFiles.get(0).toString());
            return new OpenCvImageData(imagePlus);
        } else {
            Map<ImageSliceIndex, opencv_core.Mat> imageMap = new HashMap<>();
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

                ImagePlus imagePlus = IJ.openImage(targetFile.toString());
                imageMap.put(new ImageSliceIndex(coordinates.get(0), coordinates.get(1), coordinates.get(2)), OpenCvImageUtils.toMat(imagePlus.getProcessor()));
            }
            return new OpenCvImageData(imageMap);
        }

    }

    public opencv_core.MatVector getImages() {
        return images;
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
            Path outputPath = PathUtils.ensureExtension(storage.getFileSystemPath().resolve(fileName), ".tif");
            try {
                if (!opencv_imgcodecs.imwrite(outputPath.toString(), images.get(0))) {
                    throw new IOException("Unable to write image!");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            OpenCvImageUtils.forEachIndexedZCTSlice(this, (ip, index) -> {
                String fileName = StringUtils.orElse(name, "image") + "-z" + index.getZ() + "c" + index.getC() + "t" + index.getT();
                Path outputPath = PathUtils.ensureExtension(storage.getFileSystemPath().resolve(fileName), ".tif");
                try {
                    if (!opencv_imgcodecs.imwrite(outputPath.toString(), ip)) {
                        throw new IOException("Unable to write image!");
                    }
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
        return new OpenCvImageData(this);
    }

    @Override
    public JIPipeThumbnailData createThumbnail(int width, int height, JIPipeProgressInfo progressInfo) {
        double factorX = 1.0 * width / getWidth();
        double factorY = 1.0 * height / getHeight();
        double factor = Math.min(factorX, factorY);
        int imageWidth = (int) Math.max(1, getWidth() * factor);
        int imageHeight = (int) Math.max(1, getHeight() * factor);

        try (opencv_core.Mat scaledInstance = new opencv_core.Mat()) {
            opencv_imgproc.resize(images.get(0), scaledInstance, new opencv_core.Size(imageWidth, imageHeight));
            return new JIPipeImageThumbnailData(OpenCvImageUtils.toImagePlus(scaledInstance));
        }
    }

    public ImageSliceIndex getSliceIndex(int index) {
        int t = index / (numChannels * numSlices);
        int z = (index / numChannels) % numSlices;
        int c = index % numChannels;
        return new ImageSliceIndex(c, z, t);
    }

    public ImagePlus toImagePlus() {
        ImageStack stack = new ImageStack(width, height, numChannels * numFrames * numSlices);
        for (int i = 0; i < images.size(); i++) {
            stack.setProcessor(OpenCvImageUtils.toProcessor(images.get(i)),
                    getSliceIndex(i).zeroSliceIndexToOneStackIndex(numChannels, numSlices, numFrames));
        }
        return new ImagePlus("Image", stack);
    }

    @Override
    public void close() {
        JIPipeData.super.close();
        images.close();
    }

    @Override
    public String toString() {
        return getWidth() + " x " + getHeight() + " x " + numSlices + " x " + numChannels + " x " + numFrames + " [" + OpenCvImageUtils.getTypeName(images.get(0)) + "]";
    }

    public opencv_core.Mat getImage(int i) {
        return images.get(i);
    }

    public opencv_core.Mat getImage(ImageSliceIndex index) {
        return images.get(index.zeroSliceIndexToOneStackIndex(numChannels, numSlices, numFrames) - 1);
    }

    public opencv_core.Mat getImage(int c, int z, int t) {
        return getImage(ImageJUtils.zeroSliceIndexToOneStackIndex(c, z, t, numChannels, numSlices, numFrames) - 1);
    }

    public opencv_core.Mat getImageOrExpand(ImageSliceIndex index) {
        return getImageOrExpand(index.getC(), index.getZ(), index.getT());
    }

    public opencv_core.Mat getImageOrExpand(int c, int z, int t) {
        return getImage(ImageJUtils.zeroSliceIndexToOneStackIndex(Math.min(c, numChannels - 1),
                Math.min(z, numSlices - 1),
                Math.min(t, numFrames - 1),
                numChannels,
                numSlices,
                numFrames) - 1);
    }

    public int getSize() {
        return (int) images.size();
    }

    public boolean isHyperstack() {
        return images.size() > 1;
    }

}
