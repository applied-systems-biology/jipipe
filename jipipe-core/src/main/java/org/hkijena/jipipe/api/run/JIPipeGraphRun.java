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

package org.hkijena.jipipe.api.run;

import com.google.common.base.Charsets;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import ij.IJ;
import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.DefaultJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeFixedThreadPool;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartmentOutput;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteDataStorage;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.environments.JIPipeEnvironmentConfigurationCache;
import org.hkijena.jipipe.api.environments.JIPipeEnvironmentConfigurator;
import org.hkijena.jipipe.api.grouping.JIPipeGraphWrapperAlgorithm;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithmIterationStepGenerationSettings;
import org.hkijena.jipipe.api.nodes.infos.JIPipeEmptyNodeInfo;
import org.hkijena.jipipe.api.notifications.JIPipeNotification;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationAction;
import org.hkijena.jipipe.api.progress.JIPipeLogLevel;
import org.hkijena.jipipe.api.progress.JIPipeProgressLog;
import org.hkijena.jipipe.api.progress.JIPipeProgressNode;
import org.hkijena.jipipe.api.progress.JIPipeProgressNodeKind;
import org.hkijena.jipipe.api.progress.JIPipeProgressNodeState;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.runtimepartitioning.JIPipeRuntimePartition;
import org.hkijena.jipipe.api.runtimepartitioning.RuntimePartitionReferenceParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.plugins.tunnels.JIPipeDataFlowTunnelUtils;
import org.hkijena.jipipe.plugins.tunnels.nodes.JIPipeDataFlowTunnel;
import org.hkijena.jipipe.plugins.tunnels.nodes.JIPipeDataFlowTunnelEntrance;
import org.hkijena.jipipe.plugins.tunnels.nodes.JIPipeDataFlowTunnelExit;
import org.hkijena.jipipe.utils.*;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;
import java.util.jar.Attributes;
import java.util.stream.Collectors;

public class JIPipeGraphRun extends DefaultJIPipeRunnable implements JIPipeGraphRunGCGraph.GCEventListener {

    private final JIPipeProject project;
    private final JIPipeGraph graph;
    private final JIPipeGraphRun parent;
    private final JIPipeGraphRunConfiguration configuration;
    private final List<JIPipeRuntimePartition> runtimePartitions;
    private JIPipeGraphNodeRunContext runContext;
    private final Map<Object, JIPipeProgressInfo> nodeProgressHandles = new HashMap<>();

    public JIPipeGraphRun(JIPipeGraphRun parent, JIPipeGraph graph, JIPipeGraphRunConfiguration configuration) {
        this.parent = parent;
        this.configuration = configuration;
        this.project = parent.getProject();
        this.graph = new JIPipeGraph(graph);
        this.runtimePartitions = new ArrayList<>();
        this.runtimePartitions.add(new JIPipeRuntimePartition());
    }

    public JIPipeGraphRun(JIPipeProject project, JIPipeGraphRunConfiguration configuration) {
        this.project = project;
        this.graph = new JIPipeGraph(project.getGraph());
        this.parent = null;
        this.configuration = configuration;
        this.runtimePartitions = new ArrayList<>();
        for (JIPipeRuntimePartition runtimePartition : project.getRuntimePartitions().toList()) {
            this.runtimePartitions.add(new JIPipeRuntimePartition(runtimePartition));
        }
    }

    /**
     * Writes node internal storage paths and output slot storage paths into the graph of the project
     * Used for the legacy result viewer
     *
     * @param project    the project
     * @param outputPath the output path
     */
    public static void restoreStoragePaths(JIPipeProject project, Path outputPath) {
        JIPipeGraph graph = project.getGraph();

        // Internal storage path
        for (JIPipeGraphNode node : graph.getGraphNodes()) {
            JIPipeProjectCompartment compartment = project.getCompartments().get(node.getCompartmentUUIDInParentGraph());
            if (compartment != null) {
                node.setInternalStoragePath(Paths.get(StringUtils.safeJsonify(compartment.getAliasIdInParentGraph()))
                        .resolve(StringUtils.safeJsonify(graph.getAliasIdOf(node))));
            } else {
                node.setInternalStoragePath(Paths.get("_sub")
                        .resolve(UUID.randomUUID().toString())
                        .resolve(StringUtils.safeJsonify(graph.getAliasIdOf(node))));
            }
            node.setProjectDirectory(project.getWorkDirectory());
            node.setRuntimeProject(project);
        }

        // Slot storage paths
        for (JIPipeDataSlot slot : graph.getSlotNodes()) {
            if (slot.isOutput()) {
                slot.setSlotStoragePath(outputPath.resolve(slot.getNode().getInternalStoragePath().resolve(slot.getName())));
            }
        }
    }

    public JIPipeGraphRun getParent() {
        return parent;
    }

    public JIPipeGraphRun getBaseRun() {
        if (parent == null) {
            return this;
        } else {
            return parent.getBaseRun();
        }
    }

    @Override
    public String getTaskLabel() {
        return "Pipeline run";
    }

