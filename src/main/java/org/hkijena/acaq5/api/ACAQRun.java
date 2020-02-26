package org.hkijena.acaq5.api;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmVisibility;
import org.hkijena.acaq5.api.data.ACAQDataSlot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Runnable instance of an {@link ACAQProject}
 */
public class ACAQRun implements ACAQRunnable {
    private ACAQProject project;
    ACAQAlgorithmGraph algorithmGraph;
    private BiMap<String, ACAQRunSample> samples = HashBiMap.create();
    private ACAQRunConfiguration configuration;

    private Map<ACAQAlgorithm, ACAQAlgorithm> projectAlgorithms = new HashMap<>();

    public ACAQRun(ACAQProject project, ACAQRunConfiguration configuration) {
        this.project = project;
        this.configuration = configuration;
        initializeAlgorithmGraph();
    }

    /**
     * Converts the per-sample preprocessing graphs and the global analysis graph into one large analysis graph
     * This function removes internal nodes such as PreprocessingOutput
     */
    private void initializeAlgorithmGraph() {
        algorithmGraph = new ACAQAlgorithmGraph(ACAQAlgorithmVisibility.Analysis);

        // Add nodes
        initializeAlgorithmGraphNodes();

        // Add edges
        for(Map.Entry<String, ACAQProjectSample> sampleEntry : project.getSamples().entrySet()) {
            if(configuration.getSampleRestrictions() != null && !configuration.getSampleRestrictions().isEmpty() &&
            !configuration.getSampleRestrictions().contains(sampleEntry.getKey()))
                continue;
            initializeAlgorithmGraphEdges(sampleEntry);
        }
    }

    private void initializeAlgorithmGraphEdges(Map.Entry<String, ACAQProjectSample> sampleEntry) {
//        ACAQAlgorithmGraph preprocessing = sampleEntry.getValue().getPreprocessingGraph();
//        BiMap<String, ACAQAlgorithm> preprocessingAlgorithms = preprocessing.getAlgorithmNodes();
//        BiMap<String, ACAQAlgorithm> analysisAlgorithms = project.getGraph().getAlgorithmNodes();
//        String sampleName = sampleEntry.getKey();
//
//        // Preprocessing graph
//        for (Map.Entry<ACAQDataSlot<?>, ACAQDataSlot<?>> edge : sampleEntry.getValue().getPreprocessingGraph().getSlotEdges()) {
//            ACAQAlgorithm sourceAlgorithm = edge.getKey().getAlgorithm();
//            ACAQAlgorithm targetAlgorithm = edge.getValue().getAlgorithm();
//            String sourceAlgorithmName = sampleName + "/preprocessing/" + preprocessingAlgorithms.inverse().get(sourceAlgorithm);
//            String targetAlgorithmName = sampleName + "/preprocessing/" + preprocessingAlgorithms.inverse().get(targetAlgorithm);
//            String sourceSlotName = edge.getKey().getName();
//            String targetSlotName = edge.getValue().getName();
//
//            if(!(targetAlgorithm instanceof ACAQPreprocessingOutput)) {
//                ACAQAlgorithm runSourceAlgorithm = algorithmGraph.getAlgorithmNodes().get(sourceAlgorithmName);
//                ACAQAlgorithm runTargetAlgorithm = algorithmGraph.getAlgorithmNodes().get(targetAlgorithmName);
//                algorithmGraph.connect(runSourceAlgorithm.getSlots().get(sourceSlotName),
//                        runTargetAlgorithm.getSlots().get(targetSlotName));
//            }
//        }
//
//        // Analysis graph
//        for (Map.Entry<ACAQDataSlot<?>, ACAQDataSlot<?>> edge : project.getGraph().getSlotEdges()) {
//            ACAQAlgorithm sourceAlgorithm = edge.getKey().getAlgorithm();
//            ACAQAlgorithm targetAlgorithm = edge.getValue().getAlgorithm();
//            String sourceAlgorithmName = sampleName + "/analysis/" + analysisAlgorithms.inverse().get(sourceAlgorithm);
//            String targetAlgorithmName = sampleName + "/analysis/" + analysisAlgorithms.inverse().get(targetAlgorithm);
//            String sourceSlotName = edge.getKey().getName();
//            String targetSlotName = edge.getValue().getName();
//
//            if(!(sourceAlgorithm instanceof ACAQPreprocessingOutput)) {
//                ACAQAlgorithm runSourceAlgorithm = algorithmGraph.getAlgorithmNodes().get(sourceAlgorithmName);
//                ACAQAlgorithm runTargetAlgorithm = algorithmGraph.getAlgorithmNodes().get(targetAlgorithmName);
//                algorithmGraph.connect(runSourceAlgorithm.getSlots().get(sourceSlotName),
//                        runTargetAlgorithm.getSlots().get(targetSlotName));
//            }
//        }
//
//        // Preprocessing output connections
//        ACAQAlgorithm preprocessingOutput = preprocessingAlgorithms.values().stream().filter(a -> a instanceof ACAQPreprocessingOutput).findFirst().get();
//
//        for (Map.Entry<ACAQDataSlot<?>, ACAQDataSlot<?>> edge : project.getGraph().getSlotEdges()) {
//            ACAQAlgorithm sourceAlgorithm = edge.getKey().getAlgorithm();
//            ACAQAlgorithm targetAlgorithm = edge.getValue().getAlgorithm();
//            String sourceAlgorithmName = sampleName + "/analysis/" + analysisAlgorithms.inverse().get(sourceAlgorithm);
//            String targetAlgorithmName = sampleName + "/analysis/" + analysisAlgorithms.inverse().get(targetAlgorithm);
//            String sourceSlotName = edge.getKey().getName();
//            String targetSlotName = edge.getValue().getName();
//
//            if(sourceAlgorithm instanceof ACAQPreprocessingOutput) {
//
//                // Find the generating data slot within the preprocessing namespace
//                // Then connect its output directly to the input
//                ACAQDataSlot<?> preprocessingOutputSlot = preprocessingOutput.getSlots().get(sourceSlotName);
//                ACAQDataSlot<?> rawSourceSlot = preprocessing.getSourceSlot(preprocessingOutputSlot);
//                String rawSourceSlotName = rawSourceSlot.getName();
//                ACAQAlgorithm rawSourceAlgorithm = rawSourceSlot.getAlgorithm();
//                String rawSourceAlgorithmName = sampleName + "/preprocessing/" + preprocessingAlgorithms.inverse().get(rawSourceAlgorithm);
//
//                // Connect
//                ACAQAlgorithm runSourceAlgorithm = algorithmGraph.getAlgorithmNodes().get(rawSourceAlgorithmName);
//                ACAQAlgorithm runTargetAlgorithm = algorithmGraph.getAlgorithmNodes().get(targetAlgorithmName);
//                algorithmGraph.connect(runSourceAlgorithm.getSlots().get(rawSourceSlotName),
//                        runTargetAlgorithm.getSlots().get(targetSlotName));
//            }
//        }
    }

