package org.hkijena.jipipe.api.instrumentation.pipeline_map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SegmentNode {
    private String id;
    private String label;
    private String summary;
    private SegmentStatus status = SegmentStatus.UNKNOWN;
    private String compartmentId;
    private List<String> nodeIds = new ArrayList<>();
    private List<SegmentNode> children;

    public SegmentNode() {
    }

    @JsonCreator
    public SegmentNode(
            @JsonProperty("id") String id,
            @JsonProperty("label") String label) {
        this.id = id;
        this.label = label;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public SegmentStatus getStatus() { return status; }
    public void setStatus(SegmentStatus status) { this.status = status; }

    public String getCompartmentId() { return compartmentId; }
    public void setCompartmentId(String compartmentId) { this.compartmentId = compartmentId; }

    public List<String> getNodeIds() { return nodeIds; }
    public void setNodeIds(List<String> nodeIds) { this.nodeIds = nodeIds; }

    public List<SegmentNode> getChildren() { return children; }
    public void setChildren(List<SegmentNode> children) { this.children = children; }
}
