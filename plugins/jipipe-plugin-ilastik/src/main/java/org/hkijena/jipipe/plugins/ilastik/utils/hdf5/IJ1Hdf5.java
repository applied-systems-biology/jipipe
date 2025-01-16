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

package org.hkijena.jipipe.plugins.ilastik.utils.hdf5;

import ch.systemsx.cisd.base.mdarray.MDByteArray;
import ch.systemsx.cisd.base.mdarray.MDFloatArray;
import ch.systemsx.cisd.hdf5.*;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.plugins.ilastik.utils.ImgUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.hkijena.jipipe.plugins.ilastik.utils.ImgUtils.reversed;

public final class IJ1Hdf5 {


    private IJ1Hdf5() {
        throw new AssertionError();
    }

    /**
     * Return descriptions of supported datasets in an HDF5 file, sorted by their paths.
     */
    public static List<DatasetDescription> listDatasets(Path file) {
        Objects.requireNonNull(file);

        List<DatasetDescription> result = new ArrayList<>();
        Deque<String> stack = new ArrayDeque<>();
        stack.push("/");

        try (IHDF5Reader reader = HDF5Factory.openForReading(file.toFile())) {
            while (!stack.isEmpty()) {
                String path = stack.pop();

                if (reader.object().isGroup(path)) {
                    stack.addAll(reader.object().getAllGroupMembers(path));
                } else if (reader.object().isDataSet(path)) {
                    DatasetDescription.ofHdf5(reader, path).ifPresent(result::add);
                }
            }
        }

        result.sort(Comparator.comparing(dd -> dd.path));
        return result;
    }

    /**
     * Read HDF5 dataset contents.
     * <p>
     * Only 2D-5D datasets with types enumerated in {@link DatasetType} are supported.
     * <p>
     * If not null, callback will be invoked between block writes (N + 1 invocations for N blocks).
     * The callback accepts the total number of bytes written so far.
     * This is useful for progress reporting.
     */
    public static ImagePlus readImage(
            Path file, String path, List<AxisType> axes, JIPipeProgressInfo progressInfo) {

        Objects.requireNonNull(file);
        Objects.requireNonNull(path);
        if (axes == null) {
            axes = new ArrayList<>();
        } else {
            axes = axes.stream().distinct().collect(Collectors.toList());
            if (!(axes.contains(Axes.X) && axes.contains(Axes.Y))) {
                throw new IllegalArgumentException("Axes must contain X and Y if you define them manually");
            }
        }

        String name = file
                .resolve(path.replaceFirst("/+", ""))
                .toString()
                .replace('\\', '/');

        progressInfo.log("Reading HDF5 " + name + " with axes " + formatAxes(axes));

        try (IHDF5Reader reader = HDF5Factory.openForReading(file.toFile())) {
            HDF5DataSetInformation info = reader.getDataSetInformation(path);

            long[] dims = reversed(info.getDimensions());
            if (!(2 <= dims.length && dims.length <= 5)) {
                throw new IllegalArgumentException(dims.length + "D datasets are not supported");
            }
            addMissingAxesForReading(axes, dims.length, progressInfo);

            HDF5DataTypeInformation typeInfo = info.getTypeInformation();
            DatasetType type = DatasetType.ofHdf5(typeInfo).orElseThrow(() ->
                    new IllegalArgumentException("Unsupported dataset type " + typeInfo));
            ImagePlus result = createImagePlus(name, axes, dims, type);

//            int[] blockDims = inputBlockDims(dims,
//                    info.tryGetChunkSizes() != null ? reversed(info.tryGetChunkSizes()) : null);
//
//            try (HDF5DataSet dataset = reader.object().openDataSet(path)) {
//                callback.accept(0L);
//                if (IntStream.range(0, dims.length).allMatch(i -> dims[i] == blockDims[i])) {
//                    img = readArrayImg(type, reader, dataset, dims);
//                    callback.accept(Arrays.stream(dims).reduce(type.size, (l, r) -> l * r));
//                } else {
//                    img = readCellImg(type, reader, dataset, dims, blockDims, callback);
//                }
//            }

            Map<AxisType, Integer> axisIndices = getAxisIndices(axes, dims);
            int[] blockDimensions = new int[dims.length];
            Arrays.fill(blockDimensions, 1);
            blockDimensions[dims.length - 1 - axisIndices.get(Axes.X)] = result.getWidth();
            blockDimensions[dims.length - 1 - axisIndices.get(Axes.Y)] = result.getHeight();

            long[] offsets = new long[dims.length];
            boolean invertXY = axisIndices.get(Axes.X) < axisIndices.get(Axes.Y);

            try (HDF5DataSet dataset = reader.object().openDataSet(path)) {
                for (int t = 0; t < result.getNFrames(); t++) {
                    if (axisIndices.containsKey(Axes.TIME)) {
                        offsets[dims.length - 1 - axisIndices.get(Axes.TIME)] = t;
                    }
                    for (int z = 0; z < result.getNSlices(); z++) {
                        if (axisIndices.containsKey(Axes.Z)) {
                            offsets[dims.length - 1 - axisIndices.get(Axes.Z)] = z;
                        }
                        for (int c = 0; c < result.getNChannels(); c++) {
                            if (axisIndices.containsKey(Axes.CHANNEL)) {
                                offsets[dims.length - 1 - axisIndices.get(Axes.CHANNEL)] = c;
                            }
                            MDFloatArray arrayEntries = reader.float32().readMDArrayBlockWithOffset(dataset, blockDimensions, offsets);
                            float[] src = arrayEntries.getAsFlatArray();
                            ImageProcessor target = ImageJUtils.getSliceZero(result, c, z, t);

                            if (!invertXY) {
                                for (int i = 0; i < src.length; i++) {
                                    int x = i % target.getHeight(); // Swapped!
                                    int y = i / target.getHeight(); // Swapped!
                                    target.setf(x, y, src[i]);
                                }
                            } else {
                                for (int i = 0; i < target.getPixelCount(); i++) {
                                    target.setf(i, src[i]);
                                }
                            }
                        }
                    }
                }
            }

            return result;
        }
    }

