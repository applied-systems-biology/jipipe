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

import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutAction;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutRegistry;
import org.hkijena.jipipe.api.shortcuts.KeymapSettings;
import org.hkijena.jipipe.desktop.app.commandbar.JIPipeDesktopCommandBarModel.Entry;
import org.hkijena.jipipe.desktop.app.commandbar.JIPipeDesktopCommandBarModel.ListMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the pure command bar model of
 * {@link JIPipeDesktopCommandBarModel}: category grouping without
 * re-sorting, mode application (grayed / available-only / everything),
 * effective-binding rendering, and the chord-prefix filter.
 * <p>
 * No dispatcher installation, no UI: the resolver is a stub
 * {@code Map<String, Runnable>} and the stub actions are registered in a
 * fresh registry singleton, so {@code bindingFor} resolves against exactly
 * the actions of this test. Headless-safe.
 */
class JIPipeDesktopCommandBarModelTest {
    private static final String ID_A1 = "org.hkijena.jipipe.command-bar.test.a1";
    private static final String ID_A2 = "org.hkijena.jipipe.command-bar.test.a2";
    private static final String ID_A3 = "org.hkijena.jipipe.command-bar.test.a3";
    private static final String ID_A4 = "org.hkijena.jipipe.command-bar.test.a4";
    private static final String ID_B1 = "org.hkijena.jipipe.command-bar.test.b1";

    private static final KeyStroke CTRL_T = KeyStroke.getKeyStroke("control pressed T");
    private static final KeyStroke CTRL_S = KeyStroke.getKeyStroke("control pressed S");
    private static final KeyStroke CTRL_U = KeyStroke.getKeyStroke("control pressed U");
    private static final KeyStroke CTRL_1 = KeyStroke.getKeyStroke("control pressed 1");
    private static final KeyStroke KEY_M = KeyStroke.getKeyStroke("pressed M");
    private static final KeyStroke KEY_N = KeyStroke.getKeyStroke("pressed N");
    private static final KeyStroke F1 = KeyStroke.getKeyStroke("pressed F1");

    @BeforeEach
    void setUp() {
        // collect() resolves bindings through the singleton registry: start
        // every test from a fresh registry and register only the stubs.
        JIPipeShortcutRegistry.resetInstance();
    }

    @AfterEach
    void tearDown() {
        JIPipeShortcutRegistry.resetInstance();
    }

    /**
     * Builds a stub action; no strokes means no default binding. (Not
     * {@code defaultChord()} with zero varargs: that would store one
     * zero-length variant, i.e. a <em>bound</em> action.)
     */
    private static JIPipeShortcutAction action(String id, String name, String category, KeyStroke... chord) {
        JIPipeShortcutAction.Builder builder = JIPipeShortcutAction.builder(id, name, category);
        if (chord.length > 0) {
            builder.defaultChord(chord);
        }
        return builder.build();
    }

    /**
     * Registers a stub in the fresh singleton registry and returns it.
     */
    private static JIPipeShortcutAction register(String id, String name, String category, KeyStroke... chord) {
        JIPipeShortcutAction action = action(id, name, category, chord);
        JIPipeShortcutRegistry.getInstance().register(action);
        return action;
    }

    /**
     * A stub resolver: the given id maps to the given runnable, everything
     * else to null.
     */
    private static Function<String, Runnable> resolves(String id, Runnable r) {
        Map<String, Runnable> map = Map.of(id, r);
        return map::get;
    }

    // ---------- collect: ordering ----------

