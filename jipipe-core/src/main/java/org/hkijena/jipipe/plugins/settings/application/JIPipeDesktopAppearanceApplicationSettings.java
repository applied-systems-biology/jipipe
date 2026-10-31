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
 * The merged <em>Appearance</em> section of the settings tab (modern settings
 * system, Task 19).
 * <p>
 * It is a registered application sheet so the section is first-class everywhere
 * the settings system looks at sheets: the section tree, the scope routing, the
 * key-catalog persistence and the carry-over. Its visible <em>content</em> is a
 * composition of parameters of the live
 * {@link JIPipeGeneralUIApplicationSettings} (theme, theme style) and
 * {@link JIPipeFileChooserApplicationSettings} (file dialog design + fallback)
 * sheets, rendered by the settings tab (see {@code AppearanceSectionModel}).
 * The sheet owns no settings of its own: it is the registration anchor of the
 * merged section (so the section is first-class in the tree, the scope routing,
 * the migration and the persistence), and its reset reverts the composed
 * sheets' blocks.
 */
public class JIPipeDesktopAppearanceApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {

    /**
     * The registration id (persisted in profiles; must never change).
     */
    public static final String ID = "org.hkijena.jipipe:desktop-appearance";

    /**
     * The section's category in the settings tree.
     */
    public static final String CATEGORY = "Appearance & accessibility";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("actions/palette.png");
    }

    @Override
    public String getName() {
        return "Appearance";
    }

    @Override
    public String getDescription() {
        return "Theme, UI style and file dialog design";
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Icon getCategoryIcon() {
        return JIPipe.RESOURCES.getIcon16("actions/palette.png");
    }

    @Override
    public JIPipeDefaultApplicationSettingsSheetCategory getDefaultCategory() {
        return JIPipeDefaultApplicationSettingsSheetCategory.UI;
    }
}
