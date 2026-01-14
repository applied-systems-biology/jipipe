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

package org.hkijena.jipipe.api.project;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.*;
import ij.IJ;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.*;
import org.hkijena.jipipe.api.cache.JIPipeLocalProjectMemoryCache;
import org.hkijena.jipipe.api.compartments.algorithms.IOInterfaceAlgorithm;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartmentOutput;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.environments.*;
import org.hkijena.jipipe.api.environments.sources.JIPipeEnvironmentConfiguratorProjectSource;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.history.JIPipeProjectHistoryJournal;
import org.hkijena.jipipe.api.metadata.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.notifications.JIPipeNotification;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.api.runtimepartitioning.JIPipeRuntimePartition;
import org.hkijena.jipipe.api.runtimepartitioning.JIPipeRuntimePartitionConfiguration;
import org.hkijena.jipipe.api.service.components.JIPipeEnvironmentsServiceComponent;
import org.hkijena.jipipe.api.settings.JIPipeProjectSettingsSheet;
import org.hkijena.jipipe.api.validation.*;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.plugins.parameters.api.optional.JIPipeOptionalParameter;
import org.hkijena.jipipe.plugins.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.settings.application.JIPipeProjectAuthorsApplicationSettings;
import org.hkijena.jipipe.plugins.settings.application.JIPipeProjectDefaultsApplicationSettings;
import org.hkijena.jipipe.plugins.settings.application.JIPipeRuntimeApplicationSettings;
import org.hkijena.jipipe.plugins.settings.project.JIPipeDataStorageProjectSettings;
import org.hkijena.jipipe.utils.*;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.scijava.Disposable;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

/**
 * A JIPipe project.
 * It contains all information to set up and run an analysis
 */
@JsonSerialize(using = JIPipeProject.Serializer.class)
@JsonDeserialize(using = JIPipeProject.Deserializer.class)
public class JIPipeProject implements JIPipeValidatable {

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
    private final JIPipeRunnableQueue snapshotQueue = new JIPipeRunnableQueue("History");
    private final CompartmentAddedEventEmitter compartmentAddedEventEmitter = new CompartmentAddedEventEmitter();
    private final CompartmentRemovedEventEmitter compartmentRemovedEventEmitter = new CompartmentRemovedEventEmitter();
    private final JIPipeGraphNode.BaseDirectoryChangedEventEmitter baseDirectoryChangedEventEmitter = new JIPipeGraphNode.BaseDirectoryChangedEventEmitter();
    private final Map<String, JIPipeProjectSettingsSheet> settingsSheets = new HashMap<>();
    private final Map<String, JsonNode> unloadedSettingsSheets = new HashMap<>();
    private JIPipeProjectMetadata metadata = new JIPipeProjectMetadata();
    private JIPipeRuntimePartitionConfiguration runtimePartitions = new JIPipeRuntimePartitionConfiguration();
    private JIPipeProjectRunSetsConfiguration runSetsConfiguration = new JIPipeProjectRunSetsConfiguration();
    private Map<String, JIPipeMetadataObject> additionalMetadata = new HashMap<>();
    private Path workDirectory;
    private Path temporaryBaseDirectory;
    private boolean isCleaningUp;
    private boolean isLoading;
    private Path projectFile;
    private String projectJIPipeVersion = JIPipe.getJIPipeVersion();
    private FileLocker temporaryBaseDirectoryLocker;
    private boolean disposed;

    /**
     * A JIPipe project
     */
    public JIPipeProject() {
        this.historyJournal = new JIPipeProjectHistoryJournal(this, snapshotQueue);
        this.cache = new JIPipeLocalProjectMemoryCache(this);
        this.metadata.setDescription(new HTMLText());
        this.graph.attach(JIPipeProject.class, this);
        this.graph.attach(JIPipeGraphType.Project);
        this.compartmentGraph.attach(JIPipeProject.class, this);
        this.compartmentGraph.attach(JIPipeGraphType.ProjectCompartments);

        // Init default partitions
        {
            JIPipeRuntimePartition fileSystemPartition = new JIPipeRuntimePartition();
            fileSystemPartition.setName("Filesystem");
            fileSystemPartition.setDescription(new HTMLText("Pre-defined partition useful for separating off filesystem operations"));
            fileSystemPartition.setColor(new OptionalColorParameter(new Color(0x93C6A2), true));
            this.runtimePartitions.add(fileSystemPartition);
        }
        {
            JIPipeRuntimePartition statisticsPartition = new JIPipeRuntimePartition();
            statisticsPartition.setName("Statistics");
            statisticsPartition.setDescription(new HTMLText("Pre-defined partition useful for separating off statistics and postprocessing operations"));
            statisticsPartition.setColor(new OptionalColorParameter(new Color(0xbd93c4), true));
            this.runtimePartitions.add(statisticsPartition);
        }
        {
            JIPipeRuntimePartition visualizationPartition = new JIPipeRuntimePartition();
            visualizationPartition.setName("Visualization");
            visualizationPartition.setDescription(new HTMLText("Pre-defined partition useful for separating off visualization and postprocessing operations"));
            visualizationPartition.setColor(new OptionalColorParameter(new Color(0x93bdc4), true));
            this.runtimePartitions.add(visualizationPartition);
        }
        {
            JIPipeRuntimePartition postprocessingPartition = new JIPipeRuntimePartition();
            postprocessingPartition.setName("Postprocessing");
            postprocessingPartition.setDescription(new HTMLText("Pre-defined partition useful for separating off postprocessing operations"));
            postprocessingPartition.setColor(new OptionalColorParameter(new Color(0xc4b693), true));
            this.runtimePartitions.add(postprocessingPartition);
        }

        // Init default settings
        for (Map.Entry<String, Class<? extends JIPipeProjectSettingsSheet>> entry : JIPipe.getInstance().getProjectSettings().getRegisteredSheetTypes().entrySet()) {
            JIPipeProjectSettingsSheet sheet = (JIPipeProjectSettingsSheet) ReflectionUtils.newInstance(entry.getValue());
            sheet.initialize(this);
            settingsSheets.put(entry.getKey(), sheet);
        }

        // Update the graph compartments
        compartmentGraph.getGraphChangedEventEmitter().subscribe(event -> {
            if (isCleaningUp) {
                return;
            }
            if (isLoading) {
                return;
            }
            if (event.getGraph() == compartmentGraph) {
                for (JIPipeGraphNode algorithm : compartmentGraph.getGraphNodes()) {
                    if (algorithm instanceof JIPipeProjectCompartment) {
                        JIPipeProjectCompartment compartment = (JIPipeProjectCompartment) algorithm;
                        if (!compartment.isInitialized()) {
                            compartments.put(compartment.getProjectCompartmentUUID(), compartment);
                            updateCompartmentOutputs(compartment);
                        }
                    }
                }
                updateCompartmentVisibility();
            }
        });
    }

