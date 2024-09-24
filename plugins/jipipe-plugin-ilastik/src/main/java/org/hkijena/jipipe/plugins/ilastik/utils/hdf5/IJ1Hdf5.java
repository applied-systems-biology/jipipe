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

import ch.systemsx.cisd.base.mdarray.MDFloatArray;
import ch.systemsx.cisd.hdf5.*;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.img.cell.CellImg;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.img.list.ListImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.Fraction;
import net.imglib2.view.Views;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.plugins.ilastik.utils.GridCoordinates;
import org.hkijena.jipipe.plugins.ilastik.utils.ImgUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.function.LongConsumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hkijena.jipipe.plugins.ilastik.utils.ImgUtils.*;

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
    public static ImagePlus readDataset(
            Path file, String path, List<AxisType> axes, JIPipeProgressInfo progressInfo) {

        Objects.requireNonNull(file);
        Objects.requireNonNull(path);
        if(axes == null) {
            axes = new ArrayList<>();
        }
        else {
            axes = axes.stream().distinct().collect(Collectors.toList());
            if (!(axes.contains(Axes.X) && axes.contains(Axes.Y))) {
                throw new IllegalArgumentException("Axes must contain X and Y if you define them manually");
            }
        }

        String name = file
                .resolve(path.replaceFirst("/+", ""))
                .toString()
                .replace('\\', '/');

        progressInfo.log("Reading HDF5 " + name + " with axes " +
                axes.stream().map(at -> at.getLabel().substring(0,1)).collect(Collectors.joining("")));

        try (IHDF5Reader reader = HDF5Factory.openForReading(file.toFile())) {
            HDF5DataSetInformation info = reader.getDataSetInformation(path);

            long[] dims = reversed(info.getDimensions());
            if (!(2 <= dims.length && dims.length <= 5)) {
                throw new IllegalArgumentException(dims.length + "D datasets are not supported");
            }
            addMissingAxes(axes, dims.length, progressInfo);

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
                    if(axisIndices.containsKey(Axes.TIME)) {
                        offsets[dims.length - 1 - axisIndices.get(Axes.TIME)] = t;
                    }
                    for (int z = 0; z < result.getNSlices(); z++) {
                        if(axisIndices.containsKey(Axes.Z)) {
                            offsets[dims.length - 1 - axisIndices.get(Axes.Z)] = z;
                        }
                        for (int c = 0; c < result.getNChannels(); c++) {
                            if(axisIndices.containsKey(Axes.CHANNEL)) {
                                offsets[dims.length - 1 - axisIndices.get(Axes.CHANNEL)] = c;
                            }
                            MDFloatArray arrayEntries = reader.float32().readMDArrayBlockWithOffset(dataset, blockDimensions, offsets);
                            float[] src = arrayEntries.getAsFlatArray();
                            ImageProcessor target = ImageJUtils.getSliceZero(result, c, z, t);

                            if(!invertXY) {
                                for (int i = 0; i < src.length; i++) {
                                    int x = i % target.getHeight(); // Swapped!
                                    int y = i / target.getHeight(); // Swapped!
                                    target.setf(x, y, src[i]);
                                }
                            }
                            else {
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

    private static void addMissingAxes(List<AxisType> axes, int numAxes, JIPipeProgressInfo progressInfo) {
        String originalAxes = axes.stream().map(AxisType::toString).collect(Collectors.joining(""));
        boolean changed = false;
        while(axes.size() < numAxes) {
            for(AxisType axisType : ImgUtils.DEFAULT_AXES) {
                if(!axes.contains(axisType)) {
                    axes.add( axisType);
                    break;
                }
            }
            changed = true;
        }
        if(changed) {
            progressInfo.log("Original axis configuration was \"" + originalAxes + "\" and was changed to " +
                    axes.stream().map(at -> at.getLabel().substring(0,1)).collect(Collectors.joining("")) + " to fit " + numAxes + " dimensions");
        }
    }

//    /**
//     * {@link #writeDataset} without compression, axis reordering and callback.
//     */
//    public static void writeDataset(
//            File file, String path,ImagePlus img) {
//        writeDataset(file, path, img, 0, null, null);
//    }
//
//    /**
//     * {@link #writeDataset} without axis reordering and callback.
//     */
//    public static void writeDataset(
//            File file, String path,ImagePlus img, int compressionLevel) {
//        writeDataset(file, path, img, compressionLevel, null, null);
//    }
//
//    /**
//     * {@link #writeDataset} without callback.
//     */
//    public static void writeDataset(
//            File file, String path,ImagePlus img, int compressionLevel, List<AxisType> axes) {
//        writeDataset(file, path, img, compressionLevel, axes, null);
//    }
//
//    /**
//     * Write image contents to HDF5 dataset, creating/overwriting file and dataset if needed.
//     * <p>
//     * Only 2D-5D datasets with types enumerated in {@link DatasetType} are supported.
//     * As a special case, {@link ARGBType} is supported too, but its use is discouraged.
//     * <p>
//     * if axes are specified, image will be written in the specified axis order.
//     * <p>
//     * If not null, callback will be invoked between block writes (N + 1 invocations for N blocks).
//     * The callback accepts the total number of bytes written so far.
//     * This is useful for progress reporting.
//     */
//    public static void writeDataset(
//            File file,
//            String path,
//           ImagePlus img,
//            int compressionLevel,
//            List<AxisType> axes,
//            LongConsumer callback) {
//
//        Objects.requireNonNull(file);
//        Objects.requireNonNull(path);
//        Objects.requireNonNull(img);
//        if (compressionLevel < 0) {
//            throw new IllegalArgumentException("Compression level cannot be negative");
//        }
//        if (callback == null) {
//            callback = (a) -> {
//            };
//        }
//
//        if (!(2 <= img.numDimensions() && img.numDimensions() <= 5)) {
//            throw new IllegalArgumentException(
//                    img.numDimensions() + "D datasets are not supported");
//        }
//
//        T imglib2Type = img.firstElement();
//        if (imglib2Type.getClass() == ARGBType.class) {
//            Logger.getLogger(IJ1Hdf5.class.getName()).warning("Writing ARGBType images is deprecated");
//            @SuppressWarnings("unchecked")
//            ImgPlus<ARGBType> argbImg = (ImgPlus<ARGBType>) img;
//            writeDataset(file, path, argbToMultiChannel(argbImg), compressionLevel, axes, callback);
//            return;
//        }
//
//        Img<T> data = axes == null ? img.getImg() : transformDims(img, axesOf(img), axes);
//        if (axes == null) {
//            axes = axesOf(img);
//        }
//
//        DatasetType type = DatasetType.ofImglib2(imglib2Type).orElseThrow(() ->
//                new IllegalArgumentException("Unsupported image type " + imglib2Type.getClass()));
//
//        long[] dims = data.dimensionsAsLongArray();
//        int[] chunkDims = new int[dims.length];
//        int[] blockDims = new int[dims.length];
//        outputDims(dims, axes, type, chunkDims, blockDims);
//
//        IterableInterval<RandomAccessibleInterval<T>> grid =
//                Views.flatIterable(Views.tiles(data, blockDims));
//        Cursor<RandomAccessibleInterval<T>> gridCursor = grid.cursor();
//        long bytes = 0;
//
//        try (IHDF5Writer writer = HDF5Factory.open(file);
//             HDF5DataSet dataset = type.createDataset(
//                     writer,
//                     path,
//                     reversed(dims),
//                     reversed(chunkDims),
//                     compressionLevel)) {
//
//            callback.accept(0L);
//
//            while (gridCursor.hasNext()) {
//                RandomAccessibleInterval<T> block = gridCursor.next();
//                Cursor<T> blockCursor = Views.flatIterable(block).cursor();
//                long[] currBlockDims = reversed(block.dimensionsAsLongArray());
//                long[] offset = reversed(block.minAsLongArray());
//                type.writeCursor(blockCursor, writer, dataset, currBlockDims, offset);
//
//                bytes += Arrays.stream(currBlockDims).reduce(type.size, (l, r) -> l * r);
//                callback.accept(bytes);
//            }
//        }
//    }
}