    private void initializeAlgorithmGraphNodes() {
//        // Create nodes
//        for(Map.Entry<String, ACAQProjectSample> sampleEntry : project.getSamples().entrySet()) {
//            String sampleName = sampleEntry.getKey();
//
//            if(configuration.getSampleRestrictions() != null && !configuration.getSampleRestrictions().isEmpty() &&
//                    !configuration.getSampleRestrictions().contains(sampleEntry.getKey()))
//                continue;
//
//            Set<ACAQAlgorithm> runAlgorithms = new HashSet<>();
//
//            // Preprocessing graph
//            for(Map.Entry<String, ACAQAlgorithm> algorithmEntry : sampleEntry.getValue().getPreprocessingGraph().getAlgorithmNodes().entrySet()) {
//                if(algorithmEntry.getValue().getCategory() == ACAQAlgorithmCategory.Internal)
//                    continue;
//                String algorithmName = sampleName + "/preprocessing/" + algorithmEntry.getKey();
//                ACAQAlgorithm copy = ACAQAlgorithm.clone(algorithmEntry.getValue());
//                copy.setInternalStoragePath(Paths.get(sampleName).resolve("preprocessing").resolve(algorithmEntry.getKey()));
//                algorithmGraph.insertNode(algorithmName, copy);
//                runAlgorithms.add(copy);
//                projectAlgorithms.put(copy, algorithmEntry.getValue());
//            }
//
//            // Analysis graph
//            for(Map.Entry<String, ACAQAlgorithm> algorithmEntry : project.getGraph().getAlgorithmNodes().entrySet()) {
//                if(algorithmEntry.getValue().getCategory() == ACAQAlgorithmCategory.Internal)
//                    continue;
//                String algorithmName = sampleName + "/analysis/" + algorithmEntry.getKey();
//                ACAQAlgorithm copy = ACAQAlgorithm.clone(algorithmEntry.getValue());
//                copy.setInternalStoragePath(Paths.get(sampleName).resolve("analysis").resolve(algorithmEntry.getKey()));
//                algorithmGraph.insertNode(algorithmName, copy);
//                runAlgorithms.add(copy);
//                projectAlgorithms.put(copy, algorithmEntry.getValue());
//            }
//
//            // Create sample in the sample list
//            samples.put(sampleEntry.getKey(), new ACAQRunSample(this, sampleEntry.getValue(), runAlgorithms));
//        }
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
        if(configuration.getOutputPath() != null) {
            if (!Files.exists(configuration.getOutputPath())) {
                try {
                    Files.createDirectories(configuration.getOutputPath());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            // Apply output path to the data slots
            for(ACAQDataSlot<?> slot : algorithmGraph.getSlotNodes()) {
                if(slot.isOutput()) {
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

    private void flushFinishedSlots(List<ACAQDataSlot<?>> traversedSlots, Set<ACAQAlgorithm> executedAlgorithms, int i, ACAQDataSlot<?> outputSlot) {
        if(!executedAlgorithms.contains(outputSlot.getAlgorithm()))
            return;
        if(configuration.isFlushingEnabled()) {
            boolean canFlush = true;
            for(int j = i + 1; j < traversedSlots.size(); ++j) {
                ACAQDataSlot<?> futureSlot = traversedSlots.get(j);
                if(futureSlot.isInput() && algorithmGraph.getSourceSlot(futureSlot) == outputSlot) {
                    canFlush = false;
                    break;
                }
            }
            if(canFlush) {
                if(configuration.isFlushingKeepsDataEnabled())
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
        List<ACAQDataSlot<?>> traversedSlots = algorithmGraph.traverse();

        // Update algorithm limits that may be used by
//        Set<ACAQAlgorithm> algorithmLimits = new HashSet<>();
//        if(configuration.getEndAlgorithmId() != null) {
//            if(configuration.isOnlyRunningEndAlgorithm())
//                algorithmLimits.add(configuration.getEndAlgorithm());
//            else {
//                for(ACAQDataSlot<?> slot : configuration.getEndAlgorithm().getOutputSlots()) {
//                    algorithmLimits.addAll(GraphUtils.getAllPredecessors(algorithmGraph.getGraph(), slot)
//                            .stream().map(ACAQDataSlot::getAlgorithm).collect(Collectors.toSet()));
//                }
//            }
//        }
//        else {
//            algorithmLimits.addAll(traversedSlots.stream().map(ACAQDataSlot::getAlgorithm).collect(Collectors.toSet()));
//        }

        for(int i = 0; i < traversedSlots.size(); ++i) {
            if (isCancelled.get())
                throw new RuntimeException("Execution was cancelled");
            ACAQDataSlot<?> slot = traversedSlots.get(i);
            onProgress.accept(new ACAQRunnerStatus(i, algorithmGraph.getSlotCount(), slot.getFullName()));

            if(slot.isInput()) {
                // Copy data from source
                ACAQDataSlot<?> sourceSlot = algorithmGraph.getSourceSlot(slot);
                slot.setData(sourceSlot.getData());

                // Check if we can flush the output
                flushFinishedSlots(traversedSlots, executedAlgorithms, i, sourceSlot);
            }
            else if(slot.isOutput()) {
                // Ensure the algorithm has run
                if(!executedAlgorithms.contains(slot.getAlgorithm())) {
                    onProgress.accept(new ACAQRunnerStatus(i, algorithmGraph.getSlotCount(), "Algorithm: " + slot.getAlgorithm().getName()));
                    slot.getAlgorithm().run();
                    executedAlgorithms.add(slot.getAlgorithm());
                }

                // Check if we can flush the output
                flushFinishedSlots(traversedSlots, executedAlgorithms, i, slot);
            }
            onProgress.accept(new ACAQRunnerStatus(i + 1, algorithmGraph.getSlotCount(), slot.getFullName() + " done"));
        }

        // Postprocessing
        try {
            if(configuration.getOutputPath() != null && configuration.isFlushingEnabled())
                project.saveProject(configuration.getOutputPath().resolve("parameters.json"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void forceFlushAlgorithms(Set<ACAQAlgorithm> algorithms) {
        for(ACAQAlgorithm algorithm : algorithms) {
            for(ACAQDataSlot<?> outputSlot : algorithm.getOutputSlots()) {
                if(configuration.isFlushingKeepsDataEnabled())
                    outputSlot.save();
                else
                    outputSlot.flush();
            }
        }
    }

    public ACAQAlgorithmGraph getGraph() {
        return algorithmGraph;
    }

    public BiMap<String, ACAQRunSample> getSamples() {
        return ImmutableBiMap.copyOf(samples);
    }

    /**
     * Loads an ACAQRun from a folder
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

    public ACAQRunConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Maps the run algorithm to its respective project algorithm
     */
    public Map<ACAQAlgorithm, ACAQAlgorithm> getProjectAlgorithms() {
        return Collections.unmodifiableMap(projectAlgorithms);
    }

}
