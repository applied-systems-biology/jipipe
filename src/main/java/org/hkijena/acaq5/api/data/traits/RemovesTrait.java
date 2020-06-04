package org.hkijena.acaq5.api.data.traits;

import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;

import java.lang.annotation.*;

/**
 * Annotates an {@link ACAQGraphNode} to remove given trait from all output slots
 * Attaching this annotation automatically runs removesTraits() in {@link ACAQTraitConfiguration} if autoRemove is true
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(RemovesTraits.class)
public @interface RemovesTrait {
    /**
     * The removed trait of same or specialized type
     *
     * @return trait type
     */
    String value();
}
