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

package org.hkijena.jipipe.plugins.imagejdatatypes;

import org.hkijena.jipipe.api.nodes.taxonomy.InputProfileProvider;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyInputProfile;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImageTypeInfo;

/**
 * Derives the {@link JIPipeNodeTaxonomyInputProfile} of a data class from its
 * {@link ImageTypeInfo} annotation.
 * <p>
 * The mapping of {@link ImageTypeInfo#numDimensions()} is:
 * <ul>
 * <li>{@code 2} → {@link JIPipeNodeTaxonomyInputProfile#PLANE_2D},</li>
 * <li>{@code 3} → {@link JIPipeNodeTaxonomyInputProfile#VOLUME_3D},</li>
 * <li>{@code -1} (no constraint) → {@link JIPipeNodeTaxonomyInputProfile#ANY}
 * (an image type without a dimension constraint),</li>
 * <li>annotation absent → {@link JIPipeNodeTaxonomyInputProfile#NA} (a
 * non-image type with no derivable profile).</li>
 * </ul>
 * <p>
 * This is a pure function over the annotation: it never throws, satisfying the
 * total-derivation contract of {@link InputProfileProvider}.
 */
public class ImageTypeInfoInputProfileProvider implements InputProfileProvider {

    @Override
    public JIPipeNodeTaxonomyInputProfile getProfile(Class<?> dataClass) {
        if (dataClass == null) {
            return JIPipeNodeTaxonomyInputProfile.NA;
        }
        ImageTypeInfo info = dataClass.getAnnotation(ImageTypeInfo.class);
        if (info == null) {
            return JIPipeNodeTaxonomyInputProfile.NA;
        }
        return switch (info.numDimensions()) {
            case 2 -> JIPipeNodeTaxonomyInputProfile.PLANE_2D;
            case 3 -> JIPipeNodeTaxonomyInputProfile.VOLUME_3D;
            default -> JIPipeNodeTaxonomyInputProfile.ANY;
        };
    }
}
