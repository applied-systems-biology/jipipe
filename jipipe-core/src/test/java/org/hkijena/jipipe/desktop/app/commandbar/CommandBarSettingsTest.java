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

package org.hkijena.jipipe.desktop.app.commandbar;

import org.hkijena.jipipe.JIPipe;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link CommandBarSettings}: the persisted command bar list mode
 * and its null/unknown-safe read.
 * <p>
 * {@link #currentModeParsesStoredValuesAndFallsBack()} boots the live settings
 * service headlessly the same way
 * {@code KeymapSettingsJsonTest.currentIsNonNullWithDefaultsWhenNothingStored}
 * does: the module's {@code LauncherSessionListener}s isolate the profile into
 * a temp directory and pre-patch ImageJ, so a full {@link JIPipe} instance is
 * hermetic. {@link #keyConstantIsStableStringLiteral()} and
 * {@link #parseIsNullAndUnknownSafe()} are pure and do not boot the service.
 */
class CommandBarSettingsTest {

    @Test
    void currentModeParsesStoredValuesAndFallsBack() {
        JIPipe.ensureInstance();

        // Empty store: the binding falls back to the key's default
        // ("available-only" since verification wave 4).
        assertEquals(JIPipeDesktopCommandBarModel.ListMode.AVAILABLE_ONLY,
                CommandBarSettings.currentMode(),
                "an empty store must report the default mode AVAILABLE_ONLY");

        // A stored non-default id parses to its mode.
        CommandBarSettings.binding().set("grayed");
        assertEquals(JIPipeDesktopCommandBarModel.ListMode.GRAYED,
                CommandBarSettings.currentMode(),
                "the stored 'grayed' must parse to GRAYED");

        // Writing back the default removes the sub-path (defaults are never
        // materialized) and the mode falls back to AVAILABLE_ONLY again.
        CommandBarSettings.binding().set("available-only");
        assertEquals(JIPipeDesktopCommandBarModel.ListMode.AVAILABLE_ONLY,
                CommandBarSettings.currentMode(),
                "resetting to the default must report AVAILABLE_ONLY again");

        // A hand-edited (unknown) id must fall back to GRAYED, not throw.
        CommandBarSettings.binding().set("nonsense");
        assertEquals(JIPipeDesktopCommandBarModel.ListMode.GRAYED,
                CommandBarSettings.currentMode(),
                "an unknown stored id must fall back to GRAYED");
    }

    @Test
    void parseIsNullAndUnknownSafe() {
        // Pin the parse contract purely: null and unknown ids fall back to
        // GRAYED, every known id round-trips through its own constant.
        assertEquals(JIPipeDesktopCommandBarModel.ListMode.GRAYED,
                JIPipeDesktopCommandBarModel.ListMode.parse(null));
        assertEquals(JIPipeDesktopCommandBarModel.ListMode.GRAYED,
                JIPipeDesktopCommandBarModel.ListMode.parse("nonsense"));
        assertEquals(JIPipeDesktopCommandBarModel.ListMode.GRAYED,
                JIPipeDesktopCommandBarModel.ListMode.parse(JIPipeDesktopCommandBarModel.ListMode.GRAYED.id()));
        assertEquals(JIPipeDesktopCommandBarModel.ListMode.AVAILABLE_ONLY,
                JIPipeDesktopCommandBarModel.ListMode.parse(JIPipeDesktopCommandBarModel.ListMode.AVAILABLE_ONLY.id()));
        assertEquals(JIPipeDesktopCommandBarModel.ListMode.EVERYTHING,
                JIPipeDesktopCommandBarModel.ListMode.parse(JIPipeDesktopCommandBarModel.ListMode.EVERYTHING.id()));
    }

    @Test
    void keyConstantIsStableStringLiteral() {
        // The block path and sub-path are registration identities persisted in
        // saved settings documents: pin them as stable string literals.
        assertEquals("registry:command-bar", CommandBarSettings.BLOCK_PATH);
        assertEquals("available-only", CommandBarSettings.LIST_MODE.defaultValue());
    }
}
