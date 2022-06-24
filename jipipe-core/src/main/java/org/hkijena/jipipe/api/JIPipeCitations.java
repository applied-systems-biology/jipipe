package org.hkijena.jipipe.api;

import java.lang.annotation.*;

/**
 * Multiple {@link JIPipeCitation}
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface JIPipeCitations {
    JIPipeCitation[] value();
}
