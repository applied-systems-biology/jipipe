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
import org.hkijena.jipipe.api.settings.JIPipeApplicationSettingsSheet;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

/**
 * A dynamic settings sheet that lets users select the default cache display
 */
public class JIPipeDefaultCacheDisplayApplicationSettings extends JIPipeDynamicParameterCollection implements JIPipeApplicationSettingsSheet {
    public static final String ID = "org.hkijena.jipipe:default-cache-displays";

    public JIPipeDefaultCacheDisplayApplicationSettings() {
        super(false);
    }

    public JIPipeDefaultCacheDisplayApplicationSettings(JIPipeDynamicParameterCollection other) {
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
        return UIUtils.getIconFromResources("actions/zoom.png");
    }

    @Override
    public String getName() {
        return "Default cache displays";
    }

    @Override
    public String getCategory() {
        return JIPipeDefaultApplicationSettingsSheetCategory.Data.getCategory();
    }

    @Override
    public Icon getCategoryIcon() {
        return JIPipeDefaultApplicationSettingsSheetCategory.Data.getIcon();
    }

    @Override
    public String getDescription() {
        return "Determines how the JIPipe cache browser displays data by default (double click)";
    }
}
