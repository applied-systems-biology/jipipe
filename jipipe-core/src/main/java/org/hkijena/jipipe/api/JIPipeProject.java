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

package org.hkijena.jipipe.api;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeCompartmentOutput;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.compartments.datatypes.JIPipeCompartmentOutputData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.events.CompartmentRemovedEvent;
import org.hkijena.jipipe.api.events.GraphChangedEvent;
import org.hkijena.jipipe.api.events.WorkDirectoryChangedEvent;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.utils.JsonUtils;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A JIPipe project.
 * It contains all information to setup and run an analysis
 */
@JsonSerialize(using = JIPipeProject.Serializer.class)
@JsonDeserialize(using = JIPipeProject.Deserializer.class)
public class JIPipeProject implements JIPipeValidatable {
    private EventBus eventBus = new EventBus();

    private JIPipeGraph graph = new JIPipeGraph();
    private JIPipeGraph compartmentGraph = new JIPipeGraph();
    private BiMap<String, JIPipeProjectCompartment> compartments = HashBiMap.create();
    private JIPipeMetadata metadata = new JIPipeMetadata();
    private Map<String, Object> additionalMetadata = new HashMap<String, Object>();
    private Path workDirectory;
    private JIPipeProjectCache cache;

    /**
     * A JIPipe project
     */
    public JIPipeProject() {
        this.cache = new JIPipeProjectCache(this);
        this.metadata.setDescription(MarkdownDocument.fromPluginResource("documentation/new-project-template.md").getMarkdown());
        compartmentGraph.getEventBus().register(this);
    }

