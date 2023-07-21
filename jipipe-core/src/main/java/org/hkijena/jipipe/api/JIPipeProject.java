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
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.cache.JIPipeLocalProjectMemoryCache;
import org.hkijena.jipipe.api.compartments.algorithms.IOInterfaceAlgorithm;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeCompartmentOutput;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.history.JIPipeProjectHistoryJournal;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.notifications.JIPipeNotification;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.validation.*;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.settings.NodeTemplateSettings;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.settings.JIPipeProjectInfoParameters;
import org.hkijena.jipipe.utils.NaturalOrderComparator;
import org.hkijena.jipipe.utils.ParameterUtils;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;

/**
 * A JIPipe project.
 * It contains all information to set up and run an analysis
 */
@JsonSerialize(using = JIPipeProject.Serializer.class)
@JsonDeserialize(using = JIPipeProject.Deserializer.class)
public class JIPipeProject implements JIPipeValidatable, JIPipeGraph.GraphChangedEventListener {

    /**
     * The current version of the project format.
     * This is here for any future addition.
     */
    public static final int CURRENT_PROJECT_FORMAT_VERSION = 1;
    private final JIPipeGraph graph = new JIPipeGraph();
    private final JIPipeGraph compartmentGraph = new JIPipeGraph();
    private final BiMap<UUID, JIPipeProjectCompartment> compartments = HashBiMap.create();
    private final JIPipeLocalProjectMemoryCache cache;
    private final JIPipeProjectHistoryJournal historyJournal;
    private JIPipeProjectMetadata metadata = new JIPipeProjectMetadata();
    private Map<String, Object> additionalMetadata = new HashMap<>();
    private Path workDirectory;
    private boolean isCleaningUp;
    private boolean isLoading;

    private final CompartmentAddedEventEmitter compartmentAddedEventEmitter = new CompartmentAddedEventEmitter();
    private final CompartmentRemovedEventEmitter compartmentRemovedEventEmitter = new CompartmentRemovedEventEmitter();

    private final JIPipeGraphNode.BaseDirectoryChangedEventEmitter baseDirectoryChangedEventEmitter = new JIPipeGraphNode.BaseDirectoryChangedEventEmitter();

    /**
     * A JIPipe project
     */
    public JIPipeProject() {
        this.historyJournal = new JIPipeProjectHistoryJournal(this);
        this.cache = new JIPipeLocalProjectMemoryCache(this);
        this.metadata.setDescription(new HTMLText(MarkdownDocument.fromPluginResource("documentation/new-project-template.md", new HashMap<>()).getRenderedHTML()));
        this.graph.attach(JIPipeProject.class, this);
        this.graph.attach(JIPipeGraphType.Project);
        this.compartmentGraph.attach(JIPipeProject.class, this);
        this.compartmentGraph.attach(JIPipeGraphType.ProjectCompartments);

        compartmentGraph.getGraphChangedEventEmitter().subscribe(this);
    }

    /**
     * Loads a project from a file
     *
     * @param fileName    JSON file
     * @param context the context
     * @param report      issue report
     * @return Loaded project
     * @throws IOException Triggered by {@link ObjectMapper}
     */
    public static JIPipeProject loadProject(Path fileName, JIPipeValidationReportContext context, JIPipeValidationReport report) throws IOException {
        return loadProject(fileName, context, report, new JIPipeNotificationInbox());
    }

    /**
     * Loads a project from a file
     *
     * @param fileName      JSON file
     * @param context the context
     * @param report        issue report
     * @param notifications notifications for the user
     * @return Loaded project
     * @throws IOException Triggered by {@link ObjectMapper}
     */
    public static JIPipeProject loadProject(Path fileName, JIPipeValidationReportContext context, JIPipeValidationReport report, JIPipeNotificationInbox notifications) throws IOException {
        JsonNode jsonData = JsonUtils.getObjectMapper().readValue(fileName.toFile(), JsonNode.class);
        JIPipeProject project = new JIPipeProject();
        project.fromJson(jsonData, context, report, notifications);
        project.setWorkDirectory(fileName.getParent());
        project.validateUserDirectories(notifications);
        return project;
    }