    private static String formatAxes(List<AxisType> axes) {
        return axes.stream().map(at -> at.getLabel().substring(0, 1)).collect(Collectors.joining(""));
    }

    public static void writeImage(ImagePlus image, Path file, String path, List<AxisType> axes, JIPipeProgressInfo progressInfo) {
        image = ImageJUtils.convertToGreyscaleIfNeeded(image);
        Objects.requireNonNull(file);
        Objects.requireNonNull(path);
        if (axes == null) {
            axes = new ArrayList<>();

            // For 2D images, just go with two axes (preconfigured)
            if (image.getNDimensions() == 2) {
                progressInfo.log("2D image detected. Setting default axis config to XY");
                axes.add(Axes.X);
                axes.add(Axes.Y);
            }

        } else {
            axes = axes.stream().distinct().collect(Collectors.toList());
            if (!(axes.contains(Axes.X) && axes.contains(Axes.Y))) {
                throw new IllegalArgumentException("Axes must contain X and Y if you define them manually");
            }
        }
        addMissingAxesForWriting(axes, image, progressInfo);

        progressInfo.log("Writing " + image + " to " + file + " path " + path + " with axes " + formatAxes(axes));

        // Get the dimension sizes based on the image
        final int dims = axes.size();
        int timePoints = image.getNFrames();
        int channels = image.getNChannels();
        int depth = image.getNSlices(); // Z-depth
        int width = image.getWidth();
        int height = image.getHeight();

        // Map dimensions dynamically according to the axis order
        long[] dimensions = new long[dims];
        int[] blockDimensions = new int[dims];
        long[] offsets = new long[dims];
        Arrays.fill(blockDimensions, 1);

        if (axes.contains(Axes.TIME)) {
            dimensions[dims - 1 - axes.indexOf(Axes.TIME)] = timePoints;
        }
        if (axes.contains(Axes.CHANNEL)) {
            dimensions[dims - 1 - axes.indexOf(Axes.CHANNEL)] = channels;
        }
        if (axes.contains(Axes.X)) {
            dimensions[dims - 1 - axes.indexOf(Axes.X)] = width;
            blockDimensions[dims - 1 - axes.indexOf(Axes.X)] = width;
        }
        if (axes.contains(Axes.Y)) {
            dimensions[dims - 1 - axes.indexOf(Axes.Y)] = height;
            blockDimensions[dims - 1 - axes.indexOf(Axes.Y)] = height;
        }
        if (axes.contains(Axes.Z)) {
            dimensions[dims - 1 - axes.indexOf(Axes.Z)] = depth;
        }
        boolean invertXY = axes.indexOf(Axes.X) < axes.indexOf(Axes.Y);

        try (IHDF5Writer writer = HDF5Factory.open(file.toFile())) {
            if (image.getBitDepth() == 8) {
                HDF5IntStorageFeatures features = HDF5IntStorageFeatures.build().compress().unsigned().features();
                try (HDF5DataSet dataSet = writer.uint8().createMDArrayAndOpen(path, dimensions, blockDimensions, features)) {
                    for (int t = 0; t < timePoints; t++) {
                        if (axes.contains(Axes.TIME)) {
                            offsets[dims - 1 - axes.indexOf(Axes.TIME)] = t;
                        }
                        for (int z = 0; z < depth; z++) {
                            if (axes.contains(Axes.Z)) {
                                offsets[dims - 1 - axes.indexOf(Axes.Z)] = z;
                            }
                            for (int c = 0; c < channels; c++) {
                                if (axes.contains(Axes.CHANNEL)) {
                                    offsets[dims - 1 - axes.indexOf(Axes.CHANNEL)] = c;
                                }

                                byte[] src = (byte[]) ImageJUtils.getSliceZero(image, c, z, t).getPixels();
                                byte[] target;
                                if (!invertXY) {
                                    target = new byte[src.length];
                                    for (int i = 0; i < src.length; i++) {
                                        int x = i % height; // Swapped!
                                        int y = i / height; // Swapped!
                                        target[x + y * width] = src[i];
                                    }
                                } else {
                                    target = src;
                                }
                                writer.uint8().writeMDArrayBlockWithOffset(dataSet, new MDByteArray(target, blockDimensions), offsets);
                            }
                        }
                    }
                }
            } else {
                throw new IllegalArgumentException("Unsupported bitdepth " + image.getBitDepth());
            }
        }
    }