    public JIPipeGraphRunConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public void run() {
        JIPipeProgressInfo progressInfo = getProgressInfo();

        // The root run owns the progress store: all records of the run (including those of child runs)
        // are written into <temporary-base>/logs/<run-uuid>.ndjson
        JIPipeProgressLog rootStore = null;
        if (parent == null) {
            progressInfo.setStorageBase(project != null ? project.getTemporaryBaseDirectory() : null,
                    getRunUUID().toString(), getTaskLabel());
            rootStore = progressInfo.getStore();
        }

        // Reset the per-run cache of node progress handles (R28) so it cannot leak across runs
        nodeProgressHandles.clear();

        // R32: narration lives in dedicated phase nodes; the run-root handle stays recordless
        // (parent of node rows and partition nodes only). R38: the setup phase may receive records
        // AFTER it is finished (mid-run machinery lines): the store allows records for finished
        // nodes, the row keeps its finished state while its detail grows.
        JIPipeProgressInfo setupProgress = null;
        JIPipeProgressInfo finishedProgress = null;

        try {
            setupProgress = progressInfo.resolve("Pipeline setup", JIPipeProgressNodeKind.PHASE);

            if (parent == null) {
                setupProgress.log("JIPipe version: " + VersionUtils.getJIPipeVersion());
                Attributes manifestAttributes = ReflectionUtils.getManifestAttributes();
                if (manifestAttributes != null) {
                    String implementationDateString = manifestAttributes.getValue("Implementation-Date");
                    setupProgress.log("JIPipe build date: " + StringUtils.orElse(implementationDateString, "N/A"));
                }
                setupProgress.log(StringUtils.orElse("ImageJ version: " + IJ.getVersion(), "N/A"));
                setupProgress.log("Java version: " + StringUtils.orElse(System.getProperty("java.version"), "N/A"));
                setupProgress.log("Registered nodes types: " + JIPipe.getNodes().getRegisteredNodeInfos().size() + " nodes");
                setupProgress.log("Registered data types: " + JIPipe.getDataTypes().getRegisteredDataTypes().size() + " types");
                setupProgress.log("Enabled extensions: " + String.join(", ", JIPipe.getInstance().getPlugins().getActivatedPlugins()));
                setupProgress.log("Operating system: " + SystemUtils.OS_NAME + " " + SystemUtils.OS_VERSION + " [" + SystemUtils.OS_ARCH + "]");
                setupProgress.log("Profile directory: " + JIPipe.getJIPipeUserDir(false));
            }

            setupProgress.log("Dissolving tunnels ...");
            JIPipeProgressInfo tunnelsStep = setupProgress.resolve("Preprocessing/Tunnels");
            finishWhenDone(tunnelsStep, () -> dissolveTunnels(graph, tunnelsStep));
            setupProgress.log("Erasing skipped/disabled algorithms (except direct predecessors) ...");
            JIPipeProgressInfo cleanupGraphStep = setupProgress.resolve("Preprocessing/Cleanup");
            finishWhenDone(cleanupGraphStep, () -> cleanGraph(graph, cleanupGraphStep));
            setupProgress.log("Repairing partition assignments ...");
            JIPipeProgressInfo fixPartitionsStep = setupProgress.resolve("Fix partitions");
            finishWhenDone(fixPartitionsStep, () -> fixPartitions(graph, fixPartitionsStep));

            long startTime = System.currentTimeMillis();
            if (parent == null) {
                setupProgress.log("JIPipe run starting at " + StringUtils.formatDateTime(LocalDateTime.now()));
            }

            setupProgress.log("Preparing ...");
            initializeNodeStoragePaths();
            initializeScratchDirectory();
            initializeRelativeDirectories();
            initializeSlotStoragePaths();
            if (parent == null) {
                setupProgress.log("Outputs will be written to " + configuration.getOutputPath());
            }

            // Pre-build environment configuration cache
            JIPipeEnvironmentConfigurationCache environmentConfigurationCache = new JIPipeEnvironmentConfigurationCache();

            // List of all environments
            List<JIPipeEnvironmentConfigurator<?>> allEnvironments = new ArrayList<>();

            if (parent == null) {
                setupProgress.log("Preparing artifacts and environments ...");

                // Gather all environments from all active nodes
                setupProgress.log("Gathering environments for all nodes ...");
                for (JIPipeGraphNode graphNode : graph.getGraphNodes()) {
                    graphNode.getEnvironmentDependencies(allEnvironments, environmentConfigurationCache);
                }
                allEnvironments.removeIf(Objects::isNull);

                // Apply additional configuration steps
                setupProgress.log("Configuring environments ...");
                for (int i = 0; i < allEnvironments.size(); i++) {
                    JIPipeEnvironmentConfigurator<?> reference = allEnvironments.get(i);
                    JIPipeProgressInfo configureStep = setupProgress.resolve("Configure environments", i, allEnvironments.size());
                    JIPipeProgressInfo environmentStep = configureStep.resolve(reference.getEnvironmentInfo().getId());
                    JIPipeEnvironment environment = finishWhenDone(environmentStep, () -> reference.get(environmentStep));
                    // Alias for the lambda: setupProgress is re-assigned at try start, so it is not
                    // effectively final
                    JIPipeProgressInfo setupPhase = setupProgress;
                    finishWhenDone(configureStep, () -> environment.runPreconfigure(this, setupPhase));
                }
            }

            boolean runFailed = false;

            try (JIPipeFixedThreadPool threadPool = new JIPipeFixedThreadPool(configuration.getNumThreads())) {

                // Setup context
                runContext = new JIPipeGraphNodeRunContext();
                runContext.setGraphRun(this);
                runContext.setThreadPool(threadPool);
                runContext.setEnvironmentConfigurationCache(environmentConfigurationCache);

                // Start iteration on initial graph
                setupProgress.log("--> Starting first iteration ...");
                // R38: the setup phase is complete once node execution begins (its row stops
                // spinning); mid-run machinery narration may still land in the finished node
                setupProgress.finishNode(JIPipeProgressNodeState.OK);
                runGraph(graph, progressInfo, setupProgress);
            } catch (Exception e) {
                runFailed = true;
                throw new RuntimeException(e);
            } finally {
                runContext = null;

                // R32: all post-execution narration goes into the "Pipeline finished" phase
                finishedProgress = progressInfo.resolve("Pipeline finished", JIPipeProgressNodeKind.PHASE);

                // Clear all slots
                for (JIPipeGraphNode node : graph.getGraphNodes()) {
                    for (JIPipeInputDataSlot inputSlot : node.getInputSlots()) {
                        if (inputSlot.getRowCount() > 0) {
                            finishedProgress.log("[!] Slot " + inputSlot.getDisplayName() + " still contains " + inputSlot.getRowCount() + " items! Clearing in post-processing!");
                        }
                        inputSlot.clear(false, finishedProgress);
                    }
                    for (JIPipeOutputDataSlot outputSlot : node.getOutputSlots()) {
                        if (!outputSlot.isSkipGC()) {
                            if (outputSlot.getRowCount() > 0) {
                                finishedProgress.log("[!] Slot " + outputSlot.getDisplayName() + " still contains " + outputSlot.getRowCount() + " items! Clearing in post-processing!");
                            }
                            outputSlot.clear(false, finishedProgress);
                        }
                    }
                }

                // Postprocessing
                if (parent == null) {
                    finishedProgress.log("Postprocessing steps ...");

                    // Postprocess environments
                    for (JIPipeEnvironmentConfigurator<?> reference : allEnvironments) {
                        try {
                            reference.get(finishedProgress).runPostprocessing(this, finishedProgress);
                        } catch (Throwable e) {
                            finishedProgress.log(e);
                        }
                    }

                    Path projectReSavePath = null;
                    try {
                        if (configuration.getOutputPath() != null && configuration.isStoreToDisk()) {
                            // Re-save in the project's current format
                            projectReSavePath = configuration.getOutputPath().resolve("project." +
                                    (PathUtils.isJipxPath(project.getProjectFile()) ? "jipx" : "jip"));
                            project.saveProject(projectReSavePath, false);
                        }
                    } catch (IOException e) {
                        finishedProgress.log(new JIPipeValidationRuntimeException(e,
                                "Could not save project to '" + projectReSavePath + "'!",
                                "Either the path is invalid, or you have no permission to write to the disk, or the disk space is full",
                                "Check if you can write to the output directory."));
                    }

                    finishedProgress.log("Run ending at " + StringUtils.formatDateTime(LocalDateTime.now()));
                    finishedProgress.log("--> Required " + StringUtils.formatDuration(System.currentTimeMillis() - startTime) + " to execute.");

                    // R32: the phase nodes finish with the run state before the store records the run status.
                    // R38: "Pipeline setup" normally finished when node execution began - the run state
                    // here is the fallback for a setup that was still running (failed/cancelled before
                    // execution started). "Pipeline finished" is always finished here.
                    JIPipeProgressNodeState runState = progressInfo.isCancelled() ? JIPipeProgressNodeState.CANCELLED :
                            (runFailed ? JIPipeProgressNodeState.FAILED : JIPipeProgressNodeState.OK);
                    JIPipeProgressNode setupNode = rootStore.getNode(setupProgress.getNodeId());
                    if (setupNode != null && setupNode.getState() == JIPipeProgressNodeState.RUNNING) {
                        rootStore.nodeFinished(setupProgress.getNodeId(), runState);
                    }
                    rootStore.nodeFinished(finishedProgress.getNodeId(), runState);

                    // Complete the store (final status record, final flush, final batch) before the log is rendered
                    rootStore.finish(runState);

                    try {
                        if (configuration.getOutputPath() != null) {
                            Path logFile = configuration.getOutputPath().resolve("log.txt");
                            try (BufferedWriter writer = Files.newBufferedWriter(logFile, Charsets.UTF_8)) {
                                rootStore.renderText(JIPipeLogLevel.INFO, line -> {
                                    try {
                                        writer.append(line).append('\n');
                                    } catch (IOException e) {
                                        throw new UncheckedIOException(e);
                                    }
                                });
                            }
                        }
                    } catch (IOException | UncheckedIOException e) {
                        finishedProgress.log(new JIPipeValidationRuntimeException(e,
                                "Could not write log '" + configuration.getOutputPath().resolve("log.txt") + "'!",
                                "Either the path is invalid, or you have no permission to write to the disk, or the disk space is full",
                                "Check if you can write to the output directory."));
                    }

                    if (!progressInfo.getNotifications().isEmpty()) {
                        finishedProgress.log("--> Graph run generated following reports:");
                        for (JIPipeNotification notification : progressInfo.getNotifications().getNotifications()) {
                            finishedProgress.log("@" + StringUtils.formatDateTime(notification.getDateTime()) + "    " + notification.getHeading() + ": " + notification.getDescription());
                        }
                    }

                    if ((runFailed && configuration.isCleanupOutputsAfterFailure()) || (!runFailed && configuration.isCleanupOutputsAfterSuccess())) {
                        if (configuration.getOutputPath() != null) {
                            finishedProgress.log("Removing output directory as requested: " + configuration.getOutputPath());
                            JIPipeProgressInfo cleanupStep = finishedProgress.resolve("Cleanup");
                            finishWhenDone(cleanupStep, () -> PathUtils.deleteDirectoryRecursively(configuration.getOutputPath(), cleanupStep));
                        }
                    }
                }
            }
        } finally {
            // Safety net for exit paths that did not reach the log rendering above (e.g. errors during
            // preprocessing): finish the store so no run is left unfinished. No-op if already finished.
            if (rootStore != null) {
                JIPipeProgressNodeState runState = progressInfo.isCancelled() ? JIPipeProgressNodeState.CANCELLED :
                        JIPipeProgressNodeState.FAILED;
                // R38: the setup phase is finished only if it is still running (it normally finished
                // when node execution began); "Pipeline finished" only exists once the run-end block started
                if (setupProgress != null) {
                    JIPipeProgressNode setupNode = rootStore.getNode(setupProgress.getNodeId());
                    if (setupNode != null && setupNode.getState() == JIPipeProgressNodeState.RUNNING) {
                        rootStore.nodeFinished(setupProgress.getNodeId(), runState);
                    }
                }
                if (finishedProgress != null) {
                    rootStore.nodeFinished(finishedProgress.getNodeId(), runState);
                }
                rootStore.finish(runState);
            }
        }


    }

