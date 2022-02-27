package org.hkijena.jipipe.extensions.imagejdatatypes.filters;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageDimensions;
import org.hkijena.jipipe.utils.classfilters.ClassFilter;

/**
 * Filters for image types that can be used for creating new instances.
 * These {@link org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData} are identified by their
 * {@link org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageDimensions} constructor.
 */
public class NonGenericImagePlusDataClassFilter implements ClassFilter {
    @Override
    public boolean test(Class<?> aClass) {
        return ImagePlusData.class.isAssignableFrom(aClass) && ConstructorUtils.getMatchingAccessibleConstructor(aClass, ImageDimensions.class) != null;
    }
}