    @Test void collectOrdersCategoriesAndAppendsEphemeral() {
        // registry.all() order is category then name: Alpha (A one, A two),
        // then Beta (B one).
        register(ID_A1, "A one", "Alpha", CTRL_T, KEY_M);
        register(ID_A2, "A two", "Alpha", CTRL_1);
        register(ID_B1, "B one", "Beta");
        JIPipeShortcutRegistry registry = JIPipeShortcutRegistry.getInstance();

        Entry ephemeral = Entry.ephemeral("Switch to Tab 1", "Tabs", null, () -> {
        });
        List<Entry> entries = JIPipeDesktopCommandBarModel.collect(registry.all(), new KeymapSettings(),
                ListMode.GRAYED, id -> null, List.of(ephemeral));

        assertEquals(List.of("Alpha", "A one", "A two", "Beta", "B one", "Switch to Tab 1"),
                entries.stream().map(Entry::name).toList(),
                "category headers precede their group in registry order, ephemeral entries last");
        assertEquals(List.of("Alpha", "Alpha", "Alpha", "Beta", "Beta", "Tabs"),
                entries.stream().map(Entry::category).toList());
        assertTrue(entries.get(0).isCategoryHeader());
        assertTrue(entries.get(3).isCategoryHeader());
        assertEquals("A one", entries.get(1).name());
        assertEquals("A two", entries.get(2).name());
        assertEquals("B one", entries.get(4).name());
        assertSame(ephemeral, entries.get(5), "ephemeral entries are appended in the given order");
        assertFalse(entries.get(5).isCategoryHeader(), "no extra header is emitted for an ephemeral category");
    }

    @Test void collectKeepsGivenOrderWithoutResorting() {
        // Pass the actions in a deliberate non-registry order (Beta first):
        // collect must emit the groups in the given order, not re-sort them.
        register(ID_A1, "A one", "Alpha", CTRL_T, KEY_M);
        register(ID_B1, "B one", "Beta");
        JIPipeShortcutRegistry registry = JIPipeShortcutRegistry.getInstance();
        List<JIPipeShortcutAction> givenOrder = List.of(
                registry.byId(ID_B1), registry.byId(ID_A1));

        List<Entry> entries = JIPipeDesktopCommandBarModel.collect(givenOrder, new KeymapSettings(),
                ListMode.GRAYED, id -> null, List.of());

        assertEquals(List.of("Beta", "B one", "Alpha", "A one"),
                entries.stream().map(Entry::name).toList());
        assertTrue(entries.get(0).isCategoryHeader());
        assertTrue(entries.get(2).isCategoryHeader());
    }

    // ---------- collect: mode application ----------

    @Test void grayedModeKeepsUnavailableWithoutCommand() {
        register(ID_A1, "A one", "Alpha", CTRL_T, KEY_M);
        register(ID_A2, "A two", "Alpha", CTRL_1);
        register(ID_B1, "B one", "Beta");
        JIPipeShortcutRegistry registry = JIPipeShortcutRegistry.getInstance();

        Runnable a1Run = () -> {
        };
        List<Entry> entries = JIPipeDesktopCommandBarModel.collect(registry.all(), new KeymapSettings(),
                ListMode.GRAYED, resolves(ID_A1, a1Run), List.of());

        Entry a1 = entries.get(1);
        assertTrue(a1.isAvailable());
        assertSame(a1Run, a1.command(), "an available entry carries the resolver's runnable");
        assertEquals(ID_A1, a1.actionId());
        assertTrue(a1.isConfigurable(), "registry commands are bindable");
        assertFalse(a1.isCategoryHeader());

        Entry a2 = entries.get(2);
        assertFalse(a2.isAvailable(), "GRAYED keeps the unavailable entry");
        assertNull(a2.command(), "...but its command is null");
        assertEquals(ID_A2, a2.actionId());
        assertTrue(a2.isConfigurable());
        assertEquals("Ctrl+1", a2.bindingText(), "the binding is still rendered for a grayed entry");

        Entry b1 = entries.get(4);
        assertFalse(b1.isAvailable());
        assertNull(b1.command());
    }

    @Test void availableOnlyDropsUnavailableAndEmptyHeaders() {
        register(ID_A1, "A one", "Alpha", CTRL_T, KEY_M);
        register(ID_A2, "A two", "Alpha", CTRL_1);
        register(ID_B1, "B one", "Beta");
        JIPipeShortcutRegistry registry = JIPipeShortcutRegistry.getInstance();

        Runnable a1Run = () -> {
        };
        List<Entry> entries = JIPipeDesktopCommandBarModel.collect(registry.all(), new KeymapSettings(),
                ListMode.AVAILABLE_ONLY, resolves(ID_A1, a1Run), List.of());

        assertEquals(List.of("Alpha", "A one"),
                entries.stream().map(Entry::name).toList(),
                "unavailable entries are dropped, and a header with no entries left is dropped too");
        assertTrue(entries.get(0).isCategoryHeader());
        assertSame(a1Run, entries.get(1).command());

        // A group whose every entry is unavailable vanishes entirely.
        List<Entry> none = JIPipeDesktopCommandBarModel.collect(registry.all(), new KeymapSettings(),
                ListMode.AVAILABLE_ONLY, id -> null, List.of());
        assertTrue(none.isEmpty());
    }

