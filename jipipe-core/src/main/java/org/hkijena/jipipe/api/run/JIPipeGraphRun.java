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
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeFixedThreadPool;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifact;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifactRepositoryApplyInstallUninstallRun;
import org.hkijena.jipipe.api.artifacts.JIPipeLocalArtifact;
import org.hkijena.jipipe.api.artifacts.JIPipeRemoteArtifact;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartmentOutput;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteDataStorage;
import org.hkijena.jipipe.api.environments.JIPipeArtifactEnvironment;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.grouping.JIPipeGraphWrapperAlgorithm;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithmIterationStepGenerationSettings;
import org.hkijena.jipipe.api.nodes.infos.JIPipeEmptyNodeInfo;
import org.hkijena.jipipe.api.notifications.JIPipeNotification;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationAction;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.runtimepartitioning.JIPipeRuntimePartition;
import org.hkijena.jipipe.api.runtimepartitioning.RuntimePartitionReferenceParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.plugins.artifacts.JIPipeArtifactApplicationSettings;
import org.hkijena.jipipe.utils.*;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.jar.Attributes;
import java.util.stream.Collectors;

public class JIPipeGraphRun extends AbstractJIPipeRunnable implements JIPipeGraphRunGCGraph.GCEventListener {

    private final JIPipeProject project;
    private final JIPipeGraph graph;
    private final JIPipeGraphRun parent;
    private final JIPipeGraphRunConfiguration configuration;
    private final List<JIPipeRuntimePartition> runtimePartitions;
    private JIPipeGraphNodeRunContext runContext;

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

        if (parent == null) {
            progressInfo.log("\n" +
                    "\n" +
                    "     _________  _            _____              __     ___              _  __        __ \n" +
                    " __ / /  _/ _ \\(_)__  ___   / ___/______ ____  / /    / _ \\__ _____    / |/ /____ __/ /_\n" +
                    "/ // // // ___/ / _ \\/ -_) / (_ / __/ _ `/ _ \\/ _ \\  / , _/ // / _ \\  /    / -_) \\ / __/\n" +
                    "\\___/___/_/  /_/ .__/\\__/  \\___/_/  \\_,_/ .__/_//_/ /_/|_|\\_,_/_//_/ /_/|_/\\__/_\\_\\\\__/ \n" +
                    "              /_/                      /_/                                              \n" +
                    "\n");
            progressInfo.log("JIPipe version: " + VersionUtils.getJIPipeVersion());
            Attributes manifestAttributes = ReflectionUtils.getManifestAttributes();
            if (manifestAttributes != null) {
                String implementationDateString = manifestAttributes.getValue("Implementation-Date");
                progressInfo.log("JIPipe build date: " + StringUtils.orElse(implementationDateString, "N/A"));
            }
            progressInfo.log(StringUtils.orElse("ImageJ version: " + IJ.getVersion(), "N/A"));
            progressInfo.log("Java version: " + StringUtils.orElse(System.getProperty("java.version"), "N/A"));
            progressInfo.log("Registered nodes types: " + JIPipe.getNodes().getRegisteredNodeInfos().size() + " nodes");
            progressInfo.log("Registered data types: " + JIPipe.getDataTypes().getRegisteredDataTypes().size() + " types");
            progressInfo.log("Enabled extensions: " + String.join(", ", JIPipe.getInstance().getPluginRegistry().getActivatedPlugins()));
            progressInfo.log("Operating system: " + SystemUtils.OS_NAME + " " + SystemUtils.OS_VERSION + " [" + SystemUtils.OS_ARCH + "]");
            progressInfo.log("Profile directory: " + JIPipe.getJIPipeUserDir());
            progressInfo.log("");
        }

        progressInfo.log("Erasing skipped/disabled algorithms (except direct predecessors) ...");
        cleanGraph(graph, progressInfo.resolve("Preprocessing/Cleanup"));
        progressInfo.log("Repairing partition assignments ...");
        fixPartitions(graph, progressInfo.resolve("Fix partitions"));

        long startTime = System.currentTimeMillis();
        if (parent == null) {
            progressInfo.log("JIPipe run starting at " + StringUtils.formatDateTime(LocalDateTime.now()));
        }

        progressInfo.log("Preparing ...");
        initializeNodeStoragePaths();
        initializeScratchDirectory();
        initializeRelativeDirectories();
        initializeSlotStoragePaths();
        if (parent == null) {
            progressInfo.log("Outputs will be written to " + configuration.getOutputPath());
        }

