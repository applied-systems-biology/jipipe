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

import com.google.common.base.Charsets;
import com.google.common.collect.BiMap;
import com.google.common.eventbus.Subscribe;
import ij.IJ;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.grouping.GraphWrapperAlgorithm;
import org.hkijena.jipipe.api.grouping.NodeGroup;
import org.hkijena.jipipe.api.looping.LoopGroup;
import org.hkijena.jipipe.api.looping.LoopStartNode;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.*;

/**
 * Runnable instance of an {@link JIPipeProject}
 */
public class JIPipeProjectRun implements JIPipeRunnable {
    private final JIPipeProject project;
    private final JIPipeRunSettings configuration;
    JIPipeGraph copiedGraph;
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
    private JIPipeFixedThreadPool threadPool;

    /**
     * @param project       The project
     * @param configuration Run configuration
     */
    public JIPipeProjectRun(JIPipeProject project, JIPipeRunSettings configuration) {
        // First clean up the graph
        project.rebuildAliasIds(false);
        this.project = project;
        this.configuration = configuration;
        this.copiedGraph = new JIPipeGraph(project.getGraph());
        this.copiedGraph.setAttachments(project.getGraph().getAttachments());
        this.copiedGraph.attach(this);
        initializeScratchDirectory();
        initializeRelativeDirectories();
        initializeInternalStoragePaths();
    }

    /**
     * Loads a JIPipeProjectRun from a folder
     *
     * @param folder        Folder containing the run
     * @param notifications notifications for the user
     * @return The loaded run
     * @throws IOException Triggered by {@link com.fasterxml.jackson.databind.ObjectMapper}
     */
    public static JIPipeProjectRun loadFromFolder(Path folder, JIPipeIssueReport report, JIPipeNotificationInbox notifications) throws IOException {
        Path parameterFile = folder.resolve("project.jip");
        JIPipeProject project = JIPipeProject.loadProject(parameterFile, report, notifications);
        JIPipeRunSettings configuration = new JIPipeRunSettings();
        configuration.setOutputPath(folder);
        JIPipeProjectRun run = new JIPipeProjectRun(project, configuration);
        run.prepare();
        return run;
    }

