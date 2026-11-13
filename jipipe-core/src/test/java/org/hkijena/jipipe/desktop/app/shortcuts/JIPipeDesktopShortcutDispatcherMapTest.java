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

package org.hkijena.jipipe.desktop.app.shortcuts;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutAction;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutRegistry;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Pins the stroke-map composition of
 * {@link JIPipeDesktopShortcutDispatcher#rebuildStrokeMaps()}: a stroke
 * shared by several actions must map to the ordered list of ALL of their
 * ids (registry order), never to a single last-writer id.
 * <p>
 * The test drives the published map snapshot of a fresh, isolated
 * dispatcher: no dispatcher installation on the focus manager, no key
 * events, headless-safe. Each test populates a fresh registry FIRST and
 * only then creates the dispatcher, so the constructor's EDT-marshaled
 * initial rebuild and the direct {@code rebuildStrokeMaps()} calls all read
 * the same fully populated registry and publish identical snapshots (no
 * race between the direct rebuild and the async listener rebuild).
 */
class JIPipeDesktopShortcutDispatcherMapTest {
    private static final String NODE_DELETE_ID = "org.hkijena.jipipe.test.node-delete";
    private static final String EDGE_CONTROL_POINT_ID = "org.hkijena.jipipe.test.edge-control-point";
    private static final String NODE_COPY_ID = "org.hkijena.jipipe.test.node-copy";
    private static final String EDGE_COPY_ID = "org.hkijena.jipipe.test.edge-copy";
    private static final String CHORD_FIRST_ID = "org.hkijena.jipipe.test.chord-first";
    private static final String CHORD_SECOND_ID = "org.hkijena.jipipe.test.chord-second";
    private static final String SOLO_ID = "org.hkijena.jipipe.test.solo";

    private JIPipeDesktopShortcutDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        // The settings store must exist before the dispatcher constructor
        // runs (it subscribes to the live keymap binding); pre-load the
        // keymap so rebuildStrokeMaps() reads it synchronously.
        JIPipe.ensureInstance();
        JIPipeShortcutSettings.current();
        JIPipeShortcutRegistry.resetInstance();
        JIPipeDesktopShortcutDispatcher.resetInstance();
    }

    @AfterEach
    void tearDown() {
        // Drop the isolated dispatcher (its registry change listener dies
        // with the reset registry) and give the next test a fresh registry,
        // matching the isolation protocol of JIPipeShortcutResolveTest.
        JIPipeDesktopShortcutDispatcher.resetInstance();
        JIPipeShortcutRegistry.resetInstance();
    }

    /**
     * Creates the isolated dispatcher over the (already populated) fresh
     * registry and runs its rebuild. The rebuild runs on the EDT
     * (invokeAndWait), serialized with the constructor's own EDT-marshaled
     * initial rebuild, so both read the same registry and publish
     * identical snapshots; the assertions then read a settled map.
     */
    private JIPipeDesktopShortcutDispatcher freshDispatcher() {
        JIPipeDesktopShortcutDispatcher d = JIPipeDesktopShortcutDispatcher.getInstance();
        try {
            SwingUtilities.invokeAndWait(d::rebuildStrokeMaps);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted waiting for the EDT rebuild", e);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw new IllegalStateException("The EDT rebuild threw", e.getCause());
        }
        dispatcher = d;
        return d;
    }

    @Test void sharedStrokeKeepsAllCandidateIdsInRegistryOrder() {
        JIPipeShortcutRegistry registry = JIPipeShortcutRegistry.getInstance();
        KeyStroke delete = KeyStroke.getKeyStroke("pressed DELETE");
        KeyStroke ctrlC = KeyStroke.getKeyStroke("control pressed C");
        // Registry order is category then name: "Delete" sorts before
        // "Remove control point" and "Copy edges" before "Copy nodes",
        // pinning the exact candidate order the fire path must walk.
        registry.register(JIPipeShortcutAction.builder(NODE_DELETE_ID, "Delete", "Test shared")
                .defaultChord(delete).build());
        registry.register(JIPipeShortcutAction.builder(EDGE_CONTROL_POINT_ID, "Remove control point", "Test shared")
                .defaultChord(delete).build());
        registry.register(JIPipeShortcutAction.builder(NODE_COPY_ID, "Copy nodes", "Test copy")
                .defaultChord(ctrlC).build());
        registry.register(JIPipeShortcutAction.builder(EDGE_COPY_ID, "Copy edges", "Test copy")
                .defaultChord(ctrlC).build());
        // A chord shared by two actions, Ctrl+T then M for both.
        registry.register(JIPipeShortcutAction.builder(CHORD_FIRST_ID, "Tool first", "Test chord")
                .defaultChord(KeyStroke.getKeyStroke("control pressed T"), KeyStroke.getKeyStroke("pressed M"))
                .build());
        registry.register(JIPipeShortcutAction.builder(CHORD_SECOND_ID, "Tool second", "Test chord")
                .defaultChord(KeyStroke.getKeyStroke("control pressed T"), KeyStroke.getKeyStroke("pressed M"))
                .build());
        // A stroke bound to exactly one action stays a one-candidate list.
        registry.register(JIPipeShortcutAction.builder(SOLO_ID, "Solo", "Test solo")
                .defaultChord(KeyStroke.getKeyStroke("pressed F5")).build());
        freshDispatcher();

        assertEquals(List.of(NODE_DELETE_ID, EDGE_CONTROL_POINT_ID),
                dispatcher.singleStrokeMap.get(delete),
                "the shared DELETE stroke must keep BOTH ids in registry order");
        assertEquals(List.of(EDGE_COPY_ID, NODE_COPY_ID),
                dispatcher.singleStrokeMap.get(ctrlC),
                "the shared Ctrl+C stroke must keep BOTH ids in registry order");
        assertEquals(List.of(CHORD_FIRST_ID, CHORD_SECOND_ID),
                dispatcher.chordMap.get(List.of(KeyStroke.getKeyStroke("control pressed T"),
                        KeyStroke.getKeyStroke("pressed M"))),
                "a completed chord shared by two actions must keep BOTH ids");
        assertEquals(List.of(SOLO_ID),
                dispatcher.singleStrokeMap.get(KeyStroke.getKeyStroke("pressed F5")));
        // Sanity: the published snapshot is a fresh, non-null list.
        assertNotNull(dispatcher.singleStrokeMap.get(delete));
    }

    @Test void unboundActionsContributeNoCandidates() {
        // A stroke nobody holds maps to nothing (the fire path treats that
        // as a plain miss), and the one bound stroke maps to a single id.
        JIPipeShortcutRegistry registry = JIPipeShortcutRegistry.getInstance();
        registry.register(JIPipeShortcutAction.builder(SOLO_ID, "Solo", "Test solo")
                .defaultChord(KeyStroke.getKeyStroke("pressed F5")).build());
        freshDispatcher();

        assertEquals(List.of(SOLO_ID),
                dispatcher.singleStrokeMap.get(KeyStroke.getKeyStroke("pressed F5")));
        assertNull(dispatcher.singleStrokeMap.get(KeyStroke.getKeyStroke("pressed DELETE")));
        assertNull(dispatcher.chordMap.get(List.of(KeyStroke.getKeyStroke("control pressed T"),
                KeyStroke.getKeyStroke("pressed M"))));
    }
}
