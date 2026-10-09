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

package org.hkijena.jipipe.api.nodes.taxonomy;

/**
 * Functional interface that provides the input profile for a data class.
 * Providers are registered per data class in the node service component.
 * The method is total: it must never throw for unregistered data classes,
 * instead returning {@link JIPipeNodeTaxonomyInputProfile#NA}.
 */
@FunctionalInterface
public interface InputProfileProvider {

    /**
     * Returns the input profile for the given data class.
     *
     * @param dataClass the data class to derive the profile for
     * @return the input profile, or {@link JIPipeNodeTaxonomyInputProfile#NA}
     * if the data class has no registered profile
     */
    JIPipeNodeTaxonomyInputProfile getProfile(Class<?> dataClass);
}
