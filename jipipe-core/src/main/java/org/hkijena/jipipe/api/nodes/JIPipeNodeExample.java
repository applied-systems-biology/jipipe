package org.hkijena.jipipe.api.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.utils.json.JsonUtils;

/**
 * Contains functions around a {@link org.hkijena.jipipe.api.JIPipeNodeTemplate} that stores an example of a node
 */
public class JIPipeNodeExample {
    private final JIPipeNodeTemplate nodeTemplate;
    private String nodeId;

    public JIPipeNodeExample(JIPipeNodeTemplate nodeTemplate) {
        this.nodeTemplate = nodeTemplate;
    }

    public String getNodeId() {
        if(nodeId == null) {
            try {
                JsonNode rootNode = JsonUtils.readFromString(nodeTemplate.getData(), JsonNode.class);
                JsonNode nodeList = rootNode.get("nodes");
                if(nodeList.size() != 1)
                    return null;
                JsonNode node = nodeList.fields().next().getValue();
                nodeId = node.get("jipipe:node-info-id").textValue();
            }
            catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
        return nodeId;
    }

    public JIPipeNodeTemplate getNodeTemplate() {
        return nodeTemplate;
    }
}
