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

package org.hkijena.jipipe.api.shortcuts;

import javax.swing.KeyStroke;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for effective-binding resolution ({@link JIPipeShortcutRegistry#bindingFor}),
 * conflict detection ({@link JIPipeShortcutRegistry#findConflicts}), chord prefix
 * collection ({@link JIPipeShortcutRegistry#chordPrefixes}), and the bundled
 * presets in {@link JIPipeShortcutProfiles}.
 * <p>
 * The registry is an application singleton, so every test starts from a fresh
 * instance with the core catalog registered: {@link #setUp()}.
 */
class JIPipeShortcutResolveTest {
    /**
     * Resolves the effective binding of the given core action under the given
     * settings, without spelling the singleton out at every call site.
     */
    private JIPipeShortcutBinding binding(JIPipeShortcutAction action, KeymapSettings settings) {
        return JIPipeShortcutRegistry.getInstance().bindingFor(action.id(), settings);
    }

    @BeforeEach
    void setUp() {
        // The registry is an application singleton, and a couple of tests add
        // extra actions of their own (e.g. a dock action): start every test
        // from a fresh registry with only the core catalog.
        JIPipeShortcutRegistry.resetInstance();
        JIPipeShortcutActions.registerCore();
    }

    @Test void overrideBeatsDefault() {
        KeymapSettings settings = new KeymapSettings();
        // Default for move is the chord Ctrl+T then M; the override replaces it entirely.
        List<KeyStroke> override = List.of(KeyStroke.getKeyStroke("pressed F1"));
        settings.setOverride(JIPipeShortcutActions.TOOL_MOVE.id(), List.of(override));

        JIPipeShortcutBinding b = binding(JIPipeShortcutActions.TOOL_MOVE, settings);
        assertEquals(JIPipeShortcutBinding.Source.OVERRIDE, b.source());
        assertEquals(List.of(override), b.variants());

        // An override on one action must not affect another action's default.
        assertEquals(JIPipeShortcutBinding.Source.DEFAULT,
                binding(JIPipeShortcutActions.TOOL_CONNECT, settings).source());
        assertEquals(List.of(List.of(KeyStroke.getKeyStroke("control pressed T"),
                KeyStroke.getKeyStroke("pressed C"))),
                binding(JIPipeShortcutActions.TOOL_CONNECT, settings).variants());
    }

    @Test void unassignedWhenNoDefaultAndNoOverride() {
        JIPipeShortcutRegistry registry = JIPipeShortcutRegistry.getInstance();
        // A dock-style action: no default variants, and no override stored either.
        String dockId = "dock:org.hkijena.jipipe.test-dock-panel";
        JIPipeShortcutAction dock = JIPipeShortcutAction.builder(dockId, "Test dock panel", "Dock panels")
                .scopeFamily(JIPipeShortcutAction.FAMILY_DOCK)
                .textInputSafe(true)
                .build();
        registry.register(dock);

        JIPipeShortcutBinding b = binding(dock, new KeymapSettings());
        assertEquals(JIPipeShortcutBinding.Source.UNASSIGNED, b.source());
        assertTrue(b.isUnassigned());
        assertTrue(b.variants().isEmpty());

        // An override turns an unassigned action into a bound one.
        KeymapSettings withOverride = new KeymapSettings();
        withOverride.setOverride(dockId, List.of(List.of(KeyStroke.getKeyStroke("control pressed K"))));
        assertEquals(JIPipeShortcutBinding.Source.OVERRIDE,
                registry.bindingFor(dockId, withOverride).source());
        assertFalse(registry.bindingFor(dockId, withOverride).isUnassigned());
    }

    @Test void emptyOverrideFallsBackToDefault() {
        KeymapSettings settings = new KeymapSettings();
        // setOverride with an empty variant list removes the key, so an
        // action that is "overridden" to nothing falls back to its default.
        settings.setOverride(JIPipeShortcutActions.TOOL_MOVE.id(), List.of());
        JIPipeShortcutBinding b = binding(JIPipeShortcutActions.TOOL_MOVE, settings);
        assertEquals(JIPipeShortcutBinding.Source.DEFAULT, b.source());
        assertEquals(JIPipeShortcutActions.TOOL_MOVE.defaultVariants(), b.variants());
    }

    @Test void unknownActionIdResolvesToUnassigned() {
        JIPipeShortcutBinding b = JIPipeShortcutRegistry.getInstance()
                .bindingFor("org.hkijena.jipipe.not.registered", new KeymapSettings());
        assertEquals(JIPipeShortcutBinding.Source.UNASSIGNED, b.source());
        assertTrue(b.variants().isEmpty());
    }

    @Test void zoomHasTwoDefaultVariants() {
        JIPipeShortcutBinding b = JIPipeShortcutRegistry.getInstance()
                .bindingFor(JIPipeShortcutActions.EDITOR_ZOOM_IN.id(), new KeymapSettings());
        assertEquals(JIPipeShortcutBinding.Source.DEFAULT, b.source());
        assertEquals(2, b.variants().size());
        assertEquals(List.of(KeyStroke.getKeyStroke("control pressed EQUALS")), b.variants().get(0));
        assertEquals(List.of(KeyStroke.getKeyStroke("control pressed ADD")), b.variants().get(1));
    }

    @Test void chordPrefixesContainToolPrefix() {
        Set<List<KeyStroke>> prefixes = JIPipeShortcutRegistry.getInstance()
                .chordPrefixes(new KeymapSettings());
        assertTrue(prefixes.contains(List.of(KeyStroke.getKeyStroke("control pressed T"))),
                "Ctrl+T starts every graph tool chord, so it must be a prefix");
        // Single-stroke bindings are never chord prefixes: the legacy F-key
        // tools (F1) and the default ESC / Delete bindings must not contribute.
        assertFalse(prefixes.contains(List.of(KeyStroke.getKeyStroke("pressed F1"))),
                "a bare F-key is a single stroke, so it must not appear as a prefix");
        assertFalse(prefixes.contains(List.of(KeyStroke.getKeyStroke("pressed ESCAPE"))),
                "a single ESCAPE stroke is not a chord prefix");
    }

    @Test void conflictSameFamilyOnly() {
        KeymapSettings settings = new KeymapSettings();
        // graph.node.copy and graph.edge.copy share the Ctrl+C default, in the same (graph) family.
        List<String> conflicts = JIPipeShortcutRegistry.getInstance()
                .findConflicts(JIPipeShortcutActions.NODE_COPY.id(),
                        List.of(List.of(KeyStroke.getKeyStroke("control pressed C"))), settings);
        assertTrue(conflicts.contains(JIPipeShortcutActions.EDGE_COPY.id()),
                "copy node and copy edge share Ctrl+C in the graph family: " + conflicts);
        assertFalse(conflicts.contains(JIPipeShortcutActions.NODE_COPY.id()),
                "an action never conflicts with itself");

        // project.new is in the project family: Ctrl+C must not conflict with it.
        List<String> projectConflicts = JIPipeShortcutRegistry.getInstance()
                .findConflicts(JIPipeShortcutActions.PROJECT_NEW.id(),
                        List.of(List.of(KeyStroke.getKeyStroke("control pressed C"))), settings);
        assertTrue(projectConflicts.isEmpty(), "cross-family candidates must not conflict: " + projectConflicts);

        // A candidate that no one uses conflicts with nothing.
        assertTrue(JIPipeShortcutRegistry.getInstance()
                .findConflicts(JIPipeShortcutActions.NODE_COPY.id(),
                        List.of(List.of(KeyStroke.getKeyStroke("control pressed Y"))), settings).isEmpty());

        // null/empty candidates conflict with nothing.
        assertTrue(JIPipeShortcutRegistry.getInstance()
                .findConflicts(JIPipeShortcutActions.NODE_COPY.id(), null, settings).isEmpty());
        assertTrue(JIPipeShortcutRegistry.getInstance()
                .findConflicts(JIPipeShortcutActions.NODE_COPY.id(), List.of(), settings).isEmpty());
    }

    @Test void conflictUsesEffectiveBindings() {
        KeymapSettings settings = new KeymapSettings();
        // graph.node.paste is Ctrl+V by default; remap it away and Ctrl+V must not conflict anymore.
        settings.setOverride(JIPipeShortcutActions.NODE_PASTE.id(),
                List.of(List.of(KeyStroke.getKeyStroke("pressed V"))));
        List<String> conflicts = JIPipeShortcutRegistry.getInstance()
                .findConflicts(JIPipeShortcutActions.NODE_COPY.id(),
                        List.of(List.of(KeyStroke.getKeyStroke("control pressed V"))), settings);
        assertTrue(conflicts.isEmpty(), "the effective (overridden) binding of paste no longer holds Ctrl+V: " + conflicts);
    }

    @Test void conflictDetectsChordCollisions() {
        KeymapSettings settings = new KeymapSettings();
        // A candidate chord that equals an existing 2-stroke binding must conflict with it.
        List<String> conflicts = JIPipeShortcutRegistry.getInstance()
                .findConflicts(JIPipeShortcutActions.TOOL_CONNECT.id(),
                        List.of(List.of(KeyStroke.getKeyStroke("control pressed T"),
                                KeyStroke.getKeyStroke("pressed R"))), settings);
        assertTrue(conflicts.contains(JIPipeShortcutActions.TOOL_REWIRE.id()),
                "Ctrl+T then R is the default rewire chord: " + conflicts);
        // A chord with the same first stroke but a different second stroke does not conflict.
        assertTrue(JIPipeShortcutRegistry.getInstance()
                .findConflicts(JIPipeShortcutActions.TOOL_CONNECT.id(),
                        List.of(List.of(KeyStroke.getKeyStroke("control pressed T"),
                                KeyStroke.getKeyStroke("pressed X"))), settings).isEmpty());
    }

    @Test void legacyPresetRestoresOldBindings() {
        KeymapSettings s = new KeymapSettings();
        JIPipeShortcutProfiles.apply(JIPipeShortcutProfiles.LEGACY, s);

        assertEquals(List.of(List.of(KeyStroke.getKeyStroke("pressed F1"))),
                binding(JIPipeShortcutActions.TOOL_MOVE, s).variants());
        assertEquals(List.of(List.of(KeyStroke.getKeyStroke("pressed F2"))),
                binding(JIPipeShortcutActions.TOOL_CONNECT, s).variants());
        assertEquals(List.of(List.of(KeyStroke.getKeyStroke("pressed F3"))),
                binding(JIPipeShortcutActions.TOOL_REWIRE, s).variants());
        assertEquals(List.of(List.of(KeyStroke.getKeyStroke("pressed F12"))),
                binding(JIPipeShortcutActions.TOOL_CROP_VIEW, s).variants());
        // The old numpad-only zoom (no Ctrl+= / Ctrl+- / Ctrl+0 variants).
        assertEquals(List.of(List.of(KeyStroke.getKeyStroke("control pressed ADD"))),
                binding(JIPipeShortcutActions.EDITOR_ZOOM_IN, s).variants());
        assertEquals(List.of(List.of(KeyStroke.getKeyStroke("control pressed SUBTRACT"))),
                binding(JIPipeShortcutActions.EDITOR_ZOOM_OUT, s).variants());
        assertEquals(List.of(List.of(KeyStroke.getKeyStroke("control pressed NUMPAD0"))),
                binding(JIPipeShortcutActions.EDITOR_ZOOM_RESET, s).variants());
        // The old lock/unlock pair: the weaker action held the bare key.
        assertEquals(List.of(List.of(KeyStroke.getKeyStroke("pressed L"))),
                binding(JIPipeShortcutActions.NODE_LOCK, s).variants());
        assertEquals(List.of(List.of(KeyStroke.getKeyStroke("control pressed L"))),
                binding(JIPipeShortcutActions.NODE_UNLOCK, s).variants());
        // The old pass-through pair: Ctrl+P disabled, Ctrl+Shift+P enabled.
        assertEquals(List.of(List.of(KeyStroke.getKeyStroke("control pressed P"))),
                binding(JIPipeShortcutActions.PASS_THROUGH_DISABLE, s).variants());
        assertEquals(List.of(List.of(KeyStroke.getKeyStroke("control shift pressed P"))),
                binding(JIPipeShortcutActions.PASS_THROUGH_ENABLE, s).variants());
        // The canvas-notes tool actions are registered by the plugin (Task 7),
        // so bindingFor cannot resolve them here; assert their LEGACY overrides
        // are stored under the correct stable ids with the correct F-keys/ESC.
        Map<String, List<List<String>>> stored = s.getOverrides();
        assertEquals(List.of(List.of("pressed F4")),
                stored.get("org.hkijena.jipipe.canvasnotes.tool.textbox"));
        assertEquals(List.of(List.of("pressed F6")),
                stored.get("org.hkijena.jipipe.canvasnotes.tool.arrow"));
        assertEquals(List.of(List.of("pressed F7")),
                stored.get("org.hkijena.jipipe.canvasnotes.tool.imagebox"));
        assertEquals(List.of(List.of("pressed ESCAPE")),
                stored.get("org.hkijena.jipipe.canvasnotes.tool.notes-edit"));

        // Actions whose default did not change have no legacy override.
        assertEquals(JIPipeShortcutBinding.Source.DEFAULT,
                binding(JIPipeShortcutActions.EDITOR_UNDO, s).source());
        assertEquals(JIPipeShortcutBinding.Source.DEFAULT,
                binding(JIPipeShortcutActions.TOOL_DEFAULT, s).source());
        assertEquals(JIPipeShortcutBinding.Source.OVERRIDE,
                binding(JIPipeShortcutActions.TOOL_MOVE, s).source());
        assertFalse(stored.containsKey(JIPipeShortcutActions.EDITOR_UNDO.id()),
                "undo did not change, so it must not carry a legacy override");
    }

    @Test void defaultPresetClearsOverrides() {
        KeymapSettings s = new KeymapSettings();
        s.setOverride(JIPipeShortcutActions.TOOL_MOVE.id(),
                List.of(List.of(KeyStroke.getKeyStroke("pressed F1"))));
        JIPipeShortcutProfiles.apply(JIPipeShortcutProfiles.DEFAULT, s);
        assertTrue(s.getOverrides().isEmpty(), "applying Default must clear every override");
        assertEquals(JIPipeShortcutBinding.Source.DEFAULT,
                binding(JIPipeShortcutActions.TOOL_MOVE, s).source());
    }

    @Test void applyIsIdempotent() {
        KeymapSettings s1 = new KeymapSettings();
        JIPipeShortcutProfiles.apply(JIPipeShortcutProfiles.LEGACY, s1);
        JIPipeShortcutProfiles.apply(JIPipeShortcutProfiles.LEGACY, s1);

        KeymapSettings s2 = new KeymapSettings();
        JIPipeShortcutProfiles.apply(JIPipeShortcutProfiles.LEGACY, s2);
        assertEquals(s2.getOverrides(), s1.getOverrides(), "applying LEGACY twice must not change the map");
        // And the effective bindings are identical after a second application.
        assertEquals(binding(JIPipeShortcutActions.TOOL_MOVE, s2).variants(),
                binding(JIPipeShortcutActions.TOOL_MOVE, s1).variants());
    }

    @Test void bundledListsBothPresetsInOrder() {
        assertEquals(List.of("default", "legacy"),
                JIPipeShortcutProfiles.bundled().stream().map(JIPipeShortcutProfile::id).toList());
        assertEquals("Default", JIPipeShortcutProfiles.DEFAULT.name());
        assertEquals("Legacy (pre-6.0)", JIPipeShortcutProfiles.LEGACY.name());
    }
}
