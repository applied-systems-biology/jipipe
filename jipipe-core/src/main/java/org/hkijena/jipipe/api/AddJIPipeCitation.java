package org.hkijena.jipipe.api;

import java.lang.annotation.*;

/**
 * Interface to add citations to nodes/data types
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Repeatable(AddJIPipeCitations.class)
public @interface AddJIPipeCitation {
    String value();
}
