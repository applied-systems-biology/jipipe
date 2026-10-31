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

package org.hkijena.jipipe.plugins.settings.application;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;

import javax.swing.*;

/**
 * The <em>Raw settings</em> section of the settings tab (modern settings system,
 * Task 20).
 * <p>
 * The sheet is only the registration anchor that makes the raw editor a
 * first-class section of the tree and of the persistence model; it has no
 * parameters of its own. The tab renders
 * {@link org.hkijena.jipipe.desktop.app.settings.JIPipeDesktopRawSettingsSection}
 * in its place (see
 * {@link org.hkijena.jipipe.desktop.app.settings.JIPipeDesktopRawSettingsSection#registerRawSection()}).
 */
public class JIPipeDesktopRawSettingsApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {

    /**
     * The registration id (persisted in profiles; must never change).
     */
    public static final String ID = "org.hkijena.jipipe:desktop-raw-settings";

    /**
     * The section's category in the settings tree.
     */
    public static final String CATEGORY = "Advanced";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("actions/configure.png");
    }

    @Override
    public String getName() {
        return "Raw settings";
    }

    @Override
    public String getDescription() {
        return "Every setting of the selected scope, including values without a dedicated editor";
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Icon getCategoryIcon() {
        return JIPipe.RESOURCES.getIcon16("actions/configure.png");
    }

    @Override
    public JIPipeDefaultApplicationSettingsSheetCategory getDefaultCategory() {
        return JIPipeDefaultApplicationSettingsSheetCategory.UI;
    }
}