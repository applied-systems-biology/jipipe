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
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.services.OMEXMLService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.NumericType;
import ome.xml.meta.OMEXMLMetadata;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.utils.PathUtils;
import org.janelia.saalfeldlab.n5.ij.N5IJUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
            if (chunkSize[i] > 1) {
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
            baseImg = (RandomAccessibleInterval<T>) (N5IJUtils.wrapRgbAsInt(image));
        else
            baseImg = (RandomAccessibleInterval<T>) ImageJFunctions.wrap(image);

        return baseImg;
    }

    public static void writeOMEXMLToZARR(Path localZarrPath, OMEXMLMetadata metadata, JIPipeProgressInfo progressInfo) {
        // N5 seems to no support writing arbitrary data and currently the specification is still open https://github.com/ome/ngff/issues/104
        // So I just follow whatever Qupath does
        PathUtils.createDirectories(localZarrPath.resolve("OME"));
        try {
            // Manually write the ZARR group info
            Files.write(localZarrPath.resolve("OME").resolve(".zgroup"), ("{\n" + "  \"zarr_format\" : 2\n" + "}").getBytes(StandardCharsets.UTF_8));

            // Manually write the OME metadata
            Files.write(localZarrPath.resolve("OME").resolve("METADATA.ome.xml"), metadata.dumpXML().getBytes(StandardCharsets.UTF_8));

            progressInfo.log("OME XML data was written to " + localZarrPath.toAbsolutePath() + " (QuPath flavor)");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static OMEXMLMetadata readOMEXMLFromZARR(Path localZarrPath, JIPipeProgressInfo progressInfo) throws IOException {
        Path xmlPath = localZarrPath.resolve("OME").resolve("METADATA.ome.xml");
        if(Files.isRegularFile(xmlPath)) {
            progressInfo.log("Reading OME XML metadata from " + xmlPath.toAbsolutePath() + " (QuPath flavor)");
            String omeXml = new String(Files.readAllBytes(xmlPath));
            try {
                ServiceFactory serviceFactory = new ServiceFactory();
                OMEXMLService omexmlService = serviceFactory.getInstance(OMEXMLService.class);
                return omexmlService.createOMEXMLMetadata(omeXml);
            } catch (DependencyException | ServiceException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            return null;
        }
    }

}
