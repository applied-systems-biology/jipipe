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

package org.hkijena.jipipe.plugins.imageviewer.utils.viewer3d;

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


    @Override
    public String toString() {
        switch (this) {
            case OrthoSlice:
                return "Ortho slice";
            case SurfacePlot2D:
                return "Surface plot 2D";
            case MultiOrthoSlices:
                return "Ortho slice (advanced)";
            default:
                return name();
        }
    }
}
