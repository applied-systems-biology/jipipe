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

package org.hkijena.jipipe.plugins.parameters.ui.api;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;
import org.hkijena.jipipe.plugins.parameters.library.filesystem.PathList;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * #1339: the list-editor drag-reorder index math (the one behavior the footer
 * rework actually changes) — moving the item at {@code from} to the drop
 * position {@code to} (a row index in the pre-removal list), with the shift
 * from the removal and clamping handled.
 */
class ListReorderMoveTest {

    private static PathList list(String... items) {
        PathList l = new PathList();
        for (String s : items) {
            l.add(Paths.get(s));
        }
        return l;
    }

    private static String join(JIPipeListParameter<?> l) {
        StringBuilder sb = new StringBuilder();
        for (Object o : l) {
            if (sb.length() > 0) sb.append(",");
            sb.append(((Path) o).toString().replace('\\', '/'));
        }
        return sb.toString();
    }

    @Test
    void moveDown() {
        PathList l = list("a", "b", "c");
        // drag row 0 and drop BELOW row 2 -> "b,c,a"
        assertTrue(JIPipeDesktopListParameterEditorUI.moveItem(l, 0, 3));
        assertEquals("b,c,a", join(l));
    }

    @Test
    void moveUp() {
        PathList l = list("a", "b", "c");
        // drag row 2 and drop ABOVE row 0 -> "c,a,b"
        assertTrue(JIPipeDesktopListParameterEditorUI.moveItem(l, 2, 0));
        assertEquals("c,a,b", join(l));
    }

    @Test
    void adjacentSwap() {
        PathList l = list("a", "b", "c");
        // drag row 0 and drop below row 1 (UI passes to = targetIndex+1 = 2) -> "b,a,c"
        assertTrue(JIPipeDesktopListParameterEditorUI.moveItem(l, 0, 2));
        assertEquals("b,a,c", join(l));
    }

    @Test
    void noOpWhenTargetIsSelfRow() {
        // dropping onto its own row maps to the same index -> no visible change
        PathList l = list("a", "b", "c");
        // from 1, to 1 (upper half of own row is intercepted as self by the UI;
        // the math itself: removing then re-inserting at the same adjusted spot)
        assertTrue(JIPipeDesktopListParameterEditorUI.moveItem(l, 1, 1));
        assertEquals("a,b,c", join(l));
    }

    @Test
    void outOfBoundsIsNoOpAndReturnsFalse() {
        PathList l = list("a", "b");
        assertFalse(JIPipeDesktopListParameterEditorUI.moveItem(l, 5, 0));
        assertFalse(JIPipeDesktopListParameterEditorUI.moveItem(l, -1, 0));
        assertEquals("a,b", join(l));
    }

    @Test
    void clampsInsertionBeyondEnd() {
        PathList l = list("a", "b", "c");
        // drop way beyond the end -> append
        assertTrue(JIPipeDesktopListParameterEditorUI.moveItem(l, 0, 99));
        assertEquals("b,c,a", join(l));
    }
}
