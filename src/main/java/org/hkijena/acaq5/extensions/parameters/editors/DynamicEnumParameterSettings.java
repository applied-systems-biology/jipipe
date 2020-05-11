package org.hkijena.acaq5.extensions.parameters.editors;

import java.util.List;
import java.util.function.Supplier;

/**
 * Settings for dynamic enum-like parameters for non {@link Enum} data types
 */
public @interface DynamicEnumParameterSettings {
    /**
     * Supplies the enum items. Class must have a standard constructor
     *
     * @return the enum items
     */
    Class<? extends Supplier<List<Object>>> supplier();
}
