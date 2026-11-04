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

package org.hkijena.jipipe.utils.ui;

import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the dock state that is persisted in the application settings.
 * <p>
 * Authority model: the code owns which docks are available and their default
 * activity state; the persisted settings own the placement of docks and the
 * activity state of the persistent (non-context) docks. Context docks (ids
 * starting with an underscore) are selection-scoped, so their activity state
 * must never enter the persisted state - otherwise a transient hide (for
 * example the results dock stealing the top-right slot after a run) suppresses
 * the code default forever (issue #1353).
 */
class JIPipeDesktopDockPanelStateTest {

    private JIPipeDesktopDockPanel.State poisonedState() {
        JIPipeDesktopDockPanel.State state = new JIPipeDesktopDockPanel.State();
        // The state written by the bug: the results dock stole the top-right
        // slot during a run and hid its siblings, which was persisted.
        state.put("MAP", true, JIPipeDesktopDockPanel.PanelLocation.TopLeft);
        state.put("_PARAMETERS", false, JIPipeDesktopDockPanel.PanelLocation.TopRight);
        state.put("_SLOT_MANAGER", false, JIPipeDesktopDockPanel.PanelLocation.TopRight);
        state.put("_RESULTS", true, JIPipeDesktopDockPanel.PanelLocation.TopRight);
        state.put("_HELP", false, JIPipeDesktopDockPanel.PanelLocation.BottomRight);
        state.setLeftPanelWidth(350);
        state.setRightPanelWidth(500);
        state.setMainSplitPaneRatio(0.7);
        return state;
    }

    @Test void sanitizingDropsContextDockVisibilitiesButKeepsEverythingElse() {
        JIPipeDesktopDockPanel.State original = poisonedState();
        JIPipeDesktopDockPanel.State sanitized = original.withoutContextDockVisibilities();

        // Context dock activity state is gone
        assertNull(sanitized.getVisibilities().get("_PARAMETERS"));
        assertNull(sanitized.getVisibilities().get("_SLOT_MANAGER"));
        assertNull(sanitized.getVisibilities().get("_RESULTS"));
        assertNull(sanitized.getVisibilities().get("_HELP"));

        // Persistent docks and the placement of all docks are kept
        assertTrue(sanitized.getVisibilities().get("MAP"));
        assertEquals(JIPipeDesktopDockPanel.PanelLocation.TopRight, sanitized.getLocations().get("_PARAMETERS"));
        assertEquals(JIPipeDesktopDockPanel.PanelLocation.TopRight, sanitized.getLocations().get("_RESULTS"));
        assertEquals(JIPipeDesktopDockPanel.PanelLocation.TopLeft, sanitized.getLocations().get("MAP"));

        // Sizes and ratios are kept
        assertEquals(350, sanitized.getLeftPanelWidth());
        assertEquals(500, sanitized.getRightPanelWidth());
        assertEquals(0.7, sanitized.getMainSplitPaneRatio());
    }

    @Test void sanitizingDoesNotMutateTheOriginal() {
        JIPipeDesktopDockPanel.State original = poisonedState();
        JIPipeDesktopDockPanel.State sanitized = original.withoutContextDockVisibilities();

        assertNotSame(original, sanitized);
        assertFalse(original.getVisibilities().get("_PARAMETERS"));
        assertTrue(original.getVisibilities().containsKey("_RESULTS"));
    }

    @Test void sanitizedStateSurvivesJsonRoundTrip() {
        JIPipeDesktopDockPanel.State sanitized = poisonedState().withoutContextDockVisibilities();
        String json = JsonUtils.toJsonString(sanitized);
        JIPipeDesktopDockPanel.State reloaded = JsonUtils.readFromString(json, JIPipeDesktopDockPanel.State.class);

        assertNull(reloaded.getVisibilities().get("_PARAMETERS"));
        assertNull(reloaded.getVisibilities().get("_RESULTS"));
        assertTrue(reloaded.getVisibilities().get("MAP"));
        assertEquals(JIPipeDesktopDockPanel.PanelLocation.TopRight, reloaded.getLocations().get("_PARAMETERS"));
    }

    @Test void sanitizedStateLetsContextDocksUseTheirCodeDefaults() {
        JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
        panel.setSavedState(poisonedState().withoutContextDockVisibilities());

        // What the editor does when a node is selected: the parameters dock is
        // added with the code default visible=true
        panel.addDockPanel("_PARAMETERS", "Parameters", null,
                JIPipeDesktopDockPanel.PanelLocation.TopRight, true, -80, new JPanel());

        assertTrue(panel.getPanels().get("_PARAMETERS").isVisible(),
                "the code default must not be suppressed by poisoned persisted state");
    }

    /**
     * Pins the within-session contract: when the user switches to another dock
     * (for example Slots), deselects the node and selects a node again, the
     * same view returns (activity state as a function of the available panels).
     * The parameters dock entry stays available.
     */
    @Test void dockPanelReturnsToUserViewWhenContextDocksAreReAdded() {
        JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();

        // Node selected: parameters and slot manager docks become available
        panel.addDockPanel("_PARAMETERS", "Parameters", null,
                JIPipeDesktopDockPanel.PanelLocation.TopRight, true, -80, new JPanel());
        panel.addDockPanel("_SLOT_MANAGER", "Slots", null,
                JIPipeDesktopDockPanel.PanelLocation.TopRight, false, -70, new JPanel());
        assertTrue(panel.getPanels().get("_PARAMETERS").isVisible());

        // User clicks the Slots toggle
        panel.activatePanel("_SLOT_MANAGER", true);
        assertTrue(panel.getPanels().get("_SLOT_MANAGER").isVisible());
        assertFalse(panel.getPanels().get("_PARAMETERS").isVisible());

        // Selection changes: context docks are removed and re-added
        panel.removeDockPanelsIf(p -> p.getId().startsWith("_"));
        panel.addDockPanel("_PARAMETERS", "Parameters", null,
                JIPipeDesktopDockPanel.PanelLocation.TopRight, true, -80, new JPanel());
        panel.addDockPanel("_SLOT_MANAGER", "Slots", null,
                JIPipeDesktopDockPanel.PanelLocation.TopRight, false, -70, new JPanel());

        // The user's view returns; the parameters entry stays available
        assertTrue(panel.getPanels().get("_SLOT_MANAGER").isVisible());
        assertFalse(panel.getPanels().get("_PARAMETERS").isVisible());
        assertTrue(panel.getPanels().containsKey("_PARAMETERS"));
    }
}
