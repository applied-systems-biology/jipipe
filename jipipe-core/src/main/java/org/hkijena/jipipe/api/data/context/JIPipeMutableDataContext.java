package org.hkijena.jipipe.api.data.context;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class JIPipeMutableDataContext implements JIPipeDataContext {
    private String id;
    private Set<String> predecessors = new HashSet<>();

    public JIPipeMutableDataContext() {
        this.id = "/" + UUID.randomUUID();
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
