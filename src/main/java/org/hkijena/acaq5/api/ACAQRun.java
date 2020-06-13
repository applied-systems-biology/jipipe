package org.hkijena.acaq5.api;

import com.google.common.base.Charsets;
import ij.IJ;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.acaq5.utils.StringUtils;

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
 * Runnable instance of an {@link ACAQProject}
 */
public class ACAQRun implements ACAQRunnable {
    ACAQAlgorithmGraph algorithmGraph;
    private ACAQProject project;
    private ACAQRunSettings configuration;
    private StringBuilder log = new StringBuilder();
    private ACAQFixedThreadPool threadPool;

    /**
     * @param project       The project
     * @param configuration Run configuration
     */
    public ACAQRun(ACAQProject project, ACAQRunSettings configuration) {
        this.project = project;
        this.configuration = configuration;
        this.algorithmGraph = new ACAQAlgorithmGraph(project.getGraph());
        initializeRelativeDirectories();
        initializeInternalStoragePaths();
    }

    private void initializeRelativeDirectories() {
        for (ACAQGraphNode algorithm : algorithmGraph.getAlgorithmNodes().values()) {
            algorithm.setWorkDirectory(null);
        }
    }

    private void initializeInternalStoragePaths() {
        for (ACAQGraphNode algorithm : algorithmGraph.getAlgorithmNodes().values()) {
            algorithm.setInternalStoragePath(Paths.get(StringUtils.jsonify(algorithm.getCompartment())).resolve(StringUtils.jsonify(algorithmGraph.getIdOf(algorithm))));
        }
    }

