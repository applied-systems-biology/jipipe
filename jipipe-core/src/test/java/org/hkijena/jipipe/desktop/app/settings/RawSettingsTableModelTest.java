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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hkijena.jipipe.api.service.components.JIPipeSettingsServiceComponent;
import org.hkijena.jipipe.api.service.components.JIPipeSettingsServiceComponent.ChangeOrigin;
import org.hkijena.jipipe.api.settings.JIPipeSettingKey;
import org.hkijena.jipipe.api.settings.SettingsBinding;
import org.hkijena.jipipe.api.settings.SettingsDocument;
import org.hkijena.jipipe.api.settings.SettingsScope;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.SwingUtilities;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link RawSettingsTableModel}, the pure (non-Swing) model of the raw
 * settings editor.
 * <p>
 * The model is exercised against a real {@link JIPipeSettingsServiceComponent}
 * created by {@link JIPipeSettingsServiceComponent#forTesting(Path)} (an
 * injected profile directory, no service). That is the component the section
 * itself talks to, so the model's writes run through the production path —
 * document, change origin, auto-save — instead of a mock.
 */
class RawSettingsTableModelTest {

    private static final String BLOCK = "registry:x";
    private static final String DOCUMENTED_PATH = BLOCK + "/show-labels";
    private static final String PASS_THROUGH_BLOCK = "os:unknown-block";
    private static final String RUNTIME_ONLY_BLOCK = "sheet:org.hkijena.jipipe:test-runtime";
    private static final ObjectMapper MAPPER = JsonUtils.getObjectMapper();

    @TempDir
    Path tempDir;

    private final List<JIPipeSettingsServiceComponent> components = new ArrayList<>();

    /**
     * The three brief tests, verbatim.
     */
    @Test
    void listsDocumentedAndPassThroughKeys() {
        RawSettingsTableModel m = modelWith(docWithKeyAndPassThrough());
        assertTrue(m.rows("").stream().anyMatch(RawSettingsTableModel.RawRow::isPassThrough));
        assertTrue(m.rows("").stream().anyMatch(RawSettingsTableModel.RawRow::isDocumented));
    }

    @Test
    void filterMatchesPathSubstring() {
        assertEquals(1, modelWith(docWithKeyAndPassThrough()).rows("show-labels").size());
    }

    @Test
    void invalidJsonIsRejected() {
        RawSettingsTableModel m = modelWith(docWithKeyAndPassThrough());
        assertThrows(IllegalArgumentException.class, () -> m.setValue("registry:x/a", "{not json"));
    }

    // -------------------------------------------------------- row enumeration

    @Test
    void documentedRowCarriesTypeDefaultNameDescriptionAndStoredValue() {
        RawSettingsTableModel m = modelWith(docWithKeyAndPassThrough());
        RawSettingsTableModel.RawRow row = rowOf(m.rows("show-labels"), DOCUMENTED_PATH);
        assertTrue(row.isDocumented());
        assertFalse(row.isPassThrough());
        assertEquals("Boolean", row.typeLabel());
        assertEquals("true", row.defaultText());
        assertEquals("Show labels", row.name());
        assertTrue(row.description().contains("labels"));
        // Stored value is false, the registered default is true.
        assertEquals("false" + RawSettingsTableModel.PENDING_DEFAULT_SEPARATOR + "true", row.valuePreview());
        // A stored value equal to the default (no pending reset) shows only itself.
        m.setValue(DOCUMENTED_PATH, "true");
        assertEquals("true", rowOf(m.rows("show-labels"), DOCUMENTED_PATH).valuePreview());
    }

    @Test
    void absentDocumentedKeyShowsTheDefaultPlaceholder() {
        RawSettingsTableModel m = modelWith(docWithKeyAndPassThrough());
        RawSettingsTableModel.RawRow row = rowOf(m.rows("optional"), BLOCK + "/optional");
        assertTrue(row.isDocumented(), "a registered key is documented even when nothing is stored");
        assertEquals(RawSettingsTableModel.DEFAULT_PLACEHOLDER, row.valuePreview());
    }

