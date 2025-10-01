package org.hkijena.jipipe.plugins.settings.application;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.settings.JIPipeApplicationSettingsSheet;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;

import javax.swing.*;

public class JIPipeDefaultEnvironmentsApplicationSettings extends JIPipeDynamicParameterCollection implements JIPipeApplicationSettingsSheet {
    public static final String ID = "org.hkijena.jipipe:default-environments";

    public JIPipeDefaultEnvironmentsApplicationSettings() {
        super(false);
    }

    public JIPipeDefaultEnvironmentsApplicationSettings(JIPipeDynamicParameterCollection other) {
        super(other);
    }

    public static JIPipeDefaultCacheDisplayApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, JIPipeDefaultCacheDisplayApplicationSettings.class);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("actions/kt-restore-defaults.png");
    }

    @Override
    public String getName() {
        return "Defaults";
    }

    @Override
    public String getCategory() {
        return JIPipeDefaultApplicationSettingsSheetCategory.Environments.getCategory();
    }

    @Override
    public Icon getCategoryIcon() {
        return JIPipeDefaultApplicationSettingsSheetCategory.Environments.getIcon();
    }

    @Override
    public String getDescription() {
        return "Allows to configure application-wide connected services";
    }
}
