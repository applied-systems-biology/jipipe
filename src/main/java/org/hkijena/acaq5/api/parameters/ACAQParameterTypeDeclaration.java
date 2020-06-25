/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.acaq5.api.parameters;

/**
 * A type that describes an ACAQ parameter type
 */
public interface ACAQParameterTypeDeclaration {

    /**
     * Creates a new non-null instance of the parameter type
     *
     * @return the instance
     */
    Object newInstance();

    /**
     * Duplicates the parameter
     *
     * @param original the original
     * @return the copy.
     */
    Object duplicate(Object original);

    /**
     * @return the unique ID of this parameter type
     */
    String getId();

    /**
     * @return the Java class of this parameter type
     */
    Class<?> getFieldClass();

    /**
     * @return a short descriptive name
     */
    String getName();

    /**
     * @return a description
     */
    String getDescription();
}
