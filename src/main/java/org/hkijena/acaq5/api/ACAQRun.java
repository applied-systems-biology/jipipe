package org.hkijena.acaq5.api;

import com.google.common.base.Charsets;
import ij.IJ;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.acaq5.utils.GraphUtils;
import org.hkijena.acaq5.utils.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Runnable instance of an {@link ACAQProject}
 */
public class ACAQRun implements ACAQRunnable {
    ACAQAlgorithmGraph algorithmGraph;
    private ACAQProject project;
    private ACAQRunConfiguration configuration;
    private StringBuilder log = new StringBuilder();

    /**
     * @param project       The project
     * @param configuration Run configuration
     */
    public ACAQRun(ACAQProject project, ACAQRunConfiguration configuration) {
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

    private void flushFinishedSlots(List<ACAQDataSlot> traversedSlots, Set<ACAQGraphNode> executedAlgorithms, int currentIndex, ACAQDataSlot outputSlot) {
        if (!executedAlgorithms.contains(outputSlot.getAlgorithm()))
            return;
        if (configuration.isFlushingEnabled()) {
            boolean canFlush = true;
            for (int j = currentIndex + 1; j < traversedSlots.size(); ++j) {
                ACAQDataSlot futureSlot = traversedSlots.get(j);
                if (futureSlot.isInput() && algorithmGraph.getSourceSlot(futureSlot) == outputSlot) {
                    canFlush = false;
                    break;
                }
            }
            if (canFlush) {
                if (configuration.isFlushingKeepsDataEnabled())
                    outputSlot.save();
                else
                    outputSlot.flush();
            }
        }
    }

    @Override
    public void run(Consumer<ACAQRunnerStatus> onProgress, Supplier<Boolean> isCancelled) {
        prepare();
        try {
            runAnalysis(onProgress, isCancelled);
        } catch (Exception e) {
            try {
                if (configuration.getOutputPath() != null)
                    Files.write(configuration.getOutputPath().resolve("log.txt"), log.toString().getBytes(Charsets.UTF_8));
            } catch (IOException ex) {
                IJ.handleException(ex);
            }
            throw e;
        }

        // Postprocessing
        try {
            if (configuration.getOutputPath() != null && configuration.isFlushingEnabled())
                project.saveProject(configuration.getOutputPath().resolve("parameters.json"));
        } catch (IOException e) {
            throw new UserFriendlyRuntimeException(e, "Could not save project to '" + configuration.getOutputPath().resolve("parameters.json") + "'!",
                    "Pipeline run", "Either the path is invalid, or you have no permission to write to the disk, or the disk space is full",
                    "Check if you can write to the output directory.");
        }
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
        Set<ACAQGraphNode> unExecutableAlgorithms = algorithmGraph.getAlgorithmsWithMissingInput();
        Set<ACAQGraphNode> executedAlgorithms = new HashSet<>();
        List<ACAQDataSlot> traversedSlots = algorithmGraph.traverse();

        // Update algorithm limits that may be used by
        Set<ACAQGraphNode> algorithmLimits = new HashSet<>();
        if (configuration.getEndAlgorithmId() != null) {
            ACAQGraphNode endAlgorithm = algorithmGraph.getAlgorithmNodes().get(configuration.getEndAlgorithmId());
            if (configuration.isOnlyRunningEndAlgorithm())
                algorithmLimits.add(endAlgorithm);
            else {
                for (ACAQDataSlot slot : endAlgorithm.getOutputSlots()) {
                    algorithmLimits.addAll(GraphUtils.getAllPredecessors(algorithmGraph.getGraph(), slot)
                            .stream().map(ACAQDataSlot::getAlgorithm).collect(Collectors.toSet()));
                }
            }
        } else {
            algorithmLimits.addAll(algorithmGraph.getAlgorithmNodes().values());
        }

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
            int traversionIndex = index;
            Consumer<ACAQRunnerSubStatus> algorithmProgress = s -> logStatus(onProgress, new ACAQRunnerStatus(traversionIndex, traversedSlots.size(),
                    statusMessage + " | " + s));

            if (slot.isInput()) {
                // Copy data from source
                ACAQDataSlot sourceSlot = algorithmGraph.getSourceSlot(slot);
                slot.copyFrom(sourceSlot);

                // Check if we can flush the output
                flushFinishedSlots(traversedSlots, executedAlgorithms, index, sourceSlot);
            } else if (slot.isOutput()) {
                // Ensure the algorithm has run
                if (!executedAlgorithms.contains(slot.getAlgorithm()) && algorithmLimits.contains(slot.getAlgorithm())) {
                    onProgress.accept(new ACAQRunnerStatus(index, traversedSlots.size(), statusMessage));
                    try {
                        slot.getAlgorithm().run(new ACAQRunnerSubStatus(), algorithmProgress, isCancelled);
                    } catch (Exception e) {
                        throw new UserFriendlyRuntimeException("Algorithm " + slot.getAlgorithm() + " raised an exception!",
                                e,
                                "An error occurred during processing",
                                "On running the algorithm '" + slot.getAlgorithm().getName() + "', within compartment '" + getProject().getCompartments().get(slot.getAlgorithm().getCompartment()).getName() + "'",
                                "Please refer to the other error messages.",
                                "Please follow the instructions for the other error messages.");
                    }
                    executedAlgorithms.add(slot.getAlgorithm());
                }

                // Check if we can flush the output
                flushFinishedSlots(traversedSlots, executedAlgorithms, index, slot);
            }
        }
    }

    private void logStatus(Consumer<ACAQRunnerStatus> onProgress, ACAQRunnerStatus status) {
        onProgress.accept(status);
        log.append("[").append(status.getProgress()).append("/").append(status.getMaxProgress()).append("] ").append(status.getMessage()).append("\n");
    }

    private void forceFlushAlgorithms(Set<ACAQGraphNode> algorithms) {
        for (ACAQGraphNode algorithm : algorithms) {
            for (ACAQDataSlot outputSlot : algorithm.getOutputSlots()) {
                if (configuration.isFlushingKeepsDataEnabled())
                    outputSlot.save();
                else
                    outputSlot.flush();
            }
        }
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
    public ACAQRunConfiguration getConfiguration() {
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
        ACAQMutableRunConfiguration configuration = new ACAQMutableRunConfiguration();
        configuration.setOutputPath(folder);
        ACAQRun run = new ACAQRun(project, configuration);
        run.prepare();
        return run;
    }
}
