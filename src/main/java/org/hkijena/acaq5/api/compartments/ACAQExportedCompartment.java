package org.hkijena.acaq5.api.compartments;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.acaq5.api.ACAQProject;
import org.hkijena.acaq5.api.ACAQProjectMetadata;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQCompartmentOutput;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.utils.JsonUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@JsonSerialize(using = ACAQExportedCompartment.Serializer.class)
@JsonDeserialize(using = ACAQExportedCompartment.Deserializer.class)
public class ACAQExportedCompartment {
    private ACAQProjectMetadata metadata = new ACAQProjectMetadata();
    private ACAQAlgorithmGraph graph = new ACAQAlgorithmGraph();

    public ACAQExportedCompartment() {
    }

    public ACAQExportedCompartment(ACAQProjectCompartment compartment) {
        initializeGraphFromProject(compartment);
    }

    private void initializeGraphFromProject(ACAQProjectCompartment compartment) {
        ACAQAlgorithmGraph sourceGraph = compartment.getProject().getGraph();
        Map<String, ACAQAlgorithm> copies = new HashMap<>();
        String compartmentId = compartment.getProjectCompartmentId();
        for (ACAQAlgorithm algorithm : sourceGraph.getAlgorithmNodes().values()) {
            if (!algorithm.getCompartment().equals(compartmentId))
                continue;
            ACAQAlgorithm copy = algorithm.getDeclaration().clone(algorithm);
            graph.insertNode(copy, copy.getCompartment());
            copies.put(algorithm.getIdInGraph(), copy);
        }
        for (Map.Entry<ACAQDataSlot, ACAQDataSlot> edge : sourceGraph.getSlotEdges()) {
            ACAQAlgorithm copySource = copies.get(edge.getKey().getAlgorithm().getIdInGraph());
            ACAQAlgorithm copyTarget = copies.get(edge.getValue().getAlgorithm().getIdInGraph());
            if (!copySource.getCompartment().equals(compartmentId))
                continue;
            if (!copyTarget.getCompartment().equals(compartmentId))
                continue;
            graph.connect(copySource.getSlots().get(edge.getKey().getName()), copyTarget.getSlots().get(edge.getValue().getName()));
        }
    }

    public String getSuggestedName() {
        if (metadata.getName() != null && !metadata.getName().trim().isEmpty())
            return metadata.getName();
        else
            return "Compartment";
    }

    public void addTo(ACAQProject project, String compartmentName) {
        if (project.getCompartments().containsKey(compartmentName))
            throw new RuntimeException("Compartment " + compartmentName + " already exists!");
        ACAQProjectCompartment compartment = project.addCompartment(compartmentName);
        compartmentName = compartment.getProjectCompartmentId();
        ACAQCompartmentOutput projectOutputNode = compartment.getOutputNode();

        for (ACAQAlgorithm algorithm : graph.getAlgorithmNodes().values()) {
            algorithm.setCompartment(compartmentName);
        }

        Map<String, ACAQAlgorithm> copies = new HashMap<>();
        for (ACAQAlgorithm algorithm : graph.getAlgorithmNodes().values()) {
            if (algorithm instanceof ACAQCompartmentOutput) {
                copies.put(algorithm.getIdInGraph(), projectOutputNode);

                // Copy the slot configuration over
                projectOutputNode.getSlotConfiguration().setTo(algorithm.getSlotConfiguration());
            } else {
                ACAQAlgorithm copy = algorithm.getDeclaration().clone(algorithm);
                project.getGraph().insertNode(copy, copy.getCompartment());
                copies.put(algorithm.getIdInGraph(), copy);
            }
        }
        for (Map.Entry<ACAQDataSlot, ACAQDataSlot> edge : graph.getSlotEdges()) {
            ACAQAlgorithm copySource = copies.get(edge.getKey().getAlgorithm().getIdInGraph());
            ACAQAlgorithm copyTarget = copies.get(edge.getValue().getAlgorithm().getIdInGraph());
            project.getGraph().connect(copySource.getSlots().get(edge.getKey().getName()), copyTarget.getSlots().get(edge.getValue().getName()));
        }
    }

    public void saveToJson(Path fileName) throws IOException {
        JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(fileName.toFile(), this);
    }

    public ACAQProjectMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ACAQProjectMetadata metadata) {
        this.metadata = metadata;
    }

    public static class Serializer extends JsonSerializer<ACAQExportedCompartment> {
        @Override
        public void serialize(ACAQExportedCompartment exportedCompartment, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("acaq:project-type", "compartment");
            jsonGenerator.writeObjectField("metadata", exportedCompartment.metadata);
            jsonGenerator.writeObjectField("graph", exportedCompartment.graph);
            jsonGenerator.writeEndObject();
        }
    }

    public static class Deserializer extends JsonDeserializer<ACAQExportedCompartment> {
        @Override
        public ACAQExportedCompartment deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            ACAQExportedCompartment exportedCompartment = new ACAQExportedCompartment();

            JsonNode node = jsonParser.readValueAsTree();
            exportedCompartment.graph.fromJson(node.get("graph"));
            if (node.has("metadata"))
                exportedCompartment.metadata = JsonUtils.getObjectMapper().readerFor(ACAQProjectMetadata.class).readValue(node.get("metadata"));

            return exportedCompartment;
        }
    }
}
