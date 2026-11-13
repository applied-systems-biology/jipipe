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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JIPipeShortcutRegistryTest {
    private JIPipeShortcutAction action(String id, String name, String category) {
        return JIPipeShortcutAction.builder(id, name, category).build();
    }

    @Test void registerAndLookup() {
        JIPipeShortcutRegistry r = new JIPipeShortcutRegistry();   // package-private ctor for tests; getInstance() wraps a singleton
        JIPipeShortcutAction a = JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.node.duplicate", "Duplicate", "Selection")
                .scopeFamily(JIPipeShortcutAction.FAMILY_GRAPH).defaultChord(KeyStroke.getKeyStroke("control pressed D")).build();
        r.register(a);
        assertSame(a, r.byId("org.hkijena.jipipe.graph.node.duplicate"));
        assertTrue(r.isRegistered("org.hkijena.jipipe.graph.node.duplicate"));
        assertFalse(r.isRegistered("org.hkijena.jipipe.graph.node.nope"));
    }

    @Test void duplicateIdFailsFast() {
        JIPipeShortcutRegistry r = new JIPipeShortcutRegistry();
        JIPipeShortcutAction a = action("org.hkijena.jipipe.duplicate.id", "First", "Cat");
        JIPipeShortcutAction b = action("org.hkijena.jipipe.duplicate.id", "Second", "Cat");
        r.register(a);
        assertThrows(IllegalStateException.class, () -> r.register(b));
        assertSame(a, r.byId("org.hkijena.jipipe.duplicate.id"),
                "a duplicate registration must not replace the first action");
    }

    @Test void allIsSortedByCategoryThenName() {
        JIPipeShortcutRegistry r = new JIPipeShortcutRegistry();
        JIPipeShortcutAction b = action("id.b", "B", "cat2");
        JIPipeShortcutAction a = action("id.a", "A", "cat1");
        JIPipeShortcutAction c = action("id.c", "C", "cat1");
        r.register(b);
        r.register(a);
        r.register(c);
        assertEquals(List.of("A", "C", "B"), r.all().stream().map(JIPipeShortcutAction::name).toList());
        assertEquals(List.of("A", "C"), r.byCategory("cat1").stream().map(JIPipeShortcutAction::name).toList());
        assertTrue(r.byCategory("nope").isEmpty());
    }

    @Test void changeListenerFiresOnRegister() {
        JIPipeShortcutRegistry r = new JIPipeShortcutRegistry();
        AtomicInteger count = new AtomicInteger();
        Runnable listener = count::incrementAndGet;
        r.addChangeListener(listener);
        r.register(action("id.listener", "Listener", "Cat"));
        assertEquals(1, count.get());
        r.removeChangeListener(listener);
        r.register(action("id.listener2", "Listener 2", "Cat"));
        assertEquals(1, count.get(), "a removed listener must not be notified");
    }

    @Test void getInstanceIsSingleton() {
        assertSame(JIPipeShortcutRegistry.getInstance(), JIPipeShortcutRegistry.getInstance());
    }

    @Test void formatChordRendersChordAndEmpty() {
        // The single-stroke rendering comes from UIUtils.keyStrokeToString(); the
        // letter keys used here render identically in every locale.
        assertEquals("Ctrl+T then M", JIPipeShortcutRegistry.formatChord(List.of(
                KeyStroke.getKeyStroke("control pressed T"), KeyStroke.getKeyStroke("pressed M"))));
        assertEquals("-", JIPipeShortcutRegistry.formatChord(List.of()));
        // Pinned to the real keyStrokeToString output: symbol keys render via
        // KeyEvent.getKeyText() and are locale-dependent (VK_EQUALS/VK_ADD differ
        // between e.g. de_DE and en), so the variant case uses locale-stable
        // letter keys.
        assertEquals("Ctrl+A or Ctrl+B", JIPipeShortcutRegistry.formatVariants(List.of(
                List.of(KeyStroke.getKeyStroke("control pressed A")),
                List.of(KeyStroke.getKeyStroke("control pressed B")))));
    }

    @Test void actionBuilderDefaultsAndEquality() {
        JIPipeShortcutAction a = action("id.x", "X", "Cat");
        assertEquals("id.x", a.id());
        assertEquals("X", a.name());
        assertEquals("Cat", a.category());
        assertEquals("", a.description());
        assertEquals(JIPipeShortcutAction.FAMILY_GRAPH, a.scopeFamily());
        assertFalse(a.isTextInputSafe());
        assertTrue(a.defaultVariants().isEmpty());

        JIPipeShortcutAction b = JIPipeShortcutAction.builder("id.x", "Different", "Other")
                .description("Does stuff")
                .scopeFamily(JIPipeShortcutAction.FAMILY_PROJECT)
                .textInputSafe(true)
                .defaultVariants(List.of(List.of(KeyStroke.getKeyStroke("pressed P"))))
                .build();
        // equals/hashCode depend on the id only
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(b.equals(action("id.y", "X", "Cat")));

        JIPipeShortcutAction chord = JIPipeShortcutAction.builder("id.chord", "Chord", "Cat")
                .defaultChord(KeyStroke.getKeyStroke("control pressed T"), KeyStroke.getKeyStroke("pressed M"))
                .build();
        assertEquals(List.of(List.of(KeyStroke.getKeyStroke("control pressed T"),
                KeyStroke.getKeyStroke("pressed M"))), chord.defaultVariants());
    }

    @Test void bindingSourceStates() {
        JIPipeShortcutBinding unassigned = new JIPipeShortcutBinding("id.x", List.of(), JIPipeShortcutBinding.Source.UNASSIGNED);
        assertTrue(unassigned.isUnassigned());
        JIPipeShortcutBinding override = new JIPipeShortcutBinding("id.x",
                List.of(List.of(KeyStroke.getKeyStroke("pressed F1"))), JIPipeShortcutBinding.Source.OVERRIDE);
        assertFalse(override.isUnassigned());
        JIPipeShortcutBinding def = new JIPipeShortcutBinding("id.x", List.of(), JIPipeShortcutBinding.Source.DEFAULT);
        assertFalse(def.isUnassigned());
    }
}