    private void dissolveTunnels(JIPipeGraph graph, JIPipeProgressInfo progressInfo) {
        for (JIPipeGraphNode graphNode : graph.getGraphNodes()) {
            if (graphNode instanceof JIPipeDataFlowTunnelExit tunnelExit) {
                JIPipeProgressInfo nodeProgress = progressInfo.resolveAndLog("Tunnel exit " + graphNode.getUUIDInParentGraph() + " (TUN " + tunnelExit.getTunnelKeyGroup() + "/" + tunnelExit.getTunnelKey() + ")");
                try {
                    List<JIPipeDataFlowTunnelEntrance> tunnelEntrances = JIPipeDataFlowTunnelUtils.findTunnelEntrances(graph, tunnelExit.getCompartmentUUIDInParentGraph(), tunnelExit.getTunnelKeyGroup(), tunnelExit.getTunnelKey());
                    if (tunnelEntrances.size() > 1) {
                        JIPipeValidationReportContext.UNSPECIFIED.graph(graph)
                                .node(graphNode).error().title("Duplicate tunnel entrance '" + tunnelExit.getTunnelKey() + "'")
                                .explanation("A tunnel with duplicate entrances within a compartment was detected. Unable to continue.")
                                .details(tunnelExit.getTunnelKeyGroup() + "/" + tunnelExit.getTunnelKey() + " in compartment " + tunnelExit.getCompartmentUUIDInParentGraph() + " @ node " + tunnelExit.getUUIDInParentGraph())
                                .buildAndThrow();
                    } else if (tunnelEntrances.size() == 1) {
                        JIPipeDataFlowTunnelEntrance tunnelEntrance = tunnelEntrances.getFirst();

                        for (JIPipeOutputDataSlot outputSlot : tunnelExit.getOutputSlots()) {
                            nodeProgress.log("Processing slot " + outputSlot.getName() + " ...");
                            Set<JIPipeDataSlot> newInputSlots = graph.getOutputOutgoingTargetSlots(outputSlot);
                            Set<JIPipeDataSlot> newOutputSlots = graph.getInputIncomingSourceSlots(tunnelEntrance.getInputSlot(outputSlot.getName()));

                            // Connect the corresponding output of the tunnel entrance to the new inputs
                            for (JIPipeDataSlot newInputSlot : newInputSlots) {
                                for (JIPipeDataSlot newOutputSlot : newOutputSlots) {
                                    nodeProgress.log("Connect " + newOutputSlot.getDisplayName() + " -> " + newInputSlot.getDisplayName());
                                    graph.connect(newOutputSlot, newInputSlot);
                                }
                            }
                        }
                    } else {
                        progressInfo.log("[WARN] No tunnel entrance found for " + tunnelExit.getTunnelKeyGroup() + "/" + tunnelExit.getTunnelKey() + " in compartment " + tunnelExit.getCompartmentUUIDInParentGraph() + ": ignoring");
                    }
                    nodeProgress.finishNode(JIPipeProgressNodeState.OK);
                } finally {
                    nodeProgress.finishNode(JIPipeProgressNodeState.FAILED);
                }
            }
        }

        // Delete all tunnel nodes
        progressInfo.log("Deleting all tunnel nodes ...");
        for (JIPipeGraphNode graphNode : ImmutableList.copyOf(graph.getGraphNodes())) {
            if (graphNode instanceof JIPipeDataFlowTunnel) {
                progressInfo.log("- " + graphNode.getDisplayName() + " (" + graphNode.getUUIDInParentGraph() + ") of " + graphNode.getInfo().getId());
                graph.removeNode(graphNode, false);
            }
        }

    }

    private void fixPartitions(JIPipeGraph graph, JIPipeProgressInfo progressInfo) {
        for (JIPipeGraphNode graphNode : graph.getGraphNodes()) {
            if (graphNode instanceof JIPipeAlgorithm algorithm) {
                int requestedIndex = algorithm.getRuntimePartition().getIndex();
                int mappedIndex = getValidRuntimePartitionIndex(requestedIndex);
                if (requestedIndex != mappedIndex) {
                    progressInfo.log(graphNode.getDisplayName() + " [" + graphNode.getUUIDInParentGraph() + "] mapping partition from " + requestedIndex + " to " + mappedIndex);
                    algorithm.setRuntimePartition(new RuntimePartitionReferenceParameter(mappedIndex));
                }
            }
        }
    }

    public void cleanGraph(JIPipeGraph graph, JIPipeProgressInfo progressInfo) {
        outer:
        for (JIPipeGraphNode graphNode : ImmutableList.copyOf(graph.getGraphNodes())) {
            if (graphNode instanceof JIPipeAlgorithm algorithm) {
                if (!(algorithm).isEnabled() || (algorithm).isSkipped()) {

                    // Check if algorithm is within a looped partition where looping is enabled
                    // They cannot be removed
                    // NO! DONT DO THIS! QuickRun must ensure that the assignment is correct from the start
//                    JIPipeRuntimePartition runtimePartition = getRuntimePartition(algorithm.getRuntimePartition());
//                    if(runtimePartition.getIterationMode() != JIPipeGraphWrapperAlgorithm.IterationMode.PassThrough) {
//                        if(configuration.isStoreToCache()) {
//                            if(!runtimePartition.isForcePassThroughLoopIterationInCaching()) {
//                                progressInfo.log("Keeping " + graphNode.getDisplayName() + " [caching, within looped partition]");
//                                continue;
//                            }
//                        }
//                    }

                    // Check if we need the algorithm in some way (cache access)
                    if (configuration.isLoadFromCache()) {
                        for (JIPipeOutputDataSlot outputSlot : graphNode.getOutputSlots()) {
                            for (JIPipeDataSlot inputSlot : graph.getOutputOutgoingTargetSlots(outputSlot)) {
                                JIPipeGraphNode targetNode = inputSlot.getNode();
                                if (targetNode instanceof JIPipeAlgorithm && (((JIPipeAlgorithm) targetNode).isEnabled() && !((JIPipeAlgorithm) targetNode).isSkipped())) {
                                    progressInfo.log("Keeping " + graphNode.getDisplayName() + " [is skipped or disabled, but cache predecessor]");
                                    continue outer;
                                }
                            }
                        }
                    }

                    progressInfo.log("Deleting " + graphNode.getDisplayName() + " [is skipped or disabled]");
                    graph.removeNode(graphNode, false);
                }
            } else {
                progressInfo.log("Deleting " + graphNode.getDisplayName() + " [not an algorithm]");
                graph.removeNode(graphNode, false);
            }
        }

        // Turn off skipping for remaining
        // Will interfere with loops otherwise
        // NO! DONT DO THIS! QuickRun must ensure that the assignment is correct from the start
//        for (JIPipeGraphNode graphNode : graph.getGraphNodes()) {
//            if(graphNode instanceof JIPipeAlgorithm) {
//                ((JIPipeAlgorithm) graphNode).setSkipped(false);
//            }
//        }
    }

    /**
     * Iterates through output slots and assigns the data storage paths
     */
    private void initializeSlotStoragePaths() {
        if (configuration.getOutputPath() != null) {
            if (!Files.exists(configuration.getOutputPath())) {
                try {
                    Files.createDirectories(configuration.getOutputPath());
                } catch (IOException e) {
                    throw new JIPipeValidationRuntimeException(e,
                            "Could not create necessary directory '" + configuration.getOutputPath() + "'!",
                            "Either the path is invalid, or you do not have permissions to create the directory",
                            "Check if the path is valid and the parent directories are writeable by your current user");
                }
            }

            // Apply output path to the data slots
            for (JIPipeDataSlot slot : graph.getSlotNodes()) {
                if (slot.isOutput()) {
                    slot.setSlotStoragePath(configuration.getOutputPath().resolve(slot.getNode().getInternalStoragePath().resolve(slot.getName())));
                    try {
                        Files.createDirectories(slot.getSlotStoragePath());
                    } catch (IOException e) {
                        throw new JIPipeValidationRuntimeException(e,
                                "Could not create necessary directory '" + slot.getSlotStoragePath() + "'!",
                                "Either the path is invalid, or you do not have permissions to create the directory",
                                "Check if the path is valid and the parent directories are writeable by your current user");
                    }
                }
            }
        }
    }

