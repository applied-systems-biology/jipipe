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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeStandardMetadata;
import org.hkijena.jipipe.api.compartments.algorithms.IOInterfaceAlgorithm;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartmentOutput;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.compartments.datatypes.JIPipeCompartmentOutputData;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

/**
 * Exported {@link JIPipeProjectCompartment}
 */
@JsonSerialize(using = JIPipeExportedCompartment.Serializer.class)
@JsonDeserialize(using = JIPipeExportedCompartment.Deserializer.class)
public class JIPipeExportedCompartment {
    private final JIPipeGraph exportedGraph = new JIPipeGraph();
    private JIPipeStandardMetadata metadata = new JIPipeStandardMetadata();
    private List<String> outputSlots = new ArrayList<>();

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
                if (algorithm instanceof JIPipeProjectCompartmentOutput) {
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

                    exportedGraph.insertNode(ioInterfaceAlgorithm);
                    copies.put(algorithm.getUUIDInParentGraph(), ioInterfaceAlgorithm);
                }
            } else {
                JIPipeGraphNode copy = algorithm.getInfo().duplicate(algorithm);
                exportedGraph.insertNode(copy);
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
            exportedGraph.connect(copySource.getOutputSlotMap().get(edge.getKey().getName()),
                    copyTarget.getInputSlotMap().get(edge.getValue().getName()));
        }
        metadata.setName(compartment.getName());

        // Copy over the list of output slots
        for (JIPipeOutputDataSlot outputSlot : compartment.getOutputSlots()) {
            outputSlots.add(outputSlot.getName());
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
    public JIPipeProjectCompartment addTo(JIPipeProject project, String compartmentName) {
        JIPipeProjectCompartment compartment = project.addCompartment(compartmentName);

        String locationCompartment = "";
        for (JIPipeGraphNode algorithm : exportedGraph.getGraphNodes()) {
            if (!(algorithm instanceof JIPipeProjectCompartmentOutput)) {
                if (!algorithm.getLocations().keySet().isEmpty())
                    locationCompartment = algorithm.getLocations().keySet().iterator().next();
            }
        }

        Map<JIPipeGraphNode, Map<String, Point>> locations = new HashMap<>();
        Map<String, Point> outputLocation = null;
        UUID compartmentUUID = compartment.getProjectCompartmentUUID();
        for (JIPipeGraphNode algorithm : exportedGraph.getGraphNodes()) {
            exportedGraph.setCompartment(algorithm.getUUIDInParentGraph(), compartmentUUID);
            Map<String, Point> map = algorithm.getLocations().getOrDefault(locationCompartment, null);
            if (map != null) {
                locations.put(algorithm, map);
            }
            if (algorithm instanceof JIPipeProjectCompartmentOutput) {
                outputLocation = map;
            }
        }

        // Add the output slots
        for (String outputSlot : outputSlots) {
            ((JIPipeMutableSlotConfiguration)compartment.getSlotConfiguration()).addSlot(outputSlot, new JIPipeDataSlotInfo(JIPipeCompartmentOutputData.class,
                    JIPipeSlotType.Output, outputSlot,""), false);
        }


        Map<UUID, JIPipeGraphNode> copies = new HashMap<>();
        for (JIPipeGraphNode algorithm : exportedGraph.getGraphNodes()) {
            if (algorithm instanceof JIPipeProjectCompartmentOutput) {

                JIPipeProjectCompartmentOutput outputNode = compartment.getOutputNode(((JIPipeProjectCompartmentOutput) algorithm).getOutputSlotName());

                // We just assign the existing project output
                copies.put(algorithm.getUUIDInParentGraph(), outputNode);

                // Set location
                if (outputLocation != null) {
                    outputNode.getLocations().put(compartmentUUID.toString(), outputLocation);
                }

                // Copy the slot configuration over
                outputNode.getSlotConfiguration().setTo(algorithm.getSlotConfiguration());
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
        for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> edge : exportedGraph.getSlotEdges()) {
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
    public JIPipeStandardMetadata getMetadata() {
        return metadata;
    }

    /**
     * Sets the metadata
     *
     * @param metadata The metadata
     */
    public void setMetadata(JIPipeStandardMetadata metadata) {
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
            jsonGenerator.writeObjectField("graph", exportedCompartment.exportedGraph);
            jsonGenerator.writeObjectField("output-slots", exportedCompartment.outputSlots);
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
            exportedCompartment.exportedGraph.fromJson(node.get("graph"), new UnspecifiedValidationReportContext(), new JIPipeValidationReport(), new JIPipeNotificationInbox());
            if (node.has("metadata")) {
                exportedCompartment.metadata = JsonUtils.getObjectMapper().readerFor(JIPipeStandardMetadata.class).readValue(node.get("metadata"));
            }
            if(node.has("output-slots")) {
                TypeReference<List<String>> typeReference = new TypeReference<List<String>>() {
                };
                exportedCompartment.outputSlots = JsonUtils.getObjectMapper().readerFor(typeReference).readValue(node.get("output-slots"));
            }

            return exportedCompartment;
        }
    }
}
