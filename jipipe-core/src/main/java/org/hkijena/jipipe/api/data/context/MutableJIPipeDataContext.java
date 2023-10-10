package org.hkijena.jipipe.api.data.context;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MutableJIPipeDataContext implements JIPipeDataContext {
    private String id;
    private Set<String> predecessors = new HashSet<>();

    public MutableJIPipeDataContext() {
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
}
