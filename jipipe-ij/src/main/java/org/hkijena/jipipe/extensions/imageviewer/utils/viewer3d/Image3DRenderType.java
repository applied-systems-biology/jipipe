package org.hkijena.jipipe.extensions.imageviewer.utils.viewer3d;

public enum Image3DRenderType {
    Volume(0),
    OrthoSlice(1),
    Surface(2),
    SurfacePlot2D(3),
    MultiOrthoSlices(4);

    private final int nativeValue;

    Image3DRenderType(int nativeValue) {

        this.nativeValue = nativeValue;
    }

    public int getNativeValue() {
        return nativeValue;
    }
}
