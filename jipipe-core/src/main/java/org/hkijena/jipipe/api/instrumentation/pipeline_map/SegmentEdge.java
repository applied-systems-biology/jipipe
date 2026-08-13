package org.hkijena.jipipe.api.instrumentation.pipeline_map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SegmentEdge {
    private final String source;
    private final String target;
    private final String label;

    @JsonCreator
    public SegmentEdge(
            @JsonProperty("source") String source,
            @JsonProperty("target") String target,
            @JsonProperty("label") String label) {
        this.source = source;
        this.target = target;
        this.label = label;
    }

    public String getSource() { return source; }
    public String getTarget() { return target; }
    public String getLabel() { return label; }
}
