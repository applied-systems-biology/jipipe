package org.hkijena.acaq5.api;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;

import java.io.IOException;

@JsonSerialize(using = ACAQProjectSample.Serializer.class)
public class ACAQProjectSample implements Comparable<ACAQProjectSample> {
    private ACAQProject project;
    private ACAQAlgorithmGraph preprocessingGraph;

    public ACAQProjectSample(ACAQProject project) {
        this.project = project;
        this.preprocessingGraph = new ACAQAlgorithmGraph();

        initializePreprocessingGraph();
    }

    public ACAQProjectSample(ACAQProjectSample other) {
        this.project = other.project;
        this.preprocessingGraph = new ACAQAlgorithmGraph(other.preprocessingGraph);
    }

    private void initializePreprocessingGraph() {
        preprocessingGraph.insertNode(new ACAQPreprocessingOutput(getProject().getPreprocessingOutputConfiguration(),
                project.getPreprocessingTraitConfiguration()));
    }

    public ACAQProject getProject() {
        return project;
    }

    public String getName() {
        return project.getSamples().inverse().get(this);
    }

    @Override
    public int compareTo(ACAQProjectSample o) {
        return getName().compareTo(o.getName());
    }

    public ACAQAlgorithmGraph getPreprocessingGraph() {
        return preprocessingGraph;
    }

    public static class Serializer extends JsonSerializer<ACAQProjectSample> {
        @Override
        public void serialize(ACAQProjectSample acaqProjectSample, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectField("algorithm-graph", acaqProjectSample.preprocessingGraph);
            jsonGenerator.writeEndObject();
        }
    }

    /**
     * Deserializes the sample's content from JSON
     * @param node
     */
    public void fromJson(JsonNode node) {
        if(node.has("algorithm-graph")) {
            preprocessingGraph.fromJson(node.get("algorithm-graph"));
        }
    }
}
