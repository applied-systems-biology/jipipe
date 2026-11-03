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

import org.hkijena.jipipe.api.service.components.JIPipeSettingsServiceComponent;
import org.hkijena.jipipe.api.settings.SettingsBinding;
import org.hkijena.jipipe.api.settings.SettingsDocument;
import org.hkijena.jipipe.api.settings.SettingsScope;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringAndStringPairParameterList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Task 15 of the modern settings system: the <em>binding lifecycle</em> pin.
 * <p>
 * A {@link SettingsBinding} registers a path listener on its document in its
 * constructor and removes it in {@link SettingsBinding#close()}. The seam
 * {@link SettingsDocument#pathListenerCount(String)} counts exactly those
 * binding-owned listeners (the settings service's own per-location observers
 * are registered without an owner and are not counted), so the contract
 * pinned here is absolute: creating a binding adds exactly one listener to
 * the location, closing it removes exactly that one, closing twice removes
 * nothing, and reusing a binding for many writes adds nothing. A consumer
 * that binds per user action therefore fails here; so does a binding that
 * stopped releasing its listener.
 * <p>
 * No {@code JIPipe} service is booted: the component is constructed for
 * testing over a temporary profile.
 */
class SettingsBindingLifecycleTest {

    @Test
    void bindAddsExactlyOneListenerAndCloseReleasesIt(@TempDir Path userDir) {
        JIPipeSettingsServiceComponent settings = JIPipeSettingsServiceComponent.forTesting(userDir);
        try {
            SettingsDocument document = settings.document(SettingsScope.APPLICATION);
            String path = SettingsDocument.listenerPathForTesting(SettingsKeys.NODE_BROWSER_KINDS.blockPath(),
                    SettingsKeys.NODE_BROWSER_KINDS.subPath());
            String otherPath = SettingsDocument.listenerPathForTesting(SettingsKeys.PINNED_NODE_IDS.blockPath(),
                    SettingsKeys.PINNED_NODE_IDS.subPath());

            // pathListenerCount() counts only binding-owned listeners, so the
            // settings service's own observers never mask a leak.
            int baseline = document.pathListenerCount(path);
            int otherBaseline = document.pathListenerCount(otherPath);
            assertEquals(0, baseline, "no binding-owned listener before the first bind");

            SettingsBinding<List<String>> first = settings.bind(SettingsKeys.NODE_BROWSER_KINDS);
            assertEquals(baseline + 1, document.pathListenerCount(path),
                    "a binding registers exactly one path listener");
            assertEquals(otherBaseline, document.pathListenerCount(otherPath), "other keys stay untouched");

            SettingsBinding<List<String>> second = settings.bind(SettingsKeys.NODE_BROWSER_KINDS);
            assertEquals(baseline + 2, document.pathListenerCount(path),
                    "a second binding adds its own listener");

            first.close();
            assertEquals(baseline + 1, document.pathListenerCount(path),
                    "close() releases exactly its own listener");

            first.close();
            assertEquals(baseline + 1, document.pathListenerCount(path),
                    "close() is idempotent: a second close releases nothing more and steals no listener");

            second.close();
            assertEquals(baseline, document.pathListenerCount(path),
                    "the last close leaves no binding listener behind");

            // Binding after closing works again: the owner can keep a binding
            // for the life of its consumer and never accumulates.
            SettingsBinding<List<String>> third = settings.bind(SettingsKeys.NODE_BROWSER_KINDS);
            assertEquals(baseline + 1, document.pathListenerCount(path), "re-binding does not accumulate");
            third.close();
            assertEquals(baseline, document.pathListenerCount(path));
        } finally {
            settings.dispose();
        }
    }

    /**
     * The consumer pattern the UI uses for a <em>long-lived</em> editor: bind
     * once, use it any number of times, close it when the editor dies. Repeating
     * a write must not grow the listener count (this is the "Save layout"
     * repeated-click regression), and the editor's close must return the
     * document to its baseline.
     */
    @Test
    void oneBindingReusedForRepeatedWritesDoesNotGrowListeners(@TempDir Path userDir) {
        JIPipeSettingsServiceComponent settings = JIPipeSettingsServiceComponent.forTesting(userDir);
        try {
            SettingsDocument document = settings.document(SettingsScope.APPLICATION);
            String path = SettingsDocument.listenerPathForTesting(SettingsKeys.DOCK_LAYOUT_TEMPLATES.blockPath(),
                    SettingsKeys.DOCK_LAYOUT_TEMPLATES.subPath());
            int baseline = document.pathListenerCount(path);

            SettingsBinding<StringAndStringPairParameterList> binding =
                    settings.bind(SettingsKeys.DOCK_LAYOUT_TEMPLATES);
            try {
                for (int i = 0; i < 5; i++) {
                    binding.set(binding.get());
                }
                assertEquals(baseline + 1, document.pathListenerCount(path),
                        "repeated writes through one binding must not add listeners");
            } finally {
                binding.close();
            }
            assertEquals(baseline, document.pathListenerCount(path),
                    "closing the editor-equivalent binding releases it");
        } finally {
            settings.dispose();
        }
    }

    /**
     * The regression the ruling addresses, expressed against the same seam:
     * the per-call pattern (bind-and-drop, what the per-call consumers used to
     * do) grows the listener set linearly with the user action, while the
     * owner pattern does not. Proves the count is a genuine leak detector and
     * that the fix is behavioural, not cosmetic.
     */
    @Test
    void bindAndDropGrowsButAnOwnedBindingDoesNot(@TempDir Path userDir) {
        JIPipeSettingsServiceComponent settings = JIPipeSettingsServiceComponent.forTesting(userDir);
        try {
            SettingsDocument document = settings.document(SettingsScope.APPLICATION);
            String path = SettingsDocument.listenerPathForTesting(SettingsKeys.PINNED_NODE_IDS.blockPath(),
                    SettingsKeys.PINNED_NODE_IDS.subPath());
            int baseline = document.pathListenerCount(path);

            for (int i = 0; i < 3; i++) {
                settings.bind(SettingsKeys.PINNED_NODE_IDS).get();
            }
            assertEquals(baseline + 3, document.pathListenerCount(path),
                    "bind-and-drop leaks one listener per call — never do this per user action");

            SettingsBinding<List<String>> owned = settings.bind(SettingsKeys.PINNED_NODE_IDS);
            int ownedBaseline = document.pathListenerCount(path);
            for (int i = 0; i < 10; i++) {
                owned.get();
                owned.set(List.of("pin-" + i));
            }
            assertEquals(ownedBaseline, document.pathListenerCount(path),
                    "an owned binding used repeatedly adds nothing");
            owned.close();
        } finally {
            settings.dispose();
        }
    }
}