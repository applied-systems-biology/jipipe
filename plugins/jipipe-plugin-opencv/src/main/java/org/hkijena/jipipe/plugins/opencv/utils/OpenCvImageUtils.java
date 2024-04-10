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

import ij.ImagePlus;
import ij.process.ImageProcessor;
import net.imagej.opencv.ImgToMatConverter;
import net.imagej.opencv.MatToImgConverter;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.plugins.opencv.datatypes.OpenCvImageData;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
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
        ImagePlus imagePlus = new ImagePlus("img", processor);
        Img wrapped = ImageJFunctions.wrap(imagePlus);
        return ImgToMatConverter.toMat(wrapped);
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
        return ImageJFunctions.wrap((RandomAccessibleInterval) MatToImgConverter.convert(mat), "img").getProcessor();
    }

    public static ImagePlus toImagePlus(Mat mat) {
        return ImageJFunctions.wrap((RandomAccessibleInterval) MatToImgConverter.convert(mat), "img");
    }

    public static String getTypeName(Mat mat) {
        int type = mat.type();
        if (type == opencv_core.CV_8S) {
            return "8S";
        } else if (type == opencv_core.CV_8SC1) {
            return "8SC1";
        } else if (type == opencv_core.CV_8SC2) {
            return "8SC2";
        } else if (type == opencv_core.CV_8SC3) {
            return "8SC3";
        } else if (type == opencv_core.CV_8U) {
            return "8U";
        } else if (type == opencv_core.CV_8UC1) {
            return "8UC1";
        } else if (type == opencv_core.CV_8UC2) {
            return "8UC2";
        } else if (type == opencv_core.CV_8UC3) {
            return "8UC3";
        } else if (type == opencv_core.CV_16S) {
            return "16S";
        } else if (type == opencv_core.CV_16SC1) {
            return "16SC1";
        } else if (type == opencv_core.CV_16SC2) {
            return "16SC2";
        } else if (type == opencv_core.CV_16SC3) {
            return "16SC3";
        } else if (type == opencv_core.CV_16U) {
            return "16U";
        } else if (type == opencv_core.CV_16UC1) {
            return "16UC1";
        } else if (type == opencv_core.CV_16UC2) {
            return "16UC2";
        } else if (type == opencv_core.CV_16UC3) {
            return "16UC3";
        } else if (type == opencv_core.CV_32S) {
            return "32S";
        } else if (type == opencv_core.CV_32SC1) {
            return "32SC1";
        } else if (type == opencv_core.CV_32SC2) {
            return "32SC2";
        } else if (type == opencv_core.CV_32SC3) {
            return "32SC3";
        } else if (type == opencv_core.CV_32F) {
            return "32F";
        } else if (type == opencv_core.CV_32FC1) {
            return "32FC1";
        } else if (type == opencv_core.CV_32FC2) {
            return "32FC2";
        } else if (type == opencv_core.CV_32FC3) {
            return "32FC3";
        } else {
            return String.valueOf(type);
        }
    }
}
