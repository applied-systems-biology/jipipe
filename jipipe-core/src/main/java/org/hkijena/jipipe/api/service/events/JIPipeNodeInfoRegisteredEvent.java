package org.hkijena.jipipe.api.service.events;

import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;

/**
 * Triggered when an algorithm is registered
 */
public class JIPipeNodeInfoRegisteredEvent extends AbstractJIPipeEvent {

    private final JIPipeService registry;
    private final JIPipeNodeInfo nodeInfo;

    /**
     * @param registry event source
     * @param nodeInfo the algorithm type
     */
    public JIPipeNodeInfoRegisteredEvent(JIPipeService registry, JIPipeNodeInfo nodeInfo) {
        super(registry);
        this.registry = registry;
        this.nodeInfo = nodeInfo;
    }

    public JIPipeNodeInfo getNodeInfo() {
        return nodeInfo;
    }
}