    /**
     * Loads a project from a file
     *
     * @param fileName     JSON file
     * @param context      the context
     * @param report       issue report
     * @param progressInfo the progress info
     * @return Loaded project
     * @throws IOException Triggered by {@link ObjectMapper}
     */
    public static JIPipeProject loadProject(Path fileName, JIPipeValidationReportContext context, JIPipeValidationReport report, JIPipeProgressInfo progressInfo) throws IOException {
        return loadProject(fileName, context, report, new JIPipeNotificationInbox(), progressInfo);
    }

    /**
     * Loads a project from a file
     *
     * @param fileName      JSON file
     * @param context       the context
     * @param report        issue report
     * @param notifications notifications for the user
     * @param progressInfo  the progress info
     * @return Loaded project
     * @throws IOException Triggered by {@link ObjectMapper}
     */
    public static JIPipeProject loadProject(Path fileName, JIPipeValidationReportContext context, JIPipeValidationReport report, JIPipeNotificationInbox notifications, JIPipeProgressInfo progressInfo) throws IOException {
        JsonNode jsonData = JsonUtils.getObjectMapper().readValue(fileName.toFile(), JsonNode.class);
        JIPipeProject project = new JIPipeProject();
        project.fromJson(jsonData, context, report, notifications, progressInfo);
        project.setWorkDirectory(fileName.getParent());
        project.validateUserPaths(notifications);
        if(JIPipeProjectDefaultsApplicationSettings.getInstance().isAutoFixProjectEnvironments()) {
            project.fixBrokenEnvironments(progressInfo.resolve("Fix environments"));
        }
        project.projectFile = fileName;
        return project;
    }

    /**
     * Deserializes the set of project dependencies from JSON.
     * Does not require the dependencies to be actually registered.
     *
     * @param node JSON node
     * @return The dependencies as {@link JIPipeMutableDependency}
     */
    public static Set<JIPipeDependency> loadDependenciesFromJson(JsonNode node) {
        node = node.path("dependencies");
        if (node.isMissingNode()) {
            return new HashSet<>();
        }
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
        if (node.isMissingNode()) {
            return new JIPipeProjectMetadata();
        }
        try {
            return JsonUtils.getObjectMapper().readerFor(JIPipeProjectMetadata.class).readValue(node);
        } catch (IOException e) {
            throw new JIPipeValidationRuntimeException(e, "Could not load metadata from JIPipe project",
                    "The JSON data that describes the project metadata is missing essential information",
                    "Open the file in a text editor and compare the metadata with a valid project.");
        }
    }

    /**
     * Checks if the project metadata user paths are properly set up.
     * Otherwise, generates a notification
     *
     * @param notifications the notifications
     */
    public void validateUserPaths(JIPipeNotificationInbox notifications) {
        if (workDirectory != null) {
            Map<String, Path> directoryMap = metadata.getUserPaths().getMandatoryUserPathsMap(workDirectory);
            for (Map.Entry<String, Path> entry : directoryMap.entrySet()) {
                if (entry.getValue() == null || !Files.exists(entry.getValue())) {
                    JIPipeNotification notification = new JIPipeNotification("org.hkijena.jipipe.core:invalid-project-user-directory");
                    notification.setHeading("Invalid project user path!");
                    notification.setDescription("This project defines a project user path '" + entry.getKey() + "' pointing at '" + entry.getValue() + "', but the " +
                            "referenced path does not exist.\n\nPlease open the project settings (Project > Project settings > User paths // Project > Project overview > User paths) " +
                            "and ensure that the directory is correctly configured.");
                    notifications.push(notification);
                }
            }
        }
    }

    /**
     * Gets the path to a non-existing file in the project's temporary directory
     *
     * @param baseName the base name (prefix)
     * @param suffix   the suffix (extension)
     * @return the path
     */
    public Path newTemporaryFilePath(String baseName, String suffix) {
        return PathUtils.createSubTempFilePath(getTemporaryBaseDirectory(), baseName, suffix);
    }

    /**
     * Creates a new temporary subdirectory in the project's temporary directory
     *
     * @param baseName the base name (prefix)
     * @return the temporary directory
     */
    public Path newTemporaryDirectory(String baseName) {
        return PathUtils.createTempSubDirectory(getTemporaryBaseDirectory(), baseName);
    }

    /**
     * Creates a new temporary subdirectory in the project's temporary directory
     *
     * @return the temporary directory
     */
    public Path newTemporaryDirectory() {
        return PathUtils.createTempSubDirectory(getTemporaryBaseDirectory());
    }

    public Path getTemporaryBaseDirectory() {
        JIPipeDataStorageProjectSettings settings = getSettingsSheet(JIPipeDataStorageProjectSettings.class);
        Path output;
        if (settings.isForceGlobalTempDirectory() || !JIPipeRuntimeApplicationSettings.getInstance().isPerProjectTempDirectory()) {
            output = JIPipe.getTemporaryBaseDirectory();
        } else if (settings.getOverrideTempDirectory().isEnabled() && settings.getOverrideTempDirectory().getContent() != null && settings.getOverrideTempDirectory().getContent().isAbsolute()) {
            output = settings.getOverrideTempDirectory().getContent();
        } else if (workDirectory != null) {
            if (settings.getOverrideTempDirectory().isEnabled()) {
                output = workDirectory.resolve(settings.getOverrideTempDirectory().getContent());
            } else {
                output = workDirectory.resolve("JIPipe.tmp.dir");
            }
        } else {
            output = JIPipe.getTemporaryBaseDirectory();
        }

        // Ensure that output is always parented at "JIPipe.tmp.dir"
        if(!output.getFileName().toString().equals("JIPipe.tmp.dir")) {
            output = output.resolve("JIPipe.tmp.dir");
        }

        // Create subdirectory
        if (temporaryBaseDirectory == null || !temporaryBaseDirectory.startsWith(output)) {
            PathUtils.createDirectories(output);
            output = PathUtils.createTempSubDirectory(output);
        } else {
            output = temporaryBaseDirectory;
        }

        // Release any old tmp handles
        if(!output.equals(temporaryBaseDirectory) && temporaryBaseDirectoryLocker != null) {
            temporaryBaseDirectoryLocker.releaseLock();
            temporaryBaseDirectoryLocker = null;
        }

        PathUtils.createDirectories(output);
        temporaryBaseDirectory = output;

        // Acquire new lock if necessary
        if(temporaryBaseDirectoryLocker == null) {
            temporaryBaseDirectoryLocker = new FileLocker(JIPipeProgressInfo.SILENT, temporaryBaseDirectory.resolve("lock"));
            boolean success = false;
            if(temporaryBaseDirectoryLocker.tryWriteLock()) {
                success = temporaryBaseDirectoryLocker.acquireWriteLock();
            }
            if(!success) {
                IJ.handleException(new IOException("Unable to acquire project temporary directory lock on " + temporaryBaseDirectory));
                temporaryBaseDirectoryLocker.releaseLock();
                temporaryBaseDirectoryLocker = null;
            }
        }

        return output;
    }

