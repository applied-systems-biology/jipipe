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
