package org.hkijena.jipipe.utils;

import java.awt.*;

public enum SizeFitMode {
    Cover,
    Fit,
    FitWidth,
    FitHeight;
    
    public Dimension fitSize(int availableWidth, int availableHeight, int imageWidth, int imageHeight, double scaleFactor) {
        int newWidth, newHeight;
        double factorH = 1.0 * availableHeight / imageHeight;
        double factorW = 1.0 * availableWidth / imageWidth;
        switch (this) {
            case Cover: {
                double factor = Math.max(factorH, factorW) * scaleFactor;
                newWidth = (int) (factor * imageWidth);
                newHeight = (int) (factor * imageHeight);
            }
            break;
            case FitHeight: {
                newWidth = (int) (factorH * scaleFactor * imageWidth);
                newHeight = (int) (factorH * scaleFactor * imageHeight);
            }
            break;
            case FitWidth: {
                newWidth = (int) (factorW * scaleFactor * imageWidth);
                newHeight = (int) (factorW * scaleFactor * imageHeight);
            }
            break;
            case Fit: {
                double factor = Math.min(factorH, factorW) * scaleFactor;
                newWidth = (int) (factor * imageWidth);
                newHeight = (int) (factor * imageHeight);
            }
            break;
            default:
                throw new IllegalStateException();
        }
        return new Dimension(newWidth, newHeight);
    }
}