    @Test void everythingModeBehavesLikeGrayed() {
        // EVERYTHING differs from GRAYED only in what the caller passes as
        // actions and resolver (the window-kind filter); the model output
        // for the same input must be identical.
        register(ID_A1, "A one", "Alpha", CTRL_T, KEY_M);
        register(ID_A2, "A two", "Alpha", CTRL_1);
        register(ID_B1, "B one", "Beta");
        JIPipeShortcutRegistry registry = JIPipeShortcutRegistry.getInstance();

        Runnable a1Run = () -> {
        };
        List<Entry> grayed = JIPipeDesktopCommandBarModel.collect(registry.all(), new KeymapSettings(),
                ListMode.GRAYED, resolves(ID_A1, a1Run), List.of());
        List<Entry> everything = JIPipeDesktopCommandBarModel.collect(registry.all(), new KeymapSettings(),
                ListMode.EVERYTHING, resolves(ID_A1, a1Run), List.of());

        assertEquals(grayed.size(), everything.size());
        for (int i = 0; i < grayed.size(); i++) {
            assertEquals(grayed.get(i).name(), everything.get(i).name());
            assertEquals(grayed.get(i).category(), everything.get(i).category());
            assertEquals(grayed.get(i).isCategoryHeader(), everything.get(i).isCategoryHeader());
            assertEquals(grayed.get(i).isAvailable(), everything.get(i).isAvailable());
            assertEquals(grayed.get(i).bindingText(), everything.get(i).bindingText());
            assertEquals(grayed.get(i).actionId(), everything.get(i).actionId());
            assertEquals(grayed.get(i).variants(), everything.get(i).variants());
            assertSame(grayed.get(i).command(), everything.get(i).command());
        }
        assertFalse(everything.get(2).isAvailable());
        assertNull(everything.get(2).command(), "unavailable entries stay listed, grayed, without a command");
    }

    // ---------- collectRecent ----------

    @Test void collectRecentEmitsGivenOrderWithNoHeaders() {
        // The "Recently used" rows: the given (most-recent-first) order is
        // emitted verbatim, as plain registry rows (no headers), each
        // resolved like a collect row.
        JIPipeShortcutAction a1 = register(ID_A1, "A one", "Alpha", CTRL_T, KEY_M);
        JIPipeShortcutAction b1 = register(ID_B1, "B one", "Beta");
        JIPipeShortcutRegistry registry = JIPipeShortcutRegistry.getInstance();

        Runnable a1Run = () -> {
        };
        // Deliberate non-registry order (B one first).
        List<JIPipeShortcutAction> recentOrder = List.of(b1, a1);
        List<Entry> entries = JIPipeDesktopCommandBarModel.collectRecent(
                recentOrder, new KeymapSettings(), ListMode.GRAYED, resolves(ID_A1, a1Run));

        assertEquals(List.of(ID_B1, ID_A1), entries.stream().map(Entry::actionId).toList(),
                "the given most-recent-first order is emitted, never re-sorted");
        assertTrue(entries.stream().noneMatch(Entry::isCategoryHeader),
                "no category headers are emitted: the bar prepends its own");
        assertFalse(entries.get(0).isAvailable(), "an unavailable recent is kept in GRAYED mode");
        assertNull(entries.get(0).command());
        assertEquals("Ctrl+T then M", entries.get(1).bindingText());
        assertSame(a1Run, entries.get(1).command());
        assertTrue(entries.get(1).isConfigurable());
    }

