package org.hkijena.jipipe.extensions.settings;

import org.hkijena.jipipe.JIPipeDefaultRegistry;
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
        return JIPipeDefaultRegistry.getInstance().getSettingsRegistry().getSettings(ID, DefaultResultImporterSettings.class);
    }
}
