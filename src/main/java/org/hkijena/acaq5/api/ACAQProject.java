package org.hkijena.acaq5.api;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQCompartmentOutput;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.api.compartments.datatypes.ACAQCompartmentOutputData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotDefinition;
import org.hkijena.acaq5.api.events.AlgorithmGraphChangedEvent;
import org.hkijena.acaq5.api.events.CompartmentRemovedEvent;
import org.hkijena.acaq5.api.events.WorkDirectoryChangedEvent;
import org.hkijena.acaq5.utils.JsonUtils;
import org.hkijena.acaq5.utils.StringUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * An ACAQ5 project.
 * It contains all information to setup and run an analysis
 */
@JsonSerialize(using = ACAQProject.Serializer.class)
@JsonDeserialize(using = ACAQProject.Deserializer.class)
public class ACAQProject implements ACAQValidatable {
    private EventBus eventBus = new EventBus();

    private ACAQAlgorithmGraph graph = new ACAQAlgorithmGraph();
    private ACAQAlgorithmGraph compartmentGraph = new ACAQAlgorithmGraph();
    private BiMap<String, ACAQProjectCompartment> compartments = HashBiMap.create();
    private ACAQProjectMetadata metadata = new ACAQProjectMetadata();
    private Path workDirectory;

    public ACAQProject() {
        compartmentGraph.getEventBus().register(this);
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public ACAQAlgorithmGraph getGraph() {
        return graph;
    }

    public void saveProject(Path fileName) throws IOException {
        ObjectMapper mapper = JsonUtils.getObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(fileName.toFile(), this);
    }

    public BiMap<String, ACAQProjectCompartment> getCompartments() {
        return ImmutableBiMap.copyOf(compartments);
    }

    public ACAQProjectCompartment addCompartment(String name) {
        ACAQProjectCompartment compartment = ACAQAlgorithm.newInstance("acaq:project-compartment");
        compartment.setProject(this);
        compartment.setCustomName(name);
        compartmentGraph.insertNode(compartment, ACAQAlgorithmGraph.COMPARTMENT_DEFAULT);
        return compartment;
    }

    public void connectCompartments(ACAQProjectCompartment source, ACAQProjectCompartment target) {
        ACAQDataSlot sourceSlot = source.getFirstOutputSlot();
        List<ACAQDataSlot> openInputSlots = target.getOpenInputSlots();
        if (openInputSlots.isEmpty()) {
            ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration) target.getSlotConfiguration();
            ACAQSlotDefinition slotDefinition = new ACAQSlotDefinition(ACAQCompartmentOutputData.class, ACAQDataSlot.SlotType.Input,
                    StringUtils.makeUniqueString(source.getName(), " ", slotConfiguration::hasSlot),
                    null);
            slotConfiguration.addSlot(slotDefinition.getName(), slotDefinition);
            openInputSlots = target.getOpenInputSlots();
        }
        compartmentGraph.connect(sourceSlot, openInputSlots.get(0));
    }

    private void initializeCompartment(ACAQProjectCompartment compartment) {
        compartment.setProject(this);
        ACAQCompartmentOutput compartmentOutput = null;
        for (ACAQAlgorithm algorithm : graph.getAlgorithmNodes().values()) {
            if (algorithm instanceof ACAQCompartmentOutput && algorithm.getCompartment().equals(compartment.getProjectCompartmentId())) {
                compartmentOutput = (ACAQCompartmentOutput) algorithm;
            }
        }
        if (compartmentOutput == null) {
            compartmentOutput = ACAQAlgorithm.newInstance("acaq:compartment-output");
            compartmentOutput.setCustomName(compartment.getName() + " output");
            compartmentOutput.setCompartment(compartment.getProjectCompartmentId());
            graph.insertNode(compartmentOutput, compartment.getProjectCompartmentId());
        }

        compartment.setOutputNode(compartmentOutput);
    }

    private void updateCompartmentVisibility() {
        for (ACAQProjectCompartment compartment : compartments.values()) {
            compartment.getOutputNode().getVisibleCompartments().clear();
        }

        for (ACAQAlgorithmGraphEdge edge : compartmentGraph.getGraph().edgeSet()) {
            ACAQProjectCompartment source = (ACAQProjectCompartment) compartmentGraph.getGraph().getEdgeSource(edge).getAlgorithm();
            ACAQProjectCompartment target = (ACAQProjectCompartment) compartmentGraph.getGraph().getEdgeTarget(edge).getAlgorithm();
            source.getOutputNode().getVisibleCompartments().add(target.getProjectCompartmentId());
        }

        // Remove invalid connections in the project graph
        for (ACAQAlgorithmGraphEdge edge : ImmutableList.copyOf(graph.getGraph().edgeSet())) {
            if (graph.getGraph().containsEdge(edge)) {
                ACAQDataSlot source = graph.getGraph().getEdgeSource(edge);
                ACAQDataSlot target = graph.getGraph().getEdgeTarget(edge);
                if (!source.getAlgorithm().isVisibleIn(target.getAlgorithm().getCompartment())) {
                    graph.disconnect(source, target, false);
                }
            }
        }

        graph.getEventBus().post(new AlgorithmGraphChangedEvent(graph));
    }

