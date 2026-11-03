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

package org.hkijena.jipipe.settings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Task 22 (and work item 3 of the follow-up) of the modern settings system: the
 * non-modal settings tab is the only settings UI. The former modal dialogs —
 * the application settings ({@code JIPipeDesktopApplicationSettingsUI}), the
 * "Customize JIPipe" dialog ({@code JIPipeDesktopCustomizerDialog}, removed in
 * Task 19) and the <em>project</em> settings modal
 * ({@code JIPipeDesktopProjectSettingsUI}, removed in work item 3) — must be
 * gone, so nothing can reintroduce a second, dialog-based settings surface.
 * <p>
 * This is a structural existence gate, not a Swing interaction test (AGENTS.md):
 * it pins that the removed types stay removed.
 */
class SettingsNoModalUiTest {

    @Test
    void modalSettingsAndCustomizerUiAreRemoved() {
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("org.hkijena.jipipe.desktop.app.settings.JIPipeDesktopApplicationSettingsUI"));
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("org.hkijena.jipipe.desktop.app.customizer.JIPipeDesktopCustomizerDialog"));
    }

    @Test
    void projectSettingsModalIsRemoved() {
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("org.hkijena.jipipe.desktop.app.settings.JIPipeDesktopProjectSettingsUI"));
    }
}