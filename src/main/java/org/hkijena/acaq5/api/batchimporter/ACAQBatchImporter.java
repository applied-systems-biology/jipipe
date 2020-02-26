package org.hkijena.acaq5.api.batchimporter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.acaq5.api.ACAQPreprocessingOutput;
import org.hkijena.acaq5.api.ACAQProject;
import org.hkijena.acaq5.api.ACAQRunnable;
import org.hkijena.acaq5.api.ACAQRunnerStatus;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmVisibility;
import org.hkijena.acaq5.api.batchimporter.algorithms.ACAQDataSourceFromFile;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.utils.GraphUtils;
import org.hkijena.acaq5.utils.JsonUtils;
import org.jgrapht.Graphs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

@JsonSerialize(using = ACAQBatchImporter.Serializer.class)
public class ACAQBatchImporter implements ACAQRunnable {
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

    @Override
    public void run(Consumer<ACAQRunnerStatus> onProgress, Supplier<Boolean> isCancelled) {

        // First detect which algorithms are part of data generation and which are part of data processing
        Set<ACAQAlgorithm> dataGenerationAlgorithms = new HashSet<>();


        for(ACAQAlgorithm algorithm : graph.getAlgorithmNodes().values()) {
            if(algorithm instanceof ACAQDataSourceFromFile) {
                // Assign project
                ((ACAQDataSourceFromFile) algorithm).setProject(project);

                for (ACAQDataSlot<?> outputSlot : algorithm.getOutputSlots()) {
                    for (ACAQDataSlot<?> predecessor : GraphUtils.getAllPredecessors(graph.getGraph(), outputSlot)) {
                        dataGenerationAlgorithms.add(predecessor.getAlgorithm());
                    }
                }
            }
        }

        // Generate samples by running only generation algorithms
        Set<ACAQAlgorithm> executedAlgorithms = new HashSet<>();
        for(ACAQDataSlot<?> slot : graph.traverse()) {
            if(isCancelled.get())
                return;
            if(!dataGenerationAlgorithms.contains(slot.getAlgorithm()))
                continue;
            if(slot.isInput()) {
                // Copy data from source
                ACAQDataSlot<?> sourceSlot = graph.getSourceSlot(slot);
                slot.setData(sourceSlot.getData());
            }
            else if(slot.isOutput()) {
                if(!executedAlgorithms.contains(slot.getAlgorithm())) {
                    slot.getAlgorithm().run();
                    executedAlgorithms.add(slot.getAlgorithm());
                }
            }
        }

        if(isCancelled.get())
            return;
        onProgress.accept(new ACAQRunnerStatus(1, 2, ""));
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
