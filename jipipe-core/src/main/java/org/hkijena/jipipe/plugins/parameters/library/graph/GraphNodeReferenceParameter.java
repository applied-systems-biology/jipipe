package org.hkijena.jipipe.plugins.parameters.library.graph;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.UUID;

public class GraphNodeReferenceParameter {
    private String nodeUUID;

    public GraphNodeReferenceParameter() {
    }

    public GraphNodeReferenceParameter(GraphNodeReferenceParameter other) {
        this.nodeUUID = other.nodeUUID;
    }

    public GraphNodeReferenceParameter(JIPipeGraphNode node) {
        this.nodeUUID = node.getUUIDInParentGraph().toString();
    }

    @JsonGetter("node-uuid")
    public String getNodeUUID() {
        return nodeUUID;
    }

    @JsonSetter("node-uuid")
    public void setNodeUUID(String nodeUUID) {
        this.nodeUUID = nodeUUID;
    }

    public JIPipeGraphNode resolve(JIPipeProject project) {
        if(StringUtils.isNullOrEmpty(nodeUUID)) {
            return null;
        }
        return project.getGraph().getNodeByUUID(UUID.fromString(nodeUUID));
    }

    public boolean isSet() {
        return !StringUtils.isNullOrEmpty(nodeUUID);
    }

    public static class List extends ListParameter<GraphNodeReferenceParameter> {
        public List() {
            super(GraphNodeReferenceParameter.class);
        }

        public List(List other) {
            super(GraphNodeReferenceParameter.class);
            for (GraphNodeReferenceParameter parameter : other) {
                add(new GraphNodeReferenceParameter(parameter));
            }
        }
    }
}
