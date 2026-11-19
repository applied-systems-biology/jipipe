package org.hkijena.jipipe.desktop.app.ui.input;

/**
 * Pure element-at-point resolution for the agent UI probe (Layer 2) with the cross-reference
 * to Layer 1 (the "aim right" rule): when a probed element has a semantic identity, the probe
 * names the Layer 1 operation that addresses it, so the agent prefers the deterministic path
 * over a raw coordinate click. Both mappings are tables over plain data (a canvas region or a
 * component class name chain), so they are testable without a display. The {@code ui_probe}
 * operation (Task 6) performs the actual hit-testing and feeds the results in here.
 */
public final class AgentElementProbe {

    private AgentElementProbe() {
    }

    /**
     * What a probed point IS, and which Layer 1 operation applies to it.
     *
     * @param kind                one of {@code canvas-node}, {@code canvas-slot},
     *                            {@code canvas-badge}, {@code component}, {@code dock}
     * @param detail              the node id for node regions, the slot name for {@code SLOT},
     *                            the dock id (the dock title) for a dock panel, the component
     *                            class name chain otherwise
     * @param suggestedOperation  the Layer 1 operation id to prefer, or null when no semantic
     *                            operation applies (the element is addressed by other means)
     * @param suggestedArguments  a JSON object string with the arguments for the suggested
     *                            operation, or null when there is no suggestion
     */
    public record ProbeHit(String kind, String detail, String suggestedOperation, String suggestedArguments) {
    }

    /**
     * Where a point falls within a picked node UI of the graph canvas.
     */
    public enum CanvasRegion {
        /**
         * The node's body (outside slots and badges).
         */
        NODE_BODY,
        /**
         * A slot's active area.
         */
        SLOT,
        /**
         * The node's run badge.
         */
        RUN_BADGE,
        /**
         * A node-internal UI area that is neither body, slot, nor badge.
         */
        INTERNAL
    }

    /**
     * The canvas suggestion table. A node is addressed by its id through the existing query and
     * parameter operations, so only the run badge names a Layer 1 operation ({@code run_node}).
     *
     * @param region   the canvas region under the point
     * @param nodeId   the id of the picked node
     * @param slotName the slot name when the region is {@link CanvasRegion#SLOT}, otherwise null
     * @return the probe hit for the region
     */
    public static ProbeHit suggestForCanvasRegion(CanvasRegion region, String nodeId, String slotName) {
        return switch (region) {
            case NODE_BODY -> new ProbeHit("canvas-node", nodeId, null, null);
            case SLOT -> new ProbeHit("canvas-slot", slotName, null, null);
            case RUN_BADGE -> new ProbeHit("canvas-badge", nodeId, "run_node", "{\"node\": \"" + nodeId + "\"}");
            case INTERNAL -> new ProbeHit("canvas-node", nodeId, null, null);
        };
    }

    /**
     * The component suggestion table (the dock and action cross-reference). The chain is the
     * {@link javax.swing.JComponent} class name chain from the probed component up to its window.
     * A dock panel is addressed by id via {@code set_dock_visible}; the dock id arrives here as
     * the accessible name. Buttons are candidates for {@code fire_action}, but the mapping from
     * button text to a fireable action id is a registry lookup done by the operation, so the
     * helper stays pure and suggests nothing.
     *
     * @param componentClassNameChain the class name chain of the probed component
     * @param accessibleName          the component's accessible name where available, otherwise null
     * @return the probe hit for the component
     */
    public static ProbeHit suggestForComponent(String componentClassNameChain, String accessibleName) {
        if (componentClassNameChain.contains("JIPipeDesktopDockPanel$Panel")) {
            return new ProbeHit("dock", accessibleName, "set_dock_visible",
                    "{\"dock-id\": \"" + accessibleName + "\"}");
        }
        if (componentClassNameChain.contains("JButton")
                || componentClassNameChain.contains("JToggleButton")
                || componentClassNameChain.contains("JMenuItem")) {
            return new ProbeHit("component", componentClassNameChain, null, null);
        }
        return new ProbeHit("component", componentClassNameChain, null, null);
    }
}