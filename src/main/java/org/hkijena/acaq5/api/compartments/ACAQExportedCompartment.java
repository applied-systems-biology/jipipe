/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.acaq5.api.compartments;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.acaq5.api.ACAQMetadata;
import org.hkijena.acaq5.api.ACAQProject;
import org.hkijena.acaq5.api.algorithm.ACAQGraph;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQCompartmentOutput;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.acaq5.utils.JsonUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Exported {@link ACAQProjectCompartment}
 */
@JsonSerialize(using = ACAQExportedCompartment.Serializer.class)
@JsonDeserialize(using = ACAQExportedCompartment.Deserializer.class)
public class ACAQExportedCompartment {
    private ACAQMetadata metadata = new ACAQMetadata();
    private ACAQGraph graph = new ACAQGraph();

    /**
     * Creates a new instance
     */
    public ACAQExportedCompartment() {
    }

    /**
     * Initializes the instance from a compartment
     *
     * @param compartment The compartment
     */
    public ACAQExportedCompartment(ACAQProjectCompartment compartment) {
        initializeGraphFromProject(compartment);
    }

    private void initializeGraphFromProject(ACAQProjectCompartment compartment) {
        ACAQGraph sourceGraph = compartment.getProject().getGraph();
        Map<String, ACAQGraphNode> copies = new HashMap<>();
        String compartmentId = compartment.getProjectCompartmentId();
        for (ACAQGraphNode algorithm : sourceGraph.getAlgorithmNodes().values()) {
            if (!algorithm.getCompartment().equals(compartmentId))
                continue;
            ACAQGraphNode copy = algorithm.getDeclaration().clone(algorithm);
            graph.insertNode(copy, copy.getCompartment());
            copies.put(algorithm.getIdInGraph(), copy);
        }
        for (Map.Entry<ACAQDataSlot, ACAQDataSlot> edge : sourceGraph.getSlotEdges()) {
            ACAQGraphNode copySource = copies.get(edge.getKey().getNode().getIdInGraph());
            ACAQGraphNode copyTarget = copies.get(edge.getValue().getNode().getIdInGraph());
            if (!copySource.getCompartment().equals(compartmentId))
                continue;
            if (!copyTarget.getCompartment().equals(compartmentId))
                continue;
            graph.connect(copySource.getOutputSlotMap().get(edge.getKey().getName()),
                    copyTarget.getInputSlotMap().get(edge.getValue().getName()));
        }
    }

    /**
     * @return A name suggested automatically
     */
    public String getSuggestedName() {
        if (metadata.getName() != null && !metadata.getName().trim().isEmpty())
            return metadata.getName();
        else
            return "Compartment";
    }

    /**
     * Adds the compartment to a project as the specified compartment name
     *
     * @param project         Target project
     * @param compartmentName Target compartment name
     * @return the compartment instance
     */
    public ACAQProjectCompartment addTo(ACAQProject project, String compartmentName) {
        if (project.getCompartments().containsKey(compartmentName))
            throw new UserFriendlyRuntimeException("Compartment " + compartmentName + " already exists!",
                    "Compartment " + compartmentName + " already exists!",
                    "Exported compartment " + compartmentName, "There is already a graph compartment with that name.",
                    "Please choose another unique name.");
        ACAQProjectCompartment compartment = project.addCompartment(compartmentName);
        compartmentName = compartment.getProjectCompartmentId();
        ACAQCompartmentOutput projectOutputNode = compartment.getOutputNode();

        for (ACAQGraphNode algorithm : graph.getAlgorithmNodes().values()) {
            algorithm.setCompartment(compartmentName);
        }

        Map<String, ACAQGraphNode> copies = new HashMap<>();
        for (ACAQGraphNode algorithm : graph.getAlgorithmNodes().values()) {
            if (algorithm instanceof ACAQCompartmentOutput) {
                copies.put(algorithm.getIdInGraph(), projectOutputNode);

                // Copy the slot configuration over
                projectOutputNode.getSlotConfiguration().setTo(algorithm.getSlotConfiguration());
            } else {
                ACAQGraphNode copy = algorithm.getDeclaration().clone(algorithm);
                project.getGraph().insertNode(copy, copy.getCompartment());
                copies.put(algorithm.getIdInGraph(), copy);
            }
        }
        for (Map.Entry<ACAQDataSlot, ACAQDataSlot> edge : graph.getSlotEdges()) {
            ACAQGraphNode copySource = copies.get(edge.getKey().getNode().getIdInGraph());
            ACAQGraphNode copyTarget = copies.get(edge.getValue().getNode().getIdInGraph());
            project.getGraph().connect(copySource.getOutputSlotMap().get(edge.getKey().getName()),
                    copyTarget.getInputSlotMap().get(edge.getValue().getName()));
        }
        return compartment;
    }

    /**
     * Saves the exported compartment to JSON
     *
     * @param fileName The JSON filename
     * @throws IOException Triggered by ObjectMapper
     */
    public void saveToJson(Path fileName) throws IOException {
        JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(fileName.toFile(), this);
    }

    /**
     * @return Metadata
     */
    public ACAQMetadata getMetadata() {
        return metadata;
    }

    /**
     * Sets the metadata
     *
     * @param metadata The metadata
     */
    public void setMetadata(ACAQMetadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Serializes the compartment
     */
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

    /**
     * Deserializes the compartment
     */
    public static class Deserializer extends JsonDeserializer<ACAQExportedCompartment> {
        @Override
        public ACAQExportedCompartment deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            ACAQExportedCompartment exportedCompartment = new ACAQExportedCompartment();

            JsonNode node = jsonParser.readValueAsTree();
            exportedCompartment.graph.fromJson(node.get("graph"));
            if (node.has("metadata"))
                exportedCompartment.metadata = JsonUtils.getObjectMapper().readerFor(ACAQMetadata.class).readValue(node.get("metadata"));

            return exportedCompartment;
        }
    }
}
