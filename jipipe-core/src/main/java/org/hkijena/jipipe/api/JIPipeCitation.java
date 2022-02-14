package org.hkijena.jipipe.api;

import java.lang.annotation.*;

/**
 * Interface to add citations to nodes/data types
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(JIPipeCitations.class)
public @interface JIPipeCitation {
    String value();
}
