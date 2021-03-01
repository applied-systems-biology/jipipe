package org.hkijena.jipipe.extensions.imagejdatatypes.util;

import java.util.Objects;

public class ImageSliceCube {
    private ImageSliceIndex first = new ImageSliceIndex();
    private ImageSliceIndex second = new ImageSliceIndex();

    public ImageSliceCube() {
    }

    public ImageSliceCube(ImageSliceIndex first, ImageSliceIndex second) {
        this.first = first;
        this.second = second;
    }

    public ImageSliceCube(ImageSliceCube other) {
        this.first = new ImageSliceIndex(other.first);
        this.second = new ImageSliceIndex(other.second);
    }

    public ImageSliceIndex getFirst() {
        return first;
    }

    public void setFirst(ImageSliceIndex first) {
        this.first = first;
    }

    public ImageSliceIndex getSecond() {
        return second;
    }

    public void setSecond(ImageSliceIndex second) {
        this.second = second;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImageSliceCube that = (ImageSliceCube) o;
        return first.equals(that.first) && second.equals(that.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }
}
