package org.hkijena.jipipe.extensions.imagejalgorithms.parameters;

import ij.process.ImageProcessor;
import org.hkijena.jipipe.extensions.imagejalgorithms.nodes.transform.WrapMode;

public enum SourceWrapMode {
    Skip,
    Zero,
    Wrap,
    Replicate,
    Mirror;

    public boolean isValidPosition(ImageProcessor processor, int x, int y) {
        return this != Skip || x >= 0 && y >= 0 && x < processor.getWidth() && y < processor.getHeight();
    }

    public int getPixel(ImageProcessor processor, int x, int y) {
        switch (this) {
            case Skip:
            case Zero:
                return isValidPosition(processor, x, y) ? processor.get(x, y) : 0;
            case Replicate:
                return processor.get(WrapMode.Replicate.wrap(x, 0, processor.getWidth()),
                        WrapMode.Replicate.wrap(y, 0, processor.getHeight()));
            case Mirror:
                return processor.get(WrapMode.Mirror.wrap(x, 0, processor.getWidth()),
                        WrapMode.Mirror.wrap(y, 0, processor.getHeight()));
            case Wrap:
                return processor.get(WrapMode.Wrap.wrap(x, 0, processor.getWidth()),
                        WrapMode.Wrap.wrap(y, 0, processor.getHeight()));
            default:
                throw new UnsupportedOperationException();
        }
    }

    public float getPixelFloat(ImageProcessor processor, int x, int y) {
        switch (this) {
            case Skip:
            case Zero:
                return (x >= 0 && y >= 0 && x < processor.getWidth() && y < processor.getHeight()) ? processor.getf(x, y) : 0;
            case Replicate:
                return processor.getf(WrapMode.Replicate.wrap(x, 0, processor.getWidth()),
                        WrapMode.Replicate.wrap(y, 0, processor.getHeight()));
            case Mirror:
                return processor.getf(WrapMode.Mirror.wrap(x, 0, processor.getWidth()),
                        WrapMode.Mirror.wrap(y, 0, processor.getHeight()));
            case Wrap:
                return processor.getf(WrapMode.Wrap.wrap(x, 0, processor.getWidth()),
                        WrapMode.Wrap.wrap(y, 0, processor.getHeight()));
            default:
                throw new UnsupportedOperationException();
        }
    }
}
