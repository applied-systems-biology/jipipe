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

package org.hkijena.jipipe.plugins.imagejdatatypes.util;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import org.janelia.saalfeldlab.n5.ij.N5IJUtils;
import org.janelia.saalfeldlab.n5.ij.N5Importer;
import org.janelia.saalfeldlab.n5.metadata.imagej.N5ImagePlusMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5DatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SingleScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.NgffSingleScaleAxesMetadata;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ZARRUtils {
    /**
     * Computes an optimal chunk size for each axis according to a power‐of‐two heuristic.
     * <p>
     * For each axis the following rules are applied:
     * <ul>
     *   <li>If the axis size is less than 32, use the full axis size (no subdivision).</li>
     *   <li>If the axis size is between 32 and 128 (inclusive), choose 32.</li>
     *   <li>If the axis size is between 129 and 511, choose 64.</li>
     *   <li>If the axis size is 512 or more, choose 128.</li>
     * </ul>
     * This means that an axis representing, say, time with 300 points will be stored
     * with a chunk size of 64 rather than the full extent (300), while a channel axis of 3
     * remains unchunked.
     *
     * @param axisSizes an array of ints representing the sizes of the axes in arbitrary order.
     * @return an int array of the same length where each element is the chosen chunk size for that axis.
     * @throws IllegalArgumentException if the provided array is null.
     */
    public static int[] computeOptimalChunkSizes(int[] axisSizes) {
        if (axisSizes == null) {
            throw new IllegalArgumentException("Axis sizes array cannot be null.");
        }
        int[] chunkSizes = new int[axisSizes.length];
        for (int i = 0; i < axisSizes.length; i++) {
            chunkSizes[i] = getOptimalChunkSize(axisSizes[i]);
        }
        return chunkSizes;
    }

    /**
     * Returns the optimal chunk size for a single axis given its size.
     * <p>
     * The heuristic is:
     * <pre>
     *   if (dim < 32)         -> chunk = dim      (i.e. no chunking for very small axes)
     *   else if (dim <= 128)   -> chunk = 32
     *   else if (dim < 512)    -> chunk = 64
     *   else                   -> chunk = 128
     * </pre>
     *
     * @param dim the size of the axis.
     * @return the optimal chunk size (which is a power of two) for that axis.
     */
    private static int getOptimalChunkSize(int dim) {
        if (dim < 32) {
            return dim;
        } else if (dim <= 128) {
            return 32;
        } else if (dim < 512) {
            return 64;
        } else {
            return 128;
        }
    }

    public static int[] cleanupIJ1ChunkSize(int[] chunkSize) {
        TIntList result = new TIntArrayList();
        result.add(chunkSize[0]);
        result.add(chunkSize[1]);
        for (int i = 2; i < chunkSize.length; i++) {
            if(chunkSize[i] > 1) {
                result.add(chunkSize[i]);
            }
        }
        return result.toArray();
    }

    public static String pathToZARRURI(Path path) {
        return "zarr://file:" + path.toAbsolutePath().toString().replace('\\', '/') + "/";
    }

    @SuppressWarnings("unchecked")
    public static <T extends NumericType<T>> RandomAccessibleInterval<T> wrap(ImagePlus image) {
        final RandomAccessibleInterval<T> baseImg;
        if (image.getType() == ImagePlus.COLOR_RGB)
            baseImg = (RandomAccessibleInterval<T>)(N5IJUtils.wrapRgbAsInt(image));
        else
            baseImg = (RandomAccessibleInterval<T>) ImageJFunctions.wrap(image);

        return baseImg;
    }

    @SuppressWarnings("unchecked")
    public static <M extends N5DatasetMetadata> M copyMetadata(final M metadata) {

        if (metadata == null)
            return metadata;

        // Needs to be implemented for metadata types that split channels
        if (metadata instanceof N5CosemMetadata) {
            return ((M)new N5CosemMetadata(metadata.getPath(), ((N5CosemMetadata)metadata).getCosemTransform(),
                    metadata.getAttributes()));
        } else if (metadata instanceof N5SingleScaleMetadata) {
            final N5SingleScaleMetadata ssm = (N5SingleScaleMetadata)metadata;
            return ((M)new N5SingleScaleMetadata(ssm.getPath(),
                    ssm.spatialTransform3d(), ssm.getDownsamplingFactors(),
                    ssm.getPixelResolution(), ssm.getOffset(), ssm.unit(),
                    metadata.getAttributes(),
                    ssm.minIntensity(),
                    ssm.maxIntensity(),
                    ssm.isLabelMultiset()));
        } else if (metadata instanceof NgffSingleScaleAxesMetadata) {
            final NgffSingleScaleAxesMetadata ngffMeta = (NgffSingleScaleAxesMetadata)metadata;
            return (M)new NgffSingleScaleAxesMetadata(ngffMeta.getPath(),
                    ngffMeta.getScale(), ngffMeta.getTranslation(),
                    ngffMeta.getAxes(),
                    ngffMeta.getAttributes());
        } else if (metadata instanceof N5ImagePlusMetadata) {
            final N5ImagePlusMetadata ijmeta = (N5ImagePlusMetadata)metadata;
            return (M)new N5ImagePlusMetadata(ijmeta.getPath(), ijmeta.getAttributes(),
                    ijmeta.getName(), ijmeta.fps, ijmeta.frameInterval, ijmeta.unit,
                    ijmeta.pixelWidth, ijmeta.pixelHeight, ijmeta.pixelDepth,
                    ijmeta.xOrigin, ijmeta.yOrigin, ijmeta.zOrigin,
                    ijmeta.numChannels, ijmeta.numSlices, ijmeta.numFrames,
                    ijmeta.type, ijmeta.properties);
        } else
            System.err.println("Encountered metadata of unexpected type.");

        return metadata;
    }

    public static <T extends RealType<T> & NativeType<T>, M extends N5DatasetMetadata> List<RandomAccessibleInterval<T>> splitChannels(final M metadata,
                                                                                                                                                 final RandomAccessibleInterval<T> img) {
        return Collections.singletonList(img);
    }
}
