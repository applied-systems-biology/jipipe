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

import ij.ImagePlus;

public class TurboRegResult {
    private ImagePlus transformedTargetImage;
    private TurboRegTransformationInfo transformation;

    public TurboRegTransformationInfo getTransformation() {
        return transformation;
    }

    public void setTransformation(TurboRegTransformationInfo transformation) {
        this.transformation = transformation;
    }

    public ImagePlus getTransformedTargetImage() {
        return transformedTargetImage;
    }

    public void setTransformedTargetImage(ImagePlus transformedTargetImage) {
        this.transformedTargetImage = transformedTargetImage;
    }
}
