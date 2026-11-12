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

package org.hkijena.jipipe.desktop.app.grapheditor.findpanel;

import org.hkijena.jipipe.desktop.app.grapheditor.findpanel.NoteGroupingCalculator.Group;
import org.hkijena.jipipe.desktop.app.grapheditor.findpanel.NoteGroupingCalculator.NodeRef;
import org.hkijena.jipipe.desktop.app.grapheditor.findpanel.NoteGroupingCalculator.NoteRef;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link NoteGroupingCalculator}, the pure geometry and ordering
 * logic behind the Find panel's note groups (MR 2 of the navigator overhaul,
 * #803).
 * <p>
 * All coordinates are chosen so that node centers land on exact integers.
 */
class NoteGroupingCalculatorTest {

    private static final Color FILL = new Color(255, 250, 205);
    private static final Color BORDER = new Color(120, 100, 40);

    private static NoteRef note(String id, Rectangle rect, String title) {
        return new NoteRef(id, rect, title, FILL, BORDER);
    }

    private static NodeRef node(String id, String name, Rectangle rect) {
        return new NodeRef(id, rect, new FindEntry(UUID.randomUUID(), name, "", id, UUID.randomUUID(), "Compartment", false));
    }

    private static List<String> ids(List<NodeRef> nodes) {
        List<String> result = new ArrayList<>();
        for (NodeRef node : nodes) {
            result.add(node.id());
        }
        return result;
    }

    private static List<String> titles(List<Group> groups) {
        List<String> result = new ArrayList<>();
        for (Group group : groups) {
            result.add(group.title());
        }
        return result;
    }

    @Test
    void centerInsideNoteIsClaimedButMereOverlapIsNot() {
        NoteRef main = note("note-main", new Rectangle(0, 0, 100, 100), "Main");
        // center (50, 50) lies inside the note
        NodeRef inside = node("inside", "Bravo", new Rectangle(40, 40, 20, 20));
        // rect [95,135]x[20,60] overlaps the note, but its center (115, 40) lies outside
        NodeRef overlapOnly = node("overlap", "Charlie", new Rectangle(95, 20, 40, 40));

        List<NoteGroupingCalculator.Group> groups = NoteGroupingCalculator.group(Arrays.asList(inside, overlapOnly), Collections.singletonList(main));

        assertEquals(1, groups.size());
        Group group = groups.get(0);
        assertEquals("note-main", group.noteId());
        assertEquals("Main", group.title());
        assertEquals(FILL, group.fill());
        assertEquals(BORDER, group.border());
        assertEquals(Collections.singletonList("inside"), ids(group.nodes()));
    }

    @Test
    void centerOnTheNoteEdgeIsStillClaimedInclusiveBounds() {
        NoteRef main = note("note-main", new Rectangle(0, 0, 100, 100), "Main");
        // center (100, 50) lies exactly on the right edge of the note
        NodeRef edgeNode = node("edge", "Alpha", new Rectangle(90, 40, 20, 20));

        List<NoteGroupingCalculator.Group> groups = NoteGroupingCalculator.group(Collections.singletonList(edgeNode), Collections.singletonList(main));

        assertEquals(1, groups.size());
        assertEquals(Collections.singletonList("edge"), ids(groups.get(0).nodes()));
    }

    @Test
    void nodeUnderTwoOverlappingNotesAppearsInBothGroups() {
        NoteRef noteA = note("note-a", new Rectangle(0, 0, 100, 100), "A");
        NoteRef noteB = note("note-b", new Rectangle(50, 0, 100, 100), "B");
        // center (70, 50) lies inside both notes
        NodeRef shared = node("shared", "Shared", new Rectangle(60, 40, 20, 20));
        // center (20, 20) lies only inside note-a
        NodeRef onlyA = node("only-a", "OnlyA", new Rectangle(10, 10, 20, 20));

        List<NoteGroupingCalculator.Group> groups = NoteGroupingCalculator.group(Arrays.asList(shared, onlyA), Arrays.asList(noteA, noteB));

        assertEquals(2, groups.size());
        // groups ordered by note top-left position (y, then x): note-a (0,0) before note-b (50,0)
        assertEquals("A", groups.get(0).title());
        assertEquals(Arrays.asList("only-a", "shared"), ids(groups.get(0).nodes()));
        assertEquals("B", groups.get(1).title());
        assertEquals(Collections.singletonList("shared"), ids(groups.get(1).nodes()));
    }

