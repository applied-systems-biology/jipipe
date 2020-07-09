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
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithm;
import org.hkijena.jipipe.api.algorithm.JIPipeGraph;
import org.hkijena.jipipe.api.algorithm.JIPipeGraphNode;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
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
    JIPipeGraph algorithmGraph;
    private JIPipeProject project;
    private JIPipeRunSettings configuration;
    private StringBuilder log = new StringBuilder();
    private JIPipeFixedThreadPool threadPool;

    /**
     * @param project       The project
     * @param configuration Run configuration
     */
    public JIPipeRun(JIPipeProject project, JIPipeRunSettings configuration) {
        this.project = project;
        this.configuration = configuration;
        this.algorithmGraph = new JIPipeGraph(project.getGraph());
        initializeRelativeDirectories();
        initializeInternalStoragePaths();
    }

    private void initializeRelativeDirectories() {
        for (JIPipeGraphNode algorithm : algorithmGraph.getAlgorithmNodes().values()) {
            algorithm.setWorkDirectory(null);
        }
    }

    private void initializeInternalStoragePaths() {
        for (JIPipeGraphNode algorithm : algorithmGraph.getAlgorithmNodes().values()) {
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
                JIPipeGraphNode projectAlgorithm = project.getGraph().getAlgorithmNodes().get(runAlgorithm.getIdInGraph());
                JIPipeProjectCache.State stateId = project.getStateIdOf((JIPipeAlgorithm) projectAlgorithm, traversedProjectAlgorithms);
                project.getCache().store((JIPipeAlgorithm) projectAlgorithm, stateId, outputSlot);
            }
            outputSlot.flush();
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
            try {
                if (configuration.getOutputPath() != null)
                    Files.write(configuration.getOutputPath().resolve("log.txt"), log.toString().getBytes(Charsets.UTF_8));
            } catch (IOException ex) {
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
            if (configuration.getOutputPath() != null)
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
            logStatus(onProgress, new JIPipeRunnerStatus(index, algorithmGraph.getSlotCount(), slot.getNameWithAlgorithmName()));

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
                // Ensure the algorithm has run
                if (!executedAlgorithms.contains(slot.getNode())) {
                    onProgress.accept(new JIPipeRunnerStatus(index, traversedSlots.size(), statusMessage));

                    // If enabled try to extract outputs from cache
                    boolean dataLoadedFromCache = false;
                    if (configuration.isLoadFromCache()) {
                        dataLoadedFromCache = tryLoadFromCache(slot.getNode(),
                                onProgress,
                                traversingIndex,
                                traversedSlots.size(),
                                statusMessage,
                                traversedProjectAlgorithms);
                    }

                    if (!dataLoadedFromCache) {
                        try {
                            if (slot.getNode() instanceof JIPipeAlgorithm) {
                                ((JIPipeAlgorithm) slot.getNode()).setThreadPool(threadPool);
                            }
                            slot.getNode().run(new JIPipeRunnerSubStatus(), algorithmProgress, isCancelled);
                        } catch (HeadlessException e) {
                            throw new UserFriendlyRuntimeException("Algorithm " + slot.getNode() + " does not work in a headless environment!",
                                    e,
                                    "An error occurred during processing",
                                    "On running the algorithm '" + slot.getNode().getName() + "', within compartment '" + getProject().getCompartments().get(slot.getNode().getCompartment()).getName() + "'",
                                    "The algorithm raised an error, as it is not compatible with a headless environment.",
                                    "Please contact the plugin developers about this issue. If this happens in an ImageJ method, please contact the ImageJ developers.");
                        } catch (Exception e) {
                            throw new UserFriendlyRuntimeException("Algorithm " + slot.getNode() + " raised an exception!",
                                    e,
                                    "An error occurred during processing",
                                    "On running the algorithm '" + slot.getNode().getName() + "', within compartment '" + getProject().getCompartments().get(slot.getNode().getCompartment()).getName() + "'",
                                    "Please refer to the other error messages.",
                                    "Please follow the instructions for the other error messages.");
                        } finally {
                            if (slot.getNode() instanceof JIPipeAlgorithm) {
                                ((JIPipeAlgorithm) slot.getNode()).setThreadPool(null);
                            }
                        }
                    } else {
                        onProgress.accept(new JIPipeRunnerStatus(index, traversedSlots.size(), statusMessage + " | Output data was loaded from cache. Not executing."));
                    }

                    executedAlgorithms.add(slot.getNode());
                }

                // Check if we can flush the output
                flushFinishedSlots(traversedSlots, traversedProjectAlgorithms, executedAlgorithms, index, slot, flushedSlots);
            }
        }
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
        JIPipeGraphNode projectAlgorithm = project.getGraph().getAlgorithmNodes().get(algorithm.getIdInGraph());
        JIPipeProjectCache.State stateId = project.getStateIdOf((JIPipeAlgorithm) projectAlgorithm, traversedAlgorithms);
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
                outputSlot.clearData();
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
     * Loads an JIPipeRun from a folder
     *
     * @param folder Folder containing the run
     * @return The loaded run
     * @throws IOException Triggered by {@link com.fasterxml.jackson.databind.ObjectMapper}
     */
    public static JIPipeRun loadFromFolder(Path folder) throws IOException {
        Path parameterFile = folder.resolve("project.jip");
        JIPipeProject project = JIPipeProject.loadProject(parameterFile);
        JIPipeRunSettings configuration = new JIPipeRunSettings();
        configuration.setOutputPath(folder);
        JIPipeRun run = new JIPipeRun(project, configuration);
        run.prepare();
        return run;
    }
}
