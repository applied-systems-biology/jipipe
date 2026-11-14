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
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the dock pin API: one dock pinned per side, persisted through the
 * dock state, cleared when the dock is moved (#1367).
 */
class JIPipeDesktopDockPanelPinTest {

    private void addPanel(JIPipeDesktopDockPanel panel, String id, JIPipeDesktopDockPanel.PanelLocation location, boolean visible) {
        panel.addDockPanel(id, id, null, location, visible, 0, new JPanel());
    }

    @Test void pinningReplacesTheSidesPreviousPin() {
        JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
        addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
        addPanel(panel, "ERRORS", JIPipeDesktopDockPanel.PanelLocation.BottomRight, false);
        panel.pinDock("TOOLS");
        assertEquals("TOOLS", panel.getPinnedDockId(JIPipeDesktopDockPanel.PinSide.Right));
        panel.pinDock("ERRORS");
        assertEquals("ERRORS", panel.getPinnedDockId(JIPipeDesktopDockPanel.PinSide.Right));
        assertFalse(panel.isDockPinned("TOOLS"));
    }

    @Test void pinsOnDifferentSidesCoexist() {
        JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
        addPanel(panel, "FIND", JIPipeDesktopDockPanel.PanelLocation.TopLeft, false);
        addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
        panel.pinDock("FIND");
        panel.pinDock("TOOLS");
        assertTrue(panel.isDockPinned("FIND"));
        assertTrue(panel.isDockPinned("TOOLS"));
    }

    @Test void pinningAContextDockIsANoop() {
        JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
        addPanel(panel, "_PARAMETERS", JIPipeDesktopDockPanel.PanelLocation.TopRight, true);
        panel.pinDock("_PARAMETERS");
        assertFalse(panel.isDockPinned("_PARAMETERS"));
    }

    @Test void pinningAnUnknownDockIsANoop() {
        JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
        panel.pinDock("NOPE");
        assertFalse(panel.isDockPinned("NOPE"));
    }

    @Test void movingClearsThePin() {
        JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
        addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
        panel.pinDock("TOOLS");
        panel.movePanelToLocation("TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopLeft, false);
        assertFalse(panel.isDockPinned("TOOLS"));
    }

    @Test void unpinningDoesNotCloseAnOpenDock() {
        JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
        addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
        panel.pinDock("TOOLS");
        panel.activatePanel("TOOLS", false);
        panel.unpinDock("TOOLS");
        assertFalse(panel.isDockPinned("TOOLS"));
        assertTrue(panel.getPanels().get("TOOLS").isVisible(), "unpinning must not close a user-opened dock");
    }

    @Test void pinsArePerPanelInstance() {
        JIPipeDesktopDockPanel first = new JIPipeDesktopDockPanel();
        JIPipeDesktopDockPanel second = new JIPipeDesktopDockPanel();
        addPanel(first, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
        addPanel(second, "FIND", JIPipeDesktopDockPanel.PanelLocation.TopLeft, false);
        first.pinDock("TOOLS");
        second.pinDock("FIND");
        assertNull(second.getPinnedDockId(JIPipeDesktopDockPanel.PinSide.Right));
        assertNull(first.getPinnedDockId(JIPipeDesktopDockPanel.PinSide.Left));
    }

    @Test void pinsRoundTripThroughThePanel() {
        JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
        addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
        panel.pinDock("TOOLS");
        JIPipeDesktopDockPanel reloaded = new JIPipeDesktopDockPanel();
        addPanel(reloaded, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
        reloaded.restoreState(panel.getCurrentState());
        assertEquals("TOOLS", reloaded.getPinnedDockId(JIPipeDesktopDockPanel.PinSide.Right));
    }

    @Test void pinnedDockBecomesTheSideStandIn() {
        JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
        addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
        panel.pinDock("TOOLS");
        assertEquals("TOOLS", panel.getStandInPanelId(JIPipeDesktopDockPanel.PinSide.Right));
        assertTrue(panel.isDockDisplayed("TOOLS"));
        assertFalse(panel.getPanels().get("TOOLS").isVisible(), "the stand-in must not flip the visible flag");
    }

    @Test void theStandInYieldsToAUserOpenedDock() {
        JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
        addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
        panel.pinDock("TOOLS");
        addPanel(panel, "_SLOT_MANAGER", JIPipeDesktopDockPanel.PanelLocation.TopRight, true);
        assertNull(panel.getStandInPanelId(JIPipeDesktopDockPanel.PinSide.Right));
        panel.removeDockPanelsIf(p -> p.getId().startsWith("_"));
        assertEquals("TOOLS", panel.getStandInPanelId(JIPipeDesktopDockPanel.PinSide.Right));
    }

    @Test void theStandInRespectsHideToolbars() {
        JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
        addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
        panel.pinDock("TOOLS");
        panel.setHideToolbars(true);
        assertNull(panel.getStandInPanelId(JIPipeDesktopDockPanel.PinSide.Right));
        panel.setHideToolbars(false);
        assertEquals("TOOLS", panel.getStandInPanelId(JIPipeDesktopDockPanel.PinSide.Right));
    }

    @Test void theStandInIsNotPersisted() {
        JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
        addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
        panel.pinDock("TOOLS");
        assertFalse(panel.getCurrentState().getVisibilities().getOrDefault("TOOLS", false));
        assertEquals("TOOLS", panel.getCurrentState().getPins().get("right"));
    }

    @Test void aPinForAnUnknownDockShowsNothing() {
        JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
        addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
        JIPipeDesktopDockPanel.State state = panel.getCurrentState();
        state.getPins().put("right", "NOPE");
        panel.restoreState(state);
        assertNull(panel.getStandInPanelId(JIPipeDesktopDockPanel.PinSide.Right));
    }

    @Test void theLeftAndBottomSidesHaveStandIns() {
        JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
        addPanel(panel, "FIND", JIPipeDesktopDockPanel.PanelLocation.TopLeft, false);
        addPanel(panel, "LOG", JIPipeDesktopDockPanel.PanelLocation.BottomBottom, false);
        panel.pinDock("FIND");
        panel.pinDock("LOG");
        assertEquals("FIND", panel.getStandInPanelId(JIPipeDesktopDockPanel.PinSide.Left));
        assertEquals("LOG", panel.getStandInPanelId(JIPipeDesktopDockPanel.PinSide.Bottom));
    }

    @Test void removingAndReaddingThePinnedDockSelfHeals() {
        JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
        addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
        panel.pinDock("TOOLS");
        panel.removeDockPanelsIf(p -> p.getId().equals("TOOLS"));
        assertNull(panel.getStandInPanelId(JIPipeDesktopDockPanel.PinSide.Right));
        addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
        assertEquals("TOOLS", panel.getStandInPanelId(JIPipeDesktopDockPanel.PinSide.Right));
    }

    @Test void closingTheStandInPausesTheSide() {
        JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
        addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
        panel.pinDock("TOOLS");
        panel.deactivatePanel("TOOLS", false);
        assertNull(panel.getStandInPanelId(JIPipeDesktopDockPanel.PinSide.Right));
    }

    @Test void aContextDockCycleReArmsTheStandIn() {
        JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
        addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
        panel.pinDock("TOOLS");
        panel.deactivatePanel("TOOLS", false);
        addPanel(panel, "_PARAMETERS", JIPipeDesktopDockPanel.PanelLocation.TopRight, true);
        panel.removeDockPanelsIf(p -> p.getId().startsWith("_"));
        assertEquals("TOOLS", panel.getStandInPanelId(JIPipeDesktopDockPanel.PinSide.Right));
    }

    @Test void closingAUserOpenedPinnedDockPausesTheSide() {
        JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
        addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
        panel.pinDock("TOOLS");
        panel.activatePanel("TOOLS", false);
        panel.deactivatePanel("TOOLS", false);
        assertNull(panel.getStandInPanelId(JIPipeDesktopDockPanel.PinSide.Right));
    }

    @Test void closingANonPinnedDockDoesNotPauseTheSide() {
        JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
        addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
        addPanel(panel, "ERRORS", JIPipeDesktopDockPanel.PanelLocation.BottomRight, true);
        panel.pinDock("TOOLS");
        panel.deactivatePanel("ERRORS", false);
        assertEquals("TOOLS", panel.getStandInPanelId(JIPipeDesktopDockPanel.PinSide.Right));
    }

    @Test void restoreStateClearsStandInPauses() {
        JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
        addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
        panel.pinDock("TOOLS");
        panel.deactivatePanel("TOOLS", false);
        panel.restoreState(panel.getCurrentState());
        assertEquals("TOOLS", panel.getStandInPanelId(JIPipeDesktopDockPanel.PinSide.Right));
    }

    @Test void contextDockPinsAreInertButRoundTrip() {
        JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
        addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
        JIPipeDesktopDockPanel.State state = panel.getCurrentState();
        state.getPins().put("right", "_RESULTS");
        panel.restoreState(state);
        assertNull(panel.getStandInPanelId(JIPipeDesktopDockPanel.PinSide.Right));
        assertEquals("_RESULTS", panel.getCurrentState().getPins().get("right"));
    }

    @Test void aLazyPinnedDockInstantiatesItsComponentForTheStandIn() {
        JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
        AtomicBoolean supplied = new AtomicBoolean(false);
        panel.addDockPanel("TOOLS", "Tools", null,
                JIPipeDesktopDockPanel.PanelLocation.TopRight, false, 0,
                () -> {
                    supplied.set(true);
                    return new JPanel();
                });
        panel.pinDock("TOOLS");
        assertTrue(supplied.get(), "the stand-in must instantiate a lazy component");
        assertNotNull(panel.getPanelComponent("TOOLS", JPanel.class));
        assertEquals("TOOLS", panel.getStandInPanelId(JIPipeDesktopDockPanel.PinSide.Right));
    }

    @Test void nullLocationEntriesFromHandEditedDocumentsDoNotBreakRegistration() {
        JIPipeDesktopDockPanel.State state = new JIPipeDesktopDockPanel.State();
        state.getLocations().put("TOOLS", null);
        JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
        panel.setSavedState(state);
        assertDoesNotThrow(() -> addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false));
        assertNull(panel.getPinnedDockId(JIPipeDesktopDockPanel.PinSide.Right),
                "a dock without a location cannot be pinned");
        assertDoesNotThrow(() -> panel.pinDock("TOOLS"));
    }

    @Test void movingWithinTheSameSideAlsoClearsThePin() {
        JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
        addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
        panel.pinDock("TOOLS");
        panel.movePanelToLocation("TOOLS", JIPipeDesktopDockPanel.PanelLocation.BottomRight, false);
        assertFalse(panel.isDockPinned("TOOLS"));
    }

    @Test void emptyPinsAreOmittedFromSerializedState() {
        String json = JsonUtils.toJsonString(new JIPipeDesktopDockPanel.State());
        assertFalse(json.contains("pins"), "a state without pins must not carry an empty pins object");
    }
}
