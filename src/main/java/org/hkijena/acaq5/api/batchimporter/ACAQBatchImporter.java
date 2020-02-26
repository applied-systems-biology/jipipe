package org.hkijena.acaq5.api.batchimporter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.acaq5.api.ACAQPreprocessingOutput;
import org.hkijena.acaq5.api.ACAQProject;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmVisibility;
import org.hkijena.acaq5.utils.JsonUtils;

import java.io.IOException;
import java.nio.file.Path;

@JsonSerialize(using = ACAQBatchImporter.Serializer.class)
public class ACAQBatchImporter {
    private ACAQProject project;
    private ACAQAlgorithmGraph graph = new ACAQAlgorithmGraph(ACAQAlgorithmVisibility.BatchImporterOnly);

    public ACAQBatchImporter(ACAQProject project) {
        this.project = project;
        initialize();
    }

    private void initialize() {
        graph.insertNode(new ACAQPreprocessingOutput(project.getPreprocessingOutputConfiguration(),
                project.getPreprocessingTraitConfiguration()));
    }

    public ACAQAlgorithmGraph getGraph() {
        return graph;
    }

    public void saveAs(Path path) throws IOException {
        ObjectMapper mapper = JsonUtils.getObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), this);
    }

    public void loadFrom(Path fileName) throws IOException {
        fromJson(JsonUtils.getObjectMapper().readerFor(JsonNode.class).readValue(fileName.toFile()));
    }

    public void fromJson(JsonNode node) {
        JsonNode projectTypeNode = node.path("acaq:project-type");
        if(projectTypeNode.isMissingNode() || !projectTypeNode.asText().equals("batch-importer"))
            throw new IllegalArgumentException("The JSON data does not contain data for a batch-importer!");
        graph.clear();
        initialize();
        graph.fromJson(node.get("algorithm-graph"));
    }

    public static class Serializer extends JsonSerializer<ACAQBatchImporter> {
        @Override
        public void serialize(ACAQBatchImporter project, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("acaq:project-type", "batch-importer");
            jsonGenerator.writeObjectField("algorithm-graph", project.graph);
            jsonGenerator.writeEndObject();
        }
    }
}