    @Test
    void passThroughBlockEmitsItsLeavesWithInferredTypes() {
        RawSettingsTableModel m = modelWith(docWithKeyAndPassThrough());
        List<RawSettingsTableModel.RawRow> rows = m.rows("");
        RawSettingsTableModel.RawRow width = rowOf(rows, PASS_THROUGH_BLOCK + "/window/width");
        assertTrue(width.isPassThrough());
        assertFalse(width.isDocumented());
        assertEquals("int", width.typeLabel());
        assertEquals("1024", width.valuePreview());
        assertEquals("", width.defaultText());
        // Nested objects are flattened to their leaves; the containers themselves are not rows.
        assertTrue(rows.stream().noneMatch(r -> r.path().equals(PASS_THROUGH_BLOCK + "/window")),
                "a nested object must be flattened to its leaves, not emitted as a row");
    }

    @Test
    void undocumentedSubPathOfARegisteredBlockIsPassThrough() {
        RawSettingsTableModel m = modelWith(docWithKeyAndPassThrough());
        RawSettingsTableModel.RawRow row = rowOf(m.rows(""), BLOCK + "/legacy-note");
        assertTrue(row.isPassThrough());
        assertFalse(row.isDocumented());
        assertEquals("string", row.typeLabel());
    }

    @Test
    void noLocationIsEmittedTwice() {
        List<RawSettingsTableModel.RawRow> rows = modelWith(docWithKeyAndPassThrough()).rows("");
        assertFalse(rows.isEmpty());
        long distinct = rows.stream().map(RawSettingsTableModel.RawRow::path).distinct().count();
        assertEquals(rows.size(), distinct, "a location that is both registered and stored must be emitted once");
    }

    @Test
    void aListValuedBlockIsEmittedAsOneRow() {
        JIPipeSettingsServiceComponent component = componentWithKeyAndPassThrough();
        document(component).setBlock("state:recent-projects",
                MAPPER.createArrayNode().add("/a.jip").add("/b.jip"));
        List<RawSettingsTableModel.RawRow> rows = new RawSettingsTableModel(component, SettingsScope.APPLICATION).rows("");
        RawSettingsTableModel.RawRow row = rowOf(rows, "state:recent-projects");
        assertTrue(row.isPassThrough());
        assertEquals("array", row.typeLabel());
    }

    @Test
    void onlyTheSelectedScopeIsListed() {
        JIPipeSettingsServiceComponent component = componentWithKeyAndPassThrough();
        document(component, SettingsScope.RUNTIME).set("state:runtime-only", "x", MAPPER.createObjectNode().put("a", 1));
        RawSettingsTableModel application = new RawSettingsTableModel(component, SettingsScope.APPLICATION);
        RawSettingsTableModel runtime = new RawSettingsTableModel(component, SettingsScope.RUNTIME);
        assertTrue(application.rows("runtime-only").isEmpty());
        assertEquals(1, runtime.rows("runtime-only").size());
    }

    @Test
    void filterIsCaseInsensitiveAndBlankMatchesEverything() {
        RawSettingsTableModel m = modelWith(docWithKeyAndPassThrough());
        assertEquals(1, m.rows("SHOW-Labels").size());
        assertTrue(m.rows("  ").size() > 1);
        assertTrue(m.rows(null).size() > 1);
        assertTrue(m.rows("does-not-exist").isEmpty());
    }

    /**
     * The preview rendering rule, in isolation: a plain stored value shows only
     * itself; a stored value that differs from the default shows both, so the
     * table never hides the value the next save/read will take over.
     */
    @Test
    void previewShowsAValueThatDiffersFromTheDefaultAsBoth() {
        JsonNode stored = MAPPER.getNodeFactory().booleanNode(false);
        JsonNode defaultValue = MAPPER.getNodeFactory().booleanNode(true);
        assertEquals("false", RawSettingsTableModel.renderPreview(stored, stored));
        assertEquals("false" + RawSettingsTableModel.PENDING_DEFAULT_SEPARATOR + "true",
                RawSettingsTableModel.renderPreview(stored, defaultValue));
        assertEquals(RawSettingsTableModel.DEFAULT_PLACEHOLDER,
                RawSettingsTableModel.renderPreview(null, defaultValue));
        assertEquals("\"text\"",
                RawSettingsTableModel.renderPreview(MAPPER.getNodeFactory().textNode("text"), null),
                "a string preview is rendered as its JSON, so quoting is unambiguous");
    }

