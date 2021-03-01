package org.hkijena.jipipe.extensions.imagejdatatypes.util;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.Objects;

public class ImageSliceIndices {
    private TIntList z = new TIntArrayList();
    private TIntList c = new TIntArrayList();
    private TIntList t = new TIntArrayList();

    public ImageSliceIndices() {
    }

    public ImageSliceIndices(TIntList z, TIntList c, TIntList t) {
        this.z = z;
        this.c = c;
        this.t = t;
    }

    public ImageSliceIndices(ImageSliceIndices other) {
        this.z = new TIntArrayList(other.z);
        this.c = new TIntArrayList(other.c);
        this.t = new TIntArrayList(other.t);
    }

    public TIntList getZ() {
        return z;
    }

    public void setZ(TIntList z) {
        this.z = z;
    }

    public TIntList getC() {
        return c;
    }

    public void setC(TIntList c) {
        this.c = c;
    }

    public TIntList getT() {
        return t;
    }

    public void setT(TIntList t) {
        this.t = t;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImageSliceIndices that = (ImageSliceIndices) o;
        return z.equals(that.z) && c.equals(that.c) && t.equals(that.t);
    }

    @Override
    public int hashCode() {
        return Objects.hash(z, c, t);
    }
}
