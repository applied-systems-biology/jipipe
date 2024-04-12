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

package org.hkijena.jipipe.plugins.opencv.utils;

import com.google.common.primitives.Ints;
import ij.ImagePlus;
import ij.process.*;
import net.imagej.opencv.MatToImgConverter;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import org.bytedeco.javacpp.indexer.*;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.plugins.opencv.datatypes.OpenCvImageData;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class OpenCvImageUtils {
    private OpenCvImageUtils() {

    }

    public static int findNSlices(Map<ImageSliceIndex, Mat> images) {
        return images.keySet().stream().map(ImageSliceIndex::getZ).max(Comparator.naturalOrder()).orElse(-1) + 1;
    }

    public static int findNFrames(Map<ImageSliceIndex, Mat> images) {
        return images.keySet().stream().map(ImageSliceIndex::getT).max(Comparator.naturalOrder()).orElse(-1) + 1;
    }

    public static int findNChannels(Map<ImageSliceIndex, Mat> images) {
        return images.keySet().stream().map(ImageSliceIndex::getC).max(Comparator.naturalOrder()).orElse(-1) + 1;
    }

    public static int findWidth(Collection<Mat> images) {
        int width = 0;
        for (Mat image : images) {
            if (width != 0 && width != image.cols()) {
                throw new IllegalArgumentException("Inconsistent image width!");
            }
            width = image.cols();
        }
        return width;
    }

    public static int findHeight(Collection<Mat> images) {
        int height = 0;
        for (Mat image : images) {
            if (height != 0 && height != image.rows()) {
                throw new IllegalArgumentException("Inconsistent image height!");
            }
            height = image.rows();
        }
        return height;
    }

    public static int findWidth(MatVector images) {
        int width = 0;
        for (long i = 0; i < images.size(); i++) {
            Mat image = images.get(i);
            if (width != 0 && width != image.cols()) {
                throw new IllegalArgumentException("Inconsistent image width!");
            }
            width = image.cols();
        }
        return width;
    }

    public static int findHeight(MatVector images) {
        int height = 0;
        for (long i = 0; i < images.size(); i++) {
            Mat image = images.get(i);
            if (height != 0 && height != image.rows()) {
                throw new IllegalArgumentException("Inconsistent image height!");
            }
            height = image.rows();
        }
        return height;
    }

    public static Mat toMat(ImageProcessor processor) {
//        ImagePlus imagePlus = new ImagePlus("img", processor);
//        Img wrapped = ImageJFunctions.wrap(imagePlus);
//        return ImgToMatConverter.toMat(wrapped);
        if(processor instanceof ByteProcessor) {
            return convertByteProcessorToMat((ByteProcessor) processor);
        }
        else if(processor instanceof FloatProcessor) {
            return convertFloatProcessorToMat((FloatProcessor) processor);
        }
        else if(processor instanceof ColorProcessor) {
            return convertColorProcessorToMat((ColorProcessor) processor);
        }
        else if(processor instanceof  ShortProcessor) {
            return convertShortProcessorToMat((ShortProcessor) processor);
        }
        else {
            throw new UnsupportedOperationException("Unknown processor type: " + processor);
        }
    }

    /**
     * Runs the function for each Z, C, and T slice.
     *
     * @param img          the image
     * @param function     the function. The indices are ZERO-based
     * @param progressInfo the progress
     */
    public static void forEachIndexedZCTSlice(OpenCvImageData img, BiConsumer<Mat, ImageSliceIndex> function, JIPipeProgressInfo progressInfo) {
        if (img.getImages().size() > 1) {
            int iterationIndex = 0;
            for (int t = 0; t < img.getNumFrames(); t++) {
                for (int z = 0; z < img.getNumSlices(); z++) {
                    for (int c = 0; c < img.getNumChannels(); c++) {
                        if (progressInfo.isCancelled())
                            return;
                        progressInfo.resolveAndLog("Slice", iterationIndex++, img.getSize()).log("z=" + z + ", c=" + c + ", t=" + t);
                        Mat processor = img.getImage(c, z, t);
                        function.accept(processor, new ImageSliceIndex(c, z, t));
                    }
                }
            }
        } else {
            function.accept(img.getImage(0), new ImageSliceIndex(0, 0, 0));
        }
    }

    /**
     * Runs the function for each Z, C, and T slice.
     *
     * @param img          the image
     * @param function     the function. The indices are ZERO-based
     * @param progressInfo the progress
     */
    public static OpenCvImageData generateForEachIndexedZCTSlice(OpenCvImageData img, BiFunction<Mat, ImageSliceIndex, Mat> function, JIPipeProgressInfo progressInfo) {
        Map<ImageSliceIndex, Mat> result = new HashMap<>();
        if (img.getImages().size() > 1) {
            int iterationIndex = 0;
            for (int t = 0; t < img.getNumFrames(); t++) {
                for (int z = 0; z < img.getNumSlices(); z++) {
                    for (int c = 0; c < img.getNumChannels(); c++) {
                        if (progressInfo.isCancelled())
                            return null;
                        progressInfo.resolveAndLog("Slice", iterationIndex++, img.getSize()).log("z=" + z + ", c=" + c + ", t=" + t);
                        Mat processor = img.getImage(c, z, t);
                        ImageSliceIndex index = new ImageSliceIndex(c, z, t);
                        result.put(index, function.apply(processor, index));
                    }
                }
            }
        } else {
            Mat processor = img.getImage(0,0,0);
            ImageSliceIndex index = new ImageSliceIndex(0,0,0);
            result.put(index, function.apply(processor, index));
        }
        return new OpenCvImageData(result);
    }

    public static ImageProcessor toProcessor(Mat mat) {
//        return ImageJFunctions.wrap((RandomAccessibleInterval) MatToImgConverter.convert(mat), "img").getProcessor();
        if(mat.channels() == 3) {
            return convertMatToColorProcessor(mat);
        }
        else if(mat.channels() == 1) {
            if(mat.depth() == opencv_core.CV_8U) {
                return convertMatToByteProcessor(mat);
            }
            else  if(mat.depth() == opencv_core.CV_16U) {
                return convertMatToShortProcessor(mat);
            }
            else {
                return convertMatToFloatProcessor(mat);
            }
        }
        else {
            throw new UnsupportedOperationException("Unable to convert non-grayscale images of unsupported types to ImageJ processor!");
        }
    }

    public static ImagePlus toImagePlus(Mat mat) {
        return new ImagePlus(mat.toString(), toProcessor(mat));
    }

    public static Mat toGrayscale(Mat src, int type) {
        if(src.type() != type) {
            Mat dst = new Mat();
            if(src.channels() == 3) {
                opencv_imgproc.cvtColor(src, dst, opencv_imgproc.COLOR_BGR2GRAY);
            }
            if(dst.type() != type) {
                dst.convertTo(src, type);
            }
            else {
                return dst;
            }
        }
        return src;
    }

    public static Mat toType(Mat src, OpenCvType type, OpenCvType... allowedTypes) {
        List<Integer> allowedTypesList = new ArrayList<>();
        allowedTypesList.add(type.getNativeValue());
        for (OpenCvType allowedType : allowedTypes) {
            allowedTypesList.add(allowedType.getNativeValue());
        }

        if(!allowedTypesList.contains(src.type())) {
            Mat dst = new Mat();

            // Channel conversion
            if(src.channels() == 3 && dst.channels() == 1) {
                // BGR2GRAY
                opencv_imgproc.cvtColor(src, dst, opencv_imgproc.COLOR_BGR2GRAY);
                opencv_core.swap(src, dst);
            }
            else if(src.channels() != type.getChannels()) {
                // Expand the channels
                MatVector toMerge = new MatVector();
                for (int c = 0; c < type.getChannels(); c++) {
                    Mat singleChannel = new Mat();
                    opencv_core.extractChannel(src, singleChannel, Math.min(c, src.channels() - 1));
                    toMerge.push_back(singleChannel);
                }
                opencv_core.merge(toMerge, dst);
                toMerge.close();
                opencv_core.swap(src, dst);
            }

            // Depth conversion
            src.convertTo(dst, type.getDepth());
            return dst;
        }
        else {
            return src;
        }
    }

    public static Mat toMask(Mat src) {
        src = toGrayscale(src, opencv_core.CV_8U);
        Mat mask = new Mat();
        opencv_imgproc.threshold(src, mask, 1, 255, opencv_imgproc.THRESH_BINARY);
        return mask;
    }

    public static String getTypeName(Mat mat) {
        String typeInfo = "";
        switch (mat.depth()) {
            case opencv_core.CV_8S:
                typeInfo += "8S";
                break;
            case opencv_core.CV_8U:
                typeInfo += "8U";
                break;
            case opencv_core.CV_16S:
                typeInfo += "16S";
                break;
            case opencv_core.CV_16U:
                typeInfo += "16U";
                break;
            case opencv_core.CV_32S:
                typeInfo += "32S";
                break;
            case opencv_core.CV_32F:
                typeInfo += "32F";
                break;
            default:
                typeInfo += mat.depth() + "?";
        }
        typeInfo += "C" + mat.channels();
        return typeInfo;
    }

    public static Mat convertByteProcessorToMat(ByteProcessor byteProcessor) {
        int width = byteProcessor.getWidth();
        int height = byteProcessor.getHeight();

        byte[] pixels = (byte[]) byteProcessor.getPixels();

        Mat mat = new Mat(height, width, opencv_core.CV_8UC1);
        mat.data().put(pixels);

        return mat;
    }

    public static Mat convertShortProcessorToMat(ShortProcessor shortProcessor) {
        int width = shortProcessor.getWidth();
        int height = shortProcessor.getHeight();

        short[] pixels = (short[]) shortProcessor.getPixels();
        Mat mat = new Mat(height, width, opencv_core.CV_16UC1);

        UShortRawIndexer matIndexer = mat.createIndexer();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                matIndexer.put(y, x, pixels[y * width + x]);
            }
        }

        return mat;
    }

    public static Mat convertFloatProcessorToMat(FloatProcessor floatProcessor) {
        int width = floatProcessor.getWidth();
        int height = floatProcessor.getHeight();

        float[] pixels = (float[]) floatProcessor.getPixels();
        Mat mat = new Mat(height, width, opencv_core.CV_32FC1);

        FloatRawIndexer matIndexer = mat.createIndexer();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                matIndexer.put(y, x, pixels[y * width + x]);
            }
        }

        return mat;
    }

    public static Mat convertColorProcessorToMat(ColorProcessor colorProcessor) {
        int width = colorProcessor.getWidth();
        int height = colorProcessor.getHeight();

        int[] pixels = (int[]) colorProcessor.getPixels();
        Mat mat = new Mat(height, width, opencv_core.CV_8UC3);

        UByteRawIndexer matIndexer = mat.createIndexer();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = pixels[y * width + x];
                matIndexer.put(y, x, 0, (byte) (pixel & 0xFF));          // Blue
                matIndexer.put(y, x, 1, (byte) ((pixel >> 8) & 0xFF));   // Green
                matIndexer.put(y, x, 2, (byte) ((pixel >> 16) & 0xFF));  // Red
            }
        }

        return mat;
    }

    public static ByteProcessor convertMatToByteProcessor(Mat mat) {
        // Convert Mat to CV_8U format if necessary
        Mat cv8UImage = new Mat();
        if (mat.depth() != opencv_core.CV_8U) {
            mat.convertTo(cv8UImage, opencv_core.CV_8U);
        } else {
            cv8UImage = mat;
        }

        // Extract dimensions of Mat
        int width = cv8UImage.cols();
        int height = cv8UImage.rows();

        // Create a new ByteProcessor with the same dimensions
        ByteProcessor byteProcessor = new ByteProcessor(width, height);

        // Copy pixel data from Mat to ByteProcessor
        UByteRawIndexer matIndexer = cv8UImage.createIndexer();
        byte[] pixels = new byte[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y * width + x] = (byte) matIndexer.get(y, x);
            }
        }
        byteProcessor.setPixels(pixels);

        return byteProcessor;
    }

    public static ShortProcessor convertMatToShortProcessor(Mat mat) {
        // Convert Mat to CV_16U format if necessary
        Mat cv16UImage = new Mat();
        if (mat.depth() != opencv_core.CV_16U) {
            mat.convertTo(cv16UImage, opencv_core.CV_16U);
        } else {
            cv16UImage = mat;
        }

        // Extract dimensions of Mat
        int width = cv16UImage.cols();
        int height = cv16UImage.rows();

        // Create a new ByteProcessor with the same dimensions
        ShortProcessor shortProcessor = new ShortProcessor(width, height);

        // Copy pixel data from Mat to ByteProcessor
        UShortRawIndexer matIndexer = cv16UImage.createIndexer();
        short[] pixels = new short[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y * width + x] = (short) matIndexer.get(y, x);
            }
        }
        shortProcessor.setPixels(pixels);

        return shortProcessor;
    }

    public static FloatProcessor convertMatToFloatProcessor(Mat mat) {
        // Convert Mat to CV_16U format if necessary
        Mat cv32FImage = new Mat();
        if (mat.depth() != opencv_core.CV_32F) {
            mat.convertTo(cv32FImage, opencv_core.CV_32F);
        } else {
            cv32FImage = mat;
        }

        // Extract dimensions of Mat
        int width = cv32FImage.cols();
        int height = cv32FImage.rows();

        // Create a new ByteProcessor with the same dimensions
        FloatProcessor floatProcessor = new FloatProcessor(width, height);

        // Copy pixel data from Mat to ByteProcessor
        FloatRawIndexer matIndexer = cv32FImage.createIndexer();
        float[] pixels = new float[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y * width + x] = matIndexer.get(y, x);
            }
        }
        floatProcessor.setPixels(pixels);

        return floatProcessor;
    }

    public static ColorProcessor convertMatToColorProcessor(Mat mat) {
        // Convert Mat to CV_8UC3 format if necessary
        Mat cv8UC3Image = new Mat();
        if (mat.channels() == 1) {
            opencv_imgproc.cvtColor(mat, cv8UC3Image, opencv_imgproc.COLOR_GRAY2BGR);
        } else if (mat.depth() != opencv_core.CV_8U) {
            mat.convertTo(cv8UC3Image, opencv_core.CV_8UC3);
        } else {
            cv8UC3Image = mat;
        }

        // Extract dimensions of Mat
        int width = cv8UC3Image.cols();
        int height = cv8UC3Image.rows();

        // Create a new ColorProcessor with the same dimensions
        ColorProcessor colorProcessor = new ColorProcessor(width, height);

        // Copy pixel data from Mat to ColorProcessor
        UByteRawIndexer matIndexer = cv8UC3Image.createIndexer();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int blue = matIndexer.get(y, x, 0) & 0xFF;
                int green = matIndexer.get(y, x, 1) & 0xFF;
                int red = matIndexer.get(y, x, 2) & 0xFF;
                pixels[y * width + x] = (red << 16) | (green << 8) | blue;
            }
        }
        colorProcessor.setPixels(pixels);

        return colorProcessor;
    }
}
