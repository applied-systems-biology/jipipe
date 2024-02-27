package org.hkijena.jipipe.extensions.ilastik.utils;

import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5DataTypeInformation;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedLongType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * From ilastik4ij
 * <p>
 * MIT License
 * <p>
 * Copyright (c) 2017 ilastik
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
public class Hdf5Utils {
    private static final Map<String, NativeType<?>> H5_TO_IMGLIB2_TYPE;

    static {
        Map<String, NativeType<?>> map = new HashMap<>();
        map.put("float32", new FloatType());
        map.put("uint8", new UnsignedByteType());
        map.put("uint16", new UnsignedShortType());
        map.put("uint32", new UnsignedIntType());
        map.put("uint64", new UnsignedLongType());
        H5_TO_IMGLIB2_TYPE = Collections.unmodifiableMap(map);
    }

    private static final long BLOCK_SIZE_2D = 128;
    private static final long BLOCK_SIZE_3D = 32;

    public static <T extends NativeType<T>> T getNativeType(String dtype) {
        @SuppressWarnings("unchecked")
        T result = (T) H5_TO_IMGLIB2_TYPE.get(dtype);
        return result;
    }

    public static <T extends Type<T>> String getDtype(Class<T> clazz) {
        return H5_TO_IMGLIB2_TYPE.entrySet()
                .stream()
                .filter(e -> clazz == e.getValue().getClass())
                .map(Map.Entry::getKey)
                .findFirst().orElse("unknown");
    }

    public static int[] blockSize(long[] datasetDims) {
        // expect rank 5 dims with tzyxc axis order
        long bSize;
        if (datasetDims[1] > 1) {
            // if z-axis is non-singleton use 64x64x64 chunk size
            bSize = BLOCK_SIZE_3D;
        } else {
            // otherwise use 128x128 chunk size
            bSize = BLOCK_SIZE_2D;
        }

        int[] result = new int[datasetDims.length];

        for (int i = 0; i < datasetDims.length; i++) {
            result[i] = (int) Math.min(bSize, datasetDims[i]);
        }
        // reset t axis
        result[0] = 1;
        // reset c axis
        result[4] = 1;

        return result;
    }

    public static long[] getXYSliceDims(long[] datasetDims) {
        // expect rank 5 dims with tzyxc axis order
        long[] result = datasetDims.clone();
        result[0] = 1;
        result[1] = 1;
        result[4] = 1;

        return result;
    }

    public static String getTypeInfo(HDF5DataSetInformation dsInfo) {
        HDF5DataTypeInformation dsType = dsInfo.getTypeInformation();
        int bitdepth = 8 * dsType.getElementSize();
        String type = "";

        if (!dsType.isSigned()) {
            type += "u";
        }

        switch (dsType.getDataClass()) {
            case INTEGER:
                type += "int" + bitdepth;
                break;
            case FLOAT:
                type += "float" + bitdepth;
                break;
            default:
                type += dsInfo.toString();
        }
        return type;
    }
}
