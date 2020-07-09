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

package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.ACAQRegistry;

/**
 * Triggered by {@link org.hkijena.acaq5.ACAQRegistry} when an extension is registered
 */
public class ExtensionRegisteredEvent {
    private ACAQRegistry registry;
    private ACAQDependency extension;

    /**
     * @param registry  event source
     * @param extension registered extension
     */
    public ExtensionRegisteredEvent(ACAQRegistry registry, ACAQDependency extension) {
        this.registry = registry;
        this.extension = extension;
    }

    public ACAQRegistry getRegistry() {
        return registry;
    }

    public ACAQDependency getExtension() {
        return extension;
    }
}
