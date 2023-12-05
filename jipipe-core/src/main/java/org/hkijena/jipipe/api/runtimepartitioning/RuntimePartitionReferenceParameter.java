package org.hkijena.jipipe.api.runtimepartitioning;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

public class RuntimePartitionReferenceParameter {
    private int index = 0;

    public RuntimePartitionReferenceParameter() {
    }

    public RuntimePartitionReferenceParameter(RuntimePartitionReferenceParameter other) {
        this.index = other.index;
    }

    @JsonGetter("index")
    public int getIndex() {
        return index;
    }
    @JsonSetter("index")
    public void setIndex(int index) {
        this.index = index;
    }
}