        // List of all environments
        List<JIPipeEnvironment> allEnvironments = new ArrayList<>();

        if (parent == null) {
            progressInfo.log("Preparing artifacts and environments ...");

            // Gather all environments from all active nodes
            for (JIPipeGraphNode graphNode : graph.getGraphNodes()) {
                graphNode.getEnvironmentDependencies(allEnvironments);
            }

            // Gather all requested artifact Ids
            Set<String> allRequestedArtifactIds = new HashSet<>();
            Map<String, String> requestedToTargetArtifactIds = new HashMap<>();
            Map<JIPipeArtifactEnvironment, String> environmentTargetArtifactIds = new HashMap<>();

            for (JIPipeEnvironment dependency : allEnvironments) {
                if (dependency instanceof JIPipeArtifactEnvironment) {
                    JIPipeArtifactEnvironment artifactEnvironment = (JIPipeArtifactEnvironment) dependency;
                    if (artifactEnvironment.isLoadFromArtifact()) {
                        String query = artifactEnvironment.getArtifactQuery().getQuery();
                        allRequestedArtifactIds.add(query);
                        requestedToTargetArtifactIds.put(query, query);
                        environmentTargetArtifactIds.put(artifactEnvironment, query);
                    }
                }
            }

            // Resolve the artifact IDs into the final ones
            // Then download if necessary
            if (!allRequestedArtifactIds.isEmpty()) {
                Set<String> remoteArtifactsToDownload = new HashSet<>();

                for (String requestedArtifactId : allRequestedArtifactIds) {
                    JIPipeProgressInfo artifactProgress = progressInfo.resolve("Artifacts").resolveAndLog(requestedArtifactId);
                    List<JIPipeArtifact> artifacts = JIPipe.getArtifacts().queryCachedArtifacts(requestedArtifactId);
                    artifacts.removeIf(artifact -> !artifact.isCompatible());
                    artifactProgress.log("Found " + artifacts.size() + " compatible matching artifacts");
                    JIPipeArtifact targetArtifact = null;

                    if (artifacts.isEmpty()) {
                        // Find alternative
                        artifactProgress.log("Unable to find compatible matching artifact! Finding alternative!");
                        artifacts = JIPipe.getArtifacts().queryCachedArtifacts(requestedArtifactId);
                        for (JIPipeArtifact artifact : artifacts) {
                            JIPipeArtifact closestCompatibleArtifact = JIPipe.getArtifacts().findClosestCompatibleArtifact(artifact.getFullId());
                            if (closestCompatibleArtifact != null) {
                                artifactProgress.log("SUCCEEDED in finding closest compatible artifact to " + artifact.getFullId() + " as " + closestCompatibleArtifact.getFullId());
                                targetArtifact = closestCompatibleArtifact;
                                break;
                            } else {
                                artifactProgress.log("FAILED to find closest compatible artifact to " + artifact.getFullId());
                            }
                        }
                    } else if (artifacts.size() > 1) {
                        artifactProgress.log("Warning: found " + artifacts.size() + " matching artifacts. Selecting first available one!");
                        targetArtifact = artifacts.get(0);
                    } else {
                        targetArtifact = artifacts.get(0);
                    }

                    if (targetArtifact == null) {
                        throw new RuntimeException("Unable to find matching compatible artifact for " + requestedArtifactId + ". Unable to continue.");
                    }

                    artifactProgress.log("Matched artifact: " + targetArtifact.getFullId());
                    if (targetArtifact instanceof JIPipeRemoteArtifact) {
                        artifactProgress.log("Artifact is not downloaded. Added to download queue.");
                        remoteArtifactsToDownload.add(targetArtifact.getFullId());
                    }

                    // Update the target map
                    requestedToTargetArtifactIds.put(requestedArtifactId, targetArtifact.getFullId());
                }

                // Download all missing artifacts
                if (!remoteArtifactsToDownload.isEmpty()) {

                    if (!JIPipeArtifactApplicationSettings.getInstance().isAutoDownload()) {
                        throw new JIPipeValidationRuntimeException(new UnspecifiedValidationReportContext(),
                                new UnsupportedOperationException("Artifact auto-download is disabled!"),
                                "Unable to auto-download artifacts",
                                "To run the pipeline, JIPipe requires to download external dependencies. " +
                                        "This feature is turned off in the settings.",
                                "Enable auto-downloading in Project > Application settings > General > Artifacts");
                    }

                    JIPipeProgressInfo artifactsProgress = progressInfo.resolve("Artifacts");
                    artifactsProgress.log("Artifacts to download: " + String.join(", ", remoteArtifactsToDownload));
                    List<JIPipeRemoteArtifact> toInstall = remoteArtifactsToDownload.stream().map(id -> (JIPipeRemoteArtifact) JIPipe.getArtifacts().queryCachedArtifacts(id).get(0)).collect(Collectors.toList());
                    JIPipeArtifactRepositoryApplyInstallUninstallRun run = new JIPipeArtifactRepositoryApplyInstallUninstallRun(
                            toInstall, Collections.emptyList());
                    run.setProgressInfo(artifactsProgress.resolve("Download"));
                    run.run();
                }

            }

            // Configure the environments
            progressInfo.log("Applying artifact configuration to environments ...");
            for (Map.Entry<JIPipeArtifactEnvironment, String> entry : environmentTargetArtifactIds.entrySet()) {
                String queryArtifactId = entry.getValue();
                String targetArtifactId = requestedToTargetArtifactIds.get(queryArtifactId);
                JIPipeArtifact artifact = JIPipe.getArtifacts().getCachedArtifacts().get(targetArtifactId);
                if (!(artifact instanceof JIPipeLocalArtifact)) {
                    throw new RuntimeException("Unable to find local artifact " + targetArtifactId + ", which was queried from " + queryArtifactId);
                }
                progressInfo.log("-> " + entry.getKey());
                entry.getKey().applyConfigurationFromArtifactAndSetLastArtifact((JIPipeLocalArtifact) artifact, progressInfo);
            }

            // Apply additional configuration steps
            for (JIPipeEnvironment environment : allEnvironments) {
                environment.runPreconfigure(this, progressInfo);
            }

        }

