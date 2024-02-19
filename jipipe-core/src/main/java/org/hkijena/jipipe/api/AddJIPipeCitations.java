package org.hkijena.jipipe.api;

import java.lang.annotation.*;

/**
 * Multiple {@link AddJIPipeCitation}
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface AddJIPipeCitations {
    AddJIPipeCitation[] value();
}
