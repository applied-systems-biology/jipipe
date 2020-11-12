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
import ij.IJ;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.HeadlessException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Runnable instance of an {@link JIPipeProject}
 */
public class JIPipeRun implements JIPipeRunnable {
    private final JIPipeProject project;
    private final JIPipeProjectCacheQuery cacheQuery;
    private final JIPipeRunSettings configuration;
    private final StringBuilder log = new StringBuilder();
    JIPipeGraph algorithmGraph;
    private JIPipeFixedThreadPool threadPool;

    /**
     * @param project       The project
     * @param configuration Run configuration
     */
    public JIPipeRun(JIPipeProject project, JIPipeRunSettings configuration) {
        this.project = project;
        this.cacheQuery = new JIPipeProjectCacheQuery(project);
        this.configuration = configuration;
        this.algorithmGraph = new JIPipeGraph(project.getGraph());
        initializeRelativeDirectories();
        initializeInternalStoragePaths();
    }

    private void initializeRelativeDirectories() {
        for (JIPipeGraphNode algorithm : algorithmGraph.getNodes().values()) {
            algorithm.setWorkDirectory(null);
        }
    }

    private void initializeInternalStoragePaths() {
        for (JIPipeGraphNode algorithm : algorithmGraph.getNodes().values()) {
            algorithm.setInternalStoragePath(Paths.get(StringUtils.jsonify(algorithm.getCompartment())).resolve(StringUtils.jsonify(algorithmGraph.getIdOf(algorithm))));
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
            for (JIPipeDataSlot slot : algorithmGraph.getSlotNodes()) {
                if (slot.isOutput()) {
                    slot.setStoragePath(configuration.getOutputPath().resolve(slot.getNode().getInternalStoragePath().resolve(slot.getName())));
                    try {
                        Files.createDirectories(slot.getStoragePath());
                    } catch (IOException e) {
                        throw new UserFriendlyRuntimeException(e, "Could not create necessary directory '" + slot.getStoragePath() + "'!",
                                "Pipeline run", "Either the path is invalid, or you do not have permissions to create the directory",
                                "Check if the path is valid and the parent directories are writeable by your current user");
                    }
                }
            }
        }
    }

    private void flushFinishedSlots(List<JIPipeDataSlot> traversedSlots, List<JIPipeGraphNode> traversedProjectAlgorithms, Set<JIPipeGraphNode> executedAlgorithms, int currentIndex, JIPipeDataSlot outputSlot, Set<JIPipeDataSlot> flushedSlots) {
        if (!executedAlgorithms.contains(outputSlot.getNode()))
            return;
        if (flushedSlots.contains(outputSlot))
            return;
        boolean canFlush = true;
        for (int j = currentIndex + 1; j < traversedSlots.size(); ++j) {
            JIPipeDataSlot futureSlot = traversedSlots.get(j);
            boolean isDeactivated = (futureSlot.getNode() instanceof JIPipeAlgorithm) && (!((JIPipeAlgorithm) futureSlot.getNode()).isEnabled());
            if (!isDeactivated && futureSlot.isInput() && algorithmGraph.getSourceSlot(futureSlot) == outputSlot) {
                canFlush = false;
                break;
            }
        }
        if (canFlush) {
            if (configuration.isStoreToCache()) {
                JIPipeGraphNode runAlgorithm = outputSlot.getNode();
                JIPipeGraphNode projectAlgorithm = cacheQuery.getNode(runAlgorithm.getIdInGraph());
                JIPipeProjectCache.State stateId = cacheQuery.getCachedId(projectAlgorithm);
                project.getCache().store((JIPipeAlgorithm) projectAlgorithm, stateId, outputSlot);
            }
            if (configuration.isSaveOutputs()) {
                outputSlot.flush(configuration.getOutputPath(), !configuration.isStoreToCache());
            } else {
                outputSlot.clearData(false);
            }
            flushedSlots.add(outputSlot);
        }
    }

