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

package org.hkijena.jipipe.extensions.imagejdatatypes.util;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import ij.ImagePlus;

import java.util.Objects;

/**
 * Helper class that identifies an image slice.
 * Indices are shifted compared to ImageJ (-1 indicating that no assignment is set and zero being the first index)
 */
public class ImageSliceIndex {
    private int z = -1;
    private int c = -1;
    private int t = -1;

    /**
     * Initializes a slice index
     *
     * @param c channel
     * @param z Z-index
     * @param t frame
     */
    public ImageSliceIndex(int c, int z, int t) {
        this.z = z;
        this.c = c;
        this.t = t;
    }

    /**
     * Initializes a slice index where all values are -1
     */
    public ImageSliceIndex() {
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ImageSliceIndex(ImageSliceIndex other) {
        this.z = other.z;
        this.c = other.c;
        this.t = other.t;
    }

    @JsonGetter("z")
    public int getZ() {
        return z;
    }

    @JsonSetter("z")
    public void setZ(int z) {
        this.z = z;
    }

    @JsonGetter("c")
    public int getC() {
        return c;
    }

    @JsonSetter("c")
    public void setC(int c) {
        this.c = c;
    }

    @JsonGetter("t")
    public int getT() {
        return t;
    }

    @JsonSetter("t")
    public void setT(int t) {
        this.t = t;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImageSliceIndex that = (ImageSliceIndex) o;
        return z == that.z &&
                c == that.c &&
                t == that.t;
    }

    @Override
    public int hashCode() {
        return Objects.hash(z, c, t);
    }

    @Override
    public String toString() {
        return "z=" + z + ",c=" + c + ",t=" + t;
    }

    /**
     * Interprets this index as one-based index and returns the appropriate stack index
     *
     * @param imagePlus the reference image
     * @return the one-based stack index
     */
    public int oneSliceIndexToOneStackIndex(ImagePlus imagePlus) {
        return ImageJUtils.oneSliceIndexToOneStackIndex(c, z, t, imagePlus);
    }

    /**
     * Converts this zero-based index into a one-based index and returns a one-based stack index
     *
     * @param imagePlus the reference image
     * @return the one-based stack index
     */
    public int zeroSliceIndexToOneStackIndex(ImagePlus imagePlus) {
        return ImageJUtils.oneSliceIndexToOneStackIndex(c + 1, z + 1, t + 1, imagePlus);
    }

    /**
     * Interprets this index as one-based index and returns the appropriate stack index
     *
     * @param nChannels number of channels
     * @param nSlices   number of slices
     * @param nFrames   number of frames
     * @return the one-based stack index
     */
    public int oneSliceIndexToOneStackIndex(int nChannels, int nSlices, int nFrames) {
        return ImageJUtils.oneSliceIndexToOneStackIndex(c, z, t, nChannels, nSlices, nFrames);
    }

    /**
     * Converts this zero-based index into a one-based index and returns a one-based stack index
     *
     * @param nChannels number of channels
     * @param nSlices   number of slices
     * @param nFrames   number of frames
     * @return the one-based stack index
     */
    public int zeroSliceIndexToOneStackIndex(int nChannels, int nSlices, int nFrames) {
        return ImageJUtils.oneSliceIndexToOneStackIndex(c + 1, z + 1, t + 1, nChannels, nSlices, nFrames);
    }

    public ImageSliceIndex zeroToOne() {
        return add(1);
    }

    public ImageSliceIndex oneToZero() {
        return add(-1);
    }

    /**
     * Adds the value to all components
     *
     * @param zct the value
     * @return result
     */
    public ImageSliceIndex add(int zct) {
        return new ImageSliceIndex(c + zct, z + zct, t + zct);
    }
}
