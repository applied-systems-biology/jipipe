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

package org.hkijena.acaq5;

import org.scijava.plugin.SciJavaPlugin;

/**
 * A Java extension
 */
public interface ACAQJavaExtension extends SciJavaPlugin, ACAQDependency {

    /**
     * Returns the registry
     *
     * @return The registry
     */
    ACAQDefaultRegistry getRegistry();

    /**
     * Sets the registry
     *
     * @param registry The registry
     */
    void setRegistry(ACAQDefaultRegistry registry);

    /**
     * Registers custom modules into ACAQ5
     */
    void register();
}