    @Override
    public void run(Consumer<JIPipeRunnerStatus> onProgress, Supplier<Boolean> isCancelled) {
        log.setLength(0);
        long startTime = System.currentTimeMillis();
        log.append("JIPipe run starting at ").append(StringUtils.formatDateTime(LocalDateTime.now())).append("\n");
        log.append("Preparing output folders ...\n");
        prepare();
        try {
            log.append("Running main analysis with ").append(configuration.getNumThreads()).append(" threads ...\n");
            threadPool = new JIPipeFixedThreadPool(configuration.getNumThreads());
            runAnalysis(onProgress, isCancelled);
        } catch (Exception e) {
            log.append(e.toString()).append("\n");
            log.append(ExceptionUtils.getStackTrace(e)).append("\n");
            try {
                if (configuration.getOutputPath() != null)
                    Files.write(configuration.getOutputPath().resolve("log.txt"), log.toString().getBytes(Charsets.UTF_8));
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

        // Postprocessing
        log.append("Postprocessing steps ...\n");
        try {
            if (configuration.getOutputPath() != null && configuration.isSaveOutputs())
                project.saveProject(configuration.getOutputPath().resolve("project.jip"));
        } catch (IOException e) {
            throw new UserFriendlyRuntimeException(e, "Could not save project to '" + configuration.getOutputPath().resolve("project.jip") + "'!",
                    "Pipeline run", "Either the path is invalid, or you have no permission to write to the disk, or the disk space is full",
                    "Check if you can write to the output directory.");
        }

        log.append("Run ending at ").append(StringUtils.formatDateTime(LocalDateTime.now())).append("\n");
        log.append("\nAnalysis required ").append(StringUtils.formatDuration(System.currentTimeMillis() - startTime))
                .append(" to execute.\n");

        try {
            if (configuration.getOutputPath() != null)
                Files.write(configuration.getOutputPath().resolve("log.txt"), log.toString().getBytes(Charsets.UTF_8));
        } catch (IOException e) {
            throw new UserFriendlyRuntimeException(e, "Could not write log '" + configuration.getOutputPath().resolve("log.txt") + "'!",
                    "Pipeline run", "Either the path is invalid, or you have no permission to write to the disk, or the disk space is full",
                    "Check if you can write to the output directory.");
        }
    }

    private void runAnalysis(Consumer<JIPipeRunnerStatus> onProgress, Supplier<Boolean> isCancelled) {
        Set<JIPipeGraphNode> unExecutableAlgorithms = algorithmGraph.getDeactivatedAlgorithms();
        Set<JIPipeGraphNode> executedAlgorithms = new HashSet<>();
        Set<JIPipeDataSlot> flushedSlots = new HashSet<>();
        List<JIPipeDataSlot> traversedSlots = algorithmGraph.traverseSlots();
        List<JIPipeGraphNode> traversedProjectAlgorithms = getProject().getGraph().traverseAlgorithms();

        for (int index = 0; index < traversedSlots.size(); ++index) {
            if (isCancelled.get())
                throw new UserFriendlyRuntimeException("Execution was cancelled",
                        "You cancelled the execution of the algorithm pipeline.",
                        "Pipeline run", "You clicked 'Cancel'.",
                        "Do not click 'Cancel' if you do not want to cancel the execution.");
            JIPipeDataSlot slot = traversedSlots.get(index);
            logStatus(onProgress, new JIPipeRunnerStatus(index, algorithmGraph.getSlotCount(), slot.getDisplayName()));

            // If an algorithm cannot be executed, skip it automatically
            if (unExecutableAlgorithms.contains(slot.getNode()))
                continue;

            // Let algorithms provide sub-progress
            String statusMessage = "Algorithm: " + slot.getNode().getName();
            int traversingIndex = index;
            Consumer<JIPipeRunnerSubStatus> algorithmProgress = s -> logStatus(onProgress, new JIPipeRunnerStatus(traversingIndex, traversedSlots.size(),
                    statusMessage + " | " + s));

            if (slot.isInput()) {
                // Copy data from source
                JIPipeDataSlot sourceSlot = algorithmGraph.getSourceSlot(slot);
                slot.copyFrom(sourceSlot);

                // Check if we can flush the output
                flushFinishedSlots(traversedSlots, traversedProjectAlgorithms, executedAlgorithms, index, sourceSlot, flushedSlots);
            } else if (slot.isOutput()) {
                JIPipeGraphNode node = slot.getNode();
                // Ensure the algorithm has run
                if (!executedAlgorithms.contains(node)) {
                    runNode(onProgress, isCancelled, executedAlgorithms, traversedSlots, traversedProjectAlgorithms, index, statusMessage, traversingIndex, algorithmProgress, node);
                }

                // Check if we can flush the output
                flushFinishedSlots(traversedSlots, traversedProjectAlgorithms, executedAlgorithms, index, slot, flushedSlots);
            }
        }

        // There might be some algorithms missing (ones that do not have an output)
        List<JIPipeGraphNode> additionalAlgorithms = new ArrayList<>();
        for (JIPipeGraphNode node : algorithmGraph.getNodes().values()) {
            if (!executedAlgorithms.contains(node) && !unExecutableAlgorithms.contains(node)) {
                additionalAlgorithms.add(node);
            }
        }
        for (int index = 0; index < additionalAlgorithms.size(); index++) {
            JIPipeGraphNode node = additionalAlgorithms.get(index);
            String statusMessage = "Algorithm: " + node.getName();
            int absoluteIndex = index + traversedSlots.size() - 1;
            Consumer<JIPipeRunnerSubStatus> algorithmProgress = s -> logStatus(onProgress, new JIPipeRunnerStatus(absoluteIndex, traversedSlots.size() + additionalAlgorithms.size(),
                    statusMessage + " | " + s));
            runNode(onProgress,
                    isCancelled,
                    executedAlgorithms,
                    traversedSlots,
                    traversedProjectAlgorithms,
                    absoluteIndex,
                    statusMessage,
                    absoluteIndex,
                    algorithmProgress,
                    node);
        }
    }

    private void runNode(Consumer<JIPipeRunnerStatus> onProgress, Supplier<Boolean> isCancelled, Set<JIPipeGraphNode> executedAlgorithms, List<JIPipeDataSlot> traversedSlots, List<JIPipeGraphNode> traversedProjectAlgorithms, int index, String statusMessage, int traversingIndex, Consumer<JIPipeRunnerSubStatus> algorithmProgress, JIPipeGraphNode node) {
        onProgress.accept(new JIPipeRunnerStatus(index, traversedSlots.size(), statusMessage));

        // If enabled try to extract outputs from cache
        boolean dataLoadedFromCache = false;
        if (configuration.isLoadFromCache()) {
            dataLoadedFromCache = tryLoadFromCache(node,
                    onProgress,
                    traversingIndex,
                    traversedSlots.size(),
                    statusMessage,
                    traversedProjectAlgorithms);
        }

        if (!dataLoadedFromCache) {
            try {
                if (node instanceof JIPipeAlgorithm) {
                    ((JIPipeAlgorithm) node).setThreadPool(threadPool);
                }
                node.run(new JIPipeRunnerSubStatus(), algorithmProgress, isCancelled);
            } catch (HeadlessException e) {
                throw new UserFriendlyRuntimeException("Algorithm " + node + " does not work in a headless environment!",
                        e,
                        "An error occurred during processing",
                        "On running the algorithm '" + node.getName() + "', within compartment '" + getProject().getCompartments().get(node.getCompartment()).getName() + "'",
                        "The algorithm raised an error, as it is not compatible with a headless environment.",
                        "Please contact the plugin developers about this issue. If this happens in an ImageJ method, please contact the ImageJ developers.");
            } catch (Exception e) {
                throw new UserFriendlyRuntimeException("Algorithm " + node + " raised an exception!",
                        e,
                        "An error occurred during processing",
                        "On running the algorithm '" + node.getName() + "', within compartment '" + getProject().getCompartments().get(node.getCompartment()).getName() + "'",
                        "Please refer to the other error messages.",
                        "Please follow the instructions for the other error messages.");
            } finally {
                if (node instanceof JIPipeAlgorithm) {
                    ((JIPipeAlgorithm) node).setThreadPool(null);
                }
            }
        } else {
            onProgress.accept(new JIPipeRunnerStatus(index, traversedSlots.size(), statusMessage + " | Output data was loaded from cache. Not executing."));
        }

        executedAlgorithms.add(node);
    }

    /**
     * Attempts to load data from cache
     *
     * @param algorithm           the target algorithm
     * @param onProgress          progress
     * @param progress            progress
     * @param maxProgress         max progress
     * @param statusMessage       algorithm status message
     * @param traversedAlgorithms traversed algorithms
     * @return if successful. This means all output slots were restored.
     */
    private boolean tryLoadFromCache(JIPipeGraphNode algorithm, Consumer<JIPipeRunnerStatus> onProgress, int progress, int maxProgress, String statusMessage, List<JIPipeGraphNode> traversedAlgorithms) {
        if (!configuration.isLoadFromCache())
            return false;
        JIPipeGraphNode projectAlgorithm = cacheQuery.getNode(algorithm.getIdInGraph());
        JIPipeProjectCache.State stateId = cacheQuery.getCachedId(projectAlgorithm);
        Map<String, JIPipeDataSlot> cachedData = project.getCache().extract((JIPipeAlgorithm) projectAlgorithm, stateId);
        if (!cachedData.isEmpty()) {
            logStatus(onProgress, new JIPipeRunnerStatus(progress, maxProgress,
                    String.format("%s | Accessing cache with slots %s via state id %s", statusMessage, String.join(", ",
                            cachedData.keySet()), stateId)));
            for (JIPipeDataSlot outputSlot : algorithm.getOutputSlots()) {
                if (!cachedData.containsKey(outputSlot.getName())) {
                    logStatus(onProgress, new JIPipeRunnerStatus(progress, maxProgress,
                            String.format("%s | Cache access failed. Missing output slot %s", statusMessage, outputSlot.getName())));
                    return false;
                }
            }
            for (JIPipeDataSlot outputSlot : algorithm.getOutputSlots()) {
                outputSlot.clearData(false);
                outputSlot.copyFrom(cachedData.get(outputSlot.getName()));
            }
            logStatus(onProgress, new JIPipeRunnerStatus(progress, maxProgress,
                    String.format("%s | Cache data access successful.", statusMessage)));
            return true;
        }
        return false;
    }

    private synchronized void logStatus(Consumer<JIPipeRunnerStatus> onProgress, JIPipeRunnerStatus status) {
        onProgress.accept(status);
        log.append("[").append(status.getProgress()).append("/").append(status.getMaxProgress()).append("] ").append(status.getMessage()).append("\n");
    }

    public StringBuilder getLog() {
        return log;
    }

    /**
     * @return The graph used within this run. A copy of the project graph.
     */
    public JIPipeGraph getGraph() {
        return algorithmGraph;
    }

    /**
     * @return The run configuration
     */
    public JIPipeRunSettings getConfiguration() {
        return configuration;
    }

    /**
     * Loads a JIPipeRun from a folder
     *
     * @param folder Folder containing the run
     * @return The loaded run
     * @throws IOException Triggered by {@link com.fasterxml.jackson.databind.ObjectMapper}
     */
    public static JIPipeRun loadFromFolder(Path folder, JIPipeValidityReport report) throws IOException {
        Path parameterFile = folder.resolve("project.jip");
        JIPipeProject project = JIPipeProject.loadProject(parameterFile, report);
        JIPipeRunSettings configuration = new JIPipeRunSettings();
        configuration.setOutputPath(folder);
        JIPipeRun run = new JIPipeRun(project, configuration);
        run.prepare();
        return run;
    }
}