    @Test void collectRecentDropsUnavailableInAvailableOnly() {
        register(ID_A1, "A one", "Alpha", CTRL_T, KEY_M);
        register(ID_B1, "B one", "Beta");
        JIPipeShortcutRegistry registry = JIPipeShortcutRegistry.getInstance();
        JIPipeShortcutAction b1 = registry.byId(ID_B1);

        // Only the first recent resolves: AVAILABLE_ONLY drops the second.
        List<Entry> entries = JIPipeDesktopCommandBarModel.collectRecent(
                List.of(registry.byId(ID_A1), b1), new KeymapSettings(),
                ListMode.AVAILABLE_ONLY, id -> ID_A1.equals(id) ? () -> {
                } : null);
        assertEquals(List.of(ID_A1), entries.stream().map(Entry::actionId).toList());

        // Nothing resolves: empty result (the bar then omits the section).
        List<Entry> none = JIPipeDesktopCommandBarModel.collectRecent(
                List.of(b1), new KeymapSettings(), ListMode.AVAILABLE_ONLY, id -> null);
        assertTrue(none.isEmpty());
    }

    // ---------- collect: binding rendering ----------

    @Test void registryEntryRendersEffectiveBinding() {
        register(ID_A1, "A one", "Alpha", CTRL_T, KEY_M);
        register(ID_B1, "B one", "Beta");
        JIPipeShortcutRegistry registry = JIPipeShortcutRegistry.getInstance();

        // Default variants render as a chord.
        List<Entry> entries = JIPipeDesktopCommandBarModel.collect(registry.all(), new KeymapSettings(),
                ListMode.GRAYED, id -> null, List.of());
        Entry a1 = entries.get(1);
        assertEquals("Ctrl+T then M", a1.bindingText());
        assertEquals(List.of(List.of(CTRL_T, KEY_M)), a1.variants());

        // A user override wins over the default.
        KeymapSettings overridden = new KeymapSettings();
        overridden.setOverride(ID_A1, List.of(List.of(F1)));
        Entry a1Overridden = JIPipeDesktopCommandBarModel.collect(registry.all(), overridden,
                ListMode.GRAYED, id -> null, List.of()).get(1);
        assertEquals("F1", a1Overridden.bindingText());
        assertEquals(List.of(List.of(F1)), a1Overridden.variants());

        // An unassigned action renders an empty binding and has no variants.
        Entry b1 = entries.get(3);
        assertEquals("", b1.bindingText());
        assertTrue(b1.variants().isEmpty());
    }

    // ---------- chordEntries ----------

    @Test void chordEntriesFilterByPrefixAndAvailability() {
        // Two actions share the Ctrl+T prefix, one available and one not; a
        // third has a different prefix (Ctrl+S); a fourth is bound to a
        // single stroke only (F1) and must never match a chord prefix. All
        // in one category to keep the layout flat.
        JIPipeShortcutAction a1 = register(ID_A1, "A one", "Chord", CTRL_T, KEY_M);
        JIPipeShortcutAction a2 = register(ID_A2, "A two", "Chord", CTRL_T, KEY_N);
        JIPipeShortcutAction a3 = register(ID_A3, "A three", "Chord", CTRL_S, KEY_M);
        JIPipeShortcutAction single = register(ID_B1, "B one", "Chord", F1);
        JIPipeShortcutRegistry registry = JIPipeShortcutRegistry.getInstance();

        List<Entry> entries = JIPipeDesktopCommandBarModel.collect(registry.all(), new KeymapSettings(),
                ListMode.GRAYED, id -> ID_A2.equals(id) ? null : () -> {
                }, List.of());

        List<Entry> ctrlT = JIPipeDesktopCommandBarModel.chordEntries(entries, CTRL_T);
        assertEquals(List.of(a1.id(), a2.id()),
                ctrlT.stream().map(Entry::actionId).toList(),
                "both Ctrl+T chords match; the available one comes first, the different prefix and the single-stroke entry are excluded");
        assertTrue(ctrlT.get(0).isAvailable());
        assertFalse(ctrlT.get(1).isAvailable());

        assertEquals(List.of(a3.id()),
                JIPipeDesktopCommandBarModel.chordEntries(entries, CTRL_S).stream().map(Entry::actionId).toList());
        assertTrue(JIPipeDesktopCommandBarModel.chordEntries(entries, CTRL_U).isEmpty(),
                "a stroke nobody holds as a chord prefix matches nothing");
        assertTrue(JIPipeDesktopCommandBarModel.chordEntries(entries, F1).isEmpty(),
                "a single-stroke variant is never a chord candidate");
    }

