package org.hkijena.jipipe.desktop.app.ui.input;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AgentElementProbeTest {

    @Test
    void runBadgeSuggestsRunNodeWithTheNodeId() {
        AgentElementProbe.ProbeHit hit = AgentElementProbe.suggestForCanvasRegion(
                AgentElementProbe.CanvasRegion.RUN_BADGE, "abc-123", null);
        assertEquals("canvas-badge", hit.kind());
        assertEquals("run_node", hit.suggestedOperation());
        assertTrue(hit.suggestedArguments().contains("abc-123"));
    }

    @Test
    void slotReportsItsNameAndSuggestsNothing() {
        AgentElementProbe.ProbeHit hit = AgentElementProbe.suggestForCanvasRegion(
                AgentElementProbe.CanvasRegion.SLOT, "abc-123", "Input 1");
        assertEquals("canvas-slot", hit.kind());
        assertNull(hit.suggestedOperation());
        assertEquals("Input 1", hit.detail());
    }

    @Test
    void nodeBodySuggestsNothing() {
        assertNull(AgentElementProbe.suggestForCanvasRegion(
                AgentElementProbe.CanvasRegion.NODE_BODY, "abc-123", null).suggestedOperation());
    }

    @Test
    void dockPanelSuggestsSetDockVisible() {
        AgentElementProbe.ProbeHit hit = AgentElementProbe.suggestForComponent(
                "JIPipeDesktopDockPanel$Panel", "ADD_NODES");
        assertEquals("dock", hit.kind());
        assertEquals("set_dock_visible", hit.suggestedOperation());
        assertTrue(hit.suggestedArguments().contains("ADD_NODES"));
    }

    @Test
    void plainComponentSuggestsNothing() {
        assertNull(AgentElementProbe.suggestForComponent(
                "JIPipeDesktopGraphCanvasUI", "canvas").suggestedOperation());
    }
}