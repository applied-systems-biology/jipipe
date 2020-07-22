/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.events;

import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.*;

/**
 * Triggered when an algorithm is registered
 */
public class NodeInfoRegisteredEvent {
    private JIPipeNodeInfo nodeInfo;

    /**
     * @param nodeInfo the algorithm type
     */
    public NodeInfoRegisteredEvent(JIPipeNodeInfo nodeInfo) {
        this.nodeInfo = nodeInfo;
    }

    public JIPipeNodeInfo getNodeInfo() {
        return nodeInfo;
    }
}