    @Test void chordEntriesStablePartitionAvailableFirst() {
        // Four actions share the Ctrl+T prefix; availability alternates.
        // registry.all() orders by name, and "A four" < "A one" < "A three"
        // < "A two", so the collect output order is a4 (unavailable),
        // a1 (available), a3 (available), a2 (unavailable). The result is a
        // stable partition: available first, original order inside each
        // bucket.
        JIPipeShortcutAction a1 = register(ID_A1, "A one", "Chord", CTRL_T, KEY_M);
        JIPipeShortcutAction a2 = register(ID_A2, "A two", "Chord", CTRL_T, KEY_N);
        JIPipeShortcutAction a3 = register(ID_A3, "A three", "Chord", CTRL_T, KEY_M);
        JIPipeShortcutAction a4 = register(ID_A4, "A four", "Chord", CTRL_T, KEY_N);
        JIPipeShortcutRegistry registry = JIPipeShortcutRegistry.getInstance();

        List<Entry> entries = JIPipeDesktopCommandBarModel.collect(registry.all(), new KeymapSettings(),
                ListMode.GRAYED, id -> ID_A1.equals(id) || ID_A3.equals(id) ? () -> {
                } : null, List.of());

        List<Entry> result = JIPipeDesktopCommandBarModel.chordEntries(entries, CTRL_T);
        assertEquals(List.of(a1.id(), a3.id(), a4.id(), a2.id()),
                result.stream().map(Entry::actionId).toList(),
                "registry order is preserved within each availability bucket");
    }

    // ---------- ListMode.parse ----------

    @Test void parseToleratesUnknownAndNull() {
        assertEquals(ListMode.GRAYED, ListMode.parse(null));
        assertEquals(ListMode.GRAYED, ListMode.parse("nonsense"));
        assertEquals(ListMode.GRAYED, ListMode.parse(""));
        assertEquals(ListMode.GRAYED, ListMode.parse("grayed"));
        assertEquals(ListMode.AVAILABLE_ONLY, ListMode.parse("available-only"));
        assertEquals(ListMode.EVERYTHING, ListMode.parse("everything"));
        // Every id round-trips through parse.
        for (ListMode mode : ListMode.values()) {
            assertEquals(mode, ListMode.parse(mode.id()));
        }
        assertEquals("grayed", ListMode.GRAYED.id());
        assertEquals("available-only", ListMode.AVAILABLE_ONLY.id());
        assertEquals("everything", ListMode.EVERYTHING.id());
    }

    // ---------- Entry factories ----------

    @Test void categoryHeaderContract() {
        Entry header = Entry.categoryHeader("Selection");
        assertTrue(header.isCategoryHeader());
        assertEquals("Selection", header.category());
        assertEquals("Selection", header.name());
        assertNull(header.icon());
        assertNull(header.bindingText());
        assertTrue(header.variants().isEmpty());
        assertFalse(header.isAvailable());
        assertFalse(header.isConfigurable());
        assertNull(header.actionId());
        assertNull(header.command());
    }

    @Test void ephemeralEntryContract() {
        Runnable run = () -> {
        };
        Icon icon = new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
            }

            @Override
            public int getIconWidth() {
                return 16;
            }

            @Override
            public int getIconHeight() {
                return 16;
            }
        };
        Entry e = Entry.ephemeral("Switch to Tab 1", "Tabs", icon, run);
        assertEquals("Switch to Tab 1", e.name());
        assertEquals("Tabs", e.category());
        assertSame(icon, e.icon());
        assertSame(run, e.command(), "ephemeral entries are always available with their command");
        assertTrue(e.isAvailable());
        assertFalse(e.isConfigurable(), "ephemeral entries are never bindable");
        assertNull(e.actionId());
        assertNull(e.bindingText());
        assertTrue(e.variants().isEmpty());
        assertFalse(e.isCategoryHeader());
    }
}