    public static int datasetTypeToBitDepth(DatasetType type) {
        switch (type) {
            case INT8:
            case UINT8:
                return 8;
            case INT16:
            case UINT16:
                return 16;
            default:
                return 32;
        }
    }

    private static ImagePlus createImagePlus(String title, List<AxisType> axes, long[] dims, DatasetType type) {
        Map<AxisType, Integer> axisSizes = getAxisSizes(axes, dims);
        return IJ.createHyperStack(title, axisSizes.getOrDefault(Axes.X, 1),
                axisSizes.getOrDefault(Axes.Y, 1),
                axisSizes.getOrDefault(Axes.CHANNEL, 1),
                axisSizes.getOrDefault(Axes.Z, 1),
                axisSizes.getOrDefault(Axes.TIME, 1),
                datasetTypeToBitDepth(type));
    }

    private static Map<AxisType, Integer> getAxisSizes(List<AxisType> axes, long[] dims) {
        Map<AxisType, Integer> axisSizes = new HashMap<>();
        for (int i = 0; i < Math.min(axes.size(), dims.length); i++) {
            axisSizes.put(axes.get(i), (int) dims[i]);
        }
        return axisSizes;
    }

    private static Map<AxisType, Integer> getAxisIndices(List<AxisType> axes, long[] dims) {
        Map<AxisType, Integer> axisSizes = new HashMap<>();
        for (int i = 0; i < Math.min(axes.size(), dims.length); i++) {
            axisSizes.put(axes.get(i), i);
        }
        return axisSizes;
    }

    private static void addMissingAxesForReading(List<AxisType> axes, int numAxes, JIPipeProgressInfo progressInfo) {
        String originalAxes = formatAxes(axes);
        boolean changed = false;
        while (axes.size() < numAxes) {
            for (AxisType axisType : ImgUtils.DEFAULT_AXES) {
                if (!axes.contains(axisType)) {
                    axes.add(axisType);
                    break;
                }
            }
            changed = true;
        }
        if (changed) {
            progressInfo.log("Original axis configuration was \"" + originalAxes + "\" and was changed to " + formatAxes(axes) + " to fit " + numAxes + " dimensions");
        }
    }

    private static void addMissingAxesForWriting(List<AxisType> axes, ImagePlus image, JIPipeProgressInfo progressInfo) {
        String originalAxes = formatAxes(axes);
        boolean changed = false;
        if (!axes.contains(Axes.X)) {
            axes.add(Axes.X);
            changed = true;
        }
        if (!axes.contains(Axes.Y)) {
            axes.add(Axes.Y);
            changed = true;
        }
        if (image.getNChannels() > 1 && !axes.contains(Axes.CHANNEL)) {
            axes.add(Axes.CHANNEL);
            changed = true;
        }
        if (image.getNSlices() > 1 && !axes.contains(Axes.Z)) {
            axes.add(Axes.Z);
            changed = true;
        }
        if (image.getNFrames() > 1 && !axes.contains(Axes.TIME)) {
            axes.add(Axes.TIME);
            changed = true;
        }
        if (changed) {
            progressInfo.log("Original axis configuration was \"" + originalAxes + "\" and was changed to " + formatAxes(axes) + " to fit " + image + " dimensions");
        }
    }
}
