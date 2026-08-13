package org.hkijena.jipipe.api.instrumentation.pipeline_map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PipelineMap {
    private long version;
    private SegmentNode root;
    private List<SegmentEdge> edges = new ArrayList<>();

    public PipelineMap(SegmentNode root) {
        this(0, root);
    }

    @JsonCreator
    public PipelineMap(
            @JsonProperty("version") long version,
            @JsonProperty("root") SegmentNode root) {
        this.version = version;
        this.root = root;
    }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    public SegmentNode getRoot() { return root; }
    public void setRoot(SegmentNode root) { this.root = root; }

    public List<SegmentEdge> getEdges() { return edges; }
    public void setEdges(List<SegmentEdge> edges) { this.edges = edges; }

    public SegmentNode findSegment(String id) {
        if (root == null) return null;
        return findSegmentRecursive(root, id);
    }

    private static SegmentNode findSegmentRecursive(SegmentNode node, String id) {
        if (node.getId() != null && node.getId().equals(id)) return node;
        if (node.getChildren() != null) {
            for (SegmentNode child : node.getChildren()) {
                SegmentNode found = findSegmentRecursive(child, id);
                if (found != null) return found;
            }
        }
        return null;
    }
}