    @Subscribe
    public void onCompartmentGraphChanged(AlgorithmGraphChangedEvent event) {
        if (event.getAlgorithmGraph() == compartmentGraph) {
            for (ACAQAlgorithm algorithm : compartmentGraph.getAlgorithmNodes().values()) {
                ACAQProjectCompartment compartment = (ACAQProjectCompartment) algorithm;
                if (!compartment.isInitialized()) {
                    compartments.put(compartment.getProjectCompartmentId(), compartment);
                    initializeCompartment(compartment);
                }
            }
            updateCompartmentVisibility();
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        graph.reportValidity(report);
    }

    public ACAQAlgorithmGraph getCompartmentGraph() {
        return compartmentGraph;
    }

    public void removeCompartment(ACAQProjectCompartment compartment) {
        graph.removeCompartment(compartment.getProjectCompartmentId());
        compartments.remove(compartment.getProjectCompartmentId());
        updateCompartmentVisibility();
        compartmentGraph.removeNode(compartment);
        eventBus.post(new CompartmentRemovedEvent(compartment));
    }

    public ACAQProjectMetadata getMetadata() {
        return metadata;
    }

    /**
     * Gets the folder where the project is currently working in
     *
     * @return
     */
    public Path getWorkDirectory() {
        return workDirectory;
    }

    /**
     * Sets the folder where the project is currently working in.
     * This information is passed to the algorithms to adapt to the work directory if needed (usually ony Filesystem nodes are affected)
     *
     * @param workDirectory
     */
    public void setWorkDirectory(Path workDirectory) {
        this.workDirectory = workDirectory;
        for (ACAQAlgorithm algorithm : graph.getAlgorithmNodes().values()) {
            algorithm.setWorkDirectory(workDirectory);
        }
        eventBus.post(new WorkDirectoryChangedEvent(workDirectory));
    }

    public static ACAQProject loadProject(Path fileName) throws IOException {
        return JsonUtils.getObjectMapper().readerFor(ACAQProject.class).readValue(fileName.toFile());
    }

    public static class Serializer extends JsonSerializer<ACAQProject> {
        @Override
        public void serialize(ACAQProject project, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("acaq:project-type", "project");
            jsonGenerator.writeObjectField("metadata", project.metadata);
            jsonGenerator.writeObjectField("dependencies", project.getDependencies());
            jsonGenerator.writeObjectField("algorithm-graph", project.graph);
            jsonGenerator.writeFieldName("compartments");
            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectField("compartment-graph", project.compartmentGraph);
            jsonGenerator.writeEndObject();
            jsonGenerator.writeEndObject();
        }
    }

    private Set<ACAQDependency> getDependencies() {
        Set<ACAQDependency> dependencies = graph.getDependencies();
        dependencies.addAll(compartmentGraph.getDependencies());
        return dependencies;
    }

    public static class Deserializer extends JsonDeserializer<ACAQProject> {

        @Override
        public ACAQProject deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            ACAQProject project = new ACAQProject();

            JsonNode node = jsonParser.getCodec().readTree(jsonParser);

            if (node.has("metadata")) {
                project.metadata = JsonUtils.getObjectMapper().readerFor(ACAQProjectMetadata.class).readValue(node.get("metadata"));
            }

            // We must first load the graph, as we can infer compartments later
            project.graph.fromJson(node.get("algorithm-graph"));

            // read compartments
            project.compartmentGraph.fromJson(node.get("compartments").get("compartment-graph"));
            for (ACAQAlgorithm algorithm : project.compartmentGraph.getAlgorithmNodes().values()) {
                ACAQProjectCompartment compartment = (ACAQProjectCompartment) algorithm;
                compartment.setProject(project);
                project.compartments.put(compartment.getProjectCompartmentId(), compartment);
                project.initializeCompartment(compartment);
            }

            // Reading compartments might break some connections. This will restore them
            project.graph.fromJson(node.get("algorithm-graph"));

            // Update node visibilities
            project.updateCompartmentVisibility();

            // Assign traits
            project.graph.updateDataSlotTraits();

            return project;
        }
    }
}
