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

package org.hkijena.jipipe.api.settings;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SettingsSheetBridgeTest {
    // public, like every real settings sheet: the reflection-based parameter
    // accessors live in another package and need class-level access
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

        // Array parameter: the access layer's value equality
        // (JIPipeReflectionParameterAccess.set) is reference-based for
        // arrays, so a freshly deserialized array can never short-circuit
        // there. Only the bridge's own serialized-value comparison can
        // keep a second applyFromDocument() from invoking this setter —
        // which is exactly what the regression test pins.
        @JIPipeParameter("tags")
        public String[] getTags() {
            return tags;
        }

        @JIPipeParameter("tags")
        public void setTags(String[] tags) {
            this.tags = tags;
            tagSettersInvoked++;
            emitParameterChangedEvent("tags");
        }

        private String[] tags = {"a", "b"};
        int tagSettersInvoked;

        @Override
        public String getId() {
            return "test-sheet";
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

    @Test void documentChangeUpdatesSheetWithoutWritingBack() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        ProbeSheet sheet = new ProbeSheet();
        SettingsSheetBridge bridge = new SettingsSheetBridge(d, "test-sheet", sheet);
        bridge.attach();

        d.set("sheet:test-sheet", "theme", TextNode.valueOf("Dark"));
        bridge.applyFromDocument();
        assertEquals("Dark", sheet.getTheme());
        assertEquals("Dark", d.get("sheet:test-sheet", "theme").asText());
    }

    @Test void sheetChangeWritesToDocumentOnce() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        ProbeSheet sheet = new ProbeSheet();
        SettingsSheetBridge bridge = new SettingsSheetBridge(d, "test-sheet", sheet);
        bridge.attach();
        List<String> writes = new ArrayList<>();
        d.addPathListener("sheet:test-sheet/theme", n -> writes.add(n.asText()));

        sheet.setTheme("Dark");
        assertEquals(List.of("Dark"), writes, "exactly one write, no change storm");
    }

    @Test void unknownSubPathsInBlockSurviveASheetRoundTrip() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        d.set("sheet:test-sheet", "a-newer-knob", IntNode.valueOf(42));
        ProbeSheet sheet = new ProbeSheet();
        SettingsSheetBridge bridge = new SettingsSheetBridge(d, "test-sheet", sheet);
        bridge.attach();
        sheet.setTheme("Dark");
        bridge.flushToDocument();
        assertEquals(42, d.get("sheet:test-sheet", "a-newer-knob").asInt(),
                "a knob unknown to this version must not be cleared by a sheet write");
    }

    /**
     * A document-originated change must not be echoed back into the document:
     * the theme path may fire exactly once (the original set), and a whole-block
     * write-back (setBlock) would additionally fire the block-level listener.
     */
    @Test void documentChangeFiresDocumentListenerExactlyOnce() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        ProbeSheet sheet = new ProbeSheet();
        SettingsSheetBridge bridge = new SettingsSheetBridge(d, "test-sheet", sheet);
        bridge.attach();
        List<String> blockWrites = new ArrayList<>();
        List<String> themeWrites = new ArrayList<>();
        d.addPathListener("sheet:test-sheet", n -> blockWrites.add(n.toString()));
        d.addPathListener("sheet:test-sheet/theme", n -> themeWrites.add(n.asText()));

        d.set("sheet:test-sheet", "theme", TextNode.valueOf("Dark"));
        assertEquals("Dark", sheet.getTheme());
        assertEquals(List.of(), blockWrites,
                "a document change must not be echoed back as a whole-block write");
        assertEquals(List.of("Dark"), themeWrites,
                "the theme path must fire exactly once: the original write, no echo");
    }

    /**
     * Repeatedly applying an unchanged block must produce zero document
     * writes and zero parameter change events: value comparison stops the
     * storm at the second iteration. (The first apply legitimately fires
     * events; the second must fire nothing.)
     */
    @Test void repeatedApplyIsInert() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        d.set("sheet:test-sheet", "theme", TextNode.valueOf("Dark"));
        ProbeSheet sheet = new ProbeSheet();
        SettingsSheetBridge bridge = new SettingsSheetBridge(d, "test-sheet", sheet);
        bridge.attach();
        List<String> documentWrites = new ArrayList<>();
        d.addPathListener("sheet:test-sheet", n -> documentWrites.add(n.toString()));
        List<String> parameterEvents = new ArrayList<>();
        sheet.getParameterChangedEventEmitter().subscribeLambda((emitter, event) -> parameterEvents.add(event.getKey()));

        bridge.applyFromDocument();
        assertEquals("Dark", sheet.getTheme());
        documentWrites.clear();
        parameterEvents.clear();

        bridge.applyFromDocument();
        assertEquals("Dark", sheet.getTheme());
        assertEquals(List.of(), documentWrites, "re-applying an unchanged block must not write back");
        assertEquals(List.of(), parameterEvents,
                "re-applying an unchanged block must fire no parameter events");
    }

    /**
     * Pins the bridge's serialized-value comparison itself. The tags setter
     * always emits and can never be short-circuited by the access layer's
     * reference-based array equality, so only the comparison can stop a
     * second applyFromDocument() from invoking setters. If the comparison
     * were deleted, apply #2 would re-set both parameters.
     */
    @Test void secondApplyIsInertBecauseOfValueComparison() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        d.set("sheet:test-sheet", "theme", TextNode.valueOf("Dark"));
        d.set("sheet:test-sheet", "tags", JsonUtils.toJsonNode(List.of("x", "y")));
        ProbeSheet sheet = new ProbeSheet();
        SettingsSheetBridge bridge = new SettingsSheetBridge(d, "test-sheet", sheet);
        bridge.attach();
        List<String> parameterEvents = new ArrayList<>();
        sheet.getParameterChangedEventEmitter().subscribeLambda((emitter, event) -> parameterEvents.add(event.getKey()));

        bridge.applyFromDocument();
        assertEquals("Dark", sheet.getTheme());
        assertEquals(1, sheet.tagSettersInvoked, "first apply must set the tags");

        sheet.tagSettersInvoked = 0;
        parameterEvents.clear();
        bridge.applyFromDocument();
        assertEquals(0, sheet.tagSettersInvoked,
                "an unchanged block must not re-invoke the tags setter; only the bridge's comparison can prevent that");
        assertEquals(List.of(), parameterEvents,
                "an unchanged block must not fire any parameter events");
    }

    /**
     * A detached bridge must stop both directions: sheet edits no longer reach
     * the document, and document edits no longer reach the sheet.
     */
    @Test void detachStopsBothDirections() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        ProbeSheet sheet = new ProbeSheet();
        SettingsSheetBridge bridge = new SettingsSheetBridge(d, "test-sheet", sheet);
        bridge.attach();
        bridge.detach();
        // detach() a second time must be a no-op (idempotent)
        bridge.detach();

        // Document -> sheet direction stopped:
        d.set("sheet:test-sheet", "theme", TextNode.valueOf("Dark"));
        assertEquals("Modern", sheet.getTheme(), "document changes must not reach a detached sheet");

        // Sheet -> document direction stopped: the sheet's edit must not
        // overwrite the value that just arrived in the document.
        sheet.setTheme("Light");
        assertEquals("Dark", d.get("sheet:test-sheet", "theme").asText(),
                "sheet changes must not write to a document the bridge has detached from");
    }
}
