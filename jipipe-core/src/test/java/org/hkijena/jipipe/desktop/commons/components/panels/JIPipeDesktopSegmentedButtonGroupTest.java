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

package org.hkijena.jipipe.desktop.commons.components.panels;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Pins the selection contract of the merged (segmented) button group:
 * the selection listeners fire exactly once per real change, the initial
 * selection is silent, out-of-bounds indexes clamp, and the group is
 * single-select even when the currently selected segment is clicked again
 * (a plain {@code ButtonGroup} of toggle buttons would otherwise clear the
 * selection). These are model-state contracts that run headless — the
 * visual merged/segmented look itself is verified by the user in the app.
 */
class JIPipeDesktopSegmentedButtonGroupTest {

    private static JIPipeDesktopSegmentedButtonGroup threeSegmentGroup() {
        JIPipeDesktopSegmentedButtonGroup group = new JIPipeDesktopSegmentedButtonGroup();
        group.addSegment("Application");
        group.addSegment("Project");
        group.addSegment("Runtime");
        return group;
    }

    @Test
    void theFirstAddedSegmentIsSelectedSilently() {
        JIPipeDesktopSegmentedButtonGroup group = new JIPipeDesktopSegmentedButtonGroup();
        List<Integer> fired = new ArrayList<>();
        Consumer<Integer> listener = fired::add;
        group.addSelectionListener(listener);
        try {
            group.addSegment("Application");
            group.addSegment("Project");
            group.addSegment("Runtime");
        } finally {
            group.removeSelectionListener(listener);
        }
        assertEquals(0, group.getSelectedIndex(), "the first added segment is selected by default");
        assertEquals(List.of(), fired, "the initial selection must not fire selection listeners");
    }

    @Test
    void aRealSelectionChangeFiresExactlyOnceWithTheNewIndex() {
        JIPipeDesktopSegmentedButtonGroup group = threeSegmentGroup();
        List<Integer> fired = new ArrayList<>();
        Consumer<Integer> listener = fired::add;
        group.addSelectionListener(listener);
        try {
            group.setSelectedIndex(1);
            group.setSelectedIndex(2);
        } finally {
            group.removeSelectionListener(listener);
        }
        assertEquals(List.of(1, 2), fired, "one event per real change, carrying the new index");
        assertEquals(2, group.getSelectedIndex());
    }

    @Test
    void settingTheCurrentIndexFiresNothing() {
        JIPipeDesktopSegmentedButtonGroup group = threeSegmentGroup();
        List<Integer> fired = new ArrayList<>();
        Consumer<Integer> listener = fired::add;
        group.addSelectionListener(listener);
        try {
            group.setSelectedIndex(0); // already selected (initial)
            group.setSelectedIndex(1);
            group.setSelectedIndex(1); // no-op re-select
        } finally {
            group.removeSelectionListener(listener);
        }
        assertEquals(List.of(1), fired, "re-applying the current index is a no-op and fires nothing");
    }

    @Test
    void outOfBoundsIndexesAreClampedToTheSegmentRange() {
        JIPipeDesktopSegmentedButtonGroup group = threeSegmentGroup();
        List<Integer> fired = new ArrayList<>();
        Consumer<Integer> listener = fired::add;
        group.addSelectionListener(listener);
        try {
            group.setSelectedIndex(99);
            group.setSelectedIndex(-5);
        } finally {
            group.removeSelectionListener(listener);
        }
        assertEquals(List.of(2, 0), fired, "out-of-bounds indexes clamp to the last/first segment");
        assertEquals(0, group.getSelectedIndex());
    }

    @Test
    void selectingOneSegmentDeselectsTheOthers() {
        JIPipeDesktopSegmentedButtonGroup group = threeSegmentGroup();
        group.setSelectedIndex(1);
        int selectedCount = 0;
        for (int i = 0; i < group.getSegmentCount(); i++) {
            if (group.getSegmentButton(i).isSelected()) {
                selectedCount++;
                assertSame(group.getSegmentButton(1), group.getSegmentButton(i),
                        "the selected segment must be the one that was selected");
            }
        }
        assertEquals(1, selectedCount, "the group is single-select");
    }

}
