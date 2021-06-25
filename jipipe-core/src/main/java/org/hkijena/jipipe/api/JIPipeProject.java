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
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeCompartmentOutput;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.settings.JIPipeProjectInfoParameters;
import org.hkijena.jipipe.utils.JsonUtils;
import org.hkijena.jipipe.utils.ParameterUtils;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.Point;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A JIPipe project.
 * It contains all information to setup and run an analysis
 */
@JsonSerialize(using = JIPipeProject.Serializer.class)
@JsonDeserialize(using = JIPipeProject.Deserializer.class)
public class JIPipeProject implements JIPipeValidatable {

    /**
     * The current version of the project format.
     * This is here for any future addition.
     */
    public static final int CURRENT_PROJECT_FORMAT_VERSION = 1;

    private final EventBus eventBus = new EventBus();

    private JIPipeGraph graph = new JIPipeGraph();
    private JIPipeGraph compartmentGraph = new JIPipeGraph();
    private BiMap<UUID, JIPipeProjectCompartment> compartments = HashBiMap.create();
    private JIPipeProjectMetadata metadata = new JIPipeProjectMetadata();
    private Map<String, Object> additionalMetadata = new HashMap<>();
    private Path workDirectory;
    private JIPipeProjectCache cache;
    private boolean isCleaningUp;
    private boolean isLoading;

    /**
     * A JIPipe project
     */
    public JIPipeProject() {
        this.cache = new JIPipeProjectCache(this);
        this.metadata.setDescription(new HTMLText(MarkdownDocument.fromPluginResource("documentation/new-project-template.md", new HashMap<>()).getRenderedHTML()));
        this.graph.attach(JIPipeProject.class, this);
        this.graph.attach(JIPipeGraphType.Project);
        this.compartmentGraph.attach(JIPipeProject.class, this);
        this.compartmentGraph.attach(JIPipeGraphType.ProjectCompartments);
        compartmentGraph.getEventBus().register(this);
    }