    private void initializeScratchDirectory() {
        try {
            Files.createDirectories(configuration.getOutputPath().resolve("_scratch"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (JIPipeGraphNode algorithm : copiedGraph.getGraphNodes()) {
            algorithm.setScratchBaseDirectory(configuration.getOutputPath().resolve("_scratch"));
        }
    }

    private void initializeRelativeDirectories() {
        for (JIPipeGraphNode algorithm : copiedGraph.getGraphNodes()) {
            algorithm.setBaseDirectory(null);
        }
    }

    private void initializeInternalStoragePaths() {
        for (JIPipeGraphNode algorithm : copiedGraph.getGraphNodes()) {
            JIPipeProjectCompartment compartment = project.getCompartments().get(algorithm.getCompartmentUUIDInParentGraph());
            algorithm.setInternalStoragePath(Paths.get(StringUtils.safeJsonify(compartment.getAliasIdInParentGraph()))
                    .resolve(StringUtils.safeJsonify(copiedGraph.getAliasIdOf(algorithm))));
            algorithm.setProjectDirectory(project.getWorkDirectory());
        }
    }

    /**
     * @return The project that created this run
     */
    public JIPipeProject getProject() {
        return project;
    }

    /**
     * This function must be called before running the graph
     */
    private void prepare() {
        assignDataStoragePaths();
    }

    /**
     * Iterates through output slots and assigns the data storage paths
     */
    public void assignDataStoragePaths() {
        if (configuration.getOutputPath() != null) {
            if (!Files.exists(configuration.getOutputPath())) {
                try {
                    Files.createDirectories(configuration.getOutputPath());
                } catch (IOException e) {
                    throw new UserFriendlyRuntimeException(e, "Could not create necessary directory '" + configuration.getOutputPath() + "'!",
                            "Pipeline run", "Either the path is invalid, or you do not have permissions to create the directory",
                            "Check if the path is valid and the parent directories are writeable by your current user");
                }
            }

            // Apply output path to the data slots
            for (JIPipeDataSlot slot : copiedGraph.getSlotNodes()) {
                if (slot.isOutput()) {
                    slot.setSlotStoragePath(configuration.getOutputPath().resolve(slot.getNode().getInternalStoragePath().resolve(slot.getName())));
                    try {
                        Files.createDirectories(slot.getSlotStoragePath());
                    } catch (IOException e) {
                        throw new UserFriendlyRuntimeException(e, "Could not create necessary directory '" + slot.getSlotStoragePath() + "'!",
                                "Pipeline run", "Either the path is invalid, or you do not have permissions to create the directory",
                                "Check if the path is valid and the parent directories are writeable by your current user");
                    }
                }
            }
        }
    }

    @Override
    public void run() {
        progressInfo.clearLog();
        long startTime = System.currentTimeMillis();
        progressInfo.log("JIPipe run starting at " + StringUtils.formatDateTime(LocalDateTime.now()));
        progressInfo.log("Preparing output folders ...");
        prepare();
        progressInfo.log("Outputs will be written to " + configuration.getOutputPath());
        try {
            progressInfo.log("Running pipeline with " + configuration.getNumThreads() + " threads ...\n");
            threadPool = new JIPipeFixedThreadPool(configuration.getNumThreads());
            runPipeline();
        } catch (Exception e) {
            progressInfo.log(e.toString());
            progressInfo.log(ExceptionUtils.getStackTrace(e));
            try {
                if (configuration.getOutputPath() != null)
                    Files.write(configuration.getOutputPath().resolve("log.txt"), progressInfo.getLog().toString().getBytes(Charsets.UTF_8));
            } catch (IOException ex) {
                if (!configuration.isSilent())
                    IJ.handleException(ex);
            }
            throw e;
        } finally {
            if (threadPool != null) {
                threadPool.shutdown();
            }
            threadPool = null;
        }

        // Clear all slots
        for (JIPipeGraphNode node : copiedGraph.getGraphNodes()) {
            node.clearSlotData();
        }

        // Postprocessing
        progressInfo.log("Postprocessing steps ...");
        try {
            if (configuration.getOutputPath() != null && configuration.isSaveToDisk())
                project.saveProject(configuration.getOutputPath().resolve("project.jip"));
        } catch (IOException e) {
            throw new UserFriendlyRuntimeException(e, "Could not save project to '" + configuration.getOutputPath().resolve("project.jip") + "'!",
                    "Pipeline run", "Either the path is invalid, or you have no permission to write to the disk, or the disk space is full",
                    "Check if you can write to the output directory.");
        }

        progressInfo.log("Run ending at " + StringUtils.formatDateTime(LocalDateTime.now()));
        progressInfo.log("\nAnalysis required " + StringUtils.formatDuration(System.currentTimeMillis() - startTime) + " to execute.\n");

        try {
            if (configuration.getOutputPath() != null)
                Files.write(configuration.getOutputPath().resolve("log.txt"), progressInfo.getLog().toString().getBytes(Charsets.UTF_8));
        } catch (IOException e) {
            throw new UserFriendlyRuntimeException(e, "Could not write log '" + configuration.getOutputPath().resolve("log.txt") + "'!",
                    "Pipeline run", "Either the path is invalid, or you have no permission to write to the disk, or the disk space is full",
                    "Check if you can write to the output directory.");
        }
    }

    private void runPipeline() {
        Set<JIPipeGraphNode> unExecutableAlgorithms = copiedGraph.getDeactivatedAlgorithms(!configuration.isIgnoreDeactivatedInputs());
        Set<JIPipeGraphNode> executedAlgorithms = new HashSet<>();
        List<JIPipeDataSlot> traversedSlots = copiedGraph.traverseSlots();

        // Create GC
        JIPipeGraphGCHelper gc = new JIPipeGraphGCHelper(copiedGraph);
        progressInfo.resolve("GC").log("GC status: " + gc);
        gc.getEventBus().register(this);

        List<JIPipeGraphNode> preprocessorNodes = new ArrayList<>();
        List<JIPipeGraphNode> postprocessorNodes = new ArrayList<>();
        for (JIPipeGraphNode node : copiedGraph.getGraphNodes()) {
            if (!unExecutableAlgorithms.contains(node) && node.getInputSlots().isEmpty() &&
                    node instanceof JIPipeAlgorithm && ((JIPipeAlgorithm) node).isPreprocessor()) {
                preprocessorNodes.add(node);
            } else if (!unExecutableAlgorithms.contains(node) &&
                    node instanceof JIPipeAlgorithm && ((JIPipeAlgorithm) node).isPreprocessor()) {
                if (node.getOpenInputSlots().stream().allMatch(nd -> copiedGraph.getOutputOutgoingTargetSlots(nd).isEmpty())) {
                    postprocessorNodes.add(node);
                }
            }
        }
        if (!preprocessorNodes.isEmpty()) {
            progressInfo.setProgress(0, preprocessorNodes.size() + traversedSlots.size());
            progressInfo.resolveAndLog("Preprocessing algorithms");
            for (int i = 0; i < preprocessorNodes.size(); i++) {
                JIPipeGraphNode node = preprocessorNodes.get(i);
                progressInfo.setProgress(i);
                JIPipeProgressInfo subProgress = progressInfo.resolve(node.getName());
                if (!executedAlgorithms.contains(node)) {
                    runNode(executedAlgorithms, node, subProgress);
                }
            }
        }

        // Collect loop groups
        List<LoopGroup> loopGroups = copiedGraph.extractLoopGroups(Collections.emptySet(), unExecutableAlgorithms);
        Map<JIPipeGraphNode, LoopGroup> nodeLoops = new HashMap<>();
        for (LoopGroup loopGroup : loopGroups) {
            for (JIPipeGraphNode node : loopGroup.getNodes()) {
                nodeLoops.put(node, loopGroup);
            }
        }
        Set<LoopGroup> executedLoops = new HashSet<>();

        // Run nodes
        progressInfo.setMaxProgress(traversedSlots.size());
        for (int index = 0; index < traversedSlots.size(); ++index) {
            if (progressInfo.isCancelled())
                throw new UserFriendlyRuntimeException("Execution was cancelled",
                        "You cancelled the execution of the algorithm pipeline.",
                        "Pipeline run", "You clicked 'Cancel'.",
                        "Do not click 'Cancel' if you do not want to cancel the execution.");
            JIPipeDataSlot slot = traversedSlots.get(index);
            progressInfo.setProgress(index + preprocessorNodes.size(), traversedSlots.size());
            if (!unExecutableAlgorithms.contains(slot.getNode())) {
                progressInfo.log(slot.getDisplayName());
            }

//            // If an algorithm cannot be executed, skip it automatically
//            if (unExecutableAlgorithms.contains(slot.getNode())) {
//                // Mark for gc
//                gc.deactivateNode(slot.getNode());
//                continue;
//            }

            // Let algorithms provide sub-progress
            JIPipeProgressInfo subProgress = progressInfo.resolve(slot.getNode().getName());

            if (slot.isInput()) {
                // Copy data from source
                Set<JIPipeDataSlot> sourceSlots = copiedGraph.getInputIncomingSourceSlots(slot);
                for (JIPipeDataSlot sourceSlot : sourceSlots) {
                    if (slot.getNode() instanceof JIPipeAlgorithm) {
                        // Add data from source slot
                        slot.addDataFromSlot(sourceSlot, subProgress);
                        gc.markCopyOutputToInput(sourceSlot, slot);
                    }
                }
            } else if (slot.isOutput()) {
                JIPipeGraphNode node = slot.getNode();

                // Check if this is a postprocessor
                if (!executedAlgorithms.contains(node) && postprocessorNodes.contains(node)) {
                    subProgress.resolveAndLog("Node is postprocessor. Deferring the run.");
                    continue;
                }

                LoopGroup loop = nodeLoops.getOrDefault(node, null);
                if (loop == null) {
                    // Ensure the algorithm has run
                    if (!executedAlgorithms.contains(node)) {
                        if (!unExecutableAlgorithms.contains(node)) {
                            runNode(executedAlgorithms, node, subProgress);
                        } else {
                            executedAlgorithms.add(node);
                        }
                    }

                    // Mark the node as executed
                    gc.markNodeExecuted(node);
                } else {
                    // Encountered a loop
                    if (!executedLoops.contains(loop)) {
                        if (!unExecutableAlgorithms.contains(slot.getNode())) {

                            // Only start the loop if we are at the loop start node
                            if (slot.getNode() != loop.getLoopStartNode()) {
                                continue;
                            }

                            int loopNumber = loopGroups.indexOf(loop) + 1;
                            subProgress = progressInfo.resolveAndLog("Loop id=" + loopNumber);
                            JIPipeGraph loopGraph = copiedGraph.extract(loop.getNodes(), true);
                            NodeGroup group = new NodeGroup(loopGraph, false, false, true);
                            group.setInternalStoragePath(Paths.get("loop" + loopNumber));
                            BiMap<JIPipeDataSlot, JIPipeDataSlot> loopGraphSlotMap = group.autoCreateSlots();
                            group.setIterationMode(loop.getLoopStartNode().getIterationMode());
                            if (loop.getLoopStartNode().isPassThrough()) {
                                group.setIterationMode(GraphWrapperAlgorithm.IterationMode.PassThrough);
                            }
                            group.setThreadPool(threadPool);

                            // IMPORTANT! Otherwise the nested JIPipeGraphRunner will run into an infinite depth loop
                            ((LoopStartNode) loopGraph.getEquivalentAlgorithm(loop.getLoopStartNode()))
                                    .setIterationMode(GraphWrapperAlgorithm.IterationMode.PassThrough);

                            // Pass input data from inputs of loop into equivalent input of group
                            for (JIPipeDataSlot inputSlot : loop.getLoopStartNode().getInputSlots()) {
                                JIPipeDataSlot groupInput = loopGraphSlotMap.get(loopGraph.getEquivalentSlot(inputSlot));
                                groupInput.addDataFromSlot(inputSlot, subProgress);
                            }

                            // Execute the loop
                            group.run(subProgress.detachProgress());

                            // Pass output data
                            for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> entry : loopGraphSlotMap.entrySet()) {
                                // Info: We need the value; the key already has cleared data!
                                if (entry.getKey().isOutput()) {
                                    JIPipeDataSlot originalSlot = copiedGraph.getEquivalentSlot(entry.getKey());
                                    JIPipeDataSlot sourceSlot = entry.getValue();
                                    originalSlot.addDataFromSlot(sourceSlot, subProgress);
                                }
                            }
                        }

                        executedLoops.add(loop);
                    }

                    // IMPORTANT!
                    executedAlgorithms.add(slot.getNode());

                    // Mark the whole loop in GC
                    for (JIPipeGraphNode loopNode : loop.getNodes()) {
                        for (JIPipeDataSlot inputSlot : loopNode.getInputSlots()) {
                            gc.markAsCompleted(inputSlot);
                        }
                        if (!loop.getLoopEndNodes().contains(loopNode)) {
                            for (JIPipeDataSlot outputSlot : loopNode.getOutputSlots()) {
                                gc.markAsCompleted(outputSlot);
                            }
                        }
                    }
                }
            }
        }

        // There might be some algorithms missing (ones that do not have an output)
        // Will also run any postprocessor
        List<JIPipeGraphNode> additionalAlgorithms = new ArrayList<>();
        for (JIPipeGraphNode node : copiedGraph.getGraphNodes()) {
            if (progressInfo.isCancelled())
                break;
            if (!executedAlgorithms.contains(node) && !unExecutableAlgorithms.contains(node)) {
                additionalAlgorithms.add(node);
            }
        }
        progressInfo.setMaxProgress(progressInfo.getProgress() + additionalAlgorithms.size());
        for (int index = 0; index < additionalAlgorithms.size(); index++) {
            if (progressInfo.isCancelled())
                break;
            JIPipeGraphNode node = additionalAlgorithms.get(index);
            int absoluteIndex = index + preprocessorNodes.size() + traversedSlots.size() - 1;
            progressInfo.setProgress(absoluteIndex);
            JIPipeProgressInfo subProgress = progressInfo.resolve(node.getName());
            runNode(executedAlgorithms, node, subProgress);
        }

        progressInfo.resolve("GC").log("GC status: " + gc);
        for (JIPipeDataSlot incompleteSlot : gc.getIncompleteSlots()) {
            progressInfo.resolve("GC").log("Found incomplete GC slot: " + incompleteSlot.getDisplayName() + "(" + incompleteSlot.getSlotType().name() + ")");
        }
        // GC: Mark all as completed
        gc.markAllAsCompleted();
    }

    @Subscribe
    public void onSlotCompleted(JIPipeGraphGCHelper.SlotCompletedEvent event) {
        JIPipeDataSlot slot = event.getSlot();
        if (slot.isEmpty()) {
            return;
        }
        if (!(slot.getNode() instanceof JIPipeAlgorithm)) {
            return;
        }
        if (slot.isInput()) {
            progressInfo.resolve("GC").log("Clearing input slot " + slot.getDisplayName());
            slot.destroyData();
        } else if (slot.isOutput()) {
            if (configuration.isStoreToCache() && !configuration.getDisableStoreToCacheNodes().contains(slot.getNode())) {
                JIPipeGraphNode runAlgorithm = slot.getNode();
                progressInfo.resolve("GC").log("Caching output slot " + slot.getDisplayName());
                project.getCache().store(runAlgorithm, runAlgorithm.getUUIDInParentGraph(), slot, slot.getName(), progressInfo.resolve("GC"));
            }
            if (configuration.isSaveToDisk() && !configuration.getDisableSaveToDiskNodes().contains(slot.getNode())) {
                JIPipeProgressInfo saveProgress = progressInfo.resolveAndLog(String.format("Saving data in slot '%s' (data type %s)", slot.getDisplayName(), JIPipeDataInfo.getInstance(slot.getAcceptedDataType()).getName()));
                progressInfo.resolve("GC").log("Flushing output slot " + slot.getDisplayName());
                slot.flush(saveProgress);
            } else {
                progressInfo.resolve("GC").log("Clearing output slot " + slot.getDisplayName());
                slot.clearData();
            }
        }
    }

    private void runNode(Set<JIPipeGraphNode> executedAlgorithms, JIPipeGraphNode node, JIPipeProgressInfo progressInfo) {
        progressInfo.log("");

        // If enabled try to extract outputs from cache
        boolean dataLoadedFromCache = false;
        if (configuration.isLoadFromCache()) {
            dataLoadedFromCache = tryLoadFromCache(node, progressInfo);
        }

        if (!dataLoadedFromCache) {
            JIPipeProjectCompartment nodeCompartment = getProject().getCompartments().get(node.getCompartmentUUIDInParentGraph());
            String nodeCompartmentName = nodeCompartment != null ? nodeCompartment.getName() : "<Subgraph>";
            try {
                if (node instanceof JIPipeAlgorithm) {
                    ((JIPipeAlgorithm) node).setThreadPool(threadPool);
                }
                node.run(progressInfo);
            } catch (HeadlessException e) {
                throw new UserFriendlyRuntimeException("Algorithm " + node + " does not work in a headless environment!",
                        e,
                        "An error occurred during processing",
                        "On running the algorithm '" + node.getName() + "', within compartment '" + nodeCompartmentName + "'",
                        "The algorithm raised an error, as it is not compatible with a headless environment.",
                        "Please contact the plugin developers about this issue. If this happens in an ImageJ method, please contact the ImageJ developers.");
            } catch (Exception e) {
                throw new UserFriendlyRuntimeException("Algorithm " + node + " raised an exception!",
                        e,
                        "An error occurred during processing",
                        "On running the algorithm '" + node.getName() + "', within compartment '" + nodeCompartmentName + "'",
                        "Please refer to the other error messages.",
                        "Please follow the instructions for the other error messages.");
            } finally {
                if (node instanceof JIPipeAlgorithm) {
                    ((JIPipeAlgorithm) node).setThreadPool(null);
                }
            }
        } else {
            progressInfo.log("Output data was loaded from cache. Not executing.");
        }
        executedAlgorithms.add(node);
    }

    /**
     * Attempts to load data from cache
     *
     * @param runAlgorithm  the target node (inside copy graph)
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
                outputSlot.clearData();
                outputSlot.addDataFromTable(cachedData.get(outputSlot.getName()), progressInfo);
            }
            progressInfo.log("Cache data access successful.");
            return true;
        }
        return false;
    }

    /**
     * @return The graph used within this run. A copy of the project graph.
     */
    public JIPipeGraph getGraph() {
        return copiedGraph;
    }

    /**
     * @return The run configuration
     */
    public JIPipeRunSettings getConfiguration() {
        return configuration;
    }

    @Override
    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }

    @Override
    public void setProgressInfo(JIPipeProgressInfo progressInfo) {
        this.progressInfo = progressInfo;
    }

    @Override
    public String getTaskLabel() {
        return "Run";
    }
}
