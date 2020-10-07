package org.hkijena.jipipe.extensions.settings;

import org.hkijena.jipipe.JIPipeDefaultRegistry;
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
        return JIPipeDefaultRegistry.getInstance().getSettingsRegistry().getSettings(ID, DefaultCacheDisplaySettings.class);
    }
}
