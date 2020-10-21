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
