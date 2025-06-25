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

package org.hkijena.jipipe.api.parameters;

/**
 * Available parameter archetypes that gives hints on the function of the parameter
 */
public enum JIPipeParameterArchetype {
    /**
     * A single primitive value: number, string, or boolean.
     */
    Value,

    /**
     * An optional value
     */
    OptionalValue,

    /**
     * A list of values, potentially of the same type.
     */
    List,

    /**
     * A choice from a predefined set (like an enum, only one allowed).
     */
    SingleSelect,

    /**
     * A list of predefined values (like an enum, multiple allowed).
     */
    MultiSelect,

    /**
     * A structured set of named sub-parameters (composite).
     */
    Object,

    /**
     * A dynamic key-value structure, where values may be of the same type.
     */
    Map,

    /**
     * A bounded number range (e.g., min/max).
     */
    Range,

    /**
     * One of multiple possible types (typically using a type discriminator).
     */
    Union,

    /**
     * Points to another parameter or entity (e.g., ID-based reference).
     */
    Reference,

    /**
     * Fallback type when none of the other types fit.
     */
    Unknown
}
