package org.hkijena.jipipe.extensions.imagejdatatypes.util;

/**
 * Determines how elements of ROI are drawn
 */
public enum ROIElementDrawingMode {
    Always,
    Never,
    IfAvailable;

    @Override
    public String toString() {
        if(this == IfAvailable) {
            return "If available";
        }
        return super.toString();
    }

    public boolean shouldDraw(Object existing, Object alternative) {
        switch (this) {
            case Always:
                return existing != null || alternative != null;
            case Never:
                return false;
            case IfAvailable:
                return existing != null;
            default:
                throw new UnsupportedOperationException();
        }
    }
}