    /**
     * @return The project that created this run
     */
    public ACAQProject getProject() {
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
            for (ACAQDataSlot slot : algorithmGraph.getSlotNodes()) {
                if (slot.isOutput()) {
                    slot.setStoragePath(configuration.getOutputPath().resolve(slot.getAlgorithm().getInternalStoragePath().resolve(slot.getName())));
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

    private void flushFinishedSlots(List<ACAQDataSlot> traversedSlots, List<ACAQGraphNode> traversedProjectAlgorithms, Set<ACAQGraphNode> executedAlgorithms, int currentIndex, ACAQDataSlot outputSlot, Set<ACAQDataSlot> flushedSlots) {
        if (!executedAlgorithms.contains(outputSlot.getAlgorithm()))
            return;
        if (flushedSlots.contains(outputSlot))
            return;
        boolean canFlush = true;
        for (int j = currentIndex + 1; j < traversedSlots.size(); ++j) {
            ACAQDataSlot futureSlot = traversedSlots.get(j);
            boolean isDeactivated = (futureSlot.getAlgorithm() instanceof ACAQAlgorithm) && (!((ACAQAlgorithm) futureSlot.getAlgorithm()).isEnabled());
            if (!isDeactivated && futureSlot.isInput() && algorithmGraph.getSourceSlot(futureSlot) == outputSlot) {
                canFlush = false;
                break;
            }
        }
        if (canFlush) {
            if (configuration.isStoreToCache()) {
                ACAQGraphNode runAlgorithm = outputSlot.getAlgorithm();
                ACAQGraphNode projectAlgorithm = project.getGraph().getAlgorithmNodes().get(runAlgorithm.getIdInGraph());
                ACAQProjectCache.State stateId = project.getStateIdOf((ACAQAlgorithm) projectAlgorithm, traversedProjectAlgorithms);
                project.getCache().store((ACAQAlgorithm) projectAlgorithm, stateId, outputSlot);
            }
            outputSlot.flush();
            flushedSlots.add(outputSlot);
        }
    }

    @Override
    public void run(Consumer<ACAQRunnerStatus> onProgress, Supplier<Boolean> isCancelled) {
        log.setLength(0);
        long startTime = System.currentTimeMillis();
        log.append("ACAQ5 run starting at ").append(StringUtils.formatDateTime(LocalDateTime.now())).append("\n");
        log.append("Preparing output folders ...\n");
        prepare();
        try {
            threadPool = new ACAQFixedThreadPool(configuration.getNumThreads());
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
        try {
            if (configuration.getOutputPath() != null)
                project.saveProject(configuration.getOutputPath().resolve("parameters.json"));
        } catch (IOException e) {
            throw new UserFriendlyRuntimeException(e, "Could not save project to '" + configuration.getOutputPath().resolve("parameters.json") + "'!",
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

    private void runAnalysis(Consumer<ACAQRunnerStatus> onProgress, Supplier<Boolean> isCancelled) {
        Set<ACAQGraphNode> unExecutableAlgorithms = algorithmGraph.getDeactivatedAlgorithms();
        Set<ACAQGraphNode> executedAlgorithms = new HashSet<>();
        Set<ACAQDataSlot> flushedSlots = new HashSet<>();
        List<ACAQDataSlot> traversedSlots = algorithmGraph.traverse();
        List<ACAQGraphNode> traversedProjectAlgorithms = getProject().getGraph().traverseAlgorithms();

        for (int index = 0; index < traversedSlots.size(); ++index) {
            if (isCancelled.get())
                throw new UserFriendlyRuntimeException("Execution was cancelled",
                        "You cancelled the execution of the algorithm pipeline.",
                        "Pipeline run", "You clicked 'Cancel'.",
                        "Do not click 'Cancel' if you do not want to cancel the execution.");
            ACAQDataSlot slot = traversedSlots.get(index);
            logStatus(onProgress, new ACAQRunnerStatus(index, algorithmGraph.getSlotCount(), slot.getNameWithAlgorithmName()));

            // If an algorithm cannot be executed, skip it automatically
            if (unExecutableAlgorithms.contains(slot.getAlgorithm()))
                continue;

            // Let algorithms provide sub-progress
            String statusMessage = "Algorithm: " + slot.getAlgorithm().getName();
            int traversingIndex = index;
            Consumer<ACAQRunnerSubStatus> algorithmProgress = s -> logStatus(onProgress, new ACAQRunnerStatus(traversingIndex, traversedSlots.size(),
                    statusMessage + " | " + s));

            if (slot.isInput()) {
                // Copy data from source
                ACAQDataSlot sourceSlot = algorithmGraph.getSourceSlot(slot);
                slot.copyFrom(sourceSlot);

                // Check if we can flush the output
                flushFinishedSlots(traversedSlots, traversedProjectAlgorithms, executedAlgorithms, index, sourceSlot, flushedSlots);
            } else if (slot.isOutput()) {
                // Ensure the algorithm has run
                if (!executedAlgorithms.contains(slot.getAlgorithm())) {
                    onProgress.accept(new ACAQRunnerStatus(index, traversedSlots.size(), statusMessage));

                    // If enabled try to extract outputs from cache
                    boolean dataLoadedFromCache = false;
                    if (configuration.isLoadFromCache()) {
                        dataLoadedFromCache = tryLoadFromCache(slot.getAlgorithm(),
                                onProgress,
                                traversingIndex,
                                traversedSlots.size(),
                                statusMessage,
                                traversedProjectAlgorithms);
                    }

                    if (!dataLoadedFromCache) {
                        try {
                            if (slot.getAlgorithm() instanceof ACAQAlgorithm) {
                                ((ACAQAlgorithm) slot.getAlgorithm()).setThreadPool(threadPool);
                            }
                            slot.getAlgorithm().run(new ACAQRunnerSubStatus(), algorithmProgress, isCancelled);
                        } catch (Exception e) {
                            throw new UserFriendlyRuntimeException("Algorithm " + slot.getAlgorithm() + " raised an exception!",
                                    e,
                                    "An error occurred during processing",
                                    "On running the algorithm '" + slot.getAlgorithm().getName() + "', within compartment '" + getProject().getCompartments().get(slot.getAlgorithm().getCompartment()).getName() + "'",
                                    "Please refer to the other error messages.",
                                    "Please follow the instructions for the other error messages.");
                        } finally {
                            if (slot.getAlgorithm() instanceof ACAQAlgorithm) {
                                ((ACAQAlgorithm) slot.getAlgorithm()).setThreadPool(null);
                            }
                        }
                    } else {
                        onProgress.accept(new ACAQRunnerStatus(index, traversedSlots.size(), statusMessage + " | Output data was loaded from cache. Not executing."));
                    }

                    executedAlgorithms.add(slot.getAlgorithm());
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
    private boolean tryLoadFromCache(ACAQGraphNode algorithm, Consumer<ACAQRunnerStatus> onProgress, int progress, int maxProgress, String statusMessage, List<ACAQGraphNode> traversedAlgorithms) {
        if (!configuration.isLoadFromCache())
            return false;
        ACAQGraphNode projectAlgorithm = project.getGraph().getAlgorithmNodes().get(algorithm.getIdInGraph());
        ACAQProjectCache.State stateId = project.getStateIdOf((ACAQAlgorithm) projectAlgorithm, traversedAlgorithms);
        Map<String, ACAQDataSlot> cachedData = project.getCache().extract((ACAQAlgorithm) projectAlgorithm, stateId);
        if (!cachedData.isEmpty()) {
            logStatus(onProgress, new ACAQRunnerStatus(progress, maxProgress,
                    String.format("%s | Accessing cache with slots %s via state id %s", statusMessage, String.join(", ",
                            cachedData.keySet()), stateId)));
            for (ACAQDataSlot outputSlot : algorithm.getOutputSlots()) {
                if (!cachedData.containsKey(outputSlot.getName())) {
                    logStatus(onProgress, new ACAQRunnerStatus(progress, maxProgress,
                            String.format("%s | Cache access failed. Missing output slot %s", statusMessage, outputSlot.getName())));
                    return false;
                }
            }
            for (ACAQDataSlot outputSlot : algorithm.getOutputSlots()) {
                outputSlot.clearData();
                outputSlot.copyFrom(cachedData.get(outputSlot.getName()));
            }
            logStatus(onProgress, new ACAQRunnerStatus(progress, maxProgress,
                    String.format("%s | Cache data access successful.", statusMessage)));
            return true;
        }
        return false;
    }

    private void logStatus(Consumer<ACAQRunnerStatus> onProgress, ACAQRunnerStatus status) {
        onProgress.accept(status);
        log.append("[").append(status.getProgress()).append("/").append(status.getMaxProgress()).append("] ").append(status.getMessage()).append("\n");
    }

    public StringBuilder getLog() {
        return log;
    }

    /**
     * @return The graph used within this run. A copy of the project graph.
     */
    public ACAQAlgorithmGraph getGraph() {
        return algorithmGraph;
    }

    /**
     * @return The run configuration
     */
    public ACAQRunSettings getConfiguration() {
        return configuration;
    }

    /**
     * Loads an ACAQRun from a folder
     *
     * @param folder Folder containing the run
     * @return The loaded run
     * @throws IOException Triggered by {@link com.fasterxml.jackson.databind.ObjectMapper}
     */
    public static ACAQRun loadFromFolder(Path folder) throws IOException {
        Path parameterFile = folder.resolve("parameters.json");
        ACAQProject project = ACAQProject.loadProject(parameterFile);
        ACAQRunSettings configuration = new ACAQRunSettings();
        configuration.setOutputPath(folder);
        ACAQRun run = new ACAQRun(project, configuration);
        run.prepare();
        return run;
    }
}
