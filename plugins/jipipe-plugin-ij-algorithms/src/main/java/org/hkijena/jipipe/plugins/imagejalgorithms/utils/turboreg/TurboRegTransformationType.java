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

public enum TurboRegTransformationType {
    RigidBody("Rigid body", "RIGID_BODY", 3),
    ScaledRotation("Scaled rotation", "SCALED_ROTATION", 4),
    Translation("Translation", "TRANSLATION", 2),
    GenericTransformation("None", "GENERIC_TRANSFORMATION", -1),
    Affine("Affine", "AFFINE", 6),
    Bilinear("Bilinear (UNSUPPORTED)", "BILINEAR", 8);

    private final String label;
    private final String name;
    private final int nativeValue;

    TurboRegTransformationType(String label, String name, int nativeValue) {
        this.label = label;
        this.name = name;
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

    public String getName() {
        return name;
    }

    public static TurboRegTransformationType fromNativeValue(int nativeValue) {
        for (TurboRegTransformationType value : TurboRegTransformationType.values()) {
            if(value.nativeValue == nativeValue){
                return value;
            }
        }
        return null;
    }

    public static TurboRegTransformationType fromNativeName(String name) {
        for (TurboRegTransformationType value : TurboRegTransformationType.values()) {
            if(value.name.equals(name)){
                return value;
            }
        }
        return null;
    }
}