    /**
     * Finds a compartment by UUID or alias
     *
     * @param uuidOrAlias UUID or alias
     * @return the compartment or null if it could not be found
     */
    public JIPipeProjectCompartment findCompartment(String uuidOrAlias) {
        JIPipeGraphNode node = compartmentGraph.findNode(uuidOrAlias);
        if (node instanceof JIPipeProjectCompartment)
            return (JIPipeProjectCompartment) node;
        else
            return null;
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
    public BiMap<UUID, JIPipeProjectCompartment> getCompartments() {
        return ImmutableBiMap.copyOf(compartments);
    }

    /**
     * Adds a new project compartment
     *
     * @param name Compartment name
     * @return The compartment
     */
    public JIPipeProjectCompartment addCompartment(String name) {
        JIPipeProjectCompartment compartment = JIPipe.createNode("jipipe:project-compartment", JIPipeProjectCompartment.class);
        compartment.setProject(this);
        compartment.setCustomName(name);
        compartmentGraph.insertNode(compartment);
        return compartment;
    }

    /**
     * Adds an existing compartment instance
     *
     * @param compartment Compartment
     * @param uuid
     * @return The compartment
     */
    public JIPipeProjectCompartment addCompartment(JIPipeProjectCompartment compartment, UUID uuid) {
        compartment.setProject(this);
        compartmentGraph.insertNode(uuid == null ? UUID.randomUUID() : uuid, compartment, null);
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
        compartmentGraph.connect(sourceSlot, target.getFirstInputSlot());
    }

    private void initializeCompartment(JIPipeProjectCompartment compartment) {
        compartment.setProject(this);
        JIPipeCompartmentOutput compartmentOutput = null;
        UUID compartmentUUID = compartment.getProjectCompartmentUUID();
        for (JIPipeGraphNode node : graph.getGraphNodes()) {
            if (node instanceof JIPipeCompartmentOutput && Objects.equals(compartmentUUID, graph.getCompartmentUUIDOf(node))) {
                compartmentOutput = (JIPipeCompartmentOutput) node;
            }
        }
        if (compartmentOutput == null) {
            compartmentOutput = JIPipe.createNode("jipipe:compartment-output", JIPipeCompartmentOutput.class);
            compartmentOutput.setCustomName(compartment.getName() + " output");
            graph.insertNode(compartmentOutput, compartmentUUID);
        }

        compartment.setOutputNode(compartmentOutput);
    }

    private void updateCompartmentVisibility() {
        boolean changed = false;

        Map<JIPipeProjectCompartment, Set<UUID>> oldVisibleCompartments = new HashMap<>();
        for (JIPipeProjectCompartment compartment : compartments.values()) {
            JIPipeCompartmentOutput outputNode = compartment.getOutputNode();
            Set<UUID> visibleCompartmentUUIDs = graph.getVisibleCompartmentUUIDsOf(outputNode);
            oldVisibleCompartments.put(compartment, new HashSet<>(visibleCompartmentUUIDs));
            visibleCompartmentUUIDs.clear();
        }

        for (JIPipeGraphNode targetNode : compartmentGraph.getGraphNodes()) {
            if (targetNode instanceof JIPipeProjectCompartment) {
                JIPipeProjectCompartment target = (JIPipeProjectCompartment) targetNode;
                for (JIPipeDataSlot sourceSlot : compartmentGraph.getSourceSlots(target.getFirstInputSlot())) {
                    if (sourceSlot.getNode() instanceof JIPipeProjectCompartment) {
                        JIPipeProjectCompartment source = (JIPipeProjectCompartment) sourceSlot.getNode();
                        Set<UUID> visibleCompartmentUUIDs = graph.getVisibleCompartmentUUIDsOf(source.getOutputNode());
                        visibleCompartmentUUIDs.add(target.getProjectCompartmentUUID());
                    }
                }
            }
        }

        for (JIPipeProjectCompartment compartment : compartments.values()) {
            Set<UUID> visibleCompartmentUUIDs = graph.getVisibleCompartmentUUIDsOf(compartment.getOutputNode());
            if (!Objects.equals(visibleCompartmentUUIDs, oldVisibleCompartments.get(compartment))) {
                changed = true;
                break;
            }
        }

        // Remove invalid connections in the project graph
        for (JIPipeGraphEdge edge : ImmutableList.copyOf(graph.getGraph().edgeSet())) {
            if (graph.getGraph().containsEdge(edge)) {
                JIPipeDataSlot source = graph.getGraph().getEdgeSource(edge);
                JIPipeDataSlot target = graph.getGraph().getEdgeTarget(edge);
                if (!source.getNode().isVisibleIn(target.getNode().getCompartmentUUIDInGraph())) {
                    graph.disconnect(source, target, false);
                    changed = true;
                }
            }
        }

        if (changed)
            graph.getEventBus().post(new JIPipeGraph.GraphChangedEvent(graph));
    }

    /**
     * Triggered when the compartment graph is changed
     *
     * @param event Generated event
     */
    @Subscribe
    public void onCompartmentGraphChanged(JIPipeGraph.GraphChangedEvent event) {
        if (isCleaningUp)
            return;
        if (isLoading)
            return;
        if (event.getGraph() == compartmentGraph) {
            for (JIPipeGraphNode algorithm : compartmentGraph.getGraphNodes()) {
                if (algorithm instanceof JIPipeProjectCompartment) {
                    JIPipeProjectCompartment compartment = (JIPipeProjectCompartment) algorithm;
                    if (!compartment.isInitialized()) {
                        compartments.put(compartment.getProjectCompartmentUUID(), compartment);
                        initializeCompartment(compartment);
                    }
                }
            }
            updateCompartmentVisibility();
        }
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        graph.reportValidity(report);
    }

    /**
     * Reports the validity for the target node and its dependencies
     *
     * @param report     the report
     * @param targetNode the target node
     */
    public void reportValidity(JIPipeIssueReport report, JIPipeGraphNode targetNode) {
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
        UUID compartmentId = compartment.getProjectCompartmentUUID();
        graph.removeCompartment(compartmentId);
        compartments.remove(compartmentId);
        updateCompartmentVisibility();
        compartmentGraph.removeNode(compartment, false);
        eventBus.post(new CompartmentRemovedEvent(compartment, compartmentId));
    }

    /**
     * @return Project metadata
     */
    public JIPipeProjectMetadata getMetadata() {
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
        for (JIPipeGraphNode algorithm : graph.getGraphNodes()) {
            algorithm.setProjectWorkDirectory(workDirectory);
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
     *
     * @param force force updating
     */
    public void rebuildAliasIds(boolean force) {
        try {
            isCleaningUp = true;
            compartmentGraph.rebuildAliasIds(force);
            graph.rebuildAliasIds(force);
        } finally {
            isCleaningUp = false;
        }
    }

    public Map<String, Object> getAdditionalMetadata() {
        return additionalMetadata;
    }

    public void setAdditionalMetadata(Map<String, Object> additionalMetadata) {
        this.additionalMetadata = additionalMetadata;
    }

    /**
     * Writes the project to JSON
     *
     * @param generator the JSON generator
     * @throws IOException thrown by {@link JsonGenerator}
     */
    public void toJson(JsonGenerator generator) throws IOException {
//        cleanupGraph();
        generator.writeStartObject();
        generator.writeStringField("jipipe:project-type", "project");
        generator.writeNumberField("jipipe:project-format-version", CURRENT_PROJECT_FORMAT_VERSION);
        generator.writeObjectField("metadata", metadata);
        generator.writeObjectField("dependencies", getDependencies().stream().map(JIPipeMutableDependency::new).collect(Collectors.toList()));
        if (!getAdditionalMetadata().isEmpty()) {
            generator.writeObjectFieldStart("additional-metadata");
            for (Map.Entry<String, Object> entry : getAdditionalMetadata().entrySet()) {
                if (entry.getValue() instanceof JIPipeParameterCollection) {
                    generator.writeObjectFieldStart(entry.getKey());
                    generator.writeObjectField("jipipe:type", entry.getValue().getClass());
                    ParameterUtils.serializeParametersToJson((JIPipeParameterCollection) entry.getValue(), generator);
                    generator.writeEndObject();
                } else {
                    generator.writeObjectFieldStart(entry.getKey());
                    generator.writeObjectField("jipipe:type", entry.getValue().getClass());
                    generator.writeObjectField("data", entry.getValue());
                    generator.writeEndObject();
                }
            }
            generator.writeEndObject();
        }
        generator.writeObjectField("graph", graph);
        generator.writeFieldName("compartments");
        generator.writeStartObject();
        generator.writeObjectField("compartment-graph", compartmentGraph);
        generator.writeEndObject();
        generator.writeEndObject();
    }

    /**
     * Loads the project from JSON
     *
     * @param jsonNode the node
     */
    public void fromJson(JsonNode jsonNode, JIPipeIssueReport report) throws IOException {
        try {
            isLoading = true;

            if (jsonNode.has("metadata")) {
                metadata = JsonUtils.getObjectMapper().readerFor(JIPipeProjectMetadata.class).readValue(jsonNode.get("metadata"));
            }

            // Deserialize additional metadata
            JsonNode additionalMetadataNode = jsonNode.path("additional-metadata");
            for (Map.Entry<String, JsonNode> metadataEntry : ImmutableList.copyOf(additionalMetadataNode.fields())) {
                try {
                    Class<?> metadataClass = JsonUtils.getObjectMapper().readerFor(Class.class).readValue(metadataEntry.getValue().get("jipipe:type"));
                    if (JIPipeParameterCollection.class.isAssignableFrom(metadataClass)) {
                        JIPipeParameterCollection metadata = (JIPipeParameterCollection) ReflectionUtils.newInstance(metadataClass);
                        ParameterUtils.deserializeParametersFromJson(metadata, metadataEntry.getValue(), report.resolve("Metadata"));
                        additionalMetadata.put(metadataEntry.getKey(), metadata);
                    } else {
                        Object data = JsonUtils.getObjectMapper().readerFor(metadataClass).readValue(metadataEntry.getValue().get("data"));
                        if (data != null) {
                            additionalMetadata.put(metadataEntry.getKey(), data);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // We must first load the graph, as we can infer compartments later
            graph.fromJson(jsonNode.get("graph"), new JIPipeIssueReport());

            // read compartments
            compartmentGraph.fromJson(jsonNode.get("compartments").get("compartment-graph"), new JIPipeIssueReport());

            // Fix legacy nodes
            for (Map.Entry<UUID, String> entry : graph.getNodeLegacyCompartmentIDs().entrySet()) {
                JIPipeGraphNode compartmentNode = compartmentGraph.findNode(entry.getValue());
                JIPipeGraphNode node = graph.getNodeByUUID(entry.getKey());
                if (compartmentNode != null) {
                    JIPipe.getInstance().getLogService().info("[Project format conversion] Fix legacy compartment '" + entry.getValue() + "' --> " + compartmentNode.getUUIDInGraph());
                    graph.setCompartment(entry.getKey(), compartmentNode.getUUIDInGraph());
                } else {
                    // Ghost node -> delete
                    graph.removeNode(node, false);
                    continue;
                }

                // Fix legacy node location information
                for (Map.Entry<String, Map<String, Point>> locationEntry : ImmutableList.copyOf(node.getLocations().entrySet())) {
                    Map<String, Point> location = locationEntry.getValue();
                    String compartmentUUIDString;
                    if ("DEFAULT".equals(locationEntry.getKey())) {
                        compartmentUUIDString = "";
                    } else {
                        compartmentUUIDString = StringUtils.nullToEmpty(compartmentGraph.findNodeUUID(locationEntry.getKey()));
                    }
                    node.getLocations().remove(locationEntry.getKey());
                    node.getLocations().put(compartmentUUIDString, location);
                    JIPipe.getInstance().getLogService().info("[Project format conversion] Move location within " + locationEntry.getKey() + " to " + compartmentUUIDString);
                }
            }

            // Initialize compartments
            for (JIPipeGraphNode node : compartmentGraph.getGraphNodes()) {
                if (node instanceof JIPipeProjectCompartment) {
                    JIPipeProjectCompartment compartment = (JIPipeProjectCompartment) node;
                    compartment.setProject(this);
                    compartments.put(compartment.getProjectCompartmentUUID(), compartment);
                    initializeCompartment(compartment);
                }
            }

            // Reading compartments might break some connections. This will restore them
            graph.edgesFromJson(jsonNode.get("graph"), report);

            // Update node visibilities
            updateCompartmentVisibility();

            // Checking for error
            JIPipeIssueReport checkNodesReport = report.resolve("Check nodes");
            for (JIPipeGraphNode graphNode : ImmutableList.copyOf(graph.getGraphNodes())) {
                UUID compartmentUUIDInGraph = graphNode.getCompartmentUUIDInGraph();
                if (compartmentUUIDInGraph == null || !compartments.containsKey(compartmentUUIDInGraph)) {
                    checkNodesReport.reportIsInvalid("Node has no compartment!",
                            "The node '" + graphNode.getDisplayName() + "' has no compartment assigned!",
                            "This was repaired automatically by deleting the node. Please inform the JIPipe developers about this issue.",
                            graphNode);
                    graph.removeNode(graphNode, false);
                } else {
                    JIPipeGraphNode compartmentNode = compartmentGraph.getNodeByUUID(compartmentUUIDInGraph);
                    if (compartmentNode == null) {
                        checkNodesReport.reportIsInvalid("Node has invalid compartment!",
                                "The node '" + graphNode.getDisplayName() + "' is assigned to compartment '" + compartmentUUIDInGraph + "', but it does not exist!",
                                "This was repaired automatically by deleting the node. Please inform the JIPipe developers about this issue.",
                                graphNode);
                        graph.removeNode(graphNode, false);
                    }
                }
            }
        } finally {
            isLoading = false;
        }
    }

    /**
     * Returns a list of all nodes that cannot be executed or are deactivated by the user.
     * This method works on transitive deactivation (e.g. a dependency is deactivated).
     *
     * @return list of deactivated nodes
     */
    public Set<JIPipeGraphNode> getDeactivatedAlgorithms() {
        return graph.getDeactivatedAlgorithms(true);
    }

    /**
     * Returns all nodes that have at least one connected algorithm that uses its generated data.
     *
     * @return Intermediate nodes
     */
    public Set<JIPipeGraphNode> getIntermediateAlgorithms() {
        Set<JIPipeGraphNode> result = new HashSet<>();
        outer:
        for (JIPipeGraphNode node : graph.getGraphNodes()) {
            for (JIPipeDataSlot outputSlot : node.getOutputSlots()) {
                if (graph.getTargetSlots(outputSlot).isEmpty()) {
                    continue outer;
                }
            }
            result.add(node);
        }
        return result;
    }

    /**
     * Returns a list of all nodes that generate heavy data ({@link JIPipeHeavyData}) and are intermediate (see getIntermediateAlgorithms()).
     * Skips all nodes that do not save outputs.
     *
     * @return intermediate nodes with heavy data
     */
    public Set<JIPipeDataSlot> getHeavyIntermediateAlgorithmOutputSlots() {
        Set<JIPipeDataSlot> result = new HashSet<>();
        for (JIPipeGraphNode node : graph.getGraphNodes()) {
            for (JIPipeDataSlot outputSlot : node.getOutputSlots()) {
                boolean heavy = JIPipeData.isHeavy(outputSlot.getAcceptedDataType());
                if (heavy && !graph.getTargetSlots(outputSlot).isEmpty()) {
                    result.add(outputSlot);
                }
            }
        }
        return result;
    }

    /**
     * Returns the global parameters for this pipeline
     *
     * @return global parameters
     */
    public JIPipeProjectInfoParameters getPipelineParameters() {
        Object existing = getAdditionalMetadata().getOrDefault(JIPipeProjectInfoParameters.METADATA_KEY, null);
        JIPipeProjectInfoParameters result;
        if (existing instanceof JIPipeProjectInfoParameters) {
            result = (JIPipeProjectInfoParameters) existing;
        } else {
            result = new JIPipeProjectInfoParameters();
            getAdditionalMetadata().put(JIPipeProjectInfoParameters.METADATA_KEY, result);
        }
        result.setProject(this);
        return result;
    }

    /**
     * Repairs restored compartment outputs
     */
    public void fixCompartmentOutputs() {
        for (JIPipeGraphNode node : compartmentGraph.getGraphNodes()) {
            if (node instanceof JIPipeProjectCompartment) {
                JIPipeProjectCompartment compartment = (JIPipeProjectCompartment) node;
                UUID projectCompartmentUUID = compartment.getProjectCompartmentUUID();
                JIPipeCompartmentOutput existingOutputNode = compartment.getOutputNode();
                if (!graph.containsNode(existingOutputNode)) {
                    // Find a new output node
                    boolean found = false;
                    for (JIPipeGraphNode graphNode : graph.getGraphNodes()) {
                        if (graphNode instanceof JIPipeCompartmentOutput && Objects.equals(graphNode.getCompartmentUUIDInGraph(), projectCompartmentUUID)) {
                            compartment.setOutputNode((JIPipeCompartmentOutput) graphNode);
                            found = true;
                            break;
                        }
                    }

                    // Create a new one
                    if (!found) {
                        JIPipeCompartmentOutput compartmentOutput = JIPipe.createNode("jipipe:compartment-output", JIPipeCompartmentOutput.class);
                        compartmentOutput.setCustomName(compartment.getName() + " output");
                        graph.insertNode(compartmentOutput, compartment.getProjectCompartmentUUID());
                        compartment.setOutputNode(compartmentOutput);
                    }
                }
            }
        }
    }

    /**
     * Loads a project from a file
     *
     * @param fileName JSON file
     * @param report   issue report
     * @return Loaded project
     * @throws IOException Triggered by {@link ObjectMapper}
     */
    public static JIPipeProject loadProject(Path fileName, JIPipeIssueReport report) throws IOException {
        JsonNode jsonData = JsonUtils.getObjectMapper().readValue(fileName.toFile(), JsonNode.class);
        JIPipeProject project = new JIPipeProject();
        project.fromJson(jsonData, report);
        project.setWorkDirectory(fileName.getParent());
        return project;
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
     * Deserializes the project metadata from JSON
     *
     * @param node JSON node
     * @return the metadata
     */
    public static JIPipeProjectMetadata loadMetadataFromJson(JsonNode node) {
        node = node.path("metadata");
        if (node.isMissingNode())
            return new JIPipeProjectMetadata();
        try {
            return JsonUtils.getObjectMapper().readerFor(JIPipeProjectMetadata.class).readValue(node);
        } catch (IOException e) {
            throw new UserFriendlyRuntimeException(e, "Could not load metadata from JIPipe project",
                    "Project", "The JSON data that describes the project metadata is missing essential information",
                    "Open the file in a text editor and compare the metadata with a valid project.");
        }
    }

    /**
     * Serializes a project
     */
    public static class Serializer extends JsonSerializer<JIPipeProject> {
        @Override
        public void serialize(JIPipeProject project, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            project.toJson(jsonGenerator);
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
            project.fromJson(node, new JIPipeIssueReport());
            return project;
        }
    }

    /**
     * Triggered when a sample is added to an {@link JIPipeProject}
     */
    public static class CompartmentAddedEvent {
        private JIPipeProjectCompartment compartment;

        /**
         * @param compartment the compartment
         */
        public CompartmentAddedEvent(JIPipeProjectCompartment compartment) {
            this.compartment = compartment;
        }

        public JIPipeProjectCompartment getCompartment() {
            return compartment;
        }
    }

    /**
     * Triggered when a sample is removed from an {@link JIPipeProject}
     */
    public static class CompartmentRemovedEvent {
        private final UUID compartmentUUID;
        private JIPipeProjectCompartment compartment;

        /**
         * @param compartment     the compartment
         * @param compartmentUUID the compartment id
         */
        public CompartmentRemovedEvent(JIPipeProjectCompartment compartment, UUID compartmentUUID) {
            this.compartment = compartment;
            this.compartmentUUID = compartmentUUID;
        }

        public JIPipeProjectCompartment getCompartment() {
            return compartment;
        }

        public UUID getCompartmentUUID() {
            return compartmentUUID;
        }
    }

    /**
     * Triggered when the work directory of a project or algorithm was changed
     */
    public static class WorkDirectoryChangedEvent {
        private Path workDirectory;

        /**
         * @param workDirectory the work directory
         */
        public WorkDirectoryChangedEvent(Path workDirectory) {
            this.workDirectory = workDirectory;
        }

        public Path getWorkDirectory() {
            return workDirectory;
        }
    }
}
