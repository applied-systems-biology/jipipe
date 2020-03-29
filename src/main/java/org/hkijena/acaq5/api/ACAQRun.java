package org.hkijena.acaq5.api;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
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

    public ACAQRun(ACAQProject project, ACAQRunConfiguration configuration) {
        this.project = project;
        this.configuration = configuration;
        this.algorithmGraph = new ACAQAlgorithmGraph(project.getGraph());
        initializeRelativeDirectories();
        initializeInternalStoragePaths();
    }

    /**
     * Loads an ACAQRun from a folder
     *
     * @param folder
     * @return
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

    private void initializeRelativeDirectories() {
        for (ACAQAlgorithm algorithm : algorithmGraph.getAlgorithmNodes().values()) {
            algorithm.setWorkDirectory(null);
        }
    }

    private void initializeInternalStoragePaths() {
        for (ACAQAlgorithm algorithm : algorithmGraph.getAlgorithmNodes().values()) {
            algorithm.setInternalStoragePath(Paths.get(StringUtils.jsonify(algorithm.getCompartment())).resolve(StringUtils.jsonify(algorithmGraph.getIdOf(algorithm))));
        }
    }

    public ACAQProject getProject() {
        return project;
    }

    /**
     * This function must be called before running the graph
     */
    private void prepare() {
        assignDataStoragePaths();
    }

    public void assignDataStoragePaths() {
        if (configuration.getOutputPath() != null) {
            if (!Files.exists(configuration.getOutputPath())) {
                try {
                    Files.createDirectories(configuration.getOutputPath());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            // Apply output path to the data slots
            for (ACAQDataSlot slot : algorithmGraph.getSlotNodes()) {
                if (slot.isOutput()) {
                    slot.setStoragePath(configuration.getOutputPath().resolve(slot.getAlgorithm().getInternalStoragePath().resolve(slot.getName())));
                    try {
                        Files.createDirectories(slot.getStoragePath());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private void flushFinishedSlots(List<ACAQDataSlot> traversedSlots, Set<ACAQAlgorithm> executedAlgorithms, int i, ACAQDataSlot outputSlot) {
        if (!executedAlgorithms.contains(outputSlot.getAlgorithm()))
            return;
        if (configuration.isFlushingEnabled()) {
            boolean canFlush = true;
            for (int j = i + 1; j < traversedSlots.size(); ++j) {
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
        Set<ACAQAlgorithm> executedAlgorithms = new HashSet<>();
        List<ACAQDataSlot> traversedSlots = algorithmGraph.traverse();

        // Update algorithm limits that may be used by
        Set<ACAQAlgorithm> algorithmLimits = new HashSet<>();
        if (configuration.getEndAlgorithmId() != null) {
            ACAQAlgorithm endAlgorithm = algorithmGraph.getAlgorithmNodes().get(configuration.getEndAlgorithmId());
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

        for (int i = 0; i < traversedSlots.size(); ++i) {
            if (isCancelled.get())
                throw new RuntimeException("Execution was cancelled");
            ACAQDataSlot slot = traversedSlots.get(i);
            onProgress.accept(new ACAQRunnerStatus(i, algorithmGraph.getSlotCount(), slot.getNameWithAlgorithmName()));

            if (slot.isInput()) {
                // Copy data from source
                ACAQDataSlot sourceSlot = algorithmGraph.getSourceSlot(slot);
                slot.copyFrom(sourceSlot);

                // Check if we can flush the output
                flushFinishedSlots(traversedSlots, executedAlgorithms, i, sourceSlot);
            } else if (slot.isOutput()) {
                // Ensure the algorithm has run
                if (!executedAlgorithms.contains(slot.getAlgorithm()) && algorithmLimits.contains(slot.getAlgorithm())) {
                    onProgress.accept(new ACAQRunnerStatus(i, algorithmGraph.getSlotCount(), "Algorithm: " + slot.getAlgorithm().getName()));
                    slot.getAlgorithm().run();
                    executedAlgorithms.add(slot.getAlgorithm());
                }

                // Check if we can flush the output
                flushFinishedSlots(traversedSlots, executedAlgorithms, i, slot);
            }
            onProgress.accept(new ACAQRunnerStatus(i + 1, algorithmGraph.getSlotCount(), slot.getNameWithAlgorithmName() + " done"));
        }

        // Postprocessing
        try {
            if (configuration.getOutputPath() != null && configuration.isFlushingEnabled())
                project.saveProject(configuration.getOutputPath().resolve("parameters.json"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void forceFlushAlgorithms(Set<ACAQAlgorithm> algorithms) {
        for (ACAQAlgorithm algorithm : algorithms) {
            for (ACAQDataSlot outputSlot : algorithm.getOutputSlots()) {
                if (configuration.isFlushingKeepsDataEnabled())
                    outputSlot.save();
                else
                    outputSlot.flush();
            }
        }
    }

    public ACAQAlgorithmGraph getGraph() {
        return algorithmGraph;
    }

    public ACAQRunConfiguration getConfiguration() {
        return configuration;
    }
}