    // ------------------------------------------------------------------ writes

    @Test
    void setValueStoresTheParsedJsonAndSavesImmediately() throws Exception {
        JIPipeSettingsServiceComponent component = componentWithKeyAndPassThrough();
        RawSettingsTableModel m = new RawSettingsTableModel(component, SettingsScope.APPLICATION);

        m.setValue(PASS_THROUGH_BLOCK + "/window/height", "{\"a\":1}");

        assertEquals(1, document(component).get(PASS_THROUGH_BLOCK, "window/height").get("a").asInt());
        // RAW_EDIT is a local origin: the write schedules the debounced auto-save
        // (read once — the daemon debounce executor may fire it itself).
        boolean pending = component.saveNowForTesting();
        String written = Files.readString(tempDir.resolve("settings.json"));
        assertTrue(pending || written.contains("\"height\""),
                "a raw edit must be auto-saved without an explicit save()");
        assertTrue(written.contains("\"height\""), "the raw edit must reach settings.json");
    }

    @Test
    void setValueWithInvalidJsonDoesNotTouchTheDocument() {
        JIPipeSettingsServiceComponent component = componentWithKeyAndPassThrough();
        RawSettingsTableModel m = new RawSettingsTableModel(component, SettingsScope.APPLICATION);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> m.setValue(PASS_THROUGH_BLOCK + "/window/width", "[1,"));
        assertTrue(error.getMessage().contains(PASS_THROUGH_BLOCK + "/window/width"),
                "the message must name the offending path: " + error.getMessage());
        assertEquals(1024, document(component).get(PASS_THROUGH_BLOCK, "window/width").asInt());
    }

    @Test
    void documentedKeyResetWritesTheRegisteredDefault() {
        JIPipeSettingsServiceComponent component = componentWithKeyAndPassThrough();
        RawSettingsTableModel m = new RawSettingsTableModel(component, SettingsScope.APPLICATION);
        assertEquals(false, document(component).get(BLOCK, "show-labels").asBoolean());

        m.resetKey(DOCUMENTED_PATH);

        assertEquals(true, document(component).get(BLOCK, "show-labels").asBoolean(),
                "a documented key is reset to its registered default");
    }

    @Test
    void documentedKeyWithoutDefaultIsRemovedOnReset() {
        JIPipeSettingsServiceComponent component = componentWithKeyAndPassThrough();
        RawSettingsTableModel m = new RawSettingsTableModel(component, SettingsScope.APPLICATION);
        m.setValue(BLOCK + "/optional", "\"x\"");
        assertTrue(document(component).has(BLOCK, "optional"));

        m.resetKey(BLOCK + "/optional");

        assertFalse(document(component).has(BLOCK, "optional"),
                "a default-less documented key is reset by removing the stored value");
    }

    @Test
    void passThroughResetRemovesTheLocation() {
        JIPipeSettingsServiceComponent component = componentWithKeyAndPassThrough();
        RawSettingsTableModel m = new RawSettingsTableModel(component, SettingsScope.APPLICATION);
        assertTrue(document(component).has(PASS_THROUGH_BLOCK, "window/width"));

        m.resetKey(PASS_THROUGH_BLOCK + "/window/width");

        assertFalse(document(component).has(PASS_THROUGH_BLOCK, "window/width"),
                "a pass-through value has no default: reset removes it");
    }

    @Test
    void rawJsonIsTheStoredValueAsText() {
        RawSettingsTableModel m = modelWith(docWithKeyAndPassThrough());
        assertEquals("1024", m.rawJson(PASS_THROUGH_BLOCK + "/window/width"));
        assertEquals("false", m.rawJson(DOCUMENTED_PATH));
        assertEquals("", m.rawJson("state:absent/x"), "an absent location has no raw JSON");
    }

    /**
     * <b>Finding 3.</b> A JSON {@code null} is stored as a value; only an empty
     * or unparseable input is rejected. Removing a location is the reset's job.
     */
    @Test
    void setValueStoresJsonNullAsAValue() {
        JIPipeSettingsServiceComponent component = componentWithKeyAndPassThrough();
        RawSettingsTableModel m = new RawSettingsTableModel(component, SettingsScope.APPLICATION);

        m.setValue(PASS_THROUGH_BLOCK + "/window/width", "null");

        assertTrue(document(component).has(PASS_THROUGH_BLOCK, "window/width"),
                "a stored JSON null is a value, not a removal");
        assertTrue(document(component).get(PASS_THROUGH_BLOCK, "window/width").isNull());
        assertEquals("null", m.rawJson(PASS_THROUGH_BLOCK + "/window/width"));
    }

    // ------------------------------------------------------- key scope checks

    /**
     * <b>Finding 1 (RED pin).</b> Key resolution must be scope-bounded. The
     * component's key registry is a single scope-agnostic map, so
     * {@code keyByBlockAndSubPath} happily returns a key registered in
     * <em>another</em> scope for a same-named location. A location that this
     * scope's document holds as pass-through content must therefore stay
     * pass-through: it is not documented by a key of a different scope, and
     * resetting it must remove the value of <em>this</em> document (the
     * pass-through rule), not write the other scope's default.
     * <p>
     * Variant implemented: the controller's "pin the property directly" variant.
     * Registering the same block/sub-path key in two scopes is not usable as a
     * fixture — {@code registerKey} keys {@code registeredKeys} by location only,
     * so the second registration silently <em>replaces</em> the first (noted in
     * the report; not fixed here, by instruction). One RUNTIME-scoped key plus an
     * APPLICATION-document value at the same path is the honest construction.
     */
    @Test
    void aKeyRegisteredInAnotherScopeDoesNotDocumentThisScopesLocation() {
        JIPipeSettingsServiceComponent component = componentWithKeyAndPassThrough();
        SettingsScope scope = SettingsScope.APPLICATION;
        String path = RUNTIME_ONLY_BLOCK + "/runtime-only";
        // The key exists only in the RUNTIME scope ...
        component.bind(JIPipeSettingKey.string(RUNTIME_ONLY_BLOCK, "runtime-only", String.class)
                .scope(SettingsScope.RUNTIME)
                .defaultValue("runtime-default"));
        // ... while the APPLICATION document (like any document, which is not
        // block-filtered on write) holds a value at the same location.
        document(component).set(RUNTIME_ONLY_BLOCK, "runtime-only",
                MAPPER.getNodeFactory().textNode("application-value"));

        RawSettingsTableModel application = new RawSettingsTableModel(component, scope);
        assertFalse(application.isDocumented(path),
                "a key registered in another scope must not document this scope's location");
        RawSettingsTableModel.RawRow row = rowOf(application.rows(path), path);
        assertTrue(row.isPassThrough());
        assertFalse(row.isDocumented());

        // The reset takes the pass-through branch: this document's value is
        // removed. Writing the runtime key's default here would prove the bug.
        application.resetKey(path);
        assertFalse(document(component).has(RUNTIME_ONLY_BLOCK, "runtime-only"),
                "reset must remove the application document's value (pass-through rule)");
        assertEquals("application-value",
                document(component, SettingsScope.RUNTIME).get(RUNTIME_ONLY_BLOCK, "runtime-only").asText("application-value"),
                "the runtime document must not receive the application value");
    }

    /**
     * The counterpart: in the scope the key belongs to, the same key <em>is</em>
     * documented — so the check above pins the scope boundary, not a global
     * "keys are ignored" behaviour.
     */
    @Test
    void theKeyIsDocumentedInItsOwnScope() {
        JIPipeSettingsServiceComponent component = componentWithKeyAndPassThrough();
        String path = RUNTIME_ONLY_BLOCK + "/runtime-only";
        component.bind(JIPipeSettingKey.string(RUNTIME_ONLY_BLOCK, "runtime-only", String.class)
                .scope(SettingsScope.RUNTIME)
                .defaultValue("runtime-default"));
        RawSettingsTableModel runtime = new RawSettingsTableModel(component, SettingsScope.RUNTIME);
        assertTrue(runtime.isDocumented(path));
        RawSettingsTableModel.RawRow row = rowOf(runtime.rows(path), path);
        assertTrue(row.isDocumented());
        assertFalse(row.isPassThrough());
        assertEquals("\"runtime-default\"", row.defaultText());
    }

    // --------------------------------------------------------- preview bounds

    /**
     * <b>Finding 2.</b> A table cell must never hold a multi-kilobyte value: the
     * preview and the default text are truncated with an ellipsis, while
     * {@link RawSettingsTableModel#rawJson(String)} (which prefills the edit
     * dialog) keeps the full value.
     */
    @Test
    void previewsAreBoundedButRawJsonIsNot() {
        JIPipeSettingsServiceComponent component = componentWithKeyAndPassThrough();
        RawSettingsTableModel m = new RawSettingsTableModel(component, SettingsScope.APPLICATION);
        String stored = "x".repeat(200);
        m.setValue(PASS_THROUGH_BLOCK + "/window/note", "\"" + stored + "\"");

        RawSettingsTableModel.RawRow row = rowOf(m.rows(PASS_THROUGH_BLOCK + "/window/note"),
                PASS_THROUGH_BLOCK + "/window/note");
        assertTrue(row.valuePreview().length() <= RawSettingsTableModel.MAX_PREVIEW_LENGTH + 1,
                "the preview must be bounded: " + row.valuePreview().length() + " chars");
        assertTrue(row.valuePreview().endsWith(RawSettingsTableModel.TRUNCATION_ELLIPSIS),
                "a truncated preview ends with the ellipsis: " + row.valuePreview());
        assertEquals("\"" + stored + "\"", m.rawJson(PASS_THROUGH_BLOCK + "/window/note"),
                "rawJson must keep the full value for the edit dialog");

        // The Default column is bounded the same way.
        String longDefault = "d".repeat(200);
        component.bind(JIPipeSettingKey.string("sheet:org.hkijena.jipipe:test-long-default", "text", String.class)
                .defaultValue(longDefault));
        RawSettingsTableModel withLongDefault = new RawSettingsTableModel(component, SettingsScope.APPLICATION);
        RawSettingsTableModel.RawRow defaultRow = rowOf(
                withLongDefault.rows("sheet:org.hkijena.jipipe:test-long-default/text"),
                "sheet:org.hkijena.jipipe:test-long-default/text");
        assertTrue(defaultRow.defaultText().length() <= RawSettingsTableModel.MAX_PREVIEW_LENGTH + 1,
                "the default text must be bounded: " + defaultRow.defaultText().length() + " chars");
        assertTrue(defaultRow.defaultText().endsWith(RawSettingsTableModel.TRUNCATION_ELLIPSIS));
    }

    // ------------------------------------------------- origin restore on a throw

    /**
     * <b>Finding 5.</b> {@link JIPipeSettingsServiceComponent#writeWithOrigin}
     * must restore the thread-local origin even when a document path listener
     * throws <em>through</em> it: the restore happens in the seam's
     * {@code finally}, after {@code document.set} has already notified the
     * listeners. The write reassigns the location, so a stale listener at its
     * old value throws; the write propagates the exception and the next write on
     * this thread must still be a plain local one — a leaked
     * {@link ChangeOrigin#RAW_EDIT} / {@link ChangeOrigin#RESET} would be visible
     * as a wrong origin on its change event.
     * <p>
     * The listener is registered directly on the document (the same public
     * {@code addPathListener} seam the component itself uses); it must not throw
     * on the second write, so it is removed before that write.
     */
    @Test
    void aThrowingListenerDoesNotLeakTheOriginToTheNextWrite() throws Exception {
        // Change events are dispatched inline in a headless JVM but posted to the
        // EDT otherwise, so the observations run on the EDT either way.
        AtomicReference<String> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                assertThrowingListenerRestoresTheOrigin();
            } catch (AssertionError e) {
                failure.set(e.getMessage());
            }
        });
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }

    private void assertThrowingListenerRestoresTheOrigin() {
        JIPipeSettingsServiceComponent component = componentWithKeyAndPassThrough();
        SettingsDocument doc = document(component);
        String block = PASS_THROUGH_BLOCK;
        String sub = "window/height";
        // Baseline for the component's observers first: an absent "old" value
        // dispatches nothing, and a listener on an observed path is the only
        // dispatch seam whose exception propagates through the seam (the change
        // event emitter swallows subscriber exceptions).
        component.writeWithOrigin(SettingsScope.APPLICATION, block, sub,
                MAPPER.getNodeFactory().numberNode(768), ChangeOrigin.LOCAL_PROGRAMMATIC);
        doc.set(block, sub, MAPPER.getNodeFactory().numberNode(768));

        AtomicBoolean throwNextWrite = new AtomicBoolean(true);
        Consumer<JsonNode> throwingListener = value -> {
            if (throwNextWrite.get()) {
                throw new IllegalStateException("listener failure (intentional)");
            }
        };
        doc.addPathListener(SettingsDocument.listenerPathForTesting(block, sub), throwingListener);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> component.writeWithOrigin(SettingsScope.APPLICATION, block, sub,
                        MAPPER.getNodeFactory().numberNode(700), ChangeOrigin.RAW_EDIT),
                "the listener's exception must propagate through the seam");
        assertEquals("listener failure (intentional)", error.getMessage());
        assertEquals(700, doc.get(block, sub).asInt(), "the write itself landed before the listener threw");

        doc.removePathListener(SettingsDocument.listenerPathForTesting(block, sub), throwingListener);

        // Everything is restored: an ordinary local write on this thread is
        // attributed as a local write (not a raw edit) on its change event.
        List<ChangeOrigin> origins = new ArrayList<>();
        JIPipeSettingsServiceComponent.SettingsChangedEventListener capture = event -> {
            if (event.getPath().equals(block + "/" + sub)) {
                origins.add(event.getOrigin());
            }
        };
        component.getChangedEventEmitter().subscribe(capture);
        try {
            component.writeWithOrigin(SettingsScope.APPLICATION, block, sub,
                    MAPPER.getNodeFactory().numberNode(600), ChangeOrigin.LOCAL_PROGRAMMATIC);
        } finally {
            component.getChangedEventEmitter().unsubscribe(capture);
        }
        assertEquals(List.of(ChangeOrigin.LOCAL_PROGRAMMATIC), origins,
                "the origin must be restored after a throwing listener; a leaked RAW_EDIT appears here");
    }

    /**
     * The same property through the type everyone writes settings with: after a
     * throwing {@code writeWithOrigin}, an ordinary {@code SettingsBinding.set}
     * still schedules the debounced auto-save. That gate
     * ({@code isLocalOrigin}) is the origin check itself, so a leaked
     * non-local origin would show up here as a missing save.
     */
    @Test
    void aThrowingListenerDoesNotBlockTheNextTypedWriteFromPersisting() {
        JIPipeSettingsServiceComponent component = componentWithKeyAndPassThrough();
        SettingsDocument doc = document(component);
        String block = PASS_THROUGH_BLOCK;
        String sub = "window/height";
        doc.set(block, sub, MAPPER.getNodeFactory().numberNode(768));
        AtomicBoolean throwNextWrite = new AtomicBoolean(true);
        Consumer<JsonNode> throwingListener = value -> {
            if (throwNextWrite.get()) {
                throw new IllegalStateException("listener failure (intentional)");
            }
        };
        doc.addPathListener(SettingsDocument.listenerPathForTesting(block, sub), throwingListener);
        assertThrows(IllegalStateException.class, () -> component.writeWithOrigin(SettingsScope.APPLICATION,
                block, sub, MAPPER.getNodeFactory().numberNode(700), ChangeOrigin.RAW_EDIT));
        doc.removePathListener(SettingsDocument.listenerPathForTesting(block, sub), throwingListener);

        // Drain whatever the fixture/attempt scheduled, then write through a binding.
        component.saveNowForTesting();
        JIPipeSettingKey<Boolean> other = JIPipeSettingKey.bool("registry:x", "typed-after-throw", Boolean.class)
                .defaultValue(false);
        SettingsBinding<Boolean> binding = component.bind(other);
        binding.set(true);
        assertTrue(component.saveNowForTesting(),
                "an ordinary local write after a throwing raw write must still auto-save");
    }

    // ---------------------------------------------------------------- fixtures

    /**
     * The model over the document the current fixture component owns.
     * <p>
     * <b>Adaptation of the brief's helper:</b> the brief writes
     * {@code modelWith(docWithKeyAndPassThrough())}, but a document alone cannot
     * say which locations are <em>documented</em> — that knowledge lives in the
     * component's key registry ({@code registeredKeys()}), and the model must be
     * pure (no global service lookup). The helper therefore keeps the brief's
     * {@code docWithKeyAndPassThrough()} argument and pairs it with the
     * component that owns it ({@link #componentWithKeyAndPassThrough()}); the
     * brief's three tests are otherwise unchanged.
     */
    private RawSettingsTableModel modelWith(SettingsDocument doc) {
        return new RawSettingsTableModel(componentWithKeyAndPassThrough(), doc.scope());
    }

    private SettingsDocument docWithKeyAndPassThrough() {
        return document(componentWithKeyAndPassThrough());
    }

    /**
     * A component holding a documented boolean key
     * {@code registry:x/show-labels} (default {@code true}, stored value
     * {@code false}), a documented key without a default
     * ({@code registry:x/optional}), an undocumented sub-path of the documented
     * block ({@code registry:x/legacy-note}) and a pass-through block with a
     * nested object ({@code os:unknown-block/window/...}).
     * <p>
     * {@code registry:x} is non-pass-through (it carries registered keys);
     * {@code os:unknown-block} has a known block kind but no registered content,
     * so the engine treats it as pass-through.
     */
    private JIPipeSettingsServiceComponent componentWithKeyAndPassThrough() {
        JIPipeSettingsServiceComponent component = JIPipeSettingsServiceComponent.forTesting(tempDir);
        components.add(component);
        JIPipeSettingKey<Boolean> showLabels = JIPipeSettingKey.bool(BLOCK, "show-labels", Boolean.class)
                .defaultValue(true)
                .name("Show labels")
                .description("Whether node labels are shown");
        JIPipeSettingKey<String> optional = JIPipeSettingKey.string(BLOCK, "optional", String.class)
                .name("Optional")
                .description("A key without a default");
        component.bind(optional);
        SettingsBinding<Boolean> showLabelsBinding = component.bind(showLabels);
        SettingsDocument doc = document(component);
        showLabelsBinding.set(false);
        doc.set(BLOCK, "legacy-note", MAPPER.getNodeFactory().textNode("kept"));
        doc.set(PASS_THROUGH_BLOCK, "window/height", MAPPER.createObjectNode().numberNode(768));
        component.save();
        // Drain the debounced save the fixture's binding edit scheduled, so a
        // pending save observed later can only come from the action under test.
        component.saveNowForTesting();
        // A raw edit (the same origin-graded write path the model uses) schedules
        // another debounced save; drain it too, so no background save can fire
        // while a test inspects the document.
        component.writeWithOrigin(SettingsScope.APPLICATION, PASS_THROUGH_BLOCK, "window/width",
                MAPPER.createObjectNode().numberNode(1024), ChangeOrigin.RAW_EDIT);
        component.saveNowForTesting();
        return component;
    }

    private static SettingsDocument document(JIPipeSettingsServiceComponent component) {
        return document(component, SettingsScope.APPLICATION);
    }

    private static SettingsDocument document(JIPipeSettingsServiceComponent component, SettingsScope scope) {
        return component.document(scope);
    }

    private static RawSettingsTableModel.RawRow rowOf(List<RawSettingsTableModel.RawRow> rows, String path) {
        return rows.stream().filter(r -> r.path().equals(path)).findFirst()
                .orElseThrow(() -> new AssertionError("no row for " + path + " in " + paths(rows)));
    }

    private static List<String> paths(List<RawSettingsTableModel.RawRow> rows) {
        return rows.stream().map(RawSettingsTableModel.RawRow::path).toList();
    }

    @AfterEach
    void disposeComponents() {
        for (JIPipeSettingsServiceComponent c : components) {
            c.dispose();
        }
    }
}