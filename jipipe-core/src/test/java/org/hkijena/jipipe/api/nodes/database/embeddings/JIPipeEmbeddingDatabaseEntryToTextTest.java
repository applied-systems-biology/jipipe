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

package org.hkijena.jipipe.api.nodes.database.embeddings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeNodeClassification;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabasePipelineVisibility;
import org.hkijena.jipipe.api.nodes.database.WeightedTokens;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.tables.TableAggregateFunction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.junit.jupiter.api.Test;

import javax.swing.Icon;
import java.awt.Color;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the rendered embedding entry text of
 * {@link JIPipeEmbeddingDatabase#entryToText(JIPipeNodeDatabaseEntry)}:
 * render order (name first, functions, description, condensed slots),
 * the registry resolution of the functions line, and the description-only
 * truncation to {@link JIPipeEmbeddingDatabase#MAX_EMBEDDING_TEXT_LENGTH}.
 * <p>
 * The rendered text is the input of the content-hash verification
 * ({@code getVerifiedEmbedding}), so its exact shape re-keys the vector
 * caches; the assertions pin that shape.
 * <p>
 * Uses a minimal stub entry (the {@code JIPipeMergedNodeSearchRankingTest}
 * fixture pattern). The functions-line assertions need the live taxonomy
 * registry ({@code JIPipe.ensureInstance()}).
 */
class JIPipeEmbeddingDatabaseEntryToTextTest {

    // ------------------------------------------------------------------
    // Fixture
    // ------------------------------------------------------------------

    /**
     * A minimal entry carrying exactly what {@code entryToText} reads:
     * name, description, function ids, and the slot maps.
     */
    private static class TestEntry implements JIPipeNodeDatabaseEntry {

        private final String id;
        private final String name;
        private HTMLText description;
        private Set<String> functionIds = Collections.emptySet();
        private Map<String, JIPipeDataSlotInfo> inputSlots = Collections.emptyMap();
        private Map<String, JIPipeDataSlotInfo> outputSlots = Collections.emptyMap();

        private TestEntry(String id, String name) {
            this.id = id;
            this.name = name;
        }

        private TestEntry withDescription(String plainText) {
            this.description = new HTMLText(plainText);
            return this;
        }

        private TestEntry withFunctionIds(String... ids) {
            this.functionIds = new LinkedHashSet<>(List.of(ids));
            return this;
        }

        private TestEntry withInputSlot(String name, Class<? extends JIPipeData> dataClass) {
            this.inputSlots = Map.of(name, new JIPipeDataSlotInfo(dataClass, JIPipeSlotType.Input, name, ""));
            return this;
        }

        private TestEntry withOutputSlot(String name, Class<? extends JIPipeData> dataClass) {
            this.outputSlots = Map.of(name, new JIPipeDataSlotInfo(dataClass, JIPipeSlotType.Output, name, ""));
            return this;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public HTMLText getDescription() {
            return description;
        }

        @Override
        public Set<String> getFunctionIds() {
            return functionIds;
        }

        @Override
        public Map<String, JIPipeDataSlotInfo> getInputSlots() {
            return inputSlots;
        }

        @Override
        public Map<String, JIPipeDataSlotInfo> getOutputSlots() {
            return outputSlots;
        }

        @Override
        public WeightedTokens getTokens() {
            return new WeightedTokens();
        }

        @Override
        public boolean exists() {
            return false;
        }

        @Override
        public JIPipeNodeDatabasePipelineVisibility getVisibility() {
            return JIPipeNodeDatabasePipelineVisibility.Pipeline;
        }

        @Override
        public Icon getIcon() {
            return null;
        }

        @Override
        public List<String> getLocationInfos() {
            return Collections.emptyList();
        }

        @Override
        public Color getFillColor() {
            return null;
        }

        @Override
        public Color getBorderColor() {
            return null;
        }

        @Override
        public Set<JIPipeDesktopGraphNodeUI> addToGraph(JIPipeDesktopGraphCanvasUI canvasUI) {
            return Collections.emptySet();
        }

        @Override
        public boolean canAddInputSlots() {
            return false;
        }

        @Override
        public boolean canAddOutputSlots() {
            return false;
        }

        @Override
        public boolean isDeprecatedOrUnstable() {
            return false;
        }

        @Override
        public JIPipeNodeClassification getNodeClassification() {
            return JIPipeNodeClassification.Standard;
        }

        @Override
        public <T> T getAttachment(Class<T> attachmentClass) {
            return null;
        }

        @Override
        public <T> void attach(T attachment) {
        }
    }

    private static String lineStartingWith(String text, String prefix) {
        return text.lines().filter(line -> line.startsWith(prefix)).findFirst().orElse("");
    }

    // ------------------------------------------------------------------
    // Render order
    // ------------------------------------------------------------------

    @Test
    void rendersSectionsInOrder() {
        JIPipe.ensureInstance();
        TestEntry entry = new TestEntry("test:entry", "Test entry")
                .withDescription("A test description.")
                .withFunctionIds(TableAggregateFunction.ID)
                .withInputSlot("In", JIPipeData.class)
                .withOutputSlot("Out", JIPipeData.class);

        String text = JIPipeEmbeddingDatabase.entryToText(entry);

        int functionsIndex = text.indexOf("Functions: ");
        int descriptionIndex = text.indexOf("Description: ");
        int inputsIndex = text.indexOf("Inputs: ");
        int outputsIndex = text.indexOf("Outputs: ");
        assertTrue(text.startsWith("Name: Test entry\n"), "Name must come first");
        assertTrue(functionsIndex >= 0, "Functions line missing");
        assertTrue(descriptionIndex >= 0, "Description line missing");
        assertTrue(inputsIndex >= 0, "Inputs line missing");
        assertTrue(outputsIndex >= 0, "Outputs line missing");
        assertTrue(functionsIndex < descriptionIndex, "Functions must precede the description");
        assertTrue(descriptionIndex < inputsIndex, "Description must precede the inputs");
        assertTrue(inputsIndex < outputsIndex, "Inputs must precede the outputs");
    }

    // ------------------------------------------------------------------
    // Functions line
    // ------------------------------------------------------------------

    @Test
    void functionsLineResolvesRegistryNames() {
        JIPipe.ensureInstance();
        TestEntry entry = new TestEntry("test:entry", "Test entry")
                .withFunctionIds(TableAggregateFunction.ID);

        String text = JIPipeEmbeddingDatabase.entryToText(entry);

        assertEquals("Functions: Tables > Aggregate",
                lineStartingWith(text, "Functions:"));
    }

    @Test
    void unknownFunctionIdRendersRaw() {
        JIPipe.ensureInstance();
        TestEntry entry = new TestEntry("test:entry", "Test entry")
                .withFunctionIds("jipipe:function:does-not-exist", TableAggregateFunction.ID);

        String text = JIPipeEmbeddingDatabase.entryToText(entry);

        assertEquals("Functions: jipipe:function:does-not-exist, Tables > Aggregate",
                lineStartingWith(text, "Functions:"));
    }

    @Test
    void functionsLineOmittedWithoutIds() {
        TestEntry entry = new TestEntry("test:entry", "Test entry")
                .withDescription("A test description.");

        String text = JIPipeEmbeddingDatabase.entryToText(entry);

        assertFalse(text.contains("Functions:"), "No function ids, no functions line: " + text);
    }

    // ------------------------------------------------------------------
    // Condensed slots
    // ------------------------------------------------------------------

    @Test
    void slotsAreCondensedToNameOnly() {
        JIPipe.ensureInstance();
        TestEntry entry = new TestEntry("test:entry", "Test entry")
                .withInputSlot("In", JIPipeData.class)
                .withOutputSlot("Out", JIPipeData.class);

        String text = JIPipeEmbeddingDatabase.entryToText(entry);

        String dataName = JIPipeDataInfo.getInstance(JIPipeData.class).getName();
        String dataDescription = JIPipeDataInfo.getInstance(JIPipeData.class).getDescription();
        assertEquals("Inputs: In (" + dataName + ")", lineStartingWith(text, "Inputs:"));
        assertEquals("Outputs: Out (" + dataName + ")", lineStartingWith(text, "Outputs:"));
        if (dataDescription != null && !dataDescription.isBlank()) {
            assertFalse(text.contains(dataDescription), "Per-slot data type description must be dropped");
        }
    }

    // ------------------------------------------------------------------
    // Truncation
    // ------------------------------------------------------------------

    @Test
    void longDescriptionIsTruncatedToTheBound() {
        TestEntry entry = new TestEntry("test:entry", "N")
                .withDescription("d".repeat(5000));

        String text = JIPipeEmbeddingDatabase.entryToText(entry);

        // "Name: N\n" is 8 chars; "Description: " is 13 chars; the trailing
        // newline of the last line is trimmed, so the total is exactly the bound.
        int expectedCut = JIPipeEmbeddingDatabase.MAX_EMBEDDING_TEXT_LENGTH - 8 - 13;
        assertEquals(JIPipeEmbeddingDatabase.MAX_EMBEDDING_TEXT_LENGTH, text.length());
        assertEquals("Description: " + "d".repeat(expectedCut), lineStartingWith(text, "Description:"));
        assertTrue(text.startsWith("Name: N\n"), "Name must stay first and untruncated");
    }

    @Test
    void slotLinesCountTowardTheBudgetBeforeTheDescription() {
        JIPipe.ensureInstance();
        TestEntry entry = new TestEntry("test:entry", "N")
                .withDescription("d".repeat(5000))
                .withInputSlot("In", JIPipeData.class)
                .withOutputSlot("Out", JIPipeData.class);

        String text = JIPipeEmbeddingDatabase.entryToText(entry);

        // The slot lines render after the description but their lengths
        // (including each trailing newline) are subtracted from the budget.
        String dataName = JIPipeDataInfo.getInstance(JIPipeData.class).getName();
        int slotsLength = ("Inputs: In (" + dataName + ")").length() + 1
                + ("Outputs: Out (" + dataName + ")").length() + 1;
        int expectedCut = JIPipeEmbeddingDatabase.MAX_EMBEDDING_TEXT_LENGTH - 8 - slotsLength - 13;
        assertEquals(JIPipeEmbeddingDatabase.MAX_EMBEDDING_TEXT_LENGTH, text.length());
        assertEquals("Description: " + "d".repeat(expectedCut), lineStartingWith(text, "Description:"));
        assertEquals("Inputs: In (" + dataName + ")", lineStartingWith(text, "Inputs:"));
        assertEquals("Outputs: Out (" + dataName + ")", lineStartingWith(text, "Outputs:"));
    }

    @Test
    void nameIsNeverTruncated() {
        String longName = "N".repeat(120);
        TestEntry entry = new TestEntry("test:entry", longName)
                .withDescription("d".repeat(5000));

        String text = JIPipeEmbeddingDatabase.entryToText(entry);

        assertTrue(text.startsWith("Name: " + longName + "\n"), "The full name must survive");
        assertTrue(text.length() <= JIPipeEmbeddingDatabase.MAX_EMBEDDING_TEXT_LENGTH);
        assertTrue(text.contains("Description: "), "The description still fits after a long name");
    }

    @Test
    void descriptionIsOmittedWhenOtherSectionsExceedTheBound() {
        String oversizedName = "N".repeat(JIPipeEmbeddingDatabase.MAX_EMBEDDING_TEXT_LENGTH + 100);
        TestEntry entry = new TestEntry("test:entry", oversizedName)
                .withDescription("A test description.");

        String text = JIPipeEmbeddingDatabase.entryToText(entry);

        assertTrue(text.startsWith("Name: " + oversizedName), "The name stays even when it alone exceeds the bound");
        assertFalse(text.contains("Description:"), "No room for the description, the line is omitted");
    }
}
