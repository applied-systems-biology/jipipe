/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.compartments;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeMetadata;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.compartments.algorithms.IOInterfaceAlgorithm;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeCompartmentOutput;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Exported {@link JIPipeProjectCompartment}
 */
@JsonSerialize(using = JIPipeExportedCompartment.Serializer.class)
@JsonDeserialize(using = JIPipeExportedCompartment.Deserializer.class)
public class JIPipeExportedCompartment {
    private final JIPipeGraph outputGraph = new JIPipeGraph();
    private JIPipeMetadata metadata = new JIPipeMetadata();

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
        JIPipeGraph sourceGraph = compartment.getRuntimeProject().getGraph();
        Map<UUID, JIPipeGraphNode> copies = new HashMap<>();
        UUID compartmentId = compartment.getProjectCompartmentUUID();
        for (JIPipeGraphNode algorithm : sourceGraph.getGraphNodes()) {
            if (!Objects.equals(algorithm.getCompartmentUUIDInParentGraph(), compartmentId)) {
                if (algorithm instanceof JIPipeCompartmentOutput) {
                    boolean found = false;
                    for (JIPipeOutputDataSlot outputSlot : algorithm.getOutputSlots()) {
                        for (JIPipeDataSlot targetSlot : sourceGraph.getOutputOutgoingTargetSlots(outputSlot)) {
                            if (Objects.equals(targetSlot.getNode().getCompartmentUUIDInParentGraph(), compartmentId)) {

                                // Special case: node is IOInterface and has same slots
                                if (targetSlot.getNode() instanceof IOInterfaceAlgorithm &&
                                        targetSlot.getNode().getInputSlotMap().keySet().equals(algorithm.getOutputSlotMap().keySet())) {
                                    continue;
                                }

                                found = true;
                            }
                        }
                    }
                    if (!found) {
                        continue;
                    }

                    // Convert into IO interface
                    IOInterfaceAlgorithm ioInterfaceAlgorithm = JIPipe.createNode(IOInterfaceAlgorithm.class);
                    ioInterfaceAlgorithm.setCustomName(algorithm.getName());

                    // Copy the slot configs
                    ioInterfaceAlgorithm.getSlotConfiguration().setTo(algorithm.getSlotConfiguration());

                    // Copy the location
                    Map<String, Point> pointMap = algorithm.getLocations().get(compartmentId.toString());
                    if (pointMap != null) {
                        ioInterfaceAlgorithm.getLocations().put(compartmentId.toString(), pointMap);
                    }

                    outputGraph.insertNode(ioInterfaceAlgorithm);
                    copies.put(algorithm.getUUIDInParentGraph(), ioInterfaceAlgorithm);
                }
            } else {
                JIPipeGraphNode copy = algorithm.getInfo().duplicate(algorithm);
                outputGraph.insertNode(copy);
                copies.put(algorithm.getUUIDInParentGraph(), copy);

                copy.getLocations().clear();
                Map<String, Point> pointMap = algorithm.getLocations().get(compartmentId.toString());
                if (pointMap != null) {
                    copy.getLocations().put(compartmentId.toString(), pointMap);
                }
            }
        }
        for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> edge : sourceGraph.getSlotEdges()) {
            JIPipeGraphNode copySource = copies.getOrDefault(edge.getKey().getNode().getUUIDInParentGraph(), null);
            JIPipeGraphNode copyTarget = copies.getOrDefault(edge.getValue().getNode().getUUIDInParentGraph(), null);
            if (copySource == null || copyTarget == null)
                continue;
            outputGraph.connect(copySource.getOutputSlotMap().get(edge.getKey().getName()),
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
        JIPipeProjectCompartment compartment = project.addCompartment(compartmentName);
        JIPipeCompartmentOutput projectOutputNode = compartment.getOutputNode();

        String locationCompartment = "";
        for (JIPipeGraphNode algorithm : outputGraph.getGraphNodes()) {
            if (!(algorithm instanceof JIPipeCompartmentOutput)) {
                if (!algorithm.getLocations().keySet().isEmpty())
                    locationCompartment = algorithm.getLocations().keySet().iterator().next();
            }
        }

        Map<JIPipeGraphNode, Map<String, Point>> locations = new HashMap<>();
        Map<String, Point> outputLocation = null;
        UUID compartmentUUID = compartment.getProjectCompartmentUUID();
        for (JIPipeGraphNode algorithm : outputGraph.getGraphNodes()) {
            outputGraph.setCompartment(algorithm.getUUIDInParentGraph(), compartmentUUID);
            Map<String, Point> map = algorithm.getLocations().getOrDefault(locationCompartment, null);
            if (map != null) {
                locations.put(algorithm, map);
            }
            if (algorithm instanceof JIPipeCompartmentOutput) {
                outputLocation = map;
            }
        }

        Map<UUID, JIPipeGraphNode> copies = new HashMap<>();
        for (JIPipeGraphNode algorithm : outputGraph.getGraphNodes()) {
            if (algorithm instanceof JIPipeCompartmentOutput) {
                // We just assign the existing project output
                copies.put(algorithm.getUUIDInParentGraph(), projectOutputNode);

                // Set location
                if (outputLocation != null)
                    projectOutputNode.getLocations().put(compartmentUUID.toString(), outputLocation);

                // Copy the slot configuration over
                projectOutputNode.getSlotConfiguration().setTo(algorithm.getSlotConfiguration());
            } else {
                JIPipeGraphNode copy = algorithm.getInfo().duplicate(algorithm);
                Map<String, Point> locationMapEntry = locations.getOrDefault(algorithm, null);
                if (locationMapEntry != null) {
                    copy.getLocations().put(compartmentUUID.toString(), new HashMap<>(locationMapEntry));
                }
                project.getGraph().insertNode(copy, compartmentUUID);
                copies.put(algorithm.getUUIDInParentGraph(), copy);
            }
        }
        for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> edge : outputGraph.getSlotEdges()) {
            JIPipeGraphNode copySource = copies.get(edge.getKey().getNode().getUUIDInParentGraph());
            JIPipeGraphNode copyTarget = copies.get(edge.getValue().getNode().getUUIDInParentGraph());
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
            jsonGenerator.writeObjectField("graph", exportedCompartment.outputGraph);
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
            exportedCompartment.outputGraph.fromJson(node.get("graph"), new UnspecifiedValidationReportContext(), new JIPipeValidationReport(), new JIPipeNotificationInbox());
            if (node.has("metadata"))
                exportedCompartment.metadata = JsonUtils.getObjectMapper().readerFor(JIPipeMetadata.class).readValue(node.get("metadata"));

            return exportedCompartment;
        }
    }
}
