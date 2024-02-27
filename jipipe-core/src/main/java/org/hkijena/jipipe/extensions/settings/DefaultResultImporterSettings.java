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

package org.hkijena.jipipe.extensions.settings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;

/**
 * A dynamic settings sheet that lets users select the default importer
 */
public class DefaultResultImporterSettings extends JIPipeDynamicParameterCollection {
    public static final String ID = "default-result-importers";

    public DefaultResultImporterSettings() {
        super(false);
    }

    public DefaultResultImporterSettings(JIPipeDynamicParameterCollection other) {
        super(other);
    }

    public static DefaultResultImporterSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, DefaultResultImporterSettings.class);
    }
}
