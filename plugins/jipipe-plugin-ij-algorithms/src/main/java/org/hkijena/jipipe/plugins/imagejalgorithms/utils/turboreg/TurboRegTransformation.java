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

package org.hkijena.jipipe.plugins.imagejalgorithms.utils.turboreg;

public enum TurboRegTransformation {
    RigidBody("Rigid body", 3),
    ScaledRotation("Scaled rotation", 4),
    Translation("Translation", 2),
    GenericTransformation("None", -1),
    Affine("Affine", 6),
    Bilinear("Bilinear", 8);

    private final String label;
    private final int nativeValue;

    TurboRegTransformation(String label, int nativeValue) {
        this.label = label;
        this.nativeValue = nativeValue;
    }

    @Override
    public String toString() {
        return label;
    }

    public String getLabel() {
        return label;
    }

    public int getNativeValue() {
        return nativeValue;
    }


}
