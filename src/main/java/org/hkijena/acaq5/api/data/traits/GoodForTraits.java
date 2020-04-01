package org.hkijena.acaq5.api.data.traits;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Array of {@link GoodForTrait}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GoodForTraits {
    /**
     * @return trait ids
     */
    GoodForTrait[] value();
}
