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

package org.hkijena.jipipe.plugins.settings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;

/**
 * A dynamic settings sheet that lets users select the default cache display
 */
public class DefaultCacheDisplaySettings extends JIPipeDynamicParameterCollection {
    public static final String ID = "default-cache-displays";

    public DefaultCacheDisplaySettings() {
        super(false);
    }

    public DefaultCacheDisplaySettings(JIPipeDynamicParameterCollection other) {
        super(other);
    }

    public static DefaultCacheDisplaySettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, DefaultCacheDisplaySettings.class);
    }
}
