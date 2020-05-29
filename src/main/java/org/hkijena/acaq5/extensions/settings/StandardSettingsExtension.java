package org.hkijena.acaq5.extensions.settings;

import org.hkijena.acaq5.ACAQJavaExtension;
import org.hkijena.acaq5.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.scijava.plugin.Plugin;

/**
 * Extension that provides all ACAQ5 included settings sheets
 */
@Plugin(type = ACAQJavaExtension.class)
public class StandardSettingsExtension extends ACAQPrepackagedDefaultJavaExtension {
    @Override
    public String getName() {
        return "Standard settings";
    }

    @Override
    public String getDescription() {
        return "Provides a collection of settings sheets";
    }

    @Override
    public String getDependencyId() {
        return "standard-settings";
    }

    @Override
    public String getDependencyVersion() {
        return "1.0.0";
    }

    @Override
    public void register() {
        registerSettingsSheet(RuntimeSettings.ID, "Runtime", null, null, new RuntimeSettings());
        registerSettingsSheet(RecentProjectsUISettings.ID, "Recent projects", null, null, new RecentProjectsUISettings());
        registerSettingsSheet(GraphEditorUISettings.ID, "Graph editor", "UI", null, new GraphEditorUISettings());
    }

}
