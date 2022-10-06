package org.hkijena.jipipe.api.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.util.Set;

/**
 * Contains functions around a {@link org.hkijena.jipipe.api.JIPipeNodeTemplate} that stores an example of a node
 */
public class JIPipeNodeExample {
    private final JIPipeNodeTemplate nodeTemplate;
    private String nodeId;

    private boolean nodeIdIdentified;

    private JIPipeNodeInfo cachedNodeInfo;

    private String sourceInfo;

    public JIPipeNodeExample(JIPipeNodeTemplate nodeTemplate) {
        this.nodeTemplate = nodeTemplate;
    }

    public JIPipeNodeInfo getNodeInfo() {
        if (cachedNodeInfo != null)
            return cachedNodeInfo;
        if (getNodeId() != null) {
            cachedNodeInfo = JIPipe.getNodes().getInfoById(getNodeId());
        }
        return cachedNodeInfo;
    }

    public String getNodeId() {
        if (!nodeIdIdentified) {
            if (nodeTemplate.getGraph() == null) {
                try {
                    JsonNode rootNode = JsonUtils.readFromString(nodeTemplate.getData(), JsonNode.class);
                    JsonNode nodeList = rootNode.get("nodes");
                    if (nodeList.size() != 1) {
                        nodeIdIdentified = true;
                        return null;
                    }
                    JsonNode node = nodeList.fields().next().getValue();
                    if (node == null) {
                        nodeIdIdentified = true;
                        return null;
                    }
                    JsonNode node1 = node.path("jipipe:node-info-id");
                    if (node1.isMissingNode()) {
                        nodeIdIdentified = true;
                        return null;
                    }
                    nodeId = node1.textValue();
                } catch (Throwable throwable) {
                }
            } else {
                Set<JIPipeGraphNode> graphNodes = nodeTemplate.getGraph().getGraphNodes();
                if (graphNodes.size() != 1) {
                    nodeIdIdentified = true;
                    return null;
                }
                nodeId = graphNodes.iterator().next().getInfo().getId();
                nodeIdIdentified = true;
            }
        }
        return nodeId;
    }

    public JIPipeNodeTemplate getNodeTemplate() {
        return nodeTemplate;
    }

    /**
     * An additional info label used by the UI.
     * Not serialized
     *
     * @return the info
     */
    public String getSourceInfo() {
        return sourceInfo;
    }

    public void setSourceInfo(String sourceInfo) {
        this.sourceInfo = sourceInfo;
    }
}
