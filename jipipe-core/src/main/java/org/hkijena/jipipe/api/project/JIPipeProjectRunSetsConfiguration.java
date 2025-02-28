package org.hkijena.jipipe.api.project;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.run.JIPipeProjectRunSet;

import java.util.ArrayList;
import java.util.List;

public class JIPipeProjectRunSetsConfiguration {
    private List<JIPipeProjectRunSet> runSets = new ArrayList<>();

    @JsonGetter("run-sets")
    public List<JIPipeProjectRunSet> getRunSets() {
        return runSets;
    }

    @JsonSetter("run-sets")
    public void setRunSets(List<JIPipeProjectRunSet> runSets) {
        this.runSets = runSets;
    }
}
