package org.hkijena.jipipe.extensions.imagejdatatypes.util;

/**
 * Encapsulates the dimensions of an image.
 */
public class ImageDimensions {
    private final int width;
    private final int height;
    private final int sizeZ;
    private final int sizeC;
    private final int sizeT;

    public ImageDimensions(int width, int height, int sizeZ, int sizeC, int sizeT) {
        this.width = width;
        this.height = height;
        this.sizeZ = sizeZ;
        this.sizeC = sizeC;
        this.sizeT = sizeT;
        if(width <= 0)
            throw new IllegalArgumentException("Invalid width " + width);
        if(height <= 0)
            throw new IllegalArgumentException("Invalid height " + height);
        if(sizeZ <= 0)
            throw new IllegalArgumentException("Invalid sizeZ " + sizeZ);
        if(sizeC <= 0)
            throw new IllegalArgumentException("Invalid sizeC " + sizeC);
        if(sizeT <= 0)
            throw new IllegalArgumentException("Invalid sizeT " + sizeT);
    }

    public ImageDimensions(int width, int height) {
        this(width, height, 1,1,1);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getSizeZ() {
        return sizeZ;
    }

    public int getSizeC() {
        return sizeC;
    }

    public int getSizeT() {
        return sizeT;
    }

    public int getNumDimensions() {
        return 2 + (sizeZ > 0 ? 1 : 0) + (sizeC > 0 ? 1 : 0) + (sizeT > 0 ? 1 : 0);
    }

    public boolean is2D() {
        return getNumDimensions() <= 2;
    }

    public boolean is3D() {
        return getNumDimensions() <= 3;
    }

    public boolean is4D() {
        return getNumDimensions() <= 4;
    }

    public boolean is5D() {
        return getNumDimensions() <= 5;
    }
}