    /**
     * Returns the state ID of a graph node
     * The state ID is a unique representation of how the algorithm's output was generated.
     * This is used by the data cache.
     *
     * @param node      the target algorithm
     * @param traversed traversed graph. should be {@link JIPipeGraph}.traverseAlgorithms(). This parameter is here for performance reasons.
     * @return unique representation of how the algorithm's output was generated.
     */
    public JIPipeProjectCache.State getStateIdOf(JIPipeAlgorithm node, List<JIPipeGraphNode> traversed) {
        List<JIPipeGraphNode> predecessorAlgorithms = graph.getPredecessorAlgorithms(node, traversed);
        predecessorAlgorithms.add(node);
        List<String> ids = new ArrayList<>();
        for (JIPipeGraphNode predecessorAlgorithm : predecessorAlgorithms) {
            ids.add(((JIPipeAlgorithm) predecessorAlgorithm).getStateId());
        }
        try {
            return new JIPipeProjectCache.State(LocalDateTime.now(), JsonUtils.getObjectMapper().writeValueAsString(ids));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return The event bus
     */
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * @return The algorithm graph
     */
    public JIPipeGraph getGraph() {
        return graph;
    }

    /**
     * Saves the project
     *
     * @param fileName Target file
     * @throws IOException Triggered by {@link ObjectMapper}
     */
    public void saveProject(Path fileName) throws IOException {
        ObjectMapper mapper = JsonUtils.getObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(fileName.toFile(), this);
    }

    public JIPipeProjectCache getCache() {
        return cache;
    }

    /**
     * @return The current project compartments
     */
    public BiMap<String, JIPipeProjectCompartment> getCompartments() {
        return ImmutableBiMap.copyOf(compartments);
    }

    /**
     * Adds a new project compartment
     *
     * @param name Compartment name
     * @return The compartment
     */
    public JIPipeProjectCompartment addCompartment(String name) {
        JIPipeProjectCompartment compartment = JIPipeGraphNode.newInstance("jipipe:project-compartment");
        compartment.setProject(this);
        compartment.setCustomName(name);
        compartmentGraph.insertNode(compartment, JIPipeGraph.COMPARTMENT_DEFAULT);
        return compartment;
    }

    /**
     * Adds an existing compartment instance
     *
     * @param compartment Compartment
     * @return The compartment
     */
    public JIPipeProjectCompartment addCompartment(JIPipeProjectCompartment compartment) {
        compartment.setProject(this);
        compartmentGraph.insertNode(compartment, JIPipeGraph.COMPARTMENT_DEFAULT);
        return compartment;
    }

    /**
     * Connects two compartments
     *
     * @param source Source compartment
     * @param target Target compartment
     */
    public void connectCompartments(JIPipeProjectCompartment source, JIPipeProjectCompartment target) {
        JIPipeDataSlot sourceSlot = source.getFirstOutputSlot();
        List<JIPipeDataSlot> openInputSlots = target.getOpenInputSlots();
        if (openInputSlots.isEmpty()) {
            JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) target.getSlotConfiguration();
            JIPipeDataSlotInfo slotDefinition = new JIPipeDataSlotInfo(JIPipeCompartmentOutputData.class, JIPipeSlotType.Input,
                    StringUtils.makeUniqueString(source.getName(), " ", slotConfiguration::hasInputSlot),
                    null);
            slotConfiguration.addSlot(slotDefinition.getName(), slotDefinition, false);
            openInputSlots = target.getOpenInputSlots();
        }
        compartmentGraph.connect(sourceSlot, openInputSlots.get(0));
    }

    private void initializeCompartment(JIPipeProjectCompartment compartment) {
        compartment.setProject(this);
        JIPipeCompartmentOutput compartmentOutput = null;
        for (JIPipeGraphNode algorithm : graph.getNodes().values()) {
            if (algorithm instanceof JIPipeCompartmentOutput && algorithm.getCompartment().equals(compartment.getProjectCompartmentId())) {
                compartmentOutput = (JIPipeCompartmentOutput) algorithm;
            }
        }
        if (compartmentOutput == null) {
            compartmentOutput = JIPipeGraphNode.newInstance("jipipe:compartment-output");
            compartmentOutput.setCustomName(compartment.getName() + " output");
            compartmentOutput.setCompartment(compartment.getProjectCompartmentId());
            graph.insertNode(compartmentOutput, compartment.getProjectCompartmentId());
        }

        compartment.setOutputNode(compartmentOutput);
    }

    private void updateCompartmentVisibility() {
        for (JIPipeProjectCompartment compartment : compartments.values()) {
            compartment.getOutputNode().getVisibleCompartments().clear();
        }

        for (JIPipeGraphEdge edge : compartmentGraph.getGraph().edgeSet()) {
            JIPipeProjectCompartment source = (JIPipeProjectCompartment) compartmentGraph.getGraph().getEdgeSource(edge).getNode();
            JIPipeProjectCompartment target = (JIPipeProjectCompartment) compartmentGraph.getGraph().getEdgeTarget(edge).getNode();
            source.getOutputNode().getVisibleCompartments().add(target.getProjectCompartmentId());
        }

        // Remove invalid connections in the project graph
        for (JIPipeGraphEdge edge : ImmutableList.copyOf(graph.getGraph().edgeSet())) {
            if (graph.getGraph().containsEdge(edge)) {
                JIPipeDataSlot source = graph.getGraph().getEdgeSource(edge);
                JIPipeDataSlot target = graph.getGraph().getEdgeTarget(edge);
                if (!source.getNode().isVisibleIn(target.getNode().getCompartment())) {
                    graph.disconnect(source, target, false);
                }
            }
        }

        graph.getEventBus().post(new GraphChangedEvent(graph));
    }

    /**
     * Triggered when the compartment graph is changed
     *
     * @param event Generated event
     */
    @Subscribe
    public void onCompartmentGraphChanged(GraphChangedEvent event) {
        if (event.getAlgorithmGraph() == compartmentGraph) {
            for (JIPipeGraphNode algorithm : compartmentGraph.getNodes().values()) {
                JIPipeProjectCompartment compartment = (JIPipeProjectCompartment) algorithm;
                if (!compartment.isInitialized()) {
                    compartments.put(compartment.getProjectCompartmentId(), compartment);
                    initializeCompartment(compartment);
                }
            }
            updateCompartmentVisibility();
        }
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        graph.reportValidity(report);
    }

    /**
     * Reports the validity for the target node and its dependencies
     *
     * @param report     the report
     * @param targetNode the target node
     */
    public void reportValidity(JIPipeValidityReport report, JIPipeGraphNode targetNode) {
        graph.reportValidity(report, targetNode);
    }

    /**
     * @return The compartment graph. Contains only {@link JIPipeProjectCompartment} nodes.
     */
    public JIPipeGraph getCompartmentGraph() {
        return compartmentGraph;
    }

    /**
     * Removes a compartment
     *
     * @param compartment The compartment
     */
    public void removeCompartment(JIPipeProjectCompartment compartment) {
        graph.removeCompartment(compartment.getProjectCompartmentId());
        compartments.remove(compartment.getProjectCompartmentId());
        updateCompartmentVisibility();
        compartmentGraph.removeNode(compartment, false);
        eventBus.post(new CompartmentRemovedEvent(compartment));
    }

    /**
     * @return Project metadata
     */
    public JIPipeMetadata getMetadata() {
        return metadata;
    }

    /**
     * Gets the folder where the project is currently working in
     *
     * @return the folder where the project is currently working in
     */
    public Path getWorkDirectory() {
        return workDirectory;
    }

    /**
     * Sets the folder where the project is currently working in.
     * This information is passed to the algorithms to adapt to the work directory if needed (usually ony Filesystem nodes are affected)
     *
     * @param workDirectory Project work directory
     */
    public void setWorkDirectory(Path workDirectory) {
        this.workDirectory = workDirectory;
        for (JIPipeGraphNode algorithm : graph.getNodes().values()) {
            algorithm.setWorkDirectory(workDirectory);
        }
        eventBus.post(new WorkDirectoryChangedEvent(workDirectory));
    }

    /**
     * @return All project dependencies
     */
    public Set<JIPipeDependency> getDependencies() {
        Set<JIPipeDependency> dependencies = graph.getDependencies();
        dependencies.addAll(compartmentGraph.getDependencies());
        return dependencies;
    }

    /**
     * Re-assigns graph node Ids based on their name
     */
    public void cleanupGraph() {
        Map<String, String> compartmentRenames = compartmentGraph.cleanupIds();
        Map<String, Set<JIPipeGraphNode>> compartmentNodes = new HashMap<>();
        for (Map.Entry<String, String> entry : compartmentRenames.entrySet()) {
            Set<JIPipeGraphNode> nodes = graph.getNodes().values().stream().filter(node -> Objects.equals(node.getCompartment(), entry.getKey())).collect(Collectors.toSet());
            compartmentNodes.put(entry.getKey(), nodes);
        }
        for (Map.Entry<String, Set<JIPipeGraphNode>> entry : compartmentNodes.entrySet()) {
            String newCompartment = compartmentRenames.get(entry.getKey());
            for (JIPipeGraphNode node : entry.getValue()) {

                // Rename own compartment
                node.setCompartment(newCompartment);
            }
        }
        for (JIPipeGraphNode node : graph.getNodes().values()) {
            // Rename visible compartments
            node.setVisibleCompartments(node.getVisibleCompartments().stream().map(compartmentRenames::get).collect(Collectors.toSet()));

            // Rename locations
            ImmutableList<Map.Entry<String, Map<String, Point>>> locationEntries = ImmutableList.copyOf(node.getLocations().entrySet());
            node.getLocations().clear();
            for (Map.Entry<String, Map<String, Point>> entry : locationEntries) {
                String newId = compartmentRenames.get(entry.getKey());
                node.getLocations().put(newId, entry.getValue());
            }
        }

        graph.cleanupIds();
    }

    public Map<String, Object> getAdditionalMetadata() {
        return additionalMetadata;
    }

    public void setAdditionalMetadata(Map<String, Object> additionalMetadata) {
        this.additionalMetadata = additionalMetadata;
    }

    /**
     * Loads a project from a file
     *
     * @param fileName JSON file
     * @return Loaded project
     * @throws IOException Triggered by {@link ObjectMapper}
     */
    public static JIPipeProject loadProject(Path fileName) throws IOException {
        return JsonUtils.getObjectMapper().readerFor(JIPipeProject.class).readValue(fileName.toFile());
    }

    /**
     * Loads a project from JSON data
     *
     * @param node JSON data
     * @return Loaded project
     * @throws IOException Triggered by {@link ObjectMapper}
     */
    public static JIPipeProject loadProject(JsonNode node) throws IOException {
        return JsonUtils.getObjectMapper().readerFor(JIPipeProject.class).readValue(node);
    }

    /**
     * Deserializes the set of project dependencies from JSON.
     * Does not require the dependencies to be actually registered.
     *
     * @param node JSON node
     * @return The dependencies as {@link org.hkijena.jipipe.JIPipeMutableDependency}
     */
    public static Set<JIPipeDependency> loadDependenciesFromJson(JsonNode node) {
        node = node.path("dependencies");
        if (node.isMissingNode())
            return new HashSet<>();
        TypeReference<HashSet<JIPipeDependency>> typeReference = new TypeReference<HashSet<JIPipeDependency>>() {
        };
        try {
            return JsonUtils.getObjectMapper().readerFor(typeReference).readValue(node);
        } catch (IOException e) {
            throw new UserFriendlyRuntimeException(e, "Could not load dependencies from JIPipe project",
                    "Project", "The JSON data that describes the project dependencies is missing essential information",
                    "Open the file in a text editor and compare the dependencies with a valid project. You can also try " +
                            "to delete the whole dependencies section - you just have to make sure that they are actually satisfied. " +
                            "To do this, use the plugin manager in JIPipe's GUI.");
        }
    }

    /**
     * Serializes a project
     */
    public static class Serializer extends JsonSerializer<JIPipeProject> {
        @Override
        public void serialize(JIPipeProject project, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            project.cleanupGraph();
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("jipipe:project-type", "project");
            jsonGenerator.writeObjectField("metadata", project.metadata);
            jsonGenerator.writeObjectField("dependencies", project.getDependencies().stream().map(JIPipeMutableDependency::new).collect(Collectors.toList()));
            if (!project.getAdditionalMetadata().isEmpty()) {
                jsonGenerator.writeObjectFieldStart("additional-metadata");
                for (Map.Entry<String, Object> entry : project.getAdditionalMetadata().entrySet()) {
                    if (entry.getValue() instanceof JIPipeParameterCollection) {
                        jsonGenerator.writeObjectFieldStart(entry.getKey());
                        jsonGenerator.writeObjectField("jipipe:type", entry.getValue().getClass());
                        JIPipeParameterCollection.serializeParametersToJson((JIPipeParameterCollection) entry.getValue(), jsonGenerator);
                        jsonGenerator.writeEndObject();
                    } else {
                        jsonGenerator.writeObjectFieldStart(entry.getKey());
                        jsonGenerator.writeObjectField("jipipe:type", entry.getValue().getClass());
                        jsonGenerator.writeObjectField("data", entry.getValue());
                        jsonGenerator.writeEndObject();
                    }
                }
                jsonGenerator.writeEndObject();
            }
            jsonGenerator.writeObjectField("algorithm-graph", project.graph);
            jsonGenerator.writeFieldName("compartments");
            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectField("compartment-graph", project.compartmentGraph);
            jsonGenerator.writeEndObject();
            jsonGenerator.writeEndObject();
        }
    }

    /**
     * Deserializes a project
     */
    public static class Deserializer extends JsonDeserializer<JIPipeProject> {

        @Override
        public JIPipeProject deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JIPipeProject project = new JIPipeProject();

            JsonNode node = jsonParser.getCodec().readTree(jsonParser);

            if (node.has("metadata")) {
                project.metadata = JsonUtils.getObjectMapper().readerFor(JIPipeMetadata.class).readValue(node.get("metadata"));
            }

            // Deserialize additional metadata
            JsonNode additionalMetadataNode = node.path("additional-metadata");
            for (Map.Entry<String, JsonNode> metadataEntry : ImmutableList.copyOf(additionalMetadataNode.fields())) {
                try {
                    Class<?> metadataClass = JsonUtils.getObjectMapper().readerFor(Class.class).readValue(metadataEntry.getValue().get("jipipe:type"));
                    if (JIPipeParameterCollection.class.isAssignableFrom(metadataClass)) {
                        JIPipeParameterCollection metadata = (JIPipeParameterCollection) ReflectionUtils.newInstance(metadataClass);
                        JIPipeParameterCollection.deserializeParametersFromJson(metadata, metadataEntry.getValue());
                        project.additionalMetadata.put(metadataEntry.getKey(), metadata);
                    } else {
                        Object data = JsonUtils.getObjectMapper().readerFor(metadataClass).readValue(metadataEntry.getValue().get("data"));
                        if (data != null) {
                            project.additionalMetadata.put(metadataEntry.getKey(), data);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // We must first load the graph, as we can infer compartments later
            project.graph.fromJson(node.get("algorithm-graph"));

            // read compartments
            project.compartmentGraph.fromJson(node.get("compartments").get("compartment-graph"));
            for (JIPipeGraphNode algorithm : project.compartmentGraph.getNodes().values()) {
                JIPipeProjectCompartment compartment = (JIPipeProjectCompartment) algorithm;
                compartment.setProject(project);
                project.compartments.put(compartment.getProjectCompartmentId(), compartment);
                project.initializeCompartment(compartment);
            }

            // Reading compartments might break some connections. This will restore them
            project.graph.fromJson(node.get("algorithm-graph"));

            // Update node visibilities
            project.updateCompartmentVisibility();

            // Apply some clean-up
            project.cleanupGraph();

            return project;
        }
    }
}
