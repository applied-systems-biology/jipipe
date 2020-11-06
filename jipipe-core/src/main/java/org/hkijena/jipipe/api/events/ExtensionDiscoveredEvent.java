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

package org.hkijena.jipipe.api.events;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;

/**
 * Triggered when a new extension was discovered
 */
public class ExtensionDiscoveredEvent {
    private final JIPipe registry;
    private final JIPipeDependency extension;

    public ExtensionDiscoveredEvent(JIPipe registry, JIPipeDependency extension) {
        this.registry = registry;
        this.extension = extension;
    }

    public JIPipe getRegistry() {
        return registry;
    }

    public JIPipeDependency getExtension() {
        return extension;
    }
}
