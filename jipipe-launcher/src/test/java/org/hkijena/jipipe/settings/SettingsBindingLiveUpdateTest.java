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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.settings.SettingsBinding;
import org.hkijena.jipipe.plugins.settings.SettingsKeys;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Task 15 of the modern settings system: two independent views bound to the
 * same typed setting key must stay in sync. A write through one binding has
 * to notify the other — this is the desync fix the keyified registry
 * consumers exist for.
 * <p>
 * Lives in <b>jipipe-launcher</b> because it boots the real service
 * ({@link JIPipe#ensureInstance()}) and therefore runs against the
 * developer's real profile: the original value is restored in a
 * {@code finally} block, so the test never leaves the profile mutated.
 */
class SettingsBindingLiveUpdateTest {

    @BeforeAll
    static void init() {
        // The ImageJLegacyPatcherLauncherSessionListener registered in this
        // module preinits the ImageJ1 legacy patcher before any test class
        // loads, so ensureInstance() here is safe. Idempotent.
        JIPipe.ensureInstance();
    }

    @Test
    void togglingOneGraphEditorsSettingUpdatesAnotherView() {
        SettingsBinding<Boolean> a = JIPipe.getSettingsService().bind(SettingsKeys.SHOW_DOCK_TOOLBAR_LABELS);
        SettingsBinding<Boolean> b = JIPipe.getSettingsService().bind(SettingsKeys.SHOW_DOCK_TOOLBAR_LABELS);
        boolean original = a.get();
        try {
            List<Boolean> seen = new ArrayList<>();
            b.subscribe(seen::add);
            a.set(!original);
            assertNotEquals(original, b.get(), "the write must be visible to the second view");
            assertEquals(List.of(b.get()), seen, "a second view of the same setting must be notified");
        } finally {
            a.set(original);
        }
    }

    /**
     * Echo guard: a consumer that applies the document value to its widget
     * and then writes the widget's value back must not notify anybody, so
     * two views cannot ping-pong. Writing the current value is a no-op that
     * writes nothing to the profile.
     */
    @Test
    void writingTheCurrentValueBackFiresNothing() {
        SettingsBinding<Boolean> a = JIPipe.getSettingsService().bind(SettingsKeys.SHOW_DOCK_TOOLBAR_LABELS);
        SettingsBinding<Boolean> b = JIPipe.getSettingsService().bind(SettingsKeys.SHOW_DOCK_TOOLBAR_LABELS);
        List<Boolean> seen = new ArrayList<>();
        b.subscribe(seen::add);
        a.set(a.get());
        assertTrue(seen.isEmpty(), "setting the current value must not notify subscribers");
    }
}