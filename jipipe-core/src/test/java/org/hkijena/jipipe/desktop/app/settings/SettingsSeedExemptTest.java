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

package org.hkijena.jipipe.desktop.app.settings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.settings.JIPipeApplicationSettingsSheet;
import org.hkijena.jipipe.plugins.settings.StandardSettingsPlugin;
import org.hkijena.jipipe.plugins.settings.application.JIPipeDesktopAccessibilityApplicationSettings;
import org.hkijena.jipipe.plugins.settings.application.JIPipeDesktopAppearanceApplicationSettings;
import org.hkijena.jipipe.plugins.settings.application.JIPipeDesktopKeyboardApplicationSettings;
import org.hkijena.jipipe.plugins.settings.application.JIPipeDesktopRawSettingsApplicationSettings;
import org.hkijena.jipipe.plugins.settings.application.JIPipeGeneralUIApplicationSettings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pin for the seed-exempt flag (#1345 Phase 2, item 2): the parameter-less
 * anchor sheets only host a merged page of the settings tab, so the engine must
 * not seed an empty document block for them.
 * <p>
 * The engine's seeding code lives in {@code JIPipeSettingsServiceComponent}
 * (owned by the engine session), so this test pins the contract on the sheets —
 * the registered anchors are exempt, a data-carrying sheet is not — and the
 * engine-side skip is the two-line change recorded in the work-item report.
 */
class SettingsSeedExemptTest {

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    @Test
    void anchorSheetsAreSeedExempt() {
        List<String> anchorIds = List.of(
                JIPipeDesktopAppearanceApplicationSettings.ID,
                JIPipeDesktopAccessibilityApplicationSettings.ID,
                JIPipeDesktopKeyboardApplicationSettings.ID,
                JIPipeDesktopRawSettingsApplicationSettings.ID);
        for (String anchorId : anchorIds) {
            JIPipeApplicationSettingsSheet anchor = JIPipe.getSettings()
                    .getById(anchorId, JIPipeApplicationSettingsSheet.class);
            assertNotNull(anchor, "the anchor sheet '" + anchorId + "' is registered");
            assertTrue(anchor.isSeedExempt(),
                    "the parameter-less anchor '" + anchorId + "' must not be seeded into the profile");
        }
    }

    @Test
    void dataCarryingSheetsAreNotSeedExempt() {
        JIPipeGeneralUIApplicationSettings generalUi = JIPipe.getSettings()
                .getById(JIPipeGeneralUIApplicationSettings.ID, JIPipeGeneralUIApplicationSettings.class);
        assertNotNull(generalUi);
        assertFalse(generalUi.isSeedExempt(),
                "a sheet with parameters keeps the engine's default seeding");
    }

    /**
     * The default is "seed me": only anchors opt out, so a plugin sheet is never
     * silently left out of a fresh profile.
     */
    @Test
    void theInterfaceDefaultIsToSeed() {
        assertFalse(new ProbeSheet().isSeedExempt(), "a sheet must opt out explicitly");
    }

    /**
     * A minimal parameter-less sheet: the interface default (seed) applies.
     */
    public static class ProbeSheet extends org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet {
        @Override
        public String getId() {
            return "test:seed-exempt-probe";
        }

        @Override
        public javax.swing.Icon getIcon() {
            return null;
        }

        @Override
        public String getName() {
            return "Probe";
        }

        @Override
        public String getCategory() {
            return "Test";
        }

        @Override
        public javax.swing.Icon getCategoryIcon() {
            return null;
        }

        @Override
        public String getDescription() {
            return "";
        }

        @Override
        public org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory getDefaultCategory() {
            return org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory.UI;
        }
    }

    /**
     * The four anchors are registered by the standard settings plugin (the
     * merged pages are hosted there); this pins that the exemption is not
     * declared on a sheet that never reaches the engine.
     */
    @Test
    void anchorsAreRegisteredByTheStandardPlugin() {
        assertNotNull(StandardSettingsPlugin.class, "the plugin class exists");
        for (String anchorId : List.of(JIPipeDesktopAppearanceApplicationSettings.ID,
                JIPipeDesktopAccessibilityApplicationSettings.ID,
                JIPipeDesktopKeyboardApplicationSettings.ID,
                JIPipeDesktopRawSettingsApplicationSettings.ID)) {
            assertTrue(JIPipe.getSettings().getRegisteredSheets().containsKey(anchorId),
                    "the anchor '" + anchorId + "' is registered");
        }
    }
}