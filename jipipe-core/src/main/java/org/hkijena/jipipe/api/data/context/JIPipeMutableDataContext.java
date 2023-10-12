package org.hkijena.jipipe.api.data.context;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class JIPipeMutableDataContext implements JIPipeDataContext {
    private String source;
    private String id;
    private Set<String> predecessors = new HashSet<>();

    public JIPipeMutableDataContext() {
        this.id = "local://" + UUID.randomUUID();
    }
    public JIPipeMutableDataContext(String source) {
        this.source = source;
        this.id = "local://" + UUID.randomUUID();
    }

    public JIPipeMutableDataContext(JIPipeGraphNode source) {
        this(source.getUUIDInParentGraph() != null ? source.getUUIDInParentGraph().toString() : null);
    }

    @JsonGetter("source")
    @Override
    public String getSource() {
        return source;
    }

    @JsonSetter("source")
    public void setSource(String source) {
        this.source = source;
    }

    @Override
    @JsonGetter("id")
    public String getId() {
        return id;
    }

    @JsonSetter("id")
    public void setId(String id) {
        this.id = id;
    }

    @Override
    @JsonGetter("predecessors")
    public Set<String> getPredecessors() {
        return predecessors;
    }

    @JsonSetter("predecessors")
    public void setPredecessors(Set<String> predecessors) {
        this.predecessors = predecessors;
    }

    public void addPredecessor(JIPipeDataContext predecessorContext) {
        predecessors.add(predecessorContext.getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JIPipeMutableDataContext that = (JIPipeMutableDataContext) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