        boolean runFailed = false;

        try (JIPipeFixedThreadPool threadPool = new JIPipeFixedThreadPool(configuration.getNumThreads())) {

            // Setup context
            runContext = new JIPipeGraphNodeRunContext();
            runContext.setGraphRun(this);
            runContext.setThreadPool(threadPool);

            // Start iteration on initial graph
            progressInfo.log("--> Starting first iteration ...");
            runGraph(graph, progressInfo);
        } catch (Exception e) {
            runFailed = true;
            throw new RuntimeException(e);
        } finally {
            runContext = null;

            // Clear all slots
            for (JIPipeGraphNode node : graph.getGraphNodes()) {
                for (JIPipeInputDataSlot inputSlot : node.getInputSlots()) {
                    if (inputSlot.getRowCount() > 0) {
                        progressInfo.log("[!] Slot " + inputSlot.getDisplayName() + " still contains " + inputSlot.getRowCount() + " items! Clearing in post-processing!");
                    }
                    inputSlot.clear(false, progressInfo);
                }
                for (JIPipeOutputDataSlot outputSlot : node.getOutputSlots()) {
                    if (!outputSlot.isSkipGC()) {
                        if (outputSlot.getRowCount() > 0) {
                            progressInfo.log("[!] Slot " + outputSlot.getDisplayName() + " still contains " + outputSlot.getRowCount() + " items! Clearing in post-processing!");
                        }
                        outputSlot.clear(false, progressInfo);
                    }
                }
            }

            // Postprocessing
            if (parent == null) {
                progressInfo.log("Postprocessing steps ...");

                // Postprocess environments
                for (JIPipeEnvironment environment : allEnvironments) {
                    try {
                        environment.runPostprocessing(this, progressInfo);
                    }
                    catch (Throwable e) {
                        progressInfo.log(e);
                    }
                }

                try {
                    if (configuration.getOutputPath() != null && configuration.isStoreToDisk())
                        project.saveProject(configuration.getOutputPath().resolve("project.jip"));
                } catch (IOException e) {
                    throw new JIPipeValidationRuntimeException(e,
                            "Could not save project to '" + configuration.getOutputPath().resolve("project.jip") + "'!",
                            "Either the path is invalid, or you have no permission to write to the disk, or the disk space is full",
                            "Check if you can write to the output directory.");
                }

                progressInfo.log("\n\n------------------------");
                progressInfo.log("Run ending at " + StringUtils.formatDateTime(LocalDateTime.now()));
                progressInfo.log("\n--> Required " + StringUtils.formatDuration(System.currentTimeMillis() - startTime) + " to execute.\n");

                try {
                    if (configuration.getOutputPath() != null) {
                        Files.write(configuration.getOutputPath().resolve("log.txt"), progressInfo.getLog().toString().getBytes(Charsets.UTF_8));
                    }
                } catch (IOException e) {
                    throw new JIPipeValidationRuntimeException(e,
                            "Could not write log '" + configuration.getOutputPath().resolve("log.txt") + "'!",
                            "Either the path is invalid, or you have no permission to write to the disk, or the disk space is full",
                            "Check if you can write to the output directory.");
                }

                if (!progressInfo.getNotifications().isEmpty()) {
                    progressInfo.log("\n");
                    progressInfo.log("--> Graph run generated following reports:");
                    for (JIPipeNotification notification : progressInfo.getNotifications().getNotifications()) {
                        progressInfo.log("@" + StringUtils.formatDateTime(notification.getDateTime()) + "    " + notification.getHeading() + ": " + notification.getDescription());
                    }
                }

                if ((runFailed && configuration.isCleanupOutputsAfterFailure()) || (!runFailed && configuration.isCleanupOutputsAfterSuccess())) {
                    if (configuration.getOutputPath() != null) {
                        progressInfo.log("Removing output directory as requested: " + configuration.getOutputPath());
                        PathUtils.deleteDirectoryRecursively(configuration.getOutputPath(), progressInfo.resolve("Cleanup"));
                    }
                }

                progressInfo.log("\n------------------------\n\n");
            }
        }


    }

    private void fixPartitions(JIPipeGraph graph, JIPipeProgressInfo progressInfo) {
        for (JIPipeGraphNode graphNode : graph.getGraphNodes()) {
            if (graphNode instanceof JIPipeAlgorithm) {
                JIPipeAlgorithm algorithm = (JIPipeAlgorithm) graphNode;
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
            if (graphNode instanceof JIPipeAlgorithm) {
                JIPipeAlgorithm algorithm = (JIPipeAlgorithm) graphNode;
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

    private void runGraph(JIPipeGraph graph, JIPipeProgressInfo progressInfo) {
        progressInfo.log("Iterating on graph " + graph + " ...");
        JIPipeGraphRunPartitionGraph partitionGraph = new JIPipeGraphRunPartitionGraph(graph, this);
        progressInfo.log("-> Created partition graph " + partitionGraph);

        JIPipeGraphRunGCGraph gcGraph = new JIPipeGraphRunGCGraph(graph, graph.getGraphNodes());
        gcGraph.getGcEventEmitter().subscribe(this);
        progressInfo.log("-> Created GC graph " + gcGraph);

        if (partitionGraph.vertexSet().size() == 1) {
            progressInfo.log("--> Single partition. Running graph.");
            runPartitionNodeSet(graph, partitionGraph.vertexSet().iterator().next(), gcGraph, progressInfo);
        } else if (partitionGraph.vertexSet().size() > 1) {
            progressInfo.log("--> Multiple partitions. Executing partitions in topological order.");
            TopologicalOrderIterator<Set<JIPipeGraphNode>, DefaultEdge> orderIterator = new TopologicalOrderIterator<>(partitionGraph);
            ImmutableList<Set<JIPipeGraphNode>> partitionNodeSets = ImmutableList.copyOf(orderIterator);
            for (int i = 0; i < partitionNodeSets.size(); i++) {
                Set<JIPipeGraphNode> partitionNodeSet = partitionNodeSets.get(i);
                if (partitionNodeSet.isEmpty())
                    continue;
                runPartitionNodeSet(graph, partitionNodeSet, gcGraph, progressInfo);
            }
        } else {
            progressInfo.log("--> Nothing to do");
        }
    }

    private void runPartitionNodeSet(JIPipeGraph graph, Set<JIPipeGraphNode> partitionNodeSet, JIPipeGraphRunGCGraph gcGraph, JIPipeProgressInfo progressInfo) {
        int partitionId = 0;
        for (JIPipeGraphNode graphNode : partitionNodeSet) {
            if (graphNode instanceof JIPipeAlgorithm) {
                partitionId = Math.max(0, ((JIPipeAlgorithm) graphNode).getRuntimePartition().getIndex());
            }
        }
        JIPipeRuntimePartition runtimePartition = getRuntimePartition(partitionId);
        progressInfo.log("--> Selected runtime partition id=" + partitionId + " as name=" + runtimePartition.getName());

        boolean passThroughMode = runtimePartition.getIterationMode() == JIPipeGraphWrapperAlgorithm.IterationMode.PassThrough;
        if (!passThroughMode && configuration.isStoreToCache() && runtimePartition.isForcePassThroughLoopIterationInCaching()) {
            passThroughMode = true;
        }

        if (passThroughMode) {
            // Non-iterating mode
            JIPipeProgressInfo partitionProgress = progressInfo.resolve("Partition " + partitionId);
            runPassThroughPartitionNodeSet(graph, partitionNodeSet, gcGraph, runtimePartition, partitionProgress);
        } else {
            runIteratingPartitionNodeSet(graph, partitionNodeSet, gcGraph, runtimePartition, progressInfo.resolve("Looped partition " + partitionId));
        }
    }

    private void runIteratingPartitionNodeSet(JIPipeGraph graph, Set<JIPipeGraphNode> partitionNodeSet, JIPipeGraphRunGCGraph gcGraph, JIPipeRuntimePartition runtimePartition, JIPipeProgressInfo progressInfo) {

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

        progressInfo.log("--> Continue on failure set to " + graphWrapperAlgorithm.getContinueOnFailure() + " (in config " + configuration.getContinueOnFailure() + ")");
        progressInfo.log("--> Continue on failure backup mode set to " + graphWrapperAlgorithm.getContinueOnFailureExportFailedInputs() + " (in config " + configuration.getContinueOnFailureExportFailedInputs() + ")");

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
                                progressInfo.log("--> Detecting interfacing input " + inputSlot.getDisplayName() + " (registered as " + uuid + ")");
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
                                progressInfo.log("--> Compartment output " + outputSlot.getDisplayName() + " (registered as " + uuid + ") --> endpoint will be exported [always export compartment outputs]");
                            } else {
                                progressInfo.log("--> NOT detecting endpoint output " + outputSlot.getDisplayName() + " (registered as " + uuid + ") [export terminating nodes disabled]");
                                continue;
                            }
                        }
                        if (!isCaching && !isExporting) {
                            progressInfo.log("--> NOT detecting endpoint output " + outputSlot.getDisplayName() + " (registered as " + uuid + ") [neither exporting, nor caching]");
                            continue;
                        }

                        progressInfo.log("--> Detecting endpoint output " + outputSlot.getDisplayName() + " (registered as " + uuid + ")");
                        outputMap.put(outputSlot, uuid);
                    } else {
                        for (JIPipeGraphEdge graphEdge : graph.getGraph().outgoingEdgesOf(outputSlot)) {
                            JIPipeDataSlot edgeTarget = graph.getGraph().getEdgeTarget(graphEdge);
                            if (edgeTarget.getNode() instanceof JIPipeAlgorithm) {
                                if (!runtimePartitionEquals(edgeTarget.getNode(), node)) {
                                    String uuid = UUID.randomUUID().toString();
                                    progressInfo.log("--> Detecting interfacing output " + outputSlot.getDisplayName() + " (registered as " + uuid + ")");
                                    outputMap.put(outputSlot, uuid);
                                    continue outer;
                                } else {
                                    // Detected an intermediate result

                                    if (isCaching) {
                                        // Store unless deactivated
                                        if (!outputSlot.isSkipCache() && !configuration.getDisableStoreToCacheNodes().contains(outputSlot.getNode().getUUIDInParentGraph())) {
                                            String uuid = UUID.randomUUID().toString();
                                            progressInfo.log("--> Detecting intermediate output " + outputSlot.getDisplayName() + " (registered as " + uuid + ") [cache]");
                                            outputMap.put(outputSlot, uuid);
                                            continue outer;
                                        }
                                    }

                                    if (isExporting) {
                                        // Store unless deactivated and user activated storage of intermediates
                                        if (!outputSlot.isSkipExport() && runtimePartition.getOutputSettings().isExportLoopIntermediateResults() && outputSlot.getInfo().isStoreToDisk()) {
                                            String uuid = UUID.randomUUID().toString();
                                            progressInfo.log("--> Detecting intermediate output " + outputSlot.getDisplayName() + " (registered as " + uuid + ") [export]");
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
            progressInfo.log("Wrapped graph connect " + thereOutputSlot.getDisplayName() + " >>> " + thereInputSlot.getDisplayName());
            graphWrapperAlgorithm.getWrappedGraph().connect(thereOutputSlot, thereInputSlot);
        }
        for (Map.Entry<JIPipeDataSlot, String> entry : outputMap.entrySet()) {
            // output slot --> [Group output] in
            JIPipeInputDataSlot thereInputSlot = graphWrapperAlgorithm.getGroupOutput().getInputSlot(entry.getValue());
            JIPipeOutputDataSlot hereOutputSlot = (JIPipeOutputDataSlot) entry.getKey();
            JIPipeDataSlot thereOutputSlot = graphWrapperAlgorithm.getWrappedGraph().getEquivalentSlot(hereOutputSlot);
            progressInfo.log("Wrapped graph connect " + thereOutputSlot.getDisplayName() + " >>> " + thereInputSlot.getDisplayName());
            graphWrapperAlgorithm.getWrappedGraph().connect(thereOutputSlot, thereInputSlot);
        }

        // Pull data into the original input slots
        // Trigger GC accordingly
        progressInfo.log("Gathering inputs ...");
        for (Map.Entry<JIPipeDataSlot, String> entry : inputMap.entrySet()) {
            JIPipeInputDataSlot inputSlot = (JIPipeInputDataSlot) entry.getKey();
            progressInfo.log("+I " + inputSlot.getDisplayName());

            // Add data from incoming edges within graph
            Set<JIPipeGraphEdge> incomingEdges = graph.getGraph().incomingEdgesOf(inputSlot);
            for (JIPipeGraphEdge graphEdge : incomingEdges) {
                JIPipeDataSlot outputSlot = graph.getGraph().getEdgeSource(graphEdge);

                if (!inputSlot.isSkipDataGathering()) {
                    inputSlot.addDataFromTable(outputSlot, progressInfo.resolve(outputSlot.getDisplayName() + " >>> " + inputSlot.getDisplayName()));
                } else {
                    progressInfo.log("NOT copying " + outputSlot.getDisplayName() + " >>> " + inputSlot.getDisplayName() + " [data gathering disabled]");
                }

                if (gcGraph != null) {
                    gcGraph.removeOutputToInputEdge(outputSlot, inputSlot, progressInfo.resolve("GC"));
                }
            }
        }

        // Move data from original input slots to wrapper input slots
        progressInfo.log("Moving inputs to wrapped nodes ...");
        for (Map.Entry<JIPipeDataSlot, String> entry : inputMap.entrySet()) {
            JIPipeInputDataSlot originalInputSlot = (JIPipeInputDataSlot) entry.getKey();
            JIPipeInputDataSlot targetInputSlot = graphWrapperAlgorithm.getInputSlot(entry.getValue());
            targetInputSlot.addDataFromTable(originalInputSlot, progressInfo.resolve("I -> W-I"));

            // GC the original
            originalInputSlot.clear(false, progressInfo);

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
                progressInfo.log("\n\n------------------------\n" +
                        "Partition iteration execution FAILED!\n" +
                        "Message: " + e.getMessage() + "\n" +
                        "\n" +
                        "CONTINUING AS REQUESTED!\n" +
                        "------------------------\n\n");

                // Check if we are in update cache
                if (runtimePartition.getContinueOnFailureSettings().isDisableOnUpdateCache() && runContext.getGraphRun().getConfiguration().isStoreToCache()) {
                    progressInfo.log("CONTINUE ON FAILURE IS TURNED OFF DUE TO A USER SETTING IN THE PARTITION");
                    throw e;
                }

                // Clean to prevent moving corrupted data out
                graphWrapperAlgorithm.clearSlotData(false, progressInfo);

                progressInfo.getNotifications().push(new JIPipeNotification(UUID.randomUUID().toString(), "Part of the pipeline failed",
                        "Iteration step for partition " + project.getRuntimePartitions().getFullName(runtimePartition) + " failed. " +
                                "The pipeline continued as setup within the runtime partition settings."));
            } else {
                throw e;
            }
        }

        // Copy outputs into nodes
        progressInfo.log("Moving outputs to parent graph nodes ...");
        for (Map.Entry<JIPipeDataSlot, String> entry : outputMap.entrySet()) {
            JIPipeOutputDataSlot targetOutputSlot = (JIPipeOutputDataSlot) entry.getKey();
            JIPipeOutputDataSlot sourceOutputSlot = graphWrapperAlgorithm.getOutputSlot(entry.getValue());
            targetOutputSlot.addDataFromTable(sourceOutputSlot, progressInfo.resolve("W-O -> O [" + sourceOutputSlot.getDisplayName() + " >>> " + targetOutputSlot.getDisplayName() + "]"));

            // GC the wrapped
            sourceOutputSlot.clear(false, progressInfo);
        }

        // Cleanup to be sure
        graphWrapperAlgorithm.clearSlotData(false, progressInfo);

        // Trigger GC
        if (gcGraph != null) {
            // Destroy all edges
            progressInfo.log("Informing GC ...");
            for (JIPipeGraphNode node : partitionNodeSet) {
                if (node instanceof JIPipeAlgorithm) {
                    // Not needed (done earlier)
                    for (JIPipeInputDataSlot inputSlot : node.getInputSlots()) {
                        for (JIPipeGraphEdge graphEdge : graph.getGraph().incomingEdgesOf(inputSlot)) {
                            gcGraph.removeOutputToInputEdge(graph.getGraph().getEdgeSource(graphEdge), inputSlot, progressInfo.resolve("GC"));
                        }
                    }
                    gcGraph.removeInputToNodeEdge((JIPipeAlgorithm) node, progressInfo.resolve("GC"));
                    for (JIPipeOutputDataSlot outputSlot : node.getOutputSlots()) {
                        gcGraph.removeNodeToOutputEdge(outputSlot, progressInfo.resolve("GC"));
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
                        UIUtils.getIconFromResources("actions/document-open-folder.png"), workbench -> {
                    try {
                        Desktop.getDesktop().open(outputDir.toFile());
                    } catch (IOException e) {
                        IJ.handleException(e);
                    }
                })));

        progressInfo = progressInfo.resolve("Export failed inputs");
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
    }

    private void runPassThroughPartitionNodeSet(JIPipeGraph graph, Set<JIPipeGraphNode> nodeFilter, JIPipeGraphRunGCGraph gcGraph, JIPipeRuntimePartition runtimePartition, JIPipeProgressInfo progressInfo) {

        // A copy of the graph that is destroyed
        JIPipeGraphRunDataFlowGraph dataFlowGraph = new JIPipeGraphRunDataFlowGraph(graph, nodeFilter);
        progressInfo.log("Built data flow graph " + dataFlowGraph);

        int progress = 0;
        final int maxProgress = dataFlowGraph.vertexSet().size();
        progressInfo.setProgress(progress, maxProgress);

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
                progressInfo.setProgress(progress, maxProgress);
                progressInfo.log("");

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
                        progressInfo.log("Backing up inputs of " + algorithm.getDisplayName() + " [" + uuid + "]");
                        Map<String, JIPipeDataTable> slotMap = new HashMap<>();
                        for (JIPipeInputDataSlot inputSlot : algorithm.getInputSlots()) {
                            JIPipeDataTable backupTable = new JIPipeDataTable(inputSlot, true, progressInfo);
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
                progressInfo.log("\n\n------------------------\n" +
                        "Partition execution FAILED!\n" +
                        "Message: " + e.getMessage() + "\n" +
                        "\n" +
                        "CONTINUING AS REQUESTED!\n" +
                        "------------------------\n\n");

                // Check if we are in update cache
                if (runtimePartition.getContinueOnFailureSettings().isDisableOnUpdateCache() && runContext.getGraphRun().getConfiguration().isStoreToCache()) {
                    progressInfo.log("CONTINUE ON FAILURE IS TURNED OFF DUE TO A USER SETTING IN THE PARTITION");
                    throw e;
                }

                // Dump errored data
                exportFailedInputs(graph, nodeFilter, continueOnErrorBackup, progressInfo);

                // Cleanup
                while (!dataFlowGraph.vertexSet().isEmpty()) {
                    // Find the best candidate (shortest path)
                    Object nextVertex = dataFlowGraph.getBestNextVertex();

                    if (nextVertex == null) {
                        throw new NullPointerException("No candidate found! Are there cycles in the graph?");
                    }

                    // Execute operation & cleanup
                    runFlowGraphNode(nextVertex, dataFlowGraph, gcGraph, true, runtimePartition, progressInfo.resolve("Cleanup"));
                }
            } else {
                throw e;
            }
        } finally {

            // Cleanup backup tables
            for (Map.Entry<UUID, Map<String, JIPipeDataTable>> nodeEntry : continueOnErrorBackup.entrySet()) {
                for (JIPipeDataTable dataTable : nodeEntry.getValue().values()) {
                    dataTable.clear(false, progressInfo);
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
        JIPipeProgressInfo algorithmProgress = progressInfo.resolve(algorithm.getDisplayName());
        progressInfo.log("+N " + algorithm.getDisplayName());
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
                    throw new JIPipeValidationRuntimeException(
                            new GraphNodeValidationReportContext(algorithm),
                            e,
                            "An error occurred during processing",
                            "On running the algorithm '" + algorithm.getDisplayName(),
                            "Please follow the instructions for the other error messages.");
                }
            }
        }

        if (gcGraph != null) {
            gcGraph.removeInputToNodeEdge(algorithm, progressInfo.resolve("GC"));
        }

        // Remove vertex
        dataFlowGraph.removeVertex(flowGraphNode);
    }

    private void runOutputFlowGraphNode(Object flowGraphNode, JIPipeGraphRunDataFlowGraph dataFlowGraph, JIPipeGraphRunGCGraph gcGraph, JIPipeProgressInfo progressInfo) {
        JIPipeOutputDataSlot outputSlot = (JIPipeOutputDataSlot) flowGraphNode;
        progressInfo.log("+O " + outputSlot.getDisplayName());
        progressInfo.resolve(outputSlot.getNode().getDisplayName()).log("Contents of data slot " + outputSlot);

        JIPipeOutputDataSlot outputDataSlot = (JIPipeOutputDataSlot) flowGraphNode;

        if (gcGraph != null) {
            gcGraph.removeNodeToOutputEdge(outputDataSlot, progressInfo.resolve("GC"));
        }

        // Remove vertex
        dataFlowGraph.removeVertex(flowGraphNode);
    }

    private void runInputFlowGraphNode(Object flowGraphNode, JIPipeGraphRunDataFlowGraph dataFlowGraph, JIPipeGraphRunGCGraph gcGraph, boolean skipWorkload, JIPipeProgressInfo progressInfo) {
        progressInfo.log("+I " + ((JIPipeInputDataSlot) flowGraphNode).getDisplayName());

        // Add data from incoming edges within graph
        JIPipeInputDataSlot inputSlot = (JIPipeInputDataSlot) flowGraphNode;
        Set<JIPipeGraphEdge> incomingEdges = graph.getGraph().incomingEdgesOf(inputSlot);

        for (JIPipeGraphEdge graphEdge : incomingEdges) {

            JIPipeDataSlot outputSlot = graph.getGraph().getEdgeSource(graphEdge);

            if (!skipWorkload) {
                if (!inputSlot.isSkipDataGathering()) {
                    inputSlot.addDataFromTable(outputSlot, progressInfo.resolve(outputSlot.getDisplayName() + " >>> " + inputSlot.getDisplayName()));
                } else {
                    progressInfo.log("NOT copying " + outputSlot.getDisplayName() + " >>> " + inputSlot.getDisplayName() + " [data gathering disabled]");
                }
            }

            if (gcGraph != null) {
                gcGraph.removeOutputToInputEdge(outputSlot, inputSlot, progressInfo.resolve("GC"));
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
        Map<String, JIPipeDataTable> cachedData = project.getCache().query(runAlgorithm, runAlgorithm.getUUIDInParentGraph(), progressInfo.resolve("Query cache"));
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
        if (project != null && configuration.isStoreToCache()) {
            if (!configuration.getDisableStoreToCacheNodes().contains(outputDataSlot.getNode().getUUIDInParentGraph()) && !outputDataSlot.isSkipCache()) {
                storageProgress.log("Storing " + outputDataSlot.getDisplayName() + " into cache");
                project.getCache().store(outputDataSlot.getNode(), outputDataSlot.getNode().getUUIDInParentGraph(), outputDataSlot, outputDataSlot.getName(), storageProgress);
            } else {
                storageProgress.log("NOT storing " + outputDataSlot.getDisplayName() + " [deactivated in config]");
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
                storageProgress.log("NOT Storing " + outputDataSlot.getDisplayName() + " to hard drive (is empty)");
                return;
            }

            JIPipeProgressInfo saveProgress = storageProgress.resolveAndLog(String.format("Saving data in slot '%s' (data type %s)", outputDataSlot.getDisplayName(), JIPipeDataInfo.getInstance(outputDataSlot.getAcceptedDataType()).getName()));
            storageProgress.log("Storing " + outputDataSlot.getDisplayName() + " to hard drive");

            try {
                filtered.exportData(new JIPipeFileSystemWriteDataStorage(saveProgress, outputDataSlot.getSlotStoragePath()), saveProgress);
            } finally {
                filtered.clear(false, getProgressInfo());
                outputDataSlot.clear(false, getProgressInfo());
            }
        }
    }
}
