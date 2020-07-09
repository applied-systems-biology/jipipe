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

package org.hkijena.jipipe.api.registries;

import org.hkijena.jipipe.api.JIPipeValidatable;

/**
 * A task for algorithm registration that can handle algorithm dependencies
 */
public interface JIPipeAlgorithmRegistrationTask extends JIPipeValidatable {
    /**
     * Runs the registration
     */
    void register();

    /**
     * Returns true if the registration can be done
     * This function should fail as fast as possible
     *
     * @return true if dependencies are met
     */
    boolean canRegister();
}
