package org.hkijena.jipipe.api.runtimepartitioning;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuntimePartitionReferenceParameter that = (RuntimePartitionReferenceParameter) o;
        return index == that.index;
    }

    @Override
    public int hashCode() {
        return Objects.hash(index);
    }
}
