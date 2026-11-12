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

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Pure geometry and ordering logic that groups graph nodes under the
 * annotation notes that claim them (Find panel, MR 2 of the navigator
 * overhaul, #803).
 * <p>
 * Only uses {@link Rectangle} and {@link Color} plus JDK types, so it is
 * testable without a display.
 */
public class NoteGroupingCalculator {

    /**
     * The claim mode used by {@link #group(Collection, Collection)} and
     * {@link #ungrouped(Collection, Collection)}: a note claims a node when
     * the center of the node rectangle lies inside the note rectangle
     * (inclusive bounds).
     */
    public static final String GROUP_CLAIM = "center";

    private static final Comparator<NoteRef> NOTE_TOP_LEFT_ORDER = Comparator.comparingDouble((NoteRef note) -> note.rect().getY()).thenComparingDouble(note -> note.rect().getX());

    private static final Comparator<NodeRef> NODE_NAME_ORDER = Comparator.comparing((NodeRef node) -> node.entry().getName(), String.CASE_INSENSITIVE_ORDER);

    /**
     * A graph node relevant for grouping: its id, its rectangle in real
     * canvas coordinates and its Find panel entry.
     */
    public record NodeRef(String id, Rectangle rect, FindEntry entry) {
    }

    /**
     * An annotation note: its id, its rectangle in real canvas coordinates,
     * its title and its colors.
     */
    public record NoteRef(String id, Rectangle rect, String title, Color fill, Color border) {
    }

    /**
     * A group of nodes claimed by one note. The member list is an unmodifiable
     * copy.
     */
    public record Group(String noteId, String title, Color fill, Color border, List<NodeRef> nodes) {
    }

    private NoteGroupingCalculator() {
    }

    /**
     * Groups the nodes under the notes that claim them.
     * <p>
     * Only notes with a non-blank title form groups. A note claims a node
     * when the node rectangle's center lies inside the note rectangle
     * (mode {@link #GROUP_CLAIM}); a node claimed by several notes appears
     * in each group. Groups are ordered by the note rectangle's top-left
     * position (y, then x); nodes within a group are ordered by name
     * (case-insensitive).
     */
    public static List<Group> group(Collection<NodeRef> nodes, Collection<NoteRef> notes) {
        List<NoteRef> titledNotes = titledNotes(notes);
        List<Group> groups = new ArrayList<>();
        for (NoteRef note : titledNotes) {
            List<NodeRef> members = new ArrayList<>();
            for (NodeRef node : nodes) {
                if (claims(note, node)) {
                    members.add(node);
                }
            }
            members.sort(NODE_NAME_ORDER);
            groups.add(new Group(note.id(), note.title(), note.fill(), note.border(), List.copyOf(members)));
        }
        return groups;
    }

    /**
     * Returns the nodes that no titled note claims, ordered by name
     * (case-insensitive).
     */
    public static List<NodeRef> ungrouped(Collection<NodeRef> nodes, Collection<NoteRef> notes) {
        List<NoteRef> titledNotes = titledNotes(notes);
        List<NodeRef> result = new ArrayList<>();
        for (NodeRef node : nodes) {
            boolean claimed = false;
            for (NoteRef note : titledNotes) {
                if (claims(note, node)) {
                    claimed = true;
                    break;
                }
            }
            if (!claimed) {
                result.add(node);
            }
        }
        result.sort(NODE_NAME_ORDER);
        return result;
    }

    private static List<NoteRef> titledNotes(Collection<NoteRef> notes) {
        List<NoteRef> titled = new ArrayList<>();
        for (NoteRef note : notes) {
            if (note.title() != null && !note.title().isBlank()) {
                titled.add(note);
            }
        }
        titled.sort(NOTE_TOP_LEFT_ORDER);
        return titled;
    }

    private static boolean claims(NoteRef note, NodeRef node) {
        Rectangle noteRect = note.rect();
        Rectangle nodeRect = node.rect();
        double centerX = nodeRect.getX() + nodeRect.getWidth() / 2.0;
        double centerY = nodeRect.getY() + nodeRect.getHeight() / 2.0;
        return centerX >= noteRect.getMinX()
                && centerX <= noteRect.getMaxX()
                && centerY >= noteRect.getMinY()
                && centerY <= noteRect.getMaxY();
    }
}
