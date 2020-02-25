package org.hkijena.acaq5.api.batchimporter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
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
