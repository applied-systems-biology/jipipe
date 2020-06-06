package org.hkijena.acaq5.extensions.imagejalgorithms;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.Objects;

/**
 * Helper class that identifies an image slice.
 * Indices are shifted compared to ImageJ (-1 indicating that no assignment is set and zero being the first index)
 */
public class SliceIndex {
    private int z = -1;
    private int c = -1;
    private int t = -1;

    /**
     * Initializes a slice index
     * @param z Z-index
     * @param c channel
     * @param t frame
     */
    public SliceIndex(int z, int c, int t) {
        this.z = z;
        this.c = c;
        this.t = t;
    }

    /**
     * Initializes a slice index where all values are -1
     */
    public SliceIndex() {
    }

    /**
     * Creates a copy
     * @param other the original
     */
    public SliceIndex(SliceIndex other) {
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
        SliceIndex that = (SliceIndex) o;
        return z == that.z &&
                c == that.c &&
                t == that.t;
    }

    @Override
    public int hashCode() {
        return Objects.hash(z, c, t);
    }
}