    /**
     * Checks if the project metadata user directories are properly set up.
     * Otherwise, generates a notification
     * @param notifications the notifications
     */
    public void validateUserDirectories(JIPipeNotificationInbox notifications) {
        if(workDirectory != null) {
            Map<String, Path> directoryMap = metadata.getDirectories().getDirectoryMap(workDirectory);
            for (Map.Entry<String, Path> entry : directoryMap.entrySet()) {
                if(entry.getValue() == null || !Files.isDirectory(entry.getValue())) {
                    JIPipeNotification notification = new JIPipeNotification("org.hkijena.jipipe.core:invalid-project-user-directory");
                    notification.setHeading("Invalid project user directory!");
                    notification.setDescription("This project defines a user-defined directory '" + entry.getKey() + "' pointing at '" + entry.getValue() + "', but the " +
                            "referenced path does not exist.\n\nPlease open the project settings (Project > Project settings > User directories) and ensure that the directory is correctly configured.");
                    notifications.push(notification);
                }
            }
        }
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
            throw new JIPipeValidationRuntimeException(e, "Could not load dependencies from JIPipe project",
                    "The JSON data that describes the project dependencies is missing essential information",
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
            throw new JIPipeValidationRuntimeException(e, "Could not load metadata from JIPipe project",
                    "The JSON data that describes the project metadata is missing essential information",
                    "Open the file in a text editor and compare the metadata with a valid project.");
        }
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

    /**
     * Saves the project
     *
     * @param writer Target writer
     * @throws IOException Triggered by {@link ObjectMapper}
     */
    public void saveProject(Writer writer) throws IOException {
        ObjectMapper mapper = JsonUtils.getObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(writer, this);
    }

    /**
     * Saves the project
     *
     * @param outputStream Target writer
     * @throws IOException Triggered by {@link ObjectMapper}
     */
    public void saveProject(OutputStream outputStream) throws IOException {
        ObjectMapper mapper = JsonUtils.getObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(outputStream, this);
    }

    public JIPipeLocalProjectMemoryCache getCache() {
        return cache;
    }

    /**
     * @return The current project compartments
     */
    public BiMap<UUID, JIPipeProjectCompartment> getCompartments() {
        return ImmutableBiMap.copyOf(compartments);
    }

    public CompartmentAddedEventEmitter getCompartmentAddedEventEmitter() {
        return compartmentAddedEventEmitter;
    }

    public CompartmentRemovedEventEmitter getCompartmentRemovedEventEmitter() {
        return compartmentRemovedEventEmitter;
    }

    public JIPipeGraphNode.BaseDirectoryChangedEventEmitter getBaseDirectoryChangedEventEmitter() {
        return baseDirectoryChangedEventEmitter;
    }

    /**
     * Adds a new project compartment
     *
     * @param name Compartment name
     * @return The compartment
     */
    public JIPipeProjectCompartment addCompartment(String name) {
        JIPipeProjectCompartment compartment = JIPipe.createNode("jipipe:project-compartment");
        compartment.setRuntimeProject(this);
        compartment.setCustomName(name);
        compartmentGraph.insertNode(compartment);
        return compartment;
    }

    /**
     * Adds an existing compartment instance
     *
     * @param compartment Compartment
     * @param uuid        the UUID of the compartment
     * @return The compartment
     */
    public JIPipeProjectCompartment addCompartment(JIPipeProjectCompartment compartment, UUID uuid) {
        compartment.setRuntimeProject(this);
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
        compartment.setRuntimeProject(this);
        JIPipeCompartmentOutput compartmentOutput = null;
        UUID compartmentUUID = compartment.getProjectCompartmentUUID();
        for (JIPipeGraphNode node : graph.getGraphNodes()) {
            if (node instanceof JIPipeCompartmentOutput && Objects.equals(compartmentUUID, graph.getCompartmentUUIDOf(node))) {
                compartmentOutput = (JIPipeCompartmentOutput) node;
            }
        }
        if (compartmentOutput == null) {
            compartmentOutput = JIPipe.createNode("jipipe:compartment-output");
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
                for (JIPipeDataSlot sourceSlot : compartmentGraph.getInputIncomingSourceSlots(target.getFirstInputSlot())) {
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
        List<JIPipeGraphConnection> toDisconnect = new ArrayList<>();
        for (JIPipeGraphEdge edge : ImmutableList.copyOf(graph.getGraph().edgeSet())) {
            if (graph.getGraph().containsEdge(edge)) {
                JIPipeDataSlot source = graph.getGraph().getEdgeSource(edge);
                JIPipeDataSlot target = graph.getGraph().getEdgeTarget(edge);
                if (!source.getNode().isVisibleIn(target.getNode().getCompartmentUUIDInParentGraph())) {
                    toDisconnect.add(graph.getConnection(source, target));
                }
            }
        }

        // Apply fixes
        Set<UUID> fixedCompartments = new HashSet<>();
        for (JIPipeGraphConnection connection : toDisconnect) {
            JIPipeDataSlot source = connection.getSource();
            JIPipeDataSlot target = connection.getTarget();
            if (fixedCompartments.contains(target.getNode().getCompartmentUUIDInParentGraph())) {
                continue;
            }
            if (source.getNode() instanceof JIPipeCompartmentOutput) {
                if (!(target.getNode() instanceof IOInterfaceAlgorithm) || !source.getNode().getOutputSlotMap().keySet().equals(target.getNode().getInputSlotMap().keySet())) {
                    // Place IOInterface at the same location as the compartment output
                    IOInterfaceAlgorithm ioInterfaceAlgorithm = JIPipe.createNode(IOInterfaceAlgorithm.class);
                    ioInterfaceAlgorithm.getSlotConfiguration().setTo(source.getNode().getSlotConfiguration());
                    ioInterfaceAlgorithm.setLocations(source.getNode().getLocations());
                    graph.insertNode(ioInterfaceAlgorithm, target.getNode().getCompartmentUUIDInParentGraph());

                    for (JIPipeOutputDataSlot outputSlot : source.getNode().getOutputSlots()) {
                        for (JIPipeDataSlot outputOutgoingTargetSlot : graph.getOutputOutgoingTargetSlots(outputSlot)) {
                            if (Objects.equals(outputOutgoingTargetSlot.getNode().getCompartmentUUIDInParentGraph(), target.getNode().getCompartmentUUIDInParentGraph())) {
                                graph.connect(ioInterfaceAlgorithm.getOutputSlot(outputSlot.getName()), outputOutgoingTargetSlot);
                            }
                        }
                    }

                    fixedCompartments.add(target.getNode().getCompartmentUUIDInParentGraph());
                }
            }
        }


        for (JIPipeGraphConnection connection : toDisconnect) {
            graph.disconnect(connection, false);
            changed = true;
        }


        if (changed) {
            graph.getGraphChangedEventEmitter().emit(new JIPipeGraph.GraphChangedEvent(graph));
        }
    }

    /**
     * Triggered when the compartment graph is changed
     *
     * @param event Generated event
     */
    @Override
    public void onGraphChanged(JIPipeGraph.GraphChangedEvent event) {
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
    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        graph.reportValidity(context, report);
    }

    /**
     * Reports the validity for the target node and its dependencies
     *
     * @param context the context
     * @param report      the report
     * @param targetNode  the target node
     */
    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report, JIPipeGraphNode targetNode) {
        graph.reportValidity(context, report, targetNode);
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

        JIPipeCompartmentOutput outputNode = compartment.getOutputNode();

        // Search for all targets of the compartment and convert this output into an IOInterface
        for (JIPipeDataSlot outputOutgoingTargetSlot : compartmentGraph.getOutputOutgoingTargetSlots(compartment.getFirstOutputSlot())) {
            if (outputOutgoingTargetSlot.getNode() instanceof JIPipeProjectCompartment) {
                JIPipeProjectCompartment targetCompartment = (JIPipeProjectCompartment) outputOutgoingTargetSlot.getNode();

                // Check for the special case: all target nodes of the output are IOInterface with the same set of slots
                boolean needsFixing = false;
                for (JIPipeOutputDataSlot outputSlot : outputNode.getOutputSlots()) {
                    for (JIPipeDataSlot outgoingTargetSlot : graph.getOutputOutgoingTargetSlots(outputSlot)) {
                        if (outgoingTargetSlot.getNode() instanceof IOInterfaceAlgorithm &&
                                outgoingTargetSlot.getNode().getInputSlotMap().keySet().equals(outputNode.getOutputSlotMap().keySet())) {
                            // Do nothing
                        } else {
                            needsFixing = true;
                        }
                    }
                }

                if (needsFixing) {
                    IOInterfaceAlgorithm ioInterfaceAlgorithm = JIPipe.createNode(IOInterfaceAlgorithm.class);
                    ioInterfaceAlgorithm.setCustomName(outputNode.getName());
                    ioInterfaceAlgorithm.getSlotConfiguration().setTo(outputNode.getSlotConfiguration());
                    ioInterfaceAlgorithm.setLocations(outputNode.getLocations());
                    graph.insertNode(ioInterfaceAlgorithm, targetCompartment.getProjectCompartmentUUID());

                    for (JIPipeOutputDataSlot outputSlot : outputNode.getOutputSlots()) {
                        for (JIPipeDataSlot outgoingTargetSlot : graph.getOutputOutgoingTargetSlots(outputSlot)) {
                            graph.connect(ioInterfaceAlgorithm.getOutputSlot(outputSlot.getName()), outgoingTargetSlot);
                        }
                    }
                }

            }
        }


        // Delete the compartment
        UUID compartmentId = compartment.getProjectCompartmentUUID();
        graph.removeCompartment(compartmentId);
        compartments.remove(compartmentId);
        updateCompartmentVisibility();
        compartmentGraph.removeNode(compartment, false);
        compartmentRemovedEventEmitter.emit(new CompartmentRemovedEvent(compartment, compartmentId));
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

        // Set this as base and project directory for the main nodes
        for (JIPipeGraphNode node : graph.getGraphNodes()) {
            node.setBaseDirectory(workDirectory);
            node.setProjectDirectory(workDirectory);
        }
        baseDirectoryChangedEventEmitter.emit(new JIPipeGraphNode.BaseDirectoryChangedEvent(this, workDirectory));
    }

    /**
     * @return All project dependencies
     */
    public Set<JIPipeDependency> getSimplifiedMinimalDependencies() {
        Set<JIPipeDependency> dependencies = graph.getDependencies();
        dependencies.addAll(compartmentGraph.getDependencies());
        return JIPipeDependency.simplifyAndMinimize(dependencies, true);
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
        generator.writeObjectField("dependencies", getSimplifiedMinimalDependencies());
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
     * @param jsonNode      the node
     * @param context the context
     * @param notifications notifications for the user
     */
    public void fromJson(JsonNode jsonNode, JIPipeValidationReportContext context, JIPipeValidationReport report, JIPipeNotificationInbox notifications) throws IOException {
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
                        ParameterUtils.deserializeParametersFromJson(metadata, metadataEntry.getValue(), context, report);
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
            graph.fromJson(jsonNode.get("graph"), context, new JIPipeValidationReport(), notifications);

            // read compartments
            compartmentGraph.fromJson(jsonNode.get("compartments").get("compartment-graph"), context, new JIPipeValidationReport(), notifications);

            // Fix legacy nodes
            for (Map.Entry<UUID, String> entry : graph.getNodeLegacyCompartmentIDs().entrySet()) {
                JIPipeGraphNode compartmentNode = compartmentGraph.findNode(entry.getValue());
                JIPipeGraphNode node = graph.getNodeByUUID(entry.getKey());
                if (compartmentNode != null) {
                    JIPipe.getInstance().getLogService().info("[Project format conversion] Fix legacy compartment '" + entry.getValue() + "' --> " + compartmentNode.getUUIDInParentGraph());
                    graph.setCompartment(entry.getKey(), compartmentNode.getUUIDInParentGraph());
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
                    compartment.setRuntimeProject(this);
                    compartments.put(compartment.getProjectCompartmentUUID(), compartment);
                    initializeCompartment(compartment);
                }
            }

            // Reading compartments might break some connections. This will restore them
            graph.edgesFromJson(jsonNode.get("graph"), context, report);

            // Update node visibilities
            updateCompartmentVisibility();

            // Checking for error
            for (JIPipeGraphNode graphNode : ImmutableList.copyOf(graph.getGraphNodes())) {
                UUID compartmentUUIDInGraph = graphNode.getCompartmentUUIDInParentGraph();
                if (compartmentUUIDInGraph == null || !compartments.containsKey(compartmentUUIDInGraph)) {
                    report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Warning,
                            new GraphNodeValidationReportContext(graphNode),
                            "Node has no compartment!",
                            "The node '" + graphNode.getDisplayName() + "' has no compartment assigned!",
                            "This was repaired automatically by deleting the node. Please inform the JIPipe developers about this issue.",
                            JsonUtils.toPrettyJsonString(graphNode)));
                    graph.removeNode(graphNode, false);
                } else {
                    JIPipeGraphNode compartmentNode = compartmentGraph.getNodeByUUID(compartmentUUIDInGraph);
                    if (compartmentNode == null) {
                        report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Warning,
                                new GraphNodeValidationReportContext(compartmentNode),
                                "Node has invalid compartment!",
                        "The node '" + graphNode.getDisplayName() + "' is assigned to compartment '" + compartmentUUIDInGraph + "', but it does not exist!",
                                "This was repaired automatically by deleting the node. Please inform the JIPipe developers about this issue.",
                                JsonUtils.toPrettyJsonString(graphNode)));
                        graph.removeNode(graphNode, false);
                    }
                }
            }
        } finally {
            isLoading = false;
        }
    }

    /**
     * Rebuilds the compartment list from the current state of the compartment graph
     */
    public void rebuildCompartmentsFromGraph() {
        compartments.clear();
        for (JIPipeGraphNode node : compartmentGraph.getGraphNodes()) {
            if (node instanceof JIPipeProjectCompartment) {
                JIPipeProjectCompartment compartment = (JIPipeProjectCompartment) node;
                compartment.setRuntimeProject(this);
                compartments.put(compartment.getProjectCompartmentUUID(), compartment);
                initializeCompartment(compartment);
            }
        }
        updateCompartmentVisibility();
    }

    /**
     * Returns a list of all nodes that cannot be executed or are deactivated by the user.
     * This method works on transitive deactivation (e.g. a dependency is deactivated).
     *
     * @return set of deactivated nodes
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
                if (graph.getOutputOutgoingTargetSlots(outputSlot).isEmpty()) {
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
                if (heavy && !graph.getOutputOutgoingTargetSlots(outputSlot).isEmpty()) {
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
                        if (graphNode instanceof JIPipeCompartmentOutput && Objects.equals(graphNode.getCompartmentUUIDInParentGraph(), projectCompartmentUUID)) {
                            compartment.setOutputNode((JIPipeCompartmentOutput) graphNode);
                            found = true;
                            break;
                        }
                    }

                    // Create a new one
                    if (!found) {
                        JIPipeCompartmentOutput compartmentOutput = JIPipe.createNode("jipipe:compartment-output");
                        compartmentOutput.setCustomName(compartment.getName() + " output");
                        graph.insertNode(compartmentOutput, compartment.getProjectCompartmentUUID());
                        compartment.setOutputNode(compartmentOutput);
                    }
                }
            }
        }
    }

    public JIPipeProjectHistoryJournal getHistoryJournal() {
        return historyJournal;
    }

    /**
     * Gets all examples for a node type ID.
     * Includes project templates
     *
     * @param nodeTypeId the ID
     * @return the examples
     */
    public List<JIPipeNodeExample> getNodeExamples(String nodeTypeId) {
        List<JIPipeNodeExample> result = new ArrayList<>(JIPipe.getNodes().getNodeExamples(nodeTypeId));
        for (JIPipeNodeTemplate nodeTemplate : NodeTemplateSettings.getInstance().getNodeTemplates()) {
            JIPipeNodeExample example = new JIPipeNodeExample(nodeTemplate);
            if (Objects.equals(example.getNodeId(), nodeTypeId)) {
                example.setSourceInfo("From node templates (global)");
                result.add(example);
            }
        }
        for (JIPipeNodeTemplate nodeTemplate : metadata.getNodeTemplates()) {
            JIPipeNodeExample example = new JIPipeNodeExample(nodeTemplate);
            example.setSourceInfo("From node templates (project)");
            if (Objects.equals(example.getNodeId(), nodeTypeId)) {
                result.add(example);
            }
        }
        result.sort(Comparator.comparing((JIPipeNodeExample example) -> example.getNodeTemplate().getName(), NaturalOrderComparator.INSTANCE));
        return result;
    }

    /**
     * Gets a map of user-defined directories
     */
    public Map<String, Path> getDirectoryMap() {
        return metadata.getDirectories().getDirectoryMap(getWorkDirectory());
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
            project.fromJson(node, new UnspecifiedValidationReportContext(), new JIPipeValidationReport(), new JIPipeNotificationInbox());
            return project;
        }
    }

    /**
     * Triggered when a sample is added to an {@link JIPipeProject}
     */
    public static class CompartmentAddedEvent extends AbstractJIPipeEvent {
        private final JIPipeProjectCompartment compartment;

        /**
         * @param compartment the compartment
         */
        public CompartmentAddedEvent(JIPipeProjectCompartment compartment) {
            super(compartment);
            this.compartment = compartment;
        }

        public JIPipeProjectCompartment getCompartment() {
            return compartment;
        }
    }

    public interface CompartmentAddedEventListener {
        void onProjectCompartmentAdded(CompartmentAddedEvent event);
    }

    public static class CompartmentAddedEventEmitter extends JIPipeEventEmitter<CompartmentAddedEvent, CompartmentAddedEventListener> {
        @Override
        protected void call(CompartmentAddedEventListener compartmentAddedEventListener, CompartmentAddedEvent event) {
            compartmentAddedEventListener.onProjectCompartmentAdded(event);
        }
    }

    /**
     * Triggered when a sample is removed from an {@link JIPipeProject}
     */
    public static class CompartmentRemovedEvent extends AbstractJIPipeEvent {
        private final UUID compartmentUUID;
        private final JIPipeProjectCompartment compartment;

        /**
         * @param compartment     the compartment
         * @param compartmentUUID the compartment id
         */
        public CompartmentRemovedEvent(JIPipeProjectCompartment compartment, UUID compartmentUUID) {
            super(compartment);
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

    public interface CompartmentRemovedEventListener {
        void onProjectCompartmentRemoved(CompartmentRemovedEvent event);
    }

    public static class CompartmentRemovedEventEmitter extends JIPipeEventEmitter<CompartmentRemovedEvent, CompartmentRemovedEventListener> {

        @Override
        protected void call(CompartmentRemovedEventListener compartmentRemovedEventListener, CompartmentRemovedEvent event) {
            compartmentRemovedEventListener.onProjectCompartmentRemoved(event);
        }
    }
}
