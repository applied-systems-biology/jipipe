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
 * Determines how a parameter of {@link JIPipeParameterCollection} is serialized
 */
public enum JIPipeParameterSerializationMode {
    /**
     * The parameter is serialized as collection, meaning that sub-parameters are
     * unwrapped. This is equal to the behavior of 'Object' for non {@link JIPipeParameterCollection} objects.
     * This is the default and most robust method, as the minimal amount of information is lost on API changes.
     */
    Default,
    /**
     * The parameter is serialized into an object instead of exploded into the root object.
     * Requires that the parameter inherits from {@link org.hkijena.jipipe.utils.json.JsonDeserializable}.
     * This is less robust than the Collection method, but might be preferred for certain types.
     */
    Object,
    /**
     * The parameter is not serialized
     */
    None
}
