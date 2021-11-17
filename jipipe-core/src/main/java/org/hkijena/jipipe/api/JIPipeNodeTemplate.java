package org.hkijena.jipipe.api;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.grouping.NodeGroup;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.collections.ListParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.util.Objects;

/**
 * Contains the JSON data of a node that can be created by a user for sharing
 * An intermediate between copying a node and a proper plugin.
 */
public class JIPipeNodeTemplate implements JIPipeParameterCollection {
    private final EventBus eventBus = new EventBus();
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

    @JIPipeDocumentation(name = "Name", description = "Name of the template")
    @JIPipeParameter(value = "name", uiOrder = -100)
    @JsonGetter("name")
    public String getName() {
        return name;
    }

    @JsonSetter("name")
    @JIPipeParameter("name")
    public void setName(String name) {
        this.name = name;
    }

    @JIPipeDocumentation(name = "Description", description = "A custom description")
    @JIPipeParameter(value = "description", uiOrder = -90)
    @JsonGetter("description")
    public HTMLText getDescription() {
        return description;
    }

    @JsonSetter("description")
    @JIPipeParameter("description")
    public void setDescription(HTMLText description) {
        this.description = description;
    }

    @JIPipeDocumentation(name = "Data", description = "The data contained inside the node template. Must be JSON representation of a single node. " +
            "Please note that copying a node from the graph yields a list of multiple nodes.")
    @JsonGetter("data")
    @JIPipeParameter("data")
    @StringParameterSettings(monospace = true, multiline = true)
    public String getData() {
        return data;
    }

    @JsonSetter("data")
    @JIPipeParameter("data")
    public void setData(String data) {
        this.nodeInfo = null;
        this.data = data;
    }

    @JIPipeDocumentation(name = "Paste from clipboard", description = "Sets the node data from clipboard.")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/edit-paste.png", iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/edit-paste.png")
    public void pasteDataFromClipboard(JIPipeWorkbench workbench) {
        String json = UIUtils.getStringFromClipboard();
        if (json != null) {
            try {
                JIPipeGraph graph = JsonUtils.getObjectMapper().readValue(json, JIPipeGraph.class);
                if(!graph.getGraphNodes().isEmpty()) {
                    if(graph.getGraphNodes().size() == 1) {
                        setData(JsonUtils.toPrettyJsonString(graph.getGraphNodes().iterator().next()));
                        triggerParameterChange("data");
                    }
                    else {
                        // Group them up
                        NodeGroup group = new NodeGroup(graph, true, false, true);
                        setData(JsonUtils.toPrettyJsonString(group));
                        triggerParameterChange("data");
                    }
                }
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
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
            }
        }
        return nodeInfo;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    public JIPipeGraphNode newInstance() {
        try {
            JsonNode node = JsonUtils.readFromString(data, JsonNode.class);
            return JIPipeGraphNode.fromJsonNode(node, new JIPipeIssueReport());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JIPipeNodeTemplate template = (JIPipeNodeTemplate) o;
        return Objects.equals(name, template.name) && Objects.equals(description, template.description) && Objects.equals(data, template.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, data);
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