    @Test
    void groupsAreOrderedByNoteTopLeftYThenX() {
        NoteRef lower = note("lower", new Rectangle(50, 200, 100, 50), "Lower");
        NoteRef upperRight = note("upper-right", new Rectangle(150, 100, 100, 50), "UpperRight");
        NoteRef upperLeft = note("upper-left", new Rectangle(10, 100, 100, 50), "UpperLeft");

        List<NoteGroupingCalculator.Group> groups = NoteGroupingCalculator.group(Collections.emptyList(), Arrays.asList(lower, upperRight, upperLeft));

        // same y: tie broken by x (upper-left before upper-right); smaller y first
        assertEquals(Arrays.asList("UpperLeft", "UpperRight", "Lower"), titles(groups));
    }

    @Test
    void titledNoteClaimingNoNodesStillFormsAnEmptyGroup() {
        NoteRef solo = note("note-solo", new Rectangle(1000, 1000, 50, 50), "Solo");
        NodeRef far = node("far", "Far", new Rectangle(0, 0, 20, 20));

        List<NoteGroupingCalculator.Group> groups = NoteGroupingCalculator.group(Collections.singletonList(far), Collections.singletonList(solo));

        assertEquals(1, groups.size());
        assertEquals("Solo", groups.get(0).title());
        assertTrue(groups.get(0).nodes().isEmpty());
        assertEquals(Collections.singletonList("far"), ids(NoteGroupingCalculator.ungrouped(Collections.singletonList(far), Collections.singletonList(solo))));
    }

    @Test
    void blankAndEmptyTitlesAreIgnoredEntirely() {
        NoteRef emptyTitle = note("note-empty", new Rectangle(0, 0, 100, 100), "");
        NoteRef blankTitle = note("note-blank", new Rectangle(200, 0, 100, 100), "   ");
        NoteRef titled = note("note-titled", new Rectangle(400, 0, 100, 100), "Real");

        NodeRef inEmpty = node("in-empty", "Zulu", new Rectangle(40, 40, 20, 20));
        NodeRef inBlank = node("in-blank", "Yankee", new Rectangle(240, 40, 20, 20));
        NodeRef inTitled = node("in-titled", "Xray", new Rectangle(440, 40, 20, 20));

        List<NodeRef> nodes = Arrays.asList(inEmpty, inBlank, inTitled);
        List<NoteRef> notes = Arrays.asList(emptyTitle, blankTitle, titled);

        List<NoteGroupingCalculator.Group> groups = NoteGroupingCalculator.group(nodes, notes);

        assertEquals(1, groups.size());
        assertEquals("Real", groups.get(0).title());
        assertEquals(Collections.singletonList("in-titled"), ids(groups.get(0).nodes()));

        // nodes under title-less notes are ungrouped, ordered by name (case-insensitive)
        assertEquals(Arrays.asList("in-blank", "in-empty"), ids(NoteGroupingCalculator.ungrouped(nodes, notes)));
    }

    @Test
    void ungroupedNodesAreOrderedByNameCaseInsensitive() {
        NodeRef bravo = node("bravo", "BRAVO", new Rectangle(0, 0, 20, 20));
        NodeRef alpha = node("alpha", "alpha", new Rectangle(100, 0, 20, 20));
        NodeRef charlie = node("charlie", "charlie", new Rectangle(200, 0, 20, 20));

        List<NodeRef> ungrouped = NoteGroupingCalculator.ungrouped(Arrays.asList(bravo, alpha, charlie), Collections.emptyList());

        assertEquals(Arrays.asList("alpha", "bravo", "charlie"), ids(ungrouped));
    }

    @Test
    void nodesWithinGroupAreOrderedByNameCaseInsensitive() {
        NoteRef group = note("note", new Rectangle(0, 0, 200, 200), "Group");
        NodeRef zulu = node("zulu", "ZULU", new Rectangle(10, 10, 20, 20));
        NodeRef alpha = node("alpha", "alpha", new Rectangle(50, 10, 20, 20));

        List<NoteGroupingCalculator.Group> groups = NoteGroupingCalculator.group(Arrays.asList(zulu, alpha), Collections.singletonList(group));

        assertEquals(1, groups.size());
        assertEquals(Arrays.asList("alpha", "zulu"), ids(groups.get(0).nodes()));
    }
}
