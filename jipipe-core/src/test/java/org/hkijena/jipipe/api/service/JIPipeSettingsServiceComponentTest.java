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

package org.hkijena.jipipe.api.service;

import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.service.components.JIPipeSettingsServiceComponent;
import org.hkijena.jipipe.api.settings.JIPipeApplicationSettingsSheet;
import org.hkijena.jipipe.api.settings.JIPipeSettingKey;
import org.hkijena.jipipe.api.settings.SettingsBinding;
import org.hkijena.jipipe.api.settings.SettingsDocument;
import org.hkijena.jipipe.api.settings.SettingsScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.Icon;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JIPipeSettingsServiceComponentTest {

    @TempDir
    Path tempDir;

    @Test void documentsAreScopeSeparated() {
        // Construct the component against a temp user dir override is not possible in a pure unit test;
        // this test uses the injectable file resolver constructor instead.
        JIPipeSettingsServiceComponent c = JIPipeSettingsServiceComponent.forTesting(tempDir);
        assertNotSame(c.document(SettingsScope.APPLICATION), c.document(SettingsScope.RUNTIME));
        assertEquals(SettingsScope.APPLICATION, c.document(SettingsScope.APPLICATION).scope());
    }

    @Test void registeredKeyIsResolvable() {
        JIPipeSettingsServiceComponent c = JIPipeSettingsServiceComponent.forTesting(tempDir);
        JIPipeSettingKey<Boolean> k = JIPipeSettingKey.bool("registry:x", "a", Boolean.class).defaultValue(true);
        c.registerKey(k);
        assertSame(k, c.keyByBlockAndSubPath("registry:x", "a"));
    }

    @Test void bindingRoundTripsThroughSaveAndReload() {
        JIPipeSettingsServiceComponent c = JIPipeSettingsServiceComponent.forTesting(tempDir);
        SettingsBinding<Boolean> b = c.bind(JIPipeSettingKey.bool("registry:x", "a", Boolean.class).defaultValue(true));
        b.set(false);
        c.save();
        c.reload();
        assertFalse(c.bind(JIPipeSettingKey.bool("registry:x", "a", Boolean.class).defaultValue(true)).get());
    }

    /**
     * A parameter-less sheet that opts out of the initial default-seed (the
     * merged-section page hosts): attaching it must NOT write a block into the
     * document, while a non-exempt sheet with the same shape gets its
     * defaults seeded.
     */
    public static class SeedExemptProbeSheet extends AbstractJIPipeParameterCollection implements JIPipeApplicationSettingsSheet {
        @Override
        public String getId() {
            return "seed-exempt-probe-sheet";
        }

        @Override
        public Icon getIcon() {
            return null;
        }

        @Override
        public String getName() {
            return "Seed exempt probe";
        }

        @Override
        public String getDescription() {
            return "A parameter-less seed-exempt probe sheet";
        }

        @Override
        public String getCategory() {
            return "General";
        }

        @Override
        public Icon getCategoryIcon() {
            return null;
        }

        @Override
        public boolean isSeedExempt() {
            return true;
        }
    }

    @Test void seedExemptSheetGetsNoDocumentBlock() {
        JIPipeSettingsServiceComponent c = JIPipeSettingsServiceComponent.forTesting(tempDir);
        c.attachSheet(new SeedExemptProbeSheet());
        c.document(SettingsScope.APPLICATION); // materialize
        c.save();
        assertFalse(c.document(SettingsScope.APPLICATION).hasBlock("sheet:seed-exempt-probe-sheet"),
                "a seed-exempt sheet must not seed a (empty) block into the document");
        // A non-exempt sheet with the same shape still seeds its defaults.
        c.attachSheet(new ProbeSheet());
        c.document(SettingsScope.APPLICATION);
        c.save();
        assertTrue(c.document(SettingsScope.APPLICATION).hasBlock("sheet:probe-sheet"),
                "a non-exempt sheet must keep its initial default-seed");
    }

    @Test void runtimeDocumentTransfersIntoAFreshInstanceWithoutLoss() {
        // Spec §11 success criterion 7: the runtime document can be exported and
        // re-imported into a fresh service instance without loss — the
        // prerequisite for the #1102 runtime process split. The file is the
        // transferable artifact; a fresh component reading it sees the same
        // runtime-scoped content.
        JIPipeSettingsServiceComponent writer = JIPipeSettingsServiceComponent.forTesting(tempDir);
        JIPipeSettingKey<String> key = JIPipeSettingKey.string("sheet:org.hkijena.jipipe:runtime", "threads", String.class)
                .scope(SettingsScope.RUNTIME)
                .defaultValue("4");
        writer.bind(key).set("8");
        writer.save();
        assertTrue(Files.exists(tempDir.resolve("runtime-settings.json")), "the runtime document has its own file");

        JIPipeSettingsServiceComponent reader = JIPipeSettingsServiceComponent.forTesting(tempDir);
        SettingsBinding<String> reRead = reader.bind(JIPipeSettingKey.string("sheet:org.hkijena.jipipe:runtime", "threads", String.class)
                .scope(SettingsScope.RUNTIME)
                .defaultValue("4"));
        assertEquals("8", reRead.get(), "a fresh instance re-imports the runtime document without loss");
    }

    @Test void availableScopesAlwaysContainAllThree() {
        JIPipeSettingsServiceComponent c = JIPipeSettingsServiceComponent.forTesting(tempDir);
        assertEquals(Set.of(SettingsScope.APPLICATION, SettingsScope.PROJECT, SettingsScope.RUNTIME), c.availableScopes());
    }

    @Test void testingFactoryDoesNotTouchTheService() {
        JIPipeSettingsServiceComponent c = JIPipeSettingsServiceComponent.forTesting(tempDir);
        // The testing factory constructs without a JIPipeService: the service must be null,
        // and every service-dependent accessor must be null-guarded instead of throwing NPE.
        assertNull(c.getService());
        assertNull(c.getProgressInfo());
        // PROJECT has no file on disk; the in-memory document is still reachable without a service.
        assertNull(c.fileFor(SettingsScope.PROJECT));
        assertNotNull(c.document(SettingsScope.PROJECT));
        // The change event emitters are usable standalone.
        assertNotNull(c.getChangedEventEmitter());
        assertNotNull(c.getLegacyChangedEventEmitter());
    }

    @Test void fileForMapsScopesToProfileFiles() {
        JIPipeSettingsServiceComponent c = JIPipeSettingsServiceComponent.forTesting(tempDir);
        assertEquals(tempDir.resolve("settings.json"), c.fileFor(SettingsScope.APPLICATION));
        assertEquals(tempDir.resolve("runtime-settings.json"), c.fileFor(SettingsScope.RUNTIME));
        assertNull(c.fileFor(SettingsScope.PROJECT));
    }

    @Test void changeEventsAreEmittedWithOldAndNewValues() throws Exception {
        JIPipeSettingsServiceComponent c = JIPipeSettingsServiceComponent.forTesting(tempDir);
        JIPipeSettingKey<String> key = JIPipeSettingKey.string("registry:events", "name", String.class).defaultValue("default");
        List<JIPipeSettingsServiceComponent.SettingsChangedEvent> events = new ArrayList<>();
        List<JIPipeSettingsServiceComponent.LegacyChangedEvent> legacy = new ArrayList<>();
        c.getChangedEventEmitter().subscribeLambda((emitter, event) -> events.add(event));
        c.getLegacyChangedEventEmitter().subscribeLambda((emitter, event) -> legacy.add(event));

        SettingsBinding<String> b = c.bind(key);
        // The component delivers change events inline on the Swing EDT (and
        // hands off-EDT mutations to the EDT), so the assertions run on the
        // EDT — the thread real UI subscribers use.
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            b.set("one");
            b.set("two");
            b.set("one"); // effective value changed again
        });

        assertEquals(3, events.size(), "one event per effective change: " + events);
        assertEquals("registry:events/name", events.get(0).getPath());
        assertEquals(SettingsScope.APPLICATION, events.get(0).getScope());
        // The first change of an absent setting reports MissingNode as the
        // old value (the document never materializes defaults; the key
        // registry resolves them for display).
        assertTrue(events.get(0).getOldValue().isMissingNode());
        assertEquals("one", events.get(0).getNewValue().asText());
        assertEquals(JIPipeSettingsServiceComponent.ChangeOrigin.LOCAL_PROGRAMMATIC, events.get(0).getOrigin());
        assertEquals("one", events.get(1).getOldValue().asText());
        assertEquals("two", events.get(1).getNewValue().asText());
        assertEquals("two", events.get(2).getOldValue().asText());
        assertEquals("one", events.get(2).getNewValue().asText());
        assertEquals(3, legacy.size(), "the legacy aggregate fires on every change");
    }

    @Test void disposeClosesTrackedBindingsAndEarlyCloseStaysIdempotent() {
        JIPipeSettingsServiceComponent c = JIPipeSettingsServiceComponent.forTesting(tempDir);
        JIPipeSettingKey<String> key = JIPipeSettingKey.string("registry:leak", "x", String.class).defaultValue("d");
        SettingsBinding<String> b = c.bind(key);
        SettingsDocument doc = c.document(SettingsScope.APPLICATION);
        List<String> seen = new ArrayList<>();
        b.subscribe(seen::add);
        b.set("one");
        assertEquals(List.of("one"), seen);
        c.dispose(); // must close the tracked binding
        b.set("two"); // a closed binding still writes through to the document
        assertEquals("two", doc.get("registry:leak", "x").asText());
        assertEquals(List.of("one"), seen, "dispose closed the binding: subscribers no longer fire");

        // A binding the caller closed early must not break the component's
        // later (idempotent) close on dispose.
        JIPipeSettingsServiceComponent c2 = JIPipeSettingsServiceComponent.forTesting(tempDir);
        JIPipeSettingKey<String> key2 = JIPipeSettingKey.string("registry:early", "y", String.class).defaultValue("d");
        SettingsBinding<String> b2 = c2.bind(key2);
        b2.close();
        assertDoesNotThrow(c2::dispose);
    }

    @Test void attachedSheetIsFlushedOnSaveAndReattachedOnReload() throws Exception {
        // A stored settings file from a "previous run" (the sheet's default
        // theme is "Modern"; the file says "Light").
        Files.writeString(tempDir.resolve("settings.json"),
                "{ \"$schema\": 2, \"sheet:probe-sheet\": { \"theme\": \"Light\" } }");
        JIPipeSettingsServiceComponent c = JIPipeSettingsServiceComponent.forTesting(tempDir);
        ProbeSheet sheet = new ProbeSheet();
        c.attachSheet(sheet);

        // Attach must apply the stored file content into the sheet, matching
        // today's reload-after-registration sequence.
        assertEquals("Light", sheet.getTheme(), "attach applies the stored file content to the sheet");
        SettingsDocument app = c.document(SettingsScope.APPLICATION);
        assertTrue(app.hasBlock("sheet:probe-sheet"), "the sheet block is present");

        sheet.setTheme("Dark");
        c.save();
        // The sheet change flushed into the document and was persisted.
        assertEquals("Dark", app.get("sheet:probe-sheet", "theme").asText());

        // Reload: the document is replaced, and the bridge must be re-created
        // against the fresh one (file content reaches the sheet again, and
        // sheet changes keep flushing into the fresh document).
        c.reload();
        SettingsDocument fresh = c.document(SettingsScope.APPLICATION);
        assertNotSame(app, fresh, "reload replaces the document");
        assertEquals("Dark", fresh.get("sheet:probe-sheet", "theme").asText(), "value survived the reload from disk");
        assertEquals("Dark", sheet.getTheme(), "reload applies the re-read file content to the sheet");
        sheet.setTheme("Midnight");
        c.save();
        c.reload();
        assertEquals("Midnight", c.document(SettingsScope.APPLICATION).get("sheet:probe-sheet", "theme").asText(),
                "the re-attached bridge still flushes sheet changes into the fresh document");
        assertEquals("Midnight", sheet.getTheme(), "and applies it back to the sheet");
    }

    /**
     * Minimal application settings sheet mirroring the shape of the real ones:
     * one serialized parameter, change events on set.
     */
    public static class ProbeSheet extends AbstractJIPipeParameterCollection implements JIPipeApplicationSettingsSheet {
        @JIPipeParameter("theme")
        public String getTheme() {
            return theme;
        }

        @JIPipeParameter("theme")
        public void setTheme(String theme) {
            this.theme = theme;
            emitParameterChangedEvent("theme");
        }

        private String theme = "Modern";

        @Override
        public String getId() {
            return "probe-sheet";
        }

        @Override
        public Icon getIcon() {
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
        public Icon getCategoryIcon() {
            return null;
        }

        @Override
        public String getDescription() {
            return "A probe sheet for testing";
        }
    }
}
