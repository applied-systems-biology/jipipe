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

package org.hkijena.jipipe.plugins.imagejdatatypes.filters;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageDimensions;
import org.hkijena.jipipe.utils.classfilters.ClassFilter;

/**
 * Filters for image types that can be used for creating new instances.
 * These {@link org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData} are identified by their
 * {@link ImageDimensions} constructor.
 */
public class NonGenericImagePlusDataClassFilter implements ClassFilter {
    @Override
    public boolean test(Class<?> aClass) {
        return ImagePlusData.class.isAssignableFrom(aClass) && ConstructorUtils.getMatchingAccessibleConstructor(aClass, ImageDimensions.class) != null;
    }
}
