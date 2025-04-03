package org.hkijena.jipipe.plugins.imagejdatatypes.util;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;
import org.hkijena.jipipe.utils.TriConsumer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class ImageJIterationUtils {
    /**
     * Runs the function for each slice
     *
     * @param img          the image
     * @param function     the function
     * @param progressInfo the progress
     */
    public static void forEachSlice(ImagePlus img, Consumer<ImageProcessor> function, JIPipeProgressInfo progressInfo) {
        if (img.hasImageStack()) {
            for (int i = 0; i < img.getImageStackSize(); ++i) {
                ImageProcessor ip = img.getImageStack().getProcessor(i + 1);
                progressInfo.resolveAndLog("Slice", i, img.getImageStackSize());
                function.accept(ip);
            }
        } else {
            function.accept(img.getProcessor());
        }
    }

    /**
     * Runs the function for each slice
     *
     * @param img          the image
     * @param function     the function
     * @param progressInfo the progress
     */
    public static void forEachIndexedSlice(ImagePlus img, BiConsumer<ImageProcessor, Integer> function, JIPipeProgressInfo progressInfo) {
        if (img.hasImageStack()) {
            for (int i = 0; i < img.getStack().size(); ++i) {
                if (progressInfo.isCancelled())
                    return;
                ImageProcessor ip = img.getImageStack().getProcessor(i + 1);
                progressInfo.resolveAndLog("Slice", i, img.getStackSize());
                function.accept(ip, i);
            }
        } else {
            function.accept(img.getProcessor(), 0);
        }
    }

    /**
     * Runs the function for each Z, C, and T slice.
     *
     * @param img          the image
     * @param function     the function. The indices are ZERO-based
     * @param progressInfo the progress
     */
    public static void forEachIndexedZCTSlice(ImagePlus img, BiConsumer<ImageProcessor, ImageSliceIndex> function, JIPipeProgressInfo progressInfo) {
        if (img.hasImageStack()) {
            int iterationIndex = 0;
            for (int t = 0; t < img.getNFrames(); t++) {
                for (int z = 0; z < img.getNSlices(); z++) {
                    for (int c = 0; c < img.getNChannels(); c++) {
                        if (progressInfo.isCancelled())
                            return;
                        int index = img.getStackIndex(c + 1, z + 1, t + 1);
                        progressInfo.resolveAndLog("Slice", iterationIndex++, img.getStackSize()).log("z=" + z + ", c=" + c + ", t=" + t);
                        ImageProcessor processor = img.getImageStack().getProcessor(index);
                        function.accept(processor, new ImageSliceIndex(c, z, t));
                    }
                }
            }
        } else {
            function.accept(img.getProcessor(), new ImageSliceIndex(0, 0, 0));
        }
    }

    /**
     * Runs the function for each C, and T slice.
     *
     * @param img          the image
     * @param function     the function. The indices are ZERO-based (Z is always -1)
     * @param progressInfo the progress
     */
    public static void forEachIndexedCTStack(ImagePlus img, TriConsumer<ImagePlus, ImageSliceIndex, JIPipeProgressInfo> function, JIPipeProgressInfo progressInfo) {
        if (img.hasImageStack()) {
            int iterationIndex = 0;
            for (int t = 0; t < img.getNFrames(); t++) {
                for (int c = 0; c < img.getNChannels(); c++) {
                    if (progressInfo.isCancelled())
                        return;
                    ImagePlus cube = ImageJUtils.extractCTStack(img, c, t);
                    progressInfo.resolveAndLog("Frame/Channel", iterationIndex, img.getNChannels() * img.getNFrames()).log("c=" + c + ", t=" + t);
                    JIPipeProgressInfo stackProgress = progressInfo.resolveAndLog("Frame/Channel", iterationIndex, img.getNChannels() * img.getNFrames()).resolve("c=" + c + ", t=" + t);
                    function.accept(cube, new ImageSliceIndex(c, -1, t), stackProgress);
                    ++iterationIndex;
                }
            }
        } else {
            function.accept(img, new ImageSliceIndex(0, -1, 0), progressInfo);
        }
    }

    public static void forEachIndexedZTStack(ImagePlus img, TriConsumer<ImagePlus, ImageSliceIndex, JIPipeProgressInfo> function, JIPipeProgressInfo progressInfo) {
        if (img.hasImageStack()) {
            int iterationIndex = 0;
            for (int t = 0; t < img.getNFrames(); t++) {
                for (int z = 0; z < img.getNSlices(); z++) {
                    if (progressInfo.isCancelled())
                        return;
                    ImagePlus cube = ImageJUtils.extractZTStack(img, z, t);
                    progressInfo.resolveAndLog("Z/Channel", iterationIndex, img.getNSlices() * img.getNFrames()).log("z=" + z + ", t=" + t);
                    JIPipeProgressInfo stackProgress = progressInfo.resolveAndLog("Z/Channel", iterationIndex, img.getNChannels() * img.getNFrames()).resolve("z=" + z + ", t=" + t);
                    function.accept(cube, new ImageSliceIndex(-1, z, t), stackProgress);
                    ++iterationIndex;
                }
            }
        } else {
            function.accept(img, new ImageSliceIndex(-1, 0, 0), progressInfo);
        }
    }

    /**
     * Runs the function for each T hyperstack
     *
     * @param img          the image
     * @param function     the function. The indices are ZERO-based
     * @param progressInfo the progress
     */
    public static void forEachIndexedTHyperStack(ImagePlus img, TriConsumer<ImagePlus, ImageSliceIndex, JIPipeProgressInfo> function, JIPipeProgressInfo progressInfo) {
        if (img.hasImageStack()) {
            int iterationIndex = 0;
            for (int t = 0; t < img.getNFrames(); t++) {
                if (progressInfo.isCancelled())
                    return;
                ImagePlus cube = ImageJUtils.extractTHyperStack(img, t);
                progressInfo.resolveAndLog("Frame", iterationIndex, img.getNFrames()).log("t=" + t);
                JIPipeProgressInfo stackProgress = progressInfo.resolveAndLog("Frame", iterationIndex, img.getNFrames()).resolve("t=" + t);
                function.accept(cube, new ImageSliceIndex(-1, -1, t), stackProgress);
                ++iterationIndex;
            }
        } else {
            function.accept(img, new ImageSliceIndex(-1, -1, 0), progressInfo);
        }
    }

    /**
     * Runs the function for each C hyperstack
     *
     * @param img          the image
     * @param function     the function. The indices are ZERO-based
     * @param progressInfo the progress
     */
    public static void forEachIndexedCHyperStack(ImagePlus img, TriConsumer<ImagePlus, ImageSliceIndex, JIPipeProgressInfo> function, JIPipeProgressInfo progressInfo) {
        if (img.hasImageStack()) {
            int iterationIndex = 0;
            for (int c = 0; c < img.getNChannels(); c++) {
                if (progressInfo.isCancelled())
                    return;
                ImagePlus cube = ImageJUtils.extractCHyperStack(img, c);
                progressInfo.resolveAndLog("Channel", iterationIndex, img.getNFrames()).log("c=" + c);
                JIPipeProgressInfo stackProgress = progressInfo.resolveAndLog("Channel", iterationIndex, img.getNChannels()).resolve("c=" + c);
                function.accept(cube, new ImageSliceIndex(c, -1, -1), stackProgress);
                ++iterationIndex;
            }
        } else {
            function.accept(img, new ImageSliceIndex(0, -1, -1), progressInfo);
        }
    }

    /**
     * Runs the function for each Z hyperstack
     *
     * @param img          the image
     * @param function     the function. The indices are ZERO-based
     * @param progressInfo the progress
     */
    public static void forEachIndexedZHyperStack(ImagePlus img, TriConsumer<ImagePlus, ImageSliceIndex, JIPipeProgressInfo> function, JIPipeProgressInfo progressInfo) {
        if (img.hasImageStack()) {
            int iterationIndex = 0;
            for (int z = 0; z < img.getNSlices(); z++) {
                if (progressInfo.isCancelled())
                    return;
                ImagePlus cube = ImageJUtils.extractZHyperStack(img, z);
                progressInfo.resolveAndLog("Slice", iterationIndex, img.getNFrames()).log("z=" + z);
                JIPipeProgressInfo stackProgress = progressInfo.resolveAndLog("Slice", iterationIndex, img.getNSlices()).resolve("z=" + z);
                function.accept(cube, new ImageSliceIndex(-1, z, -1), stackProgress);
                ++iterationIndex;
            }
        } else {
            function.accept(img, new ImageSliceIndex(-1, 0, -1), progressInfo);
        }
    }

    /**
     * Runs the function for each Z, C, and T slice.
     *
     * @param img          the image
     * @param function     the function. The indices are ZERO-based
     * @param progressInfo the progress
     */
    public static void forEachIndexedZCTSliceWithProgress(ImagePlus img, TriConsumer<ImageProcessor, ImageSliceIndex, JIPipeProgressInfo> function, JIPipeProgressInfo progressInfo) {
        if (img.hasImageStack()) {
            int iterationIndex = 0;
            for (int t = 0; t < img.getNFrames(); t++) {
                for (int z = 0; z < img.getNSlices(); z++) {
                    for (int c = 0; c < img.getNChannels(); c++) {
                        if (progressInfo.isCancelled())
                            return;
                        int index = img.getStackIndex(c + 1, z + 1, t + 1);
                        JIPipeProgressInfo stackProgress = progressInfo.resolveAndLog("Slice", iterationIndex++, img.getStackSize()).resolve("z=" + z + ", c=" + c + ", t=" + t);
                        ImageProcessor processor = img.getImageStack().getProcessor(index);
                        function.accept(processor, new ImageSliceIndex(c, z, t), stackProgress);
                    }
                }
            }
        } else {
            function.accept(img.getProcessor(), new ImageSliceIndex(0, 0, 0), progressInfo);
        }
    }

    /**
     * Runs the function for each Z, C, and T slice.
     *
     * @param sourceImage  the image
     * @param function     the function. The indices are ZERO-based. Should return the result slice for this index
     * @param progressInfo the progress
     */
    public static ImagePlus generateForEachIndexedZCTSlice(ImagePlus sourceImage, BiFunction<ImageProcessor, ImageSliceIndex, ImageProcessor> function, JIPipeProgressInfo progressInfo) {
        if (sourceImage.hasImageStack()) {
            ImageStack stack = new ImageStack(sourceImage.getWidth(), sourceImage.getHeight(), sourceImage.getStackSize());
            int iterationIndex = 0;
            for (int t = 0; t < sourceImage.getNFrames(); t++) {
                for (int z = 0; z < sourceImage.getNSlices(); z++) {
                    for (int c = 0; c < sourceImage.getNChannels(); c++) {
                        if (progressInfo.isCancelled())
                            return null;
                        int index = sourceImage.getStackIndex(c + 1, z + 1, t + 1);
                        progressInfo.resolveAndLog("Slice", iterationIndex++, sourceImage.getStackSize()).log("z=" + z + ", c=" + c + ", t=" + t);
                        ImageProcessor processor = sourceImage.getImageStack().getProcessor(index);
                        ImageProcessor resultProcessor = function.apply(processor, new ImageSliceIndex(c, z, t));
                        stack.setProcessor(resultProcessor, index);
                    }
                }
            }
            ImagePlus resultImage = new ImagePlus(sourceImage.getTitle(), stack);
            resultImage.setDimensions(sourceImage.getNChannels(), sourceImage.getNSlices(), sourceImage.getNFrames());
            resultImage.copyScale(sourceImage);
            return resultImage;
        } else {
            ImageProcessor processor = function.apply(sourceImage.getProcessor(), new ImageSliceIndex(0, 0, 0));
            ImagePlus resultImage = new ImagePlus(sourceImage.getTitle(), processor);
            resultImage.copyScale(sourceImage);
            return resultImage;
        }
    }

    /**
     * Runs the function for each Z and T slice.
     * The function consumes a map from channel index to the channel slice.
     * The slice index channel is always set to -1
     *
     * @param img          the image
     * @param function     the function. The indices are ZERO-based
     * @param progressInfo the progress
     */
    @Deprecated
    public static void forEachIndexedZTSlice(ImagePlus img, BiConsumer<Map<Integer, ImageProcessor>, ImageSliceIndex> function, JIPipeProgressInfo progressInfo) {
        int iterationIndex = 0;
        for (int t = 0; t < img.getNFrames(); t++) {
            for (int z = 0; z < img.getNSlices(); z++) {
                Map<Integer, ImageProcessor> channels = new HashMap<>();
                for (int c = 0; c < img.getNChannels(); c++) {
                    if (progressInfo.isCancelled())
                        return;
                    progressInfo.resolveAndLog("Slice", iterationIndex++, img.getStackSize()).log("z=" + z + ", c=" + c + ", t=" + t);
                    channels.put(c, img.getStack().getProcessor(img.getStackIndex(c + 1, z + 1, t + 1)));
                }
                function.accept(channels, new ImageSliceIndex(-1, z, t));
            }
        }
    }
}
