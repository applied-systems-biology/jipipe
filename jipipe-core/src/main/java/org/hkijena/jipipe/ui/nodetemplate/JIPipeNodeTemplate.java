package org.hkijena.jipipe.ui.nodetemplate;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.extensions.parameters.collections.ListParameter;
import org.hkijena.jipipe.extensions.parameters.pairs.StringAndStringPairParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.utils.json.JsonUtils;

/**
 * Contains the JSON data of a node that can be created by a user for sharing
 * An intermediate between copying a node and a proper plugin.
 */
public class JIPipeNodeTemplate {
    private String name = "Unnamed template";
    private HTMLText description = new HTMLText();
    private String data;
    private JIPipeNodeInfo nodeInfo;

    public JIPipeNodeTemplate() {
    }

    public JIPipeNodeTemplate(JIPipeNodeTemplate other) {
        this.name = other.name;
        this.description = new HTMLText(other.description);
        this.data = other.data;
        this.nodeInfo = other.nodeInfo;
    }

    @JsonGetter("name")
    public String getName() {
        return name;
    }

    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonGetter("description")
    public HTMLText getDescription() {
        return description;
    }

    @JsonSetter("description")
    public void setDescription(HTMLText description) {
        this.description = description;
    }

    @JsonGetter("data")
    public String getData() {
        return data;
    }

    @JsonSetter("data")
    public void setData(String data) {
        this.data = data;
    }

    /**
     * Gets the node type stored inside the data.
     * Returns null if the node type could not be found.
     * @return the node type or null if it could not be found
     */
    public JIPipeNodeInfo getNodeInfo() {
        if(nodeInfo == null) {
            try {
                JsonNode node = JsonUtils.readFromString(data, JsonNode.class);
                String nodeInfoId = node.get("jipipe:node-info-id").asText();
                nodeInfo = JIPipe.getNodes().getInfoById(nodeInfoId);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return nodeInfo;
    }

    public static class List extends ListParameter<JIPipeNodeTemplate> {
        /**
         * Creates a new instance
         */
        public List() {
            super(JIPipeNodeTemplate.class);
        }

        /**
         * Creates a copy
         *
         * @param other the original
         */
        public List(JIPipeNodeTemplate.List other) {
            super(JIPipeNodeTemplate.class);
            for (JIPipeNodeTemplate template : other) {
                add(new JIPipeNodeTemplate(template));
            }
        }
    }
}