    private void initializeScratchDirectory() {
        try {
            Files.createDirectories(configuration.getOutputPath().resolve("_scratch"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (JIPipeGraphNode algorithm : graph.getGraphNodes()) {
            algorithm.setScratchBaseDirectory(configuration.getOutputPath().resolve("_scratch"));
        }
    }

    private void initializeRelativeDirectories() {
        for (JIPipeGraphNode algorithm : graph.getGraphNodes()) {
            algorithm.setBaseDirectory(null);
        }
    }

    private void initializeNodeStoragePaths() {
        for (JIPipeGraphNode node : graph.getGraphNodes()) {
            JIPipeProjectCompartment compartment = project.getCompartments().get(node.getCompartmentUUIDInParentGraph());
            if (compartment != null) {
                node.setInternalStoragePath(Paths.get(StringUtils.safeJsonify(compartment.getAliasIdInParentGraph()))
                        .resolve(StringUtils.safeJsonify(graph.getAliasIdOf(node))));
            } else {
                node.setInternalStoragePath(Paths.get("_sub")
                        .resolve(UUID.randomUUID().toString())
                        .resolve(StringUtils.safeJsonify(graph.getAliasIdOf(node))));
            }
            node.setProjectDirectory(project.getWorkDirectory());
            node.setRuntimeProject(project);
        }
    }

    private void runGraph(JIPipeGraph graph, JIPipeProgressInfo progressInfo, JIPipeProgressInfo setupProgress) {
        setupProgress.log("Iterating on graph " + graph + " ...");
        JIPipeGraphRunPartitionGraph partitionGraph = new JIPipeGraphRunPartitionGraph(graph, this);
        setupProgress.log("-> Created partition graph " + partitionGraph);

        JIPipeGraphRunGCGraph gcGraph = new JIPipeGraphRunGCGraph(graph, graph.getGraphNodes());
        gcGraph.getGcEventEmitter().subscribe(this);
        setupProgress.log("-> Created GC graph " + gcGraph);

        if (partitionGraph.vertexSet().size() == 1) {
            setupProgress.log("--> Single partition. Running graph.");
            runPartitionNodeSet(graph, partitionGraph.vertexSet().iterator().next(), gcGraph, progressInfo, setupProgress);
        } else if (partitionGraph.vertexSet().size() > 1) {
            setupProgress.log("--> Multiple partitions. Executing partitions in topological order.");
            TopologicalOrderIterator<Set<JIPipeGraphNode>, DefaultEdge> orderIterator = new TopologicalOrderIterator<>(partitionGraph);
            ImmutableList<Set<JIPipeGraphNode>> partitionNodeSets = ImmutableList.copyOf(orderIterator);
            for (int i = 0; i < partitionNodeSets.size(); i++) {
                Set<JIPipeGraphNode> partitionNodeSet = partitionNodeSets.get(i);
                if (partitionNodeSet.isEmpty())
                    continue;
                runPartitionNodeSet(graph, partitionNodeSet, gcGraph, progressInfo, setupProgress);
            }
        } else {
            setupProgress.log("--> Nothing to do");
        }
    }

    private void runPartitionNodeSet(JIPipeGraph graph, Set<JIPipeGraphNode> partitionNodeSet, JIPipeGraphRunGCGraph gcGraph, JIPipeProgressInfo progressInfo, JIPipeProgressInfo setupProgress) {
        int partitionId = 0;
        for (JIPipeGraphNode graphNode : partitionNodeSet) {
            if (graphNode instanceof JIPipeAlgorithm) {
                partitionId = Math.max(0, ((JIPipeAlgorithm) graphNode).getRuntimePartition().getIndex());
            }
        }
        JIPipeRuntimePartition runtimePartition = getRuntimePartition(partitionId);
        setupProgress.log("--> Selected runtime partition id=" + partitionId + " as name=" + runtimePartition.getName());

        boolean passThroughMode = runtimePartition.getIterationMode() == JIPipeGraphWrapperAlgorithm.IterationMode.PassThrough;
        if (!passThroughMode && configuration.isStoreToCache() && runtimePartition.isForcePassThroughLoopIterationInCaching()) {
            passThroughMode = true;
        }

        if (passThroughMode) {
            // Non-iterating mode
            // R26: the partition node is a PARTITION-kind grouping node (squashed by the panel into a
            // chip); partition machinery narration goes to the "Pipeline setup" phase, the partition
            // handle is solely the parent for node rows.
            // Binding: 5th argument = the PARTITION handle (parent of node rows and op children),
            // 6th argument = the "Pipeline setup" phase handle (narration, failure banners),
            // 7th argument = the run-root handle (machinery progress for the footer, R38).
            JIPipeProgressInfo partitionProgress = progressInfo.resolve("Partition " + partitionId, JIPipeProgressNodeKind.PARTITION);
            runPassThroughPartitionNodeSet(graph, partitionNodeSet, gcGraph, runtimePartition, partitionProgress, setupProgress, progressInfo);
        } else {
            // R26: partition nodes are recordless. Wrapper/partition machinery narration goes to the
            // "Pipeline setup" phase; the partition handle is solely the parent for node rows and op
            // children.
            // Binding: 5th argument = the PARTITION handle (parent of node rows and op children),
            // 6th argument = the "Pipeline setup" phase handle (narration, progress, failure banners).
            JIPipeProgressInfo partitionProgress = progressInfo.resolve("Looped partition " + partitionId, JIPipeProgressNodeKind.PARTITION);
            runIteratingPartitionNodeSet(graph, partitionNodeSet, gcGraph, runtimePartition, partitionProgress, setupProgress);
        }
    }

    private void runIteratingPartitionNodeSet(JIPipeGraph graph, Set<JIPipeGraphNode> partitionNodeSet, JIPipeGraphRunGCGraph gcGraph, JIPipeRuntimePartition runtimePartition, JIPipeProgressInfo progressInfo, JIPipeProgressInfo setupProgress) {

//        System.out.println("--> START NEW LOOP");

        JIPipeGraph extracted = graph.extract(partitionNodeSet, true, false);
        JIPipeGraphWrapperAlgorithm graphWrapperAlgorithm = new JIPipeGraphWrapperAlgorithm(new JIPipeEmptyNodeInfo(), extracted);
        graphWrapperAlgorithm.getGroupInput().setRuntimePartition(new RuntimePartitionReferenceParameter(project.getRuntimePartitions().indexOf(runtimePartition)));
        graphWrapperAlgorithm.getGroupOutput().setRuntimePartition(new RuntimePartitionReferenceParameter(project.getRuntimePartitions().indexOf(runtimePartition)));

        // Configure iterations
        graphWrapperAlgorithm.setIterationMode(runtimePartition.getIterationMode());

        // Configure continue on failure
        switch (configuration.getContinueOnFailure()) {
            case Enable:
            case Disable:
                // We are in a nested loop -> Disable (handled by the parent loop)
                graphWrapperAlgorithm.setContinueOnFailure(JIPipeGraphRunPartitionInheritedBoolean.Disable);
                break;
            case InheritFromPartition:

                // We are in the main loop -> Set from partition
                graphWrapperAlgorithm.setContinueOnFailure(isContinueOnFailure(runtimePartition) ? JIPipeGraphRunPartitionInheritedBoolean.Enable : JIPipeGraphRunPartitionInheritedBoolean.Disable);
                break;
        }

        // Configure failure export
        switch (configuration.getContinueOnFailureExportFailedInputs()) {
            case Disable:
            case Enable:
                // We are in a nested loop -> Disable (handled by the parent loop)
                graphWrapperAlgorithm.setContinueOnFailureExportFailedInputs(JIPipeGraphRunPartitionInheritedBoolean.Disable);
                break;
            default:
                // We are in a main loop -> Decide from partition
                graphWrapperAlgorithm.setContinueOnFailureExportFailedInputs(runtimePartition.getContinueOnFailureSettings().isExportFailedLoopInputs() ?
                        JIPipeGraphRunPartitionInheritedBoolean.Enable : JIPipeGraphRunPartitionInheritedBoolean.Disable);

                break;
        }

        setupProgress.log("--> Continue on failure set to " + graphWrapperAlgorithm.getContinueOnFailure() + " (in config " + configuration.getContinueOnFailure() + ")");
        setupProgress.log("--> Continue on failure backup mode set to " + graphWrapperAlgorithm.getContinueOnFailureExportFailedInputs() + " (in config " + configuration.getContinueOnFailureExportFailedInputs() + ")");

        if (runtimePartition.getIterationMode() == JIPipeGraphWrapperAlgorithm.IterationMode.IteratingDataBatch) {
            graphWrapperAlgorithm.setBatchGenerationSettings(new JIPipeMergingAlgorithmIterationStepGenerationSettings(runtimePartition.getLoopIterationIteratingSettings()));
        } else {
            graphWrapperAlgorithm.setBatchGenerationSettings(new JIPipeMergingAlgorithmIterationStepGenerationSettings(runtimePartition.getLoopIterationMergingSettings()));
        }

        // Create a group i/o slot for all interfacing inputs and all outputs
        BiMap<JIPipeDataSlot, String> inputMap = HashBiMap.create();
        BiMap<JIPipeDataSlot, String> outputMap = HashBiMap.create();
        final boolean isCaching = configuration.isStoreToCache();
        final boolean isExporting = configuration.isStoreToDisk();
        for (JIPipeGraphNode node : partitionNodeSet) {
            if (node instanceof JIPipeAlgorithm) {

                // Register interfacing inputs (all others MUST be left alone!)
                outer:
                for (JIPipeInputDataSlot inputSlot : node.getInputSlots()) {
                    for (JIPipeGraphEdge graphEdge : graph.getGraph().incomingEdgesOf(inputSlot)) {
                        JIPipeDataSlot edgeSource = graph.getGraph().getEdgeSource(graphEdge);
                        if (edgeSource.getNode() instanceof JIPipeAlgorithm) {
                            if (!runtimePartitionEquals(edgeSource.getNode(), node)) {
                                String uuid = UUID.randomUUID().toString();
                                setupProgress.log("--> Detecting interfacing input " + inputSlot.getDisplayName() + " (registered as " + uuid + ")");
                                inputMap.put(inputSlot, uuid);
                                continue outer;
                            }
                        }
                    }
                }

                // Register all interfacing or endpoint outputs (important due to caching) or intermediate outputs
                outer:
                for (JIPipeOutputDataSlot outputSlot : node.getOutputSlots()) {
                    if (graph.getGraph().outDegreeOf(outputSlot) == 0) {
                        // Detected an endpoint
                        String uuid = UUID.randomUUID().toString();

                        if (!isCaching && !runtimePartition.getOutputSettings().isExportLoopTerminating()) {
                            if (runtimePartition.getOutputSettings().isAlwaysExportCompartmentOutputs() && outputSlot.getNode() instanceof JIPipeProjectCompartmentOutput) {
                                setupProgress.log("--> Compartment output " + outputSlot.getDisplayName() + " (registered as " + uuid + ") --> endpoint will be exported [always export compartment outputs]");
                            } else {
                                setupProgress.log("--> NOT detecting endpoint output " + outputSlot.getDisplayName() + " (registered as " + uuid + ") [export terminating nodes disabled]");
                                continue;
                            }
                        }
                        if (!isCaching && !isExporting) {
                            setupProgress.log("--> NOT detecting endpoint output " + outputSlot.getDisplayName() + " (registered as " + uuid + ") [neither exporting, nor caching]");
                            continue;
                        }

                        setupProgress.log("--> Detecting endpoint output " + outputSlot.getDisplayName() + " (registered as " + uuid + ")");
                        outputMap.put(outputSlot, uuid);
                    } else {
                        for (JIPipeGraphEdge graphEdge : graph.getGraph().outgoingEdgesOf(outputSlot)) {
                            JIPipeDataSlot edgeTarget = graph.getGraph().getEdgeTarget(graphEdge);
                            if (edgeTarget.getNode() instanceof JIPipeAlgorithm) {
                                if (!runtimePartitionEquals(edgeTarget.getNode(), node)) {
                                    String uuid = UUID.randomUUID().toString();
                                    setupProgress.log("--> Detecting interfacing output " + outputSlot.getDisplayName() + " (registered as " + uuid + ")");
                                    outputMap.put(outputSlot, uuid);
                                    continue outer;
                                } else {
                                    // Detected an intermediate result

                                    if (isCaching) {
                                        // Store unless deactivated
                                        if (!outputSlot.isSkipCache() && !configuration.getDisableStoreToCacheNodes().contains(outputSlot.getNode().getUUIDInParentGraph())) {
                                            String uuid = UUID.randomUUID().toString();
                                            setupProgress.log("--> Detecting intermediate output " + outputSlot.getDisplayName() + " (registered as " + uuid + ") [cache]");
                                            outputMap.put(outputSlot, uuid);
                                            continue outer;
                                        }
                                    }

                                    if (isExporting) {
                                        // Store unless deactivated and user activated storage of intermediates
                                        if (!outputSlot.isSkipExport() && runtimePartition.getOutputSettings().isExportLoopIntermediateResults() && outputSlot.getInfo().isStoreToDisk()) {
                                            String uuid = UUID.randomUUID().toString();
                                            setupProgress.log("--> Detecting intermediate output " + outputSlot.getDisplayName() + " (registered as " + uuid + ") [export]");
                                            outputMap.put(outputSlot, uuid);
                                            continue outer;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        for (Map.Entry<JIPipeDataSlot, String> entry : inputMap.entrySet()) {
            JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) graphWrapperAlgorithm.getGroupInput().getSlotConfiguration();
            JIPipeDataSlotInfo source = entry.getKey().getInfo();
            slotConfiguration.addSlot(new JIPipeDataSlotInfo(source.getDataClass(), JIPipeSlotType.Input, entry.getValue(), "", source.isOptional()), false);
        }
        for (Map.Entry<JIPipeDataSlot, String> entry : outputMap.entrySet()) {
            JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) graphWrapperAlgorithm.getGroupOutput().getSlotConfiguration();
            JIPipeDataSlotInfo source = entry.getKey().getInfo();
            slotConfiguration.addSlot(new JIPipeDataSlotInfo(source.getDataClass(), JIPipeSlotType.Output, entry.getValue(), "", source.isOptional()), false);
        }

        graphWrapperAlgorithm.updateGroupSlots();

        // Connected graph wrapper algorithm internally
        for (Map.Entry<JIPipeDataSlot, String> entry : inputMap.entrySet()) {
            // [Group input] out --> input slot
            JIPipeOutputDataSlot thereOutputSlot = graphWrapperAlgorithm.getGroupInput().getOutputSlot(entry.getValue());
            JIPipeInputDataSlot hereInputSlot = (JIPipeInputDataSlot) entry.getKey();
            JIPipeDataSlot thereInputSlot = graphWrapperAlgorithm.getWrappedGraph().getEquivalentSlot(hereInputSlot);
            setupProgress.log("Wrapped graph connect " + thereOutputSlot.getDisplayName() + " >>> " + thereInputSlot.getDisplayName());
            graphWrapperAlgorithm.getWrappedGraph().connect(thereOutputSlot, thereInputSlot);
        }
        for (Map.Entry<JIPipeDataSlot, String> entry : outputMap.entrySet()) {
            // output slot --> [Group output] in
            JIPipeInputDataSlot thereInputSlot = graphWrapperAlgorithm.getGroupOutput().getInputSlot(entry.getValue());
            JIPipeOutputDataSlot hereOutputSlot = (JIPipeOutputDataSlot) entry.getKey();
            JIPipeDataSlot thereOutputSlot = graphWrapperAlgorithm.getWrappedGraph().getEquivalentSlot(hereOutputSlot);
            setupProgress.log("Wrapped graph connect " + thereOutputSlot.getDisplayName() + " >>> " + thereInputSlot.getDisplayName());
            graphWrapperAlgorithm.getWrappedGraph().connect(thereOutputSlot, thereInputSlot);
        }

        // Pull data into the original input slots
        // Trigger GC accordingly
        setupProgress.log("Gathering inputs ...");
        for (Map.Entry<JIPipeDataSlot, String> entry : inputMap.entrySet()) {
            JIPipeInputDataSlot inputSlot = (JIPipeInputDataSlot) entry.getKey();
            // R51: the slot narration is debug-level (not informative enough for INFO) and names
            // the node UUID (the same idiom as the "Executing <uuid>" record); without a node the
            // rendered slot name is the fallback (mirrors getDisplayName's null tolerance)
            JIPipeGraphNode inputNode = inputSlot.getNode();
            String slotNarration = inputNode != null
                    ? inputSlot.getName() + " (" + inputNode.getUUIDInParentGraph() + ")"
                    : inputSlot.getDisplayName();
            nodeProgressHandle(inputNode, progressInfo).debug("+I " + slotNarration);

            // Add data from incoming edges within graph
            Set<JIPipeGraphEdge> incomingEdges = graph.getGraph().incomingEdgesOf(inputSlot);
            for (JIPipeGraphEdge graphEdge : incomingEdges) {
                JIPipeDataSlot outputSlot = graph.getGraph().getEdgeSource(graphEdge);

                if (!inputSlot.isSkipDataGathering()) {
                    // R28: the gathering operation nests under the consuming node's row
                    // R35: the gathering operation row terminates with the operation
                    JIPipeProgressInfo gatherStep = nodeProgressHandle(inputSlot.getNode(), progressInfo).resolve(outputSlot.getDisplayName() + " >>> " + inputSlot.getDisplayName());
                    finishWhenDone(gatherStep, () -> inputSlot.addDataFromTable(outputSlot, gatherStep));
                } else {
                    nodeProgressHandle(inputSlot.getNode(), progressInfo).log("NOT copying " + outputSlot.getDisplayName() + " >>> " + inputSlot.getDisplayName() + " [data gathering disabled]");
                }

                if (gcGraph != null) {
                    JIPipeProgressInfo gcStep = nodeProgressHandle(inputSlot.getNode(), progressInfo).resolve("GC", JIPipeProgressNodeKind.ITEM, JIPipeLogLevel.DEBUG);
                    finishWhenDone(gcStep, () -> gcGraph.removeOutputToInputEdge(outputSlot, inputSlot, gcStep));
                }
            }
        }

        // Move data from original input slots to wrapper input slots
        setupProgress.log("Moving inputs to wrapped nodes ...");
        for (Map.Entry<JIPipeDataSlot, String> entry : inputMap.entrySet()) {
            JIPipeInputDataSlot originalInputSlot = (JIPipeInputDataSlot) entry.getKey();
            JIPipeInputDataSlot targetInputSlot = graphWrapperAlgorithm.getInputSlot(entry.getValue());
            JIPipeProgressInfo moveStep = setupProgress.resolve("I -> W-I");
            finishWhenDone(moveStep, () -> targetInputSlot.addDataFromTable(originalInputSlot, moveStep));

            // GC the original
            originalInputSlot.clear(false, setupProgress);

//            System.out.println("GWI: " + targetInputSlot);
        }

        // Execute the graph wrapper
        try {
            graphWrapperAlgorithm.run(runContext, progressInfo);
        } catch (Throwable e) {
            if (progressInfo.isCancelled()) {
                throw e;
            }
            if (isContinueOnFailure(runtimePartition)) {
                setupProgress.log("Partition iteration execution FAILED!");
                setupProgress.log("Message: " + e.getMessage());
                setupProgress.log("CONTINUING AS REQUESTED!");

                // Check if we are in update cache
                if (runtimePartition.getContinueOnFailureSettings().isDisableOnUpdateCache() && runContext.getGraphRun().getConfiguration().isStoreToCache()) {
                    setupProgress.log("CONTINUE ON FAILURE IS TURNED OFF DUE TO A USER SETTING IN THE PARTITION");
                    throw e;
                }

                // Clean to prevent moving corrupted data out
                graphWrapperAlgorithm.clearSlotData(false, setupProgress);

                progressInfo.getNotifications().push(new JIPipeNotification(UUID.randomUUID().toString(), "Part of the pipeline failed",
                        "Iteration step for partition " + project.getRuntimePartitions().getFullName(runtimePartition) + " failed. " +
                                "The pipeline continued as setup within the runtime partition settings."));
            } else {
                throw e;
            }
        }

        // Copy outputs into nodes
        setupProgress.log("Moving outputs to parent graph nodes ...");
        for (Map.Entry<JIPipeDataSlot, String> entry : outputMap.entrySet()) {
            JIPipeOutputDataSlot targetOutputSlot = (JIPipeOutputDataSlot) entry.getKey();
            JIPipeOutputDataSlot sourceOutputSlot = graphWrapperAlgorithm.getOutputSlot(entry.getValue());
            JIPipeProgressInfo moveStep = setupProgress.resolve("W-O -> O [" + sourceOutputSlot.getDisplayName() + " >>> " + targetOutputSlot.getDisplayName() + "]");
            finishWhenDone(moveStep, () -> targetOutputSlot.addDataFromTable(sourceOutputSlot, moveStep));

            // GC the wrapped
            sourceOutputSlot.clear(false, setupProgress);
        }

        // Cleanup to be sure
        graphWrapperAlgorithm.clearSlotData(false, setupProgress);

        // Trigger GC
        if (gcGraph != null) {
            // Destroy all edges
            setupProgress.log("Informing GC ...");
            for (JIPipeGraphNode node : partitionNodeSet) {
                if (node instanceof JIPipeAlgorithm) {
                    // Not needed (done earlier)
                    for (JIPipeInputDataSlot inputSlot : node.getInputSlots()) {
                        for (JIPipeGraphEdge graphEdge : graph.getGraph().incomingEdgesOf(inputSlot)) {
                            JIPipeProgressInfo gcStep = nodeProgressHandle(node, progressInfo).resolve("GC", JIPipeProgressNodeKind.ITEM, JIPipeLogLevel.DEBUG);
                            finishWhenDone(gcStep, () -> gcGraph.removeOutputToInputEdge(graph.getGraph().getEdgeSource(graphEdge), inputSlot, gcStep));
                        }
                    }
                    JIPipeProgressInfo nodeGcStep = nodeProgressHandle(node, progressInfo).resolve("GC", JIPipeProgressNodeKind.ITEM, JIPipeLogLevel.DEBUG);
                    finishWhenDone(nodeGcStep, () -> gcGraph.removeInputToNodeEdge((JIPipeAlgorithm) node, nodeGcStep));
                    for (JIPipeOutputDataSlot outputSlot : node.getOutputSlots()) {
                        JIPipeProgressInfo gcStep = nodeProgressHandle(node, progressInfo).resolve("GC", JIPipeProgressNodeKind.ITEM, JIPipeLogLevel.DEBUG);
                        finishWhenDone(gcStep, () -> gcGraph.removeNodeToOutputEdge(outputSlot, gcStep));
                    }
                }
            }
        }
    }

    private void exportFailedInputs(JIPipeGraph graph, Set<JIPipeGraphNode> partitionNodeSet, Map<UUID, Map<String, JIPipeDataTable>> inputs, JIPipeProgressInfo progressInfo) {
        Path outputDir = configuration.getOutputPath().resolve("_error").resolve(UUID.randomUUID().toString());

        List<JIPipeTextAnnotation> annotations = new ArrayList<>();
        for (Map.Entry<UUID, Map<String, JIPipeDataTable>> nodeEntry : inputs.entrySet()) {
            for (JIPipeDataTable dataTable : nodeEntry.getValue().values()) {
                annotations.addAll(dataTable.getAllTextAnnotations());
            }
        }
        Map<String, String> annotationsStringMap = JIPipeTextAnnotation.annotationListToMap(annotations, JIPipeTextAnnotationMergeMode.Merge);
        String annotationsInfo = "";
        if (!annotationsStringMap.isEmpty()) {
            annotationsInfo = "Associated annotations: " + annotationsStringMap.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(", ")) + ". ";
        }


        progressInfo.getNotifications().push(new JIPipeNotification(UUID.randomUUID().toString(), "Part of the pipeline failed",
                "Iteration step for step " + progressInfo.getLogPrepend() + " failed. " +
                        annotationsInfo +
                        "The pipeline continued as setup within the runtime partition settings. " +
                        "The inputs and graph were exported to " + outputDir,
                new JIPipeNotificationAction("Open directory", "Opens the directory containing the inputs",
                        JIPipe.RESOURCES.getIcon16("actions/document-open-folder.png"), workbench -> {
                    UIUtils.desktopOpenFile(outputDir.toFile());
                })));

        progressInfo = progressInfo.resolve("Export failed inputs");
        try {
            progressInfo.log("Target directory: " + outputDir);
            try {
                Files.createDirectories(outputDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            for (Map.Entry<UUID, Map<String, JIPipeDataTable>> nodeMap : inputs.entrySet()) {
                for (Map.Entry<String, JIPipeDataTable> slotMap : nodeMap.getValue().entrySet()) {
                    Path slotOutputDir = outputDir.resolve(nodeMap.getKey().toString()).resolve(slotMap.getKey());
                    progressInfo.log("Writing input '" + slotMap.getKey() + "' of '" + nodeMap.getKey() + "' to " + slotOutputDir);
                    slotMap.getValue().exportData(new JIPipeFileSystemWriteDataStorage(progressInfo, slotOutputDir), progressInfo);
                }
            }

            // Export the graph
            progressInfo.log("Exporting graph ...");
            JIPipeGraph extracted = graph.extractShallow(partitionNodeSet, true, false);
            JsonUtils.saveToFile(extracted, outputDir.resolve("graph.json"));
            progressInfo.finishNode(JIPipeProgressNodeState.OK);
        } finally {
            progressInfo.finishNode(JIPipeProgressNodeState.FAILED);
        }
    }

    private void runPassThroughPartitionNodeSet(JIPipeGraph graph, Set<JIPipeGraphNode> nodeFilter, JIPipeGraphRunGCGraph gcGraph, JIPipeRuntimePartition runtimePartition, JIPipeProgressInfo progressInfo, JIPipeProgressInfo setupProgress, JIPipeProgressInfo rootProgress) {

        // A copy of the graph that is destroyed
        JIPipeGraphRunDataFlowGraph dataFlowGraph = new JIPipeGraphRunDataFlowGraph(graph, nodeFilter);
        // R26: partition nodes are recordless: partition-scope narration goes to the "Pipeline
        // setup" phase, the partition handle is solely the parent for node rows
        setupProgress.log("Built data flow graph " + dataFlowGraph);

        int progress = 0;
        final int maxProgress = dataFlowGraph.vertexSet().size();
        // R38: machinery progress feeds the footer via the run root (p records on the root render
        // nowhere and keep the conditional root row hidden)
        rootProgress.setProgress(progress, maxProgress);

        // Save for error state
        Map<UUID, Map<String, JIPipeDataTable>> continueOnErrorBackup = new HashMap<>();

        try {
            while (!dataFlowGraph.vertexSet().isEmpty()) {

                if (progressInfo.isCancelled()) {
                    throw new JIPipeValidationRuntimeException(new InterruptedException(),
                            "Execution was cancelled",
                            "You cancelled the execution of the pipeline.",
                            null);
                }

                // Increment progress
                ++progress;
                rootProgress.setProgress(progress, maxProgress);

                // Find the best candidate (shortest path)
                Object nextVertex = dataFlowGraph.getBestNextVertex();

                if (nextVertex == null) {
                    throw new NullPointerException("No candidate found! Are there cycles in the graph?");
                }

                if (isContinueOnFailure(runtimePartition) && isContinueOnFailureBackup(runtimePartition) && nextVertex instanceof JIPipeAlgorithm) {
                    JIPipeAlgorithm algorithm = (JIPipeAlgorithm) nextVertex;
                    UUID uuid = algorithm.getUUIDInParentGraph();
                    // Backup pre execution if enabled
                    // We back up the data if one of the inputs is sourced from another partition OR gathering is turned off
                    boolean backup = false;
                    for (JIPipeInputDataSlot inputSlot : algorithm.getInputSlots()) {
                        if (inputSlot.isSkipDataGathering()) {
                            backup = true;
                            break;
                        }
                        if (!backup) {
                            for (JIPipeGraphEdge edge : graph.getGraph().incomingEdgesOf(inputSlot)) {
                                JIPipeDataSlot edgeSource = graph.getGraph().getEdgeSource(edge);
                                if (!runtimePartitionEquals(edgeSource.getNode(), inputSlot.getNode())) {
                                    backup = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (backup) {
                        nodeProgressHandle(algorithm, progressInfo).log("Backing up inputs of " + algorithm.getDisplayName() + " [" + uuid + "]");
                        Map<String, JIPipeDataTable> slotMap = new HashMap<>();
                        for (JIPipeInputDataSlot inputSlot : algorithm.getInputSlots()) {
                            JIPipeDataTable backupTable = new JIPipeDataTable(inputSlot, true, setupProgress);
                            slotMap.put(inputSlot.getName(), backupTable);
                        }
                        continueOnErrorBackup.put(uuid, slotMap);
                    }
                }

                // Execute operation & cleanup
                runFlowGraphNode(nextVertex, dataFlowGraph, gcGraph, false, runtimePartition, progressInfo);
            }
        } catch (Throwable e) {
            if (progressInfo.isCancelled()) {
                throw e;
            }
            if (isContinueOnFailure(runtimePartition)) {
                setupProgress.log("Partition execution FAILED!");
                setupProgress.log("Message: " + e.getMessage());
                setupProgress.log("CONTINUING AS REQUESTED!");

                // Check if we are in update cache
                if (runtimePartition.getContinueOnFailureSettings().isDisableOnUpdateCache() && runContext.getGraphRun().getConfiguration().isStoreToCache()) {
                    setupProgress.log("CONTINUE ON FAILURE IS TURNED OFF DUE TO A USER SETTING IN THE PARTITION");
                    throw e;
                }

                // Dump errored data
                exportFailedInputs(graph, nodeFilter, continueOnErrorBackup, setupProgress);

                // Cleanup
                while (!dataFlowGraph.vertexSet().isEmpty()) {
                    // Find the best candidate (shortest path)
                    Object nextVertex = dataFlowGraph.getBestNextVertex();

                    if (nextVertex == null) {
                        throw new NullPointerException("No candidate found! Are there cycles in the graph?");
                    }

                    // Execute operation & cleanup
                    JIPipeProgressInfo cleanupStep = progressInfo.resolve("Cleanup");
                    finishWhenDone(cleanupStep, () -> runFlowGraphNode(nextVertex, dataFlowGraph, gcGraph, true, runtimePartition, cleanupStep));
                }
            } else {
                throw e;
            }
        } finally {

            // Cleanup backup tables
            for (Map.Entry<UUID, Map<String, JIPipeDataTable>> nodeEntry : continueOnErrorBackup.entrySet()) {
                for (JIPipeDataTable dataTable : nodeEntry.getValue().values()) {
                    dataTable.clear(false, setupProgress);
                }
            }

        }

    }

    private boolean isContinueOnFailure(JIPipeRuntimePartition runtimePartition) {
        switch (configuration.getContinueOnFailure()) {
            case Enable:
                return true;
            case Disable:
                return false;
            default:
                return runtimePartition.getContinueOnFailureSettings().isContinueOnFailure();
        }
    }

    private boolean isContinueOnFailureBackup(JIPipeRuntimePartition runtimePartition) {
        switch (configuration.getContinueOnFailureExportFailedInputs()) {
            case Enable:
                return true;
            case Disable:
                return false;
            default:
                return runtimePartition.getContinueOnFailureSettings().isExportFailedPartitionInputs();
        }
    }


    /**
     * Returns the per-run progress handle of a graph node, creating it as a child of the given
     * parent on first use (R28: operations of a node nest under the node's single row). The
     * node's location URI carries the graph node UUID (spec §Desktop UI "Location URIs"): the
     * progress tree UI renders it as the faint [uuid] suffix and can navigate to the node.
     */
    private JIPipeProgressInfo nodeProgressHandle(JIPipeGraphNode node, JIPipeProgressInfo parent) {
        return nodeProgressHandles.computeIfAbsent(node,
                key -> parent.resolve(node.getDisplayName(), "node://" + node.getUUIDInParentGraph()));
    }

    /**
     * Runs an operation and finishes its operation row (R35): OK when the operation completes,
     * FAILED when it throws (the exception propagates). An operation row never survives RUNNING.
     *
     * @param step      the operation row created for the operation
     * @param operation the operation
     * @return the operation's result
     */
    private <T> T finishWhenDone(JIPipeProgressInfo step, Supplier<T> operation) {
        try {
            T result = operation.get();
            step.finishNode(JIPipeProgressNodeState.OK);
            return result;
        } finally {
            step.finishNode(JIPipeProgressNodeState.FAILED);
        }
    }

    private void finishWhenDone(JIPipeProgressInfo step, Runnable operation) {
        finishWhenDone(step, () -> {
            operation.run();
            return null;
        });
    }

    private void runFlowGraphNode(Object flowGraphNode, JIPipeGraphRunDataFlowGraph dataFlowGraph, JIPipeGraphRunGCGraph gcGraph, boolean skipWorkload, JIPipeRuntimePartition runtimePartition, JIPipeProgressInfo progressInfo) {
        if (flowGraphNode instanceof JIPipeInputDataSlot) {
            runInputFlowGraphNode(flowGraphNode, dataFlowGraph, gcGraph, skipWorkload, progressInfo);
        } else if (flowGraphNode instanceof JIPipeOutputDataSlot) {
            runOutputFlowGraphNode(flowGraphNode, dataFlowGraph, gcGraph, progressInfo);
        } else if (flowGraphNode instanceof JIPipeAlgorithm) {
            runAlgorithmFlowGraphNode(flowGraphNode, dataFlowGraph, gcGraph, skipWorkload, progressInfo);

        } else {
            progressInfo.log("[!] Unsupported flow graph node type: " + flowGraphNode);

            // Remove vertex
            dataFlowGraph.removeVertex(flowGraphNode);
        }
    }

    private void runAlgorithmFlowGraphNode(Object flowGraphNode, JIPipeGraphRunDataFlowGraph dataFlowGraph, JIPipeGraphRunGCGraph gcGraph, boolean skipWorkload, JIPipeProgressInfo progressInfo) {
        JIPipeAlgorithm algorithm = (JIPipeAlgorithm) flowGraphNode;
        // R28: the node row is the cached per-node handle (the former "+N" narration line is
        // superseded by the row itself)
        JIPipeProgressInfo algorithmProgress = nodeProgressHandle(algorithm, progressInfo);
        algorithmProgress.log("Executing " + algorithm.getUUIDInParentGraph());

        if (!skipWorkload) {
            if (!tryLoadFromCache(algorithm, algorithmProgress)) {
                try {
                    if (!algorithm.isSkipped() && algorithm.isEnabled() && algorithm.getInfo().isRunnable()) {
                        algorithm.run(runContext, algorithmProgress);
                    } else {
                        algorithmProgress.log("Not runnable (skipped/disabled/node info not marked as runnable). Workload will not be executed!");
                    }
                } catch (Exception e) {
                    algorithmProgress.finishNode(JIPipeProgressNodeState.FAILED);
                    throw new JIPipeValidationRuntimeException(
                            new GraphNodeValidationReportContext(algorithm),
                            e,
                            "An error occurred during processing",
                            "On running the algorithm '" + algorithm.getDisplayName(),
                            "Please follow the instructions for the other error messages.");
                }
            }
            // R35: the node row terminates when its workload completes (cache hit, skipped/disabled, or run returned)
            algorithmProgress.finishNode(JIPipeProgressNodeState.OK);
        }

        if (gcGraph != null) {
            JIPipeProgressInfo gcStep = algorithmProgress.resolve("GC", JIPipeProgressNodeKind.ITEM, JIPipeLogLevel.DEBUG);
            finishWhenDone(gcStep, () -> gcGraph.removeInputToNodeEdge(algorithm, gcStep));
        }

        // Remove vertex
        dataFlowGraph.removeVertex(flowGraphNode);
    }

    private void runOutputFlowGraphNode(Object flowGraphNode, JIPipeGraphRunDataFlowGraph dataFlowGraph, JIPipeGraphRunGCGraph gcGraph, JIPipeProgressInfo progressInfo) {
        JIPipeOutputDataSlot outputSlot = (JIPipeOutputDataSlot) flowGraphNode;
        // R28: the "+O" narration and the contents log are records on the owning node's row
        // (the former duplicate child row with the node's display name is gone)
        JIPipeProgressInfo nodeProgress = nodeProgressHandle(outputSlot.getNode(), progressInfo);
        // R51: debug-level with the node UUID (the same idiom as the "Executing <uuid>" record)
        JIPipeGraphNode outputNode = outputSlot.getNode();
        String outputNarration = outputNode != null
                ? outputSlot.getName() + " (" + outputNode.getUUIDInParentGraph() + ")"
                : outputSlot.getDisplayName();
        nodeProgress.debug("+O " + outputNarration);
        nodeProgress.log("Contents of data slot " + outputSlot);

        if (gcGraph != null) {
            JIPipeProgressInfo gcStep = nodeProgress.resolve("GC", JIPipeProgressNodeKind.ITEM, JIPipeLogLevel.DEBUG);
            finishWhenDone(gcStep, () -> gcGraph.removeNodeToOutputEdge(outputSlot, gcStep));
        }

        // Remove vertex
        dataFlowGraph.removeVertex(flowGraphNode);
    }

    private void runInputFlowGraphNode(Object flowGraphNode, JIPipeGraphRunDataFlowGraph dataFlowGraph, JIPipeGraphRunGCGraph gcGraph, boolean skipWorkload, JIPipeProgressInfo progressInfo) {
        JIPipeInputDataSlot inputSlot = (JIPipeInputDataSlot) flowGraphNode;
        // R28: the "+I" narration and the gathering operations go under the consuming node's row;
        // creating the row here is correct: the node appears when its inputs gather
        JIPipeProgressInfo nodeProgress = nodeProgressHandle(inputSlot.getNode(), progressInfo);
        // R51: debug-level with the node UUID (the same idiom as the "Executing <uuid>" record);
        // without a node the rendered slot name is the fallback
        JIPipeGraphNode inputNode = inputSlot.getNode();
        String inputNarration = inputNode != null
                ? inputSlot.getName() + " (" + inputNode.getUUIDInParentGraph() + ")"
                : inputSlot.getDisplayName();
        nodeProgress.debug("+I " + inputNarration);

        // Add data from incoming edges within graph
        Set<JIPipeGraphEdge> incomingEdges = graph.getGraph().incomingEdgesOf(inputSlot);

        for (JIPipeGraphEdge graphEdge : incomingEdges) {

            JIPipeDataSlot outputSlot = graph.getGraph().getEdgeSource(graphEdge);

            if (!skipWorkload) {
                if (!inputSlot.isSkipDataGathering()) {
                    // R35: the gathering operation row terminates with the operation
                    JIPipeProgressInfo gatherStep = nodeProgress.resolve(outputSlot.getDisplayName() + " >>> " + inputSlot.getDisplayName());
                    finishWhenDone(gatherStep, () -> inputSlot.addDataFromTable(outputSlot, gatherStep));
                } else {
                    nodeProgress.log("NOT copying " + outputSlot.getDisplayName() + " >>> " + inputSlot.getDisplayName() + " [data gathering disabled]");
                }
            }

            if (gcGraph != null) {
                JIPipeProgressInfo gcStep = nodeProgress.resolve("GC", JIPipeProgressNodeKind.ITEM, JIPipeLogLevel.DEBUG);
                finishWhenDone(gcStep, () -> gcGraph.removeOutputToInputEdge(outputSlot, inputSlot, gcStep));
            }
        }

        // Remove vertex
        dataFlowGraph.removeVertex(flowGraphNode);
    }

    /**
     * Attempts to load data from cache
     *
     * @param runAlgorithm the target node (inside copy graph)
     * @return if successful. This means all output slots were restored.
     */
    private boolean tryLoadFromCache(JIPipeGraphNode runAlgorithm, JIPipeProgressInfo progressInfo) {
        if (!configuration.isLoadFromCache())
            return false;
        if (!(runAlgorithm instanceof JIPipeAlgorithm))
            return false;
        JIPipeProgressInfo queryStep = progressInfo.resolve("Query cache");
        Map<String, JIPipeDataTable> cachedData = finishWhenDone(queryStep,
                () -> project.getCache().query(runAlgorithm, runAlgorithm.getUUIDInParentGraph(), queryStep));
        if (!cachedData.isEmpty()) {
            progressInfo.log("Accessing cache of node " + runAlgorithm.getUUIDInParentGraph() + " (" + runAlgorithm.getDisplayName() + ")");
            for (JIPipeDataSlot outputSlot : runAlgorithm.getOutputSlots()) {
                if (!cachedData.containsKey(outputSlot.getName())) {
                    progressInfo.log(String.format("Cache access failed. Missing output slot %s", outputSlot.getName()));
                    return false;
                }
            }
            for (JIPipeDataSlot outputSlot : runAlgorithm.getOutputSlots()) {
                if (cachedData.get(outputSlot.getName()).isEmpty()) {
                    // If it's empty, we don't know
                    progressInfo.log(String.format("Cache for slot %s is empty!", outputSlot.getName()));
                    return false;
                }
                outputSlot.clearData(false, progressInfo);
                outputSlot.addDataFromTable(cachedData.get(outputSlot.getName()), progressInfo);
            }
            progressInfo.log("Cache data access successful.");
            return true;
        }
        return false;
    }

    public JIPipeProject getProject() {
        return project;
    }

    public JIPipeGraph getGraph() {
        return graph;
    }

    public boolean runtimePartitionEquals(JIPipeGraphNode node1, JIPipeGraphNode node2) {
        if (node1 instanceof JIPipeAlgorithm && node2 instanceof JIPipeAlgorithm) {
            return getValidRuntimePartitionIndex(((JIPipeAlgorithm) node1).getRuntimePartition().getIndex()) ==
                    getValidRuntimePartitionIndex(((JIPipeAlgorithm) node2).getRuntimePartition().getIndex());
        } else {
            throw new IllegalArgumentException("Not an algorithm!");
        }
    }

    public int getValidRuntimePartitionIndex(int index) {
        if (index < 0 || index >= runtimePartitions.size()) {
            return 0;
        } else {
            return index;
        }
    }

    public JIPipeRuntimePartition getRuntimePartition(RuntimePartitionReferenceParameter reference) {
        return getRuntimePartition(reference.getIndex());
    }

    public JIPipeRuntimePartition getRuntimePartition(int index) {
        return runtimePartitions.get(getValidRuntimePartitionIndex(index));
    }

    @Override
    public void onGCEvent(JIPipeGraphRunGCGraph.GCEvent event) {
        storeOutput(event.getSlot());
    }

    private void storeOutput(JIPipeOutputDataSlot outputDataSlot) {
        JIPipeProgressInfo storageProgress = getProgressInfo().resolve("Storage");
        try {
            if (project != null && configuration.isStoreToCache()) {
                if (!configuration.getDisableStoreToCacheNodes().contains(outputDataSlot.getNode().getUUIDInParentGraph()) && !outputDataSlot.isSkipCache()) {
                    storageProgress.log("Storing " + outputDataSlot.getDisplayName() + " into cache");
                    project.getCache().store(outputDataSlot.getNode(), outputDataSlot.getNode().getUUIDInParentGraph(), outputDataSlot, outputDataSlot.getName(), storageProgress);
                } else {
                    storageProgress.debug("NOT storing " + outputDataSlot.getDisplayName() + " [deactivated in config]");
                }
            }
            if (configuration.isStoreToDisk() && !configuration.getDisableStoreToDiskNodes().contains(outputDataSlot.getNode().getUUIDInParentGraph()) && outputDataSlot.getInfo().isStoreToDisk() && !outputDataSlot.isSkipExport()) {
                JIPipeRuntimePartition runtimePartition = getRuntimePartition(((JIPipeAlgorithm) outputDataSlot.getNode()).getRuntimePartition());

                JIPipeDataTable filtered = outputDataSlot.filter((table, row) -> {
                    JIPipeDataInfo info = table.getDataInfo(row);
                    if (info.isHeavy()) {
                        return runtimePartition.getOutputSettings().isExportHeavyData();
                    } else {
                        return runtimePartition.getOutputSettings().isExportLightweightData();
                    }
                });

                if (filtered.isEmpty()) {
                    storageProgress.debug("NOT Storing " + outputDataSlot.getDisplayName() + " to hard drive (is empty)");
                } else {
                    JIPipeProgressInfo saveProgress = storageProgress.resolveAndLog(String.format("Saving data in slot '%s' (data type %s)", outputDataSlot.getDisplayName(), JIPipeDataInfo.getInstance(outputDataSlot.getAcceptedDataType()).getName()));
                    storageProgress.log("Storing " + outputDataSlot.getDisplayName() + " to hard drive");

                    try {
                        filtered.exportData(new JIPipeFileSystemWriteDataStorage(saveProgress, outputDataSlot.getSlotStoragePath()), saveProgress);
                        saveProgress.finishNode(JIPipeProgressNodeState.OK);
                    } finally {
                        filtered.clear(false, storageProgress);
                        outputDataSlot.clear(false, storageProgress);
                        saveProgress.finishNode(JIPipeProgressNodeState.FAILED);
                    }
                }
            }
            storageProgress.finishNode(JIPipeProgressNodeState.OK);
        } finally {
            storageProgress.finishNode(JIPipeProgressNodeState.FAILED);
        }
    }
}