    /**
     * Finds a compartment by UUID or alias
     *
     * @param uuidOrAlias UUID or alias
     * @return the compartment or null if it could not be found
     */
    public JIPipeProjectCompartment findCompartment(String uuidOrAlias) {
        JIPipeGraphNode node = compartmentGraph.findNode(uuidOrAlias);
        if (node instanceof JIPipeProjectCompartment) {
            return (JIPipeProjectCompartment) node;
        } else {
            return null;
        }
    }

    public JIPipeRunnableQueue getSnapshotQueue() {
        return snapshotQueue;
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
     * @param fileName       Target file
     * @param updateSavePath if the internally tracked project file path should be updated to the new fileName
     * @throws IOException Triggered by {@link ObjectMapper}
     */
    public void saveProject(Path fileName, boolean updateSavePath) throws IOException {

        // Add authors from global list
        if (metadata.isAutoAddAuthors()) {
            addAuthorsFromGlobalList();
        }

        ObjectMapper mapper = JsonUtils.getObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(fileName.toFile(), this);

        if (updateSavePath) {
            projectFile = fileName;
        }
    }

    private void addAuthorsFromGlobalList() {
        JIPipeProjectAuthorsApplicationSettings settings = JIPipeProjectAuthorsApplicationSettings.getInstance();
        if (settings.isAutomaticallyAddToProjects()) {
            for (OptionalJIPipeAuthorMetadata projectAuthor_ : settings.getProjectAuthors()) {
                if (projectAuthor_.isEnabled()) {
                    JIPipeAuthorMetadata projectAuthor = projectAuthor_.getContent();
                    Optional<JIPipeAuthorMetadata> existing = metadata.getAuthors().stream().filter(a -> a.fuzzyEquals(projectAuthor)).findFirst();
                    if (existing.isPresent()) {
                        existing.get().mergeWith(projectAuthor);
                    } else {
                        metadata.getAuthors().add(projectAuthor);
                    }
                }
            }
        }
    }

    public void close(JIPipeProgressInfo progressInfo) {

        this.disposed = true;

        // Clear the cache
        progressInfo.log("Clearing cache ...");
        cache.clearAll(progressInfo.resolve("Clear cache"));

        if(temporaryBaseDirectoryLocker != null) {
            // Release temporary directory lock
            progressInfo.log("Releasing temporary directory lock on " + getTemporaryBaseDirectory() + " ...");
            temporaryBaseDirectoryLocker.releaseLock();
        }

        // Clear temporary directory
        if(JIPipeRuntimeApplicationSettings.getInstance().isAutoCleanClosedProjectTemporaryDirectories()) {
            progressInfo.log("Clearing temporary directory ...");
            try {
                PathUtils.deleteDirectoryRecursively(getTemporaryBaseDirectory(), progressInfo.resolve("Delete temporary directory"));
            } catch (Exception e) {
                progressInfo.log(e);
            }
        }
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

    public Map<String, JIPipeProjectSettingsSheet> getSettingsSheets() {
        return ImmutableMap.copyOf(settingsSheets);
    }

    public <T extends JIPipeProjectSettingsSheet> T getSettingsSheet(String id, Class<T> klass) {
        return (T) settingsSheets.getOrDefault(id, null);
    }

    public <T extends JIPipeProjectSettingsSheet> T getSettingsSheet(Class<T> klass) {
        for (JIPipeProjectSettingsSheet settingsSheet : settingsSheets.values()) {
            if (klass.isAssignableFrom(settingsSheet.getClass())) {
                return (T) settingsSheet;
            }
        }
        return null;
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
        UUID uuid = compartmentGraph.insertNode(compartment);
        compartments.put(uuid, compartment);
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

    /**
     * Validates the ability to create a project archive
     *
     * @return the report
     */
    public JIPipeValidationReport validateArchivability() {
        JIPipeValidationReport report = new JIPipeValidationReport();
        if (getWorkDirectory() == null) {
            new UnspecifiedValidationReportContext().error()
                    .title("Project must be saved")
                    .explanation("The project must be saved at least once")
                    .report(report);
        } else {
            for (JIPipeGraphNode node : graph.getGraphNodes()) {
                node.archiveReportValidation(JIPipeValidationReportContext.UNSPECIFIED.node(node), report, getWorkDirectory());
            }
        }
        return report;
    }

    public void updateCompartmentOutputs(JIPipeProjectCompartment compartment) {
        compartment.setRuntimeProject(this);
        UUID compartmentUUID = compartment.getProjectCompartmentUUID();

        // Ensure that the names are correct
        for (Map.Entry<String, JIPipeProjectCompartmentOutput> entry : compartment.getOutputNodes().entrySet()) {
            entry.getValue().setOutputSlotName(entry.getKey());
        }

        // Find all outputs that should be deleted
        Set<JIPipeProjectCompartmentOutput> toDelete = new HashSet<>();
        for (Map.Entry<String, JIPipeProjectCompartmentOutput> entry : compartment.getOutputNodes().entrySet()) {
            if (!compartment.getOutputSlotMap().containsKey(entry.getKey())) {
                toDelete.add(entry.getValue());
            }
        }

        // Delete the outputs
        for (JIPipeProjectCompartmentOutput compartmentOutput : toDelete) {
            graph.removeNode(compartmentOutput, false);
            compartment.getOutputNodes().remove(compartmentOutput.getOutputSlotName());
        }

        // Add new outputs
        for (JIPipeOutputDataSlot outputSlot : compartment.getOutputSlots()) {
            if (!compartment.getOutputNodes().containsKey(outputSlot.getName())) {
                JIPipeProjectCompartmentOutput node = null;

                // First try to search for an existing node
                for (JIPipeGraphNode graphNode : graph.getGraphNodes()) {
                    if (graphNode instanceof JIPipeProjectCompartmentOutput &&
                            Objects.equals(graphNode.getCompartmentUUIDInParentGraph(), compartmentUUID) &&
                            Objects.equals(((JIPipeProjectCompartmentOutput) graphNode).getOutputSlotName(), outputSlot.getName())) {
                        node = (JIPipeProjectCompartmentOutput) graphNode;
                        break;
                    }
                }

                // Try to find a legacy (has no output slot name)
                if (compartment.getOutputSlots().size() == 1) {
                    for (JIPipeGraphNode graphNode : graph.getGraphNodes()) {
                        if (graphNode instanceof JIPipeProjectCompartmentOutput &&
                                Objects.equals(graphNode.getCompartmentUUIDInParentGraph(), compartmentUUID) &&
                                StringUtils.isNullOrEmpty(((JIPipeProjectCompartmentOutput) graphNode).getOutputSlotName())) {
                            System.out.println("[project loading] Successfully matched legacy compartment output " + graphNode.getUUIDInParentGraph() + " to compartment output slot " + outputSlot.getName());
                            node = (JIPipeProjectCompartmentOutput) graphNode;
                            break;
                        }
                    }
                }

                // No node present, so create one
                if (node == null) {
                    node = JIPipe.createNode(JIPipeProjectCompartmentOutput.class);
                    node.setOutputSlotName(outputSlot.getName());
                    graph.insertNode(node, compartmentUUID);
                } else {
                    node.setOutputSlotName(outputSlot.getName());
                }

                compartment.getOutputNodes().put(outputSlot.getName(), node);
            }
        }

    }

    private void updateCompartmentVisibility() {
        boolean changed = false;

        // Remember old visibilities and clear the existing map
        Map<JIPipeProjectCompartmentOutput, Set<UUID>> oldVisibleCompartments = new HashMap<>();

        for (JIPipeProjectCompartment compartment : compartments.values()) {
            for (JIPipeProjectCompartmentOutput compartmentOutput : compartment.getOutputNodes().values()) {
                Set<UUID> visibleCompartmentUUIDs = graph.getVisibleCompartmentUUIDsOf(compartmentOutput);
                oldVisibleCompartments.put(compartmentOutput, new HashSet<>(visibleCompartmentUUIDs));
                visibleCompartmentUUIDs.clear();
            }
        }

        // Add the visibilities back in
        for (JIPipeGraphNode edgeTarget : compartmentGraph.getGraphNodes()) {
            if (edgeTarget instanceof JIPipeProjectCompartment) {
                JIPipeProjectCompartment targetProjectCompartment = (JIPipeProjectCompartment) edgeTarget;
                for (JIPipeDataSlot sourceSlot : compartmentGraph.getInputIncomingSourceSlots(targetProjectCompartment.getFirstInputSlot())) {
                    if (sourceSlot.getNode() instanceof JIPipeProjectCompartment) {
                        JIPipeProjectCompartment edgeSource = (JIPipeProjectCompartment) sourceSlot.getNode();
                        String outputNameToShow = sourceSlot.getName();

                        // Grab the output of the edgeSource that we want to show in edgeTarget
                        JIPipeProjectCompartmentOutput outputToShow = edgeSource.getOutputNode(outputNameToShow);
                        Set<UUID> visibleCompartmentUUIDs = graph.getVisibleCompartmentUUIDsOf(outputToShow);
                        visibleCompartmentUUIDs.add(targetProjectCompartment.getProjectCompartmentUUID());
                    }
                }
            }
        }

        // Check for changes
        for (JIPipeProjectCompartment compartment : compartments.values()) {
            for (JIPipeProjectCompartmentOutput compartmentOutput : compartment.getOutputNodes().values()) {
                Set<UUID> visibleCompartmentUUIDs = graph.getVisibleCompartmentUUIDsOf(compartmentOutput);
                if (!Objects.equals(visibleCompartmentUUIDs, oldVisibleCompartments.get(compartmentOutput))) {
                    changed = true;
                    break;
                }
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
            if (source.getNode() instanceof JIPipeProjectCompartmentOutput) {
                if (!(target.getNode() instanceof IOInterfaceAlgorithm) || !source.getNode().getOutputSlotMap().keySet().equals(target.getNode().getInputSlotMap().keySet())) {
                    // Place IOInterface at the same location as the compartment output
                    IOInterfaceAlgorithm ioInterfaceAlgorithm = JIPipe.createNode(IOInterfaceAlgorithm.class);
                    ioInterfaceAlgorithm.getSlotConfiguration().setTo(source.getNode().getSlotConfiguration());
                    ioInterfaceAlgorithm.getNodeMetadata().putAll(source.getNode().getNodeMetadata());
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

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReportSettings reportSettings, JIPipeValidationReport report, JIPipeProgressInfo progressInfo) {
        graph.reportValidity(reportContext, reportSettings, report, progressInfo);
        JIPipeEnvironmentConfigurationCache configurationCache = new JIPipeEnvironmentConfigurationCache();

        // Check environments
        Set<JIPipeEnvironment> checkedEnvironments = new HashSet<>();
        List<JIPipeEnvironmentConfigurator<?>> allEnvironmentReferences = new ArrayList<>();
        for (JIPipeGraphNode node : graph.getGraphNodes()) {
            node.getEnvironmentDependencies(allEnvironmentReferences, configurationCache);
        }
        for (JIPipeEnvironmentConfigurator<?> environmentReference : allEnvironmentReferences) {
            JIPipeEnvironment environment = environmentReference.getBaseEnvironment();
            if (!checkedEnvironments.contains(environment)) {
                environmentReference.reportValidity(reportContext, reportSettings, report, progressInfo);
                checkedEnvironments.add(environment);
            }
        }
    }

    /**
     * Fixes broken project-wide environments and corrects the associated settings
     *
     * @param progressInfo the progress info
     */
    public void fixBrokenEnvironments(JIPipeProgressInfo progressInfo) {
        try {
            JIPipeEnvironmentConfiguratorProjectSource source = new JIPipeEnvironmentConfiguratorProjectSource<>(this);
            boolean didRepositoryRefresh = false;
            for (Map.Entry<String, JIPipeEnvironmentsServiceComponent.EnvironmentInfo> entry : JIPipe.getEnvironments().getInfosById().entrySet()) {
                JIPipeEnvironmentsServiceComponent.EnvironmentInfo environmentInfo = entry.getValue();
                if (environmentInfo.isArtifact() && environmentInfo.hasArtifactQuery() && environmentInfo.getArchetype() == JIPipeEnvironmentArchetype.Managed) {
                    JIPipeProgressInfo environmentProgress = progressInfo.resolveAndLog("Checking environment type " + entry.getKey());
                    JIPipeOptionalParameter optionalParameter = source.resolve(environmentInfo.getEnvironmentClass(), environmentInfo);
                    Object content = optionalParameter.getContent();
                    boolean broken = false;
                    if (content == null) {
                        environmentProgress.log("Found null value!");
                        broken = true;
                    } else if (content instanceof JIPipeEnvironment environment) {
                        if (!environment.isValid()) {
                            environmentProgress.log("Found misconfigured environment!");
                            broken = true;
                        }
                    }

                    if (broken) {
                        environmentProgress.log("Attempting to repair ...");
                        if (!didRepositoryRefresh) {
                            progressInfo.log("Refreshing artifact repositories (internet connection required!) ...");
                            JIPipe.getArtifacts().updateCachedArtifacts(progressInfo.resolve("Update artifacts"));
                            didRepositoryRefresh = true;
                        }

                        JIPipeEnvironment environment = (JIPipeEnvironment) JIPipe.getParameterTypes().getInfoByFieldClass(environmentInfo.getEnvironmentClass()).newInstance();
                        if (environment instanceof JIPipeArtifactEnvironment artifactEnvironment) {
                            environmentProgress.log("Apply standard version pinning ...");
                            artifactEnvironment.trySetToLatestVersionPinnedArtifact();
                            optionalParameter.setContent(artifactEnvironment);
                        } else {
                            environmentProgress.error("Environment is not an artifact environment!");
                        }

                    }
                }
            }
        }
        catch (Throwable ex) {
            progressInfo.log(ex);
        }
    }

    /**
     * Reports the validity for the target node and its dependencies
     *
     * @param context      the context
     * @param report       the report
     * @param targetNode   the target node
     * @param progressInfo the progress info
     */
    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report, JIPipeGraphNode targetNode, JIPipeProgressInfo progressInfo) {
        graph.reportValidity(context, report, targetNode, progressInfo);
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

        for (JIPipeProjectCompartmentOutput outputNode : compartment.getOutputNodes().values()) {
            replaceCompartmentOutputWithIOInterface(compartment, outputNode);
        }

        // Remove the outputs
        for (JIPipeProjectCompartmentOutput outputNode : compartment.getOutputNodes().values()) {
            graph.removeNode(outputNode, false);
        }

        // Delete the compartment
        UUID compartmentId = compartment.getProjectCompartmentUUID();
        graph.removeCompartment(compartmentId);
        compartments.remove(compartmentId);
        updateCompartmentVisibility();
        compartmentGraph.removeNode(compartment, false);
        compartmentRemovedEventEmitter.emit(new CompartmentRemovedEvent(compartment, compartmentId));
    }

    private void replaceCompartmentOutputWithIOInterface(JIPipeProjectCompartment compartment, JIPipeProjectCompartmentOutput outputNode) {
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
                    ioInterfaceAlgorithm.getNodeMetadata().putAll(outputNode.getNodeMetadata());
                    graph.insertNode(ioInterfaceAlgorithm, targetCompartment.getProjectCompartmentUUID());

                    for (JIPipeOutputDataSlot outputSlot : outputNode.getOutputSlots()) {
                        for (JIPipeDataSlot outgoingTargetSlot : graph.getOutputOutgoingTargetSlots(outputSlot)) {
                            graph.connect(ioInterfaceAlgorithm.getOutputSlot(outputSlot.getName()), outgoingTargetSlot);
                        }
                    }
                }

            }
        }
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

    public JIPipeProjectRunSetsConfiguration getRunSetsConfiguration() {
        return runSetsConfiguration;
    }

    public JIPipeRuntimePartitionConfiguration getRuntimePartitions() {
        return runtimePartitions;
    }

    public Map<String, JIPipeMetadataObject> getAdditionalMetadata() {
        return additionalMetadata;
    }

    public void setAdditionalMetadata(Map<String, JIPipeMetadataObject> additionalMetadata) {
        this.additionalMetadata = additionalMetadata;
    }

    /**
     * Writes the project to JSON
     *
     * @param generator the JSON generator
     * @throws IOException thrown by {@link JsonGenerator}
     */
    public void toJson(JsonGenerator generator) throws IOException {

        generator.writeStartObject();
        // Write standard metadata
        generator.writeStringField("jipipe:project-type", "project");
        generator.writeNumberField("jipipe:project-format-version", CURRENT_PROJECT_FORMAT_VERSION);
        generator.writeStringField("jipipe:project-jipipe-version", JIPipe.getJIPipeVersion());
        generator.writeObjectField("metadata", metadata);
        generator.writeObjectField("dependencies", getSimplifiedMinimalDependencies());
        generator.writeObjectField("runtime-partitions", runtimePartitions);
        generator.writeObjectField("run-sets", runSetsConfiguration);

        // Write automated project documentations
        generator.writeObjectFieldStart("jipipe:doc:parameters");
        writeParameterDocumentationJson(generator);
        generator.writeEndObject();

        // Write settings
        generator.writeObjectFieldStart("settings");
        writeSettingsJson(generator);
        generator.writeEndObject();

        // Write list of external environments
        writeExternalEnvironmentsJson(generator);

        // Write additional metadata
        if (!getAdditionalMetadata().isEmpty()) {
            generator.writeObjectFieldStart("additional-metadata");
            writeAdditionalMetadataJson(generator);
            generator.writeEndObject();
        }

        // Write graph and compartments
        generator.writeObjectField("graph", graph);
        generator.writeFieldName("compartments");
        generator.writeStartObject();
        generator.writeObjectField("compartment-graph", compartmentGraph);
        generator.writeEndObject();
        generator.writeEndObject();
    }

    private void writeParameterDocumentationJson(JsonGenerator generator) throws IOException {
        Map<String, String> parameterAssignment = new HashMap<>();
        Map<String, Map<String, Object>> parameterDocumentation = new HashMap<>();

        // Global parameters
        for (JIPipeParameterAccess access : getMetadata().getGlobalParameters().getParameters().values()) {
            String globalKey = "/" + access.getKey();
            String typeId = JIPipe.getParameterTypes().getInfoByFieldClass(access.getFieldClass()).getId();

            parameterAssignment.put(globalKey, typeId);
            Map<String, Object> documentation = new HashMap<>();
            createParameterInstanceDocumentation(access, documentation);
            if (!documentation.isEmpty()) {
                parameterDocumentation.put(globalKey, documentation);
            }
        }

        // Node parameters
        for (UUID uuid : graph.getGraphNodeUUIDs()) {
            JIPipeGraphNode node = graph.getNodeByUUID(uuid);
            if (node.getInfo().isRunnable()) {
                JIPipeParameterTree tree = new JIPipeParameterTree(node);
                for (Map.Entry<String, JIPipeParameterAccess> entry : tree.getParameters().entrySet()) {
                    JIPipeParameterAccess access = entry.getValue();
                    String globalKey = uuid + "/" + entry.getKey();
                    String typeId = JIPipe.getParameterTypes().getInfoByFieldClass(access.getFieldClass()).getId();

                    parameterAssignment.put(globalKey, typeId);

                    Map<String, Object> documentation = new HashMap<>();
                    createParameterInstanceDocumentation(access, documentation);
                    if (!documentation.isEmpty()) {
                        parameterDocumentation.put(globalKey, documentation);
                    }
                }
            }
        }

        generator.writeObjectField("types", parameterAssignment);
        generator.writeObjectField("docs", parameterDocumentation);

        generator.writeObjectFieldStart("ref");
        for (String parameterTypeId : new HashSet<>(parameterAssignment.values())) {
            JIPipeParameterTypeInfo info = JIPipe.getParameterTypes().getInfoById(parameterTypeId);
            generator.writeObjectFieldStart(parameterTypeId);
            generator.writeStringField("name", info.getName());
            generator.writeStringField("description", info.getDescription());
            generator.writeStringField("class", info.getFieldClass().getCanonicalName());
            generator.writeObjectField("allowed-values", info.getAllowedValues());
            generator.writeObjectField("archetype", info.getArchetype());
            generator.writeEndObject();
        }
        generator.writeEndObject();

    }

    private void createParameterInstanceDocumentation(JIPipeParameterAccess access, Map<String, Object> documentation) {
        if (!StringUtils.isNullOrEmpty(access.getName())) {
            documentation.put("name", access.getName());
        }
        if (!StringUtils.isNullOrEmpty(access.getDescription())) {
            documentation.put("description", access.getDescription());
        }
        if (access.isImportant()) {
            documentation.put("important", access.isImportant());
        }
        if (access.isPinned()) {
            documentation.put("pinned", access.isPinned());
        }
        if (access.isHidden()) {
            documentation.put("hidden", access.isHidden());
        }
    }

    /**
     * Writes the additional metadata into JSON format
     *
     * @param generator the generator
     * @throws IOException the exception
     */
    private void writeAdditionalMetadataJson(JsonGenerator generator) throws IOException {
        for (Map.Entry<String, JIPipeMetadataObject> entry : getAdditionalMetadata().entrySet()) {
            String typeId = JIPipe.getInstance().getMetadataTypes().getId(entry.getValue().getClass());

            if (typeId != null) {
                if (entry.getValue() instanceof JIPipeParameterCollection) {
                    generator.writeObjectFieldStart(entry.getKey());
                    generator.writeObjectField("jipipe:type", typeId);
                    ParameterUtils.serializeParametersToJson((JIPipeParameterCollection) entry.getValue(), generator);
                    generator.writeEndObject();
                } else {
                    generator.writeObjectFieldStart(entry.getKey());
                    generator.writeObjectField("jipipe:type", typeId);
                    generator.writeObjectField("data", entry.getValue());
                    generator.writeEndObject();
                }
            } else {
                System.err.println("Unable to serialize " + entry.getValue() + " as metadata object: not registered!");
            }
        }
    }

    /**
     * Write information about external environments into JSON
     *
     * @param generator the generator
     * @throws IOException exceptions
     */
    private void writeExternalEnvironmentsJson(JsonGenerator generator) throws IOException {
        List<JIPipeEnvironmentConfigurator<?>> externalEnvironments = getAllEnvironments();
        generator.writeArrayFieldStart("external-environments");
        Set<JIPipeEnvironment> alreadyAdded = new HashSet<>();
        for (JIPipeEnvironmentConfigurator<?> configurator : externalEnvironments) {
            JIPipeEnvironment environment = configurator.getBaseEnvironment();
            if (!alreadyAdded.contains(environment)) {
                generator.writeObject(environment);
                alreadyAdded.add(environment);
            }
        }
        generator.writeEndArray();
    }

    /**
     * Gets all utilized environments in this project
     *
     * @return the list of environment references
     */
    public List<JIPipeEnvironmentConfigurator<?>> getAllEnvironments() {
        JIPipeEnvironmentConfigurationCache configurationCache = new JIPipeEnvironmentConfigurationCache();
        List<JIPipeEnvironmentConfigurator<?>> externalEnvironments = new ArrayList<>();
        for (JIPipeGraphNode graphNode : getGraph().getGraphNodes()) {
            graphNode.getEnvironmentDependencies(externalEnvironments, configurationCache);
        }
        externalEnvironments.removeIf(Objects::isNull);
        return externalEnvironments;
    }

    /**
     * Write the project settings into JSON
     *
     * @param generator the generator
     * @throws IOException exceptions
     */
    private void writeSettingsJson(JsonGenerator generator) throws IOException {
        for (Map.Entry<String, JIPipeProjectSettingsSheet> entry : settingsSheets.entrySet()) {
            generator.writeObjectFieldStart(entry.getKey());
            entry.getValue().serializeToJsonGenerator(generator);
            generator.writeEndObject();
        }
        for (Map.Entry<String, JsonNode> entry : unloadedSettingsSheets.entrySet()) {
            if (!settingsSheets.containsKey(entry.getKey())) {
                generator.writeObjectField(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Loads the project from JSON
     *
     * @param jsonNode      the node
     * @param context       the context
     * @param notifications notifications for the user
     * @param progressInfo  the progress info
     */
    public void fromJson(JsonNode jsonNode, JIPipeValidationReportContext context, JIPipeValidationReport report, JIPipeNotificationInbox notifications, JIPipeProgressInfo progressInfo) throws IOException {
        try {
            isLoading = true;

            // Load metadata
            if (jsonNode.has("metadata")) {
                metadata = JsonUtils.getObjectMapper().readerFor(JIPipeProjectMetadata.class).readValue(jsonNode.get("metadata"));
            }

            // Load dependencies
            projectJIPipeVersion = "0.0.0";
            if (jsonNode.has("jipipe:project-jipipe-version")) {
                projectJIPipeVersion = jsonNode.get("jipipe:project-jipipe-version").asText();
            } else if (jsonNode.has("dependencies")) {
                // Search for the org.hkijena.jipipe:core dependency
                List<JIPipeDependency> dependencies = JsonUtils.getObjectMapper().readerForListOf(JIPipeDependency.class).readValue(jsonNode.get("dependencies"));
                for (JIPipeDependency dependency : dependencies) {
                    if ("org.hkijena.jipipe:core".equals(dependency.getDependencyId())) {
                        projectJIPipeVersion = dependency.getDependencyVersion();
                        break;
                    }
                }
            }

            // Load partitions
            if (jsonNode.has("runtime-partitions")) {
                JsonNode sub = jsonNode.get("runtime-partitions");
                this.runtimePartitions = JsonUtils.getObjectMapper().convertValue(sub, JIPipeRuntimePartitionConfiguration.class);
            }

            // Load run sets
            if (jsonNode.has("run-sets")) {
                JsonNode sub = jsonNode.get("run-sets");
                this.runSetsConfiguration = JsonUtils.getObjectMapper().convertValue(sub, JIPipeProjectRunSetsConfiguration.class);
            }

            // Load settings sheets
            if (jsonNode.has("settings")) {
                for (Map.Entry<String, JsonNode> entry : ImmutableList.copyOf(jsonNode.get("settings").fields())) {
                    try {
                        if (settingsSheets.containsKey(entry.getKey())) {
                            settingsSheets.get(entry.getKey()).deserializeFromJsonNode(entry.getValue());
                        } else {
                            unloadedSettingsSheets.put(entry.getKey(), entry.getValue());
//                            new UnspecifiedValidationReportContext().warning()
//                                    .title("Unable to load settings")
//                                    .explanation("The project settings for the sheet with the ID '" + entry.getKey() + "' are not known to JIPipe. ")
//                                    .solution("Please check if all required plugins are up-to-date and activated.")
//                                    .report(report);
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
//                        new UnspecifiedValidationReportContext().error()
//                                .title("Unable to load settings")
//                                .explanation("The project settings for the sheet with the ID '" + entry.getKey() + "' could not be loaded.")
//                                .solution("Please check if you are using an up-to-date JIPipe version.")
//                                .details(ExceptionUtils.getStackTrace(e))
//                                .report(report);
                    }
                }
            }

            // Deserialize additional metadata
            JsonNode additionalMetadataNode = jsonNode.path("additional-metadata");
            for (Map.Entry<String, JsonNode> metadataEntry : ImmutableList.copyOf(additionalMetadataNode.fields())) {
                try {
                    String typeId = metadataEntry.getValue().get("jipipe:type").textValue();
                    Class<? extends JIPipeMetadataObject> metadataClass = JIPipe.getInstance().getMetadataTypes().findById(typeId);

                    if (metadataClass == null) {
                        throw new NullPointerException("Unable to find metadata object ID '" + typeId + "'");
                    }

                    if (JIPipeParameterCollection.class.isAssignableFrom(metadataClass)) {
                        JIPipeParameterCollection metadata = (JIPipeParameterCollection) ReflectionUtils.newInstance(metadataClass);
                        ParameterUtils.deserializeParametersFromJson(metadata, metadataEntry.getValue(), context, report);
                        additionalMetadata.put(metadataEntry.getKey(), (JIPipeMetadataObject) metadata);
                    } else {
                        Object data = JsonUtils.getObjectMapper().readerFor(metadataClass).readValue(metadataEntry.getValue().get("data"));
                        if (data != null) {
                            additionalMetadata.put(metadataEntry.getKey(), (JIPipeMetadataObject) data);
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
                    progressInfo.log("[Project format conversion] Fix legacy compartment '" + entry.getValue() + "' --> " + compartmentNode.getUUIDInParentGraph());
                    graph.setCompartment(entry.getKey(), compartmentNode.getUUIDInParentGraph());
                } else {
                    // Ghost node -> delete
                    graph.removeNode(node, false);
                    continue;
                }

                // Fix legacy node location information
                Map<String, Point> allNodeUILocations = node.getAllNodeUILocations();
                for (Map.Entry<String, Point> locationEntry : allNodeUILocations.entrySet()) {
                    String compartmentUUIDString;
                    if ("DEFAULT".equals(locationEntry.getKey())) {
                        compartmentUUIDString = "";
                    } else {
                        compartmentUUIDString = StringUtils.nullToEmpty(compartmentGraph.findNodeUUID(locationEntry.getKey()));
                    }
                    node.setNodeUILocationWithin(compartmentUUIDString, locationEntry.getValue());
                    progressInfo.log("[Project format conversion] Move location within " + locationEntry.getKey() + " to " + compartmentUUIDString);
                }
            }

            // Initialize compartments
            for (JIPipeGraphNode node : compartmentGraph.getGraphNodes()) {
                if (node instanceof JIPipeProjectCompartment compartment) {
                    compartment.setRuntimeProject(this);
                    compartments.put(compartment.getProjectCompartmentUUID(), compartment);
                    updateCompartmentOutputs(compartment);
                }
            }

            // Reading compartments might break some connections. This will restore them
            graph.edgesFromJson(jsonNode.get("graph"), context, report);

            // Update node visibilities
            updateCompartmentVisibility();

            // Apply upgrades
            if (VersionUtils.compareVersions(projectJIPipeVersion, JIPipe.getJIPipeVersion()) < 0) {
                applyProjectUpgrade(context, report, progressInfo);
            }

            // Checking for errors
            for (JIPipeGraphNode graphNode : ImmutableList.copyOf(graph.getGraphNodes())) {
                UUID compartmentUUIDInGraph = graphNode.getCompartmentUUIDInParentGraph();
                if (compartmentUUIDInGraph == null || !compartments.containsKey(compartmentUUIDInGraph)) {
                    new GraphNodeValidationReportContext(graphNode).warning()
                            .title("Node has no compartment!")
                            .explanation("The node '" + graphNode.getDisplayName() + "' has no compartment assigned!")
                            .solution("This was repaired automatically by deleting the node. Please inform the JIPipe developers about this issue.")
                            .details(JsonUtils.toPrettyJsonString(graphNode))
                            .report(report);
                    graph.removeNode(graphNode, false);
                } else {
                    JIPipeGraphNode compartmentNode = compartmentGraph.getNodeByUUID(compartmentUUIDInGraph);
                    if (compartmentNode == null) {
                        new GraphNodeValidationReportContext(compartmentNode).warning()
                                .title("Node has invalid compartment!")
                                .explanation("The node '" + graphNode.getDisplayName() + "' is assigned to compartment '" + compartmentUUIDInGraph + "', but it does not exist!")
                                .solution("This was repaired automatically by deleting the node. Please inform the JIPipe developers about this issue.")
                                .details(JsonUtils.toPrettyJsonString(graphNode))
                                .report(report);
                        graph.removeNode(graphNode, false);
                    }
                }
            }
        } finally {
            isLoading = false;
        }
    }

    private void applyProjectUpgrade(JIPipeValidationReportContext context, JIPipeValidationReport report, JIPipeProgressInfo progressInfo) {
        progressInfo.log("Loaded project, which was designed for JIPipe " + projectJIPipeVersion + ", but this is JIPipe " + JIPipe.getJIPipeVersion() + ". Upgrading ...");
        for (JIPipeGraphNode graphNode : ImmutableList.copyOf(graph.getGraphNodes())) {
            graphNode.applyProjectUpgrade(projectJIPipeVersion, context.custom("Upgrades"), report);
        }
        for (JIPipeProjectSettingsSheet sheet : settingsSheets.values()) {
            sheet.applyProjectUpgrade(projectJIPipeVersion, context.custom("Upgrades"), report);
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
                updateCompartmentOutputs(compartment);
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
        return graph.getDeactivatedNodes(true);
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
     * Returns a list of all nodes that generate heavy data ({@link LabelAsJIPipeHeavyData}) and are intermediate (see getIntermediateAlgorithms()).
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
        JIPipeMetadataObject existing = getAdditionalMetadata().getOrDefault(JIPipeProjectInfoParameters.METADATA_KEY, null);
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
        for (JIPipeNodeTemplate nodeTemplate : JIPipe.getInstance().getNodeTemplates().getGlobalTemplates()) {
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
    public Map<String, Path> getUserPathMap() {
        return metadata.getUserPaths().getDirectoryMap(getWorkDirectory());
    }

    /**
     * Gets a description of the project as text
     *
     * @param stringBuilder the string builder
     * @param headingLevel  the heading level
     */
    public void getTextDescription(StringBuilder stringBuilder, int headingLevel) {

        Map<UUID, Integer> compartmentIndices = new HashMap<>();
        Map<UUID, Integer> nodeIndices = new HashMap<>();
        for (JIPipeGraphNode node : compartmentGraph.traverse()) {
            if (node instanceof JIPipeProjectCompartment) {
                UUID uuid = node.getUUIDInParentGraph();
                compartmentIndices.put(uuid, compartmentIndices.size() + 1);
                stringBuilder.append("<h").append(headingLevel).append(">Compartment C").append(compartmentIndices.get(uuid)).append(" \"").append(node.getName()).append("\"</h").append(headingLevel).append(">\n\n");
                stringBuilder.append("<ul>");
                // Resolve sources
                for (JIPipeDataSlot sourceSlot : compartmentGraph.getInputIncomingSourceSlots(node.getFirstInputSlot())) {
                    JIPipeGraphNode sourceNode = sourceSlot.getNode();
                    UUID sourceUUID = sourceNode.getUUIDInParentGraph();
                    if (sourceNode instanceof JIPipeProjectCompartment) {
                        stringBuilder.append("<li>The \"").append(node.getName()).append("\" compartment (C").append(compartmentIndices.get(uuid)).append(") receives data from the \"")
                                .append(sourceNode.getName()).append("\" compartment (C").append(compartmentIndices.get(sourceUUID)).append(")").append("</li>\n");
                    }
                }
                stringBuilder.append("</ul>");

                // Resolve nodes
                graph.getTextDescription(stringBuilder, uuid, nodeIndices, headingLevel + 1);


            }
        }
    }

    public Path getProjectFile() {
        return projectFile;
    }

    public void setProjectFile(Path projectFile) {
        this.projectFile = projectFile;
    }

    /**
     * Saves the project to the existing project file (if already set)
     */
    public void saveProject() throws IOException {
        saveProject(projectFile, false);
    }

    /**
     * Gets a fully configured environment
     *
     * @param klass              the environment class
     * @param configurationCache the environment cache
     * @param progressInfo       the progress info
     * @param <T>                the environment class
     * @return the environment
     */
    public <T extends JIPipeEnvironment> T getEnvironment(Class<T> klass, JIPipeEnvironmentConfigurationCache configurationCache, JIPipeProgressInfo progressInfo) {
        return getEnvironmentConfigurator(klass, configurationCache).get(progressInfo);
    }

    /**
     * Returns the environment reference for the environment class
     *
     * @param klass the environment class
     * @param <T>   the environment type
     * @return the environment reference
     */
    public <T extends JIPipeEnvironment> JIPipeEnvironmentConfigurator<T> getEnvironmentConfigurator(Class<T> klass, JIPipeEnvironmentConfigurationCache configurationCache) {
        return new JIPipeEnvironmentConfigurator<>(klass, configurationCache, null, this);
    }

    /**
     * The JIPipe version that this project was designed for.
     * Defaults to the current JIPipe version.
     *
     * @return the project's JIPipe version
     */
    public String getProjectJIPipeVersion() {
        return projectJIPipeVersion;
    }

    public boolean isDisposed() {
        return disposed;
    }

    public interface CompartmentAddedEventListener {
        void onProjectCompartmentAdded(CompartmentAddedEvent event);
    }

    public interface CompartmentRemovedEventListener {
        void onProjectCompartmentRemoved(CompartmentRemovedEvent event);
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
            project.fromJson(node, new UnspecifiedValidationReportContext(), new JIPipeValidationReport(), new JIPipeNotificationInbox(), JIPipeProgressInfo.STDOUT);
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

    public static class CompartmentRemovedEventEmitter extends JIPipeEventEmitter<CompartmentRemovedEvent, CompartmentRemovedEventListener> {

        @Override
        protected void call(CompartmentRemovedEventListener compartmentRemovedEventListener, CompartmentRemovedEvent event) {
            compartmentRemovedEventListener.onProjectCompartmentRemoved(event);
        }
    }

}
