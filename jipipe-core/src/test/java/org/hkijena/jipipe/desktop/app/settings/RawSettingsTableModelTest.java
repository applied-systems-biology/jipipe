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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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