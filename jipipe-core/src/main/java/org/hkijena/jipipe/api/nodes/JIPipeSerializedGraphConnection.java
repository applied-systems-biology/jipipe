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

package org.hkijena.jipipe.api.nodes;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.UUID;

/**
 * Serialized form of a {@link JIPipeGraphConnection}
 */
public class JIPipeSerializedGraphConnection {

    private String sourceNodeUUID;
    private String targetNodeUUID;
    private String sourceSlotName;
    private String targetSlotName;
    private JIPipeGraphEdge edge;

    public JIPipeSerializedGraphConnection() {
    }

    public JIPipeSerializedGraphConnection(JIPipeDataSlot source, JIPipeDataSlot target, JIPipeGraphEdge edge) {
        this.sourceNodeUUID = StringUtils.nullToEmpty(source.getNode().getUUIDInParentGraph());
        this.targetNodeUUID = StringUtils.nullToEmpty(target.getNode().getUUIDInParentGraph());
        this.sourceSlotName = source.getName();
        this.targetSlotName = target.getName();
        this.edge = edge;
    }

    @JsonGetter("source-node")
    public String getSourceNodeUUID() {
        return sourceNodeUUID;
    }

    @JsonSetter("source-node")
    public void setSourceNodeUUID(String sourceNodeUUID) {
        this.sourceNodeUUID = sourceNodeUUID;
    }

    @JsonGetter("target-node")
    public String getTargetNodeUUID() {
        return targetNodeUUID;
    }

    @JsonSetter("target-node")
    public void setTargetNodeUUID(String targetNodeUUID) {
        this.targetNodeUUID = targetNodeUUID;
    }

    @JsonGetter("source-slot")
    public String getSourceSlotName() {
        return sourceSlotName;
    }

    @JsonSetter("source-slot")
    public void setSourceSlotName(String sourceSlotName) {
        this.sourceSlotName = sourceSlotName;
    }

    @JsonGetter("target-slot")
    public String getTargetSlotName() {
        return targetSlotName;
    }

    @JsonSetter("target-slot")
    public void setTargetSlotName(String targetSlotName) {
        this.targetSlotName = targetSlotName;
    }

    public JIPipeDataSlot findSourceSlot(JIPipeGraph graph) {
        JIPipeGraphNode sourceNode = graph.getNodeByUUID(UUID.fromString(sourceNodeUUID));
        if(sourceNode != null) {
            return sourceNode.getOutputSlot(sourceSlotName);
        }
        return null;
    }

    public JIPipeDataSlot findTargetSlot(JIPipeGraph graph) {
        JIPipeGraphNode targetNode = graph.getNodeByUUID(UUID.fromString(targetNodeUUID));
        if(targetNode != null) {
            return targetNode.getInputSlot(targetSlotName);
        }
        return null;
    }

    @JsonGetter("edge")
    public JIPipeGraphEdge getEdge() {
        return edge;
    }

    @JsonSetter("edge")
    public void setEdge(JIPipeGraphEdge edge) {
        this.edge = edge;
    }
}
