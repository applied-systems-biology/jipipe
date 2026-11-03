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
 * The merged <em>Accessibility</em> section of the settings tab (modern settings
 * system, Task 19).
 * <p>
 * A registered application sheet, so the section is first-class in the section
 * tree, the scope routing, the migration and the persistence. It owns no
 * settings of its own: its content (reduced motion from
 * {@link JIPipeGeneralUIApplicationSettings} plus the UI-scale control, which
 * lives in the jaunch config files rather than in a settings block) is rendered
 * by the settings tab from the shared {@code AppearanceSectionModel}
 * composition, and its reset reverts the composed sheets' blocks.
 */
public class JIPipeDesktopAccessibilityApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {

    /**
     * The registration id (persisted in profiles; must never change).
     */
    public static final String ID = "org.hkijena.jipipe:desktop-accessibility";

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
        return JIPipe.RESOURCES.getIcon16("actions/universal-access.png");
    }

    @Override
    public String getName() {
        return "Accessibility";
    }

    @Override
    public String getDescription() {
        return "Reduced motion and UI scale";
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Icon getCategoryIcon() {
        return JIPipe.RESOURCES.getIcon16("actions/universal-access.png");
    }

    @Override
    public JIPipeDefaultApplicationSettingsSheetCategory getDefaultCategory() {
        return JIPipeDefaultApplicationSettingsSheetCategory.UI;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The merged-section anchor has no parameters of its own (it hosts a merged page of the settings tab), so an empty document block would be pure profile noise. The engine must not seed one.
     *
     * @return always {@code true}
     */
    @Override
    public boolean isSeedExempt() {
        return true;
    }
}
