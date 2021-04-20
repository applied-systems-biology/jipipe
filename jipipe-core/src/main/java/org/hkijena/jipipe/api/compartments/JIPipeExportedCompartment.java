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

package org.hkijena.jipipe.api.compartments;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.jipipe.api.JIPipeMetadata;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeCompartmentOutput;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.utils.JsonUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Exported {@link JIPipeProjectCompartment}
 */
@JsonSerialize(using = JIPipeExportedCompartment.Serializer.class)
@JsonDeserialize(using = JIPipeExportedCompartment.Deserializer.class)
public class JIPipeExportedCompartment {
    private JIPipeMetadata metadata = new JIPipeMetadata();
    private JIPipeGraph graph = new JIPipeGraph();

    /**
     * Creates a new instance
     */
    public JIPipeExportedCompartment() {
    }

    /**
     * Initializes the instance from a compartment
     *
     * @param compartment The compartment
     */
    public JIPipeExportedCompartment(JIPipeProjectCompartment compartment) {
        initializeGraphFromProject(compartment);
    }

    private void initializeGraphFromProject(JIPipeProjectCompartment compartment) {
        JIPipeGraph sourceGraph = compartment.getProject().getGraph();
        Map<String, JIPipeGraphNode> copies = new HashMap<>();
        String compartmentId = compartment.getProjectCompartmentId();
        for (JIPipeGraphNode algorithm : sourceGraph.getGraphNodes()) {
            if (!algorithm.getCompartment().equals(compartmentId))
                continue;
            JIPipeGraphNode copy = algorithm.getInfo().duplicate(algorithm);
            graph.insertNode(copy, copy.getCompartment());
            copies.put(algorithm.getIdInGraph(), copy);
        }
        for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> edge : sourceGraph.getSlotEdges()) {
            JIPipeGraphNode copySource = copies.getOrDefault(edge.getKey().getNode().getIdInGraph(), null);
            JIPipeGraphNode copyTarget = copies.getOrDefault(edge.getValue().getNode().getIdInGraph(), null);
            if (copySource == null || copyTarget == null)
                continue;
            if (!copySource.getCompartment().equals(compartmentId))
                continue;
            if (!copyTarget.getCompartment().equals(compartmentId))
                continue;
            graph.connect(copySource.getOutputSlotMap().get(edge.getKey().getName()),
                    copyTarget.getInputSlotMap().get(edge.getValue().getName()));
        }
        metadata.setName(compartment.getName());
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
    public JIPipeProjectCompartment addTo(JIPipeProject project, String compartmentName) {
        if (project.getCompartments().containsKey(compartmentName))
            throw new UserFriendlyRuntimeException("Compartment " + compartmentName + " already exists!",
                    "Compartment " + compartmentName + " already exists!",
                    "Exported compartment " + compartmentName, "There is already a graph compartment with that name.",
                    "Please choose another unique name.");
        JIPipeProjectCompartment compartment = project.addCompartment(compartmentName);
        compartmentName = compartment.getProjectCompartmentId();
        JIPipeCompartmentOutput projectOutputNode = compartment.getOutputNode();

        for (JIPipeGraphNode algorithm : graph.getGraphNodes()) {
            algorithm.setCompartment(compartmentName);
        }

        Map<String, JIPipeGraphNode> copies = new HashMap<>();
        for (JIPipeGraphNode algorithm : graph.getGraphNodes()) {
            if (algorithm instanceof JIPipeCompartmentOutput) {
                copies.put(algorithm.getIdInGraph(), projectOutputNode);

                // Copy the slot configuration over
                projectOutputNode.getSlotConfiguration().setTo(algorithm.getSlotConfiguration());
            } else {
                JIPipeGraphNode copy = algorithm.getInfo().duplicate(algorithm);
                project.getGraph().insertNode(copy, copy.getCompartment());
                copies.put(algorithm.getIdInGraph(), copy);
            }
        }
        for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> edge : graph.getSlotEdges()) {
            JIPipeGraphNode copySource = copies.get(edge.getKey().getNode().getIdInGraph());
            JIPipeGraphNode copyTarget = copies.get(edge.getValue().getNode().getIdInGraph());
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
    public JIPipeMetadata getMetadata() {
        return metadata;
    }

    /**
     * Sets the metadata
     *
     * @param metadata The metadata
     */
    public void setMetadata(JIPipeMetadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Serializes the compartment
     */
    public static class Serializer extends JsonSerializer<JIPipeExportedCompartment> {
        @Override
        public void serialize(JIPipeExportedCompartment exportedCompartment, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("jipipe:project-type", "compartment");
            jsonGenerator.writeObjectField("metadata", exportedCompartment.metadata);
            jsonGenerator.writeObjectField("graph", exportedCompartment.graph);
            jsonGenerator.writeEndObject();
        }
    }

    /**
     * Deserializes the compartment
     */
    public static class Deserializer extends JsonDeserializer<JIPipeExportedCompartment> {
        @Override
        public JIPipeExportedCompartment deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JIPipeExportedCompartment exportedCompartment = new JIPipeExportedCompartment();

            JsonNode node = jsonParser.readValueAsTree();
            exportedCompartment.graph.fromJson(node.get("graph"), new JIPipeValidityReport());
            if (node.has("metadata"))
                exportedCompartment.metadata = JsonUtils.getObjectMapper().readerFor(JIPipeMetadata.class).readValue(node.get("metadata"));

            return exportedCompartment;
        }
    }
}
