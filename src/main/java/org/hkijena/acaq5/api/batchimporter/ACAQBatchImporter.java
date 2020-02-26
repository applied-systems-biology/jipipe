package org.hkijena.acaq5.api.batchimporter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQPreprocessingOutput;
import org.hkijena.acaq5.api.ACAQProject;
import org.hkijena.acaq5.api.ACAQProjectSample;
import org.hkijena.acaq5.api.ACAQRunnable;
import org.hkijena.acaq5.api.ACAQRunnerStatus;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmVisibility;
import org.hkijena.acaq5.api.batchimporter.algorithms.ACAQDataSourceFromFile;
import org.hkijena.acaq5.api.batchimporter.dataslots.ACAQFilesDataSlot;
import org.hkijena.acaq5.api.batchimporter.dataypes.ACAQFilesData;
import org.hkijena.acaq5.api.batchimporter.traits.ProjectSampleTrait;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.utils.GraphUtils;
import org.hkijena.acaq5.utils.JsonUtils;
import org.hkijena.acaq5.utils.StringUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@JsonSerialize(using = ACAQBatchImporter.Serializer.class)
public class ACAQBatchImporter implements ACAQRunnable, ACAQValidatable {
    private ACAQProject project;
    private ACAQAlgorithmGraph graph = new ACAQAlgorithmGraph(ACAQAlgorithmVisibility.BatchImporter);
    private ConflictResolution conflictResolution = ConflictResolution.Skip;
    private Map<String, ACAQProjectSample> currentSampleAssignment = new HashMap<>();

    public ACAQBatchImporter(ACAQProject project) {
        this.project = project;
        initialize();
    }

    private void initialize() {
//        graph.insertNode(new ACAQPreprocessingOutput(project.getPreprocessingOutputConfiguration(),
//                project.getPreprocessingTraitConfiguration()), ACAQAlgorithmGraph.COMPARTMENT_BATCHIMPORTER);
    }

    public ACAQAlgorithmGraph getGraph() {
        return graph;
    }

    public void saveAs(Path path) throws IOException {
        ObjectMapper mapper = JsonUtils.getObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), this);
    }

    public static ACAQBatchImporter loadFromFile(Path fileName, ACAQProject project) throws IOException {
        ACAQBatchImporter importer = new ACAQBatchImporter(project);
        importer.fromJson(JsonUtils.getObjectMapper().readerFor(JsonNode.class).readValue(fileName.toFile()));
        return importer;
    }

    /**
     * Returns all samples that might conflict with existing samples
     * @return
     */
    public Set<String> getConflictingSamples() {
        runFilesystemAlgorithms();
        Set<String> sampleNames = new HashSet<>();
        for(ACAQAlgorithm algorithm : graph.getAlgorithmNodes().values()) {
            if (algorithm instanceof ACAQDataSourceFromFile) {
                for(ACAQDataSlot<?> inputSlot : algorithm.getInputSlots()) {
                    if(inputSlot instanceof ACAQFilesDataSlot) {
                        ACAQFilesData data = ((ACAQFilesDataSlot) inputSlot).getData();
                        sampleNames.addAll(data.getFiles().stream().map(f -> (String)f.findAnnotation(ProjectSampleTrait.FILESYSTEM_ANNOTATION_SAMPLE)).collect(Collectors.toSet()));
                    }
                    else {
                        throw new RuntimeException("Invalid DataSourceFromFile input slot!");
                    }
                }
            }
        }
        return sampleNames.stream().filter(s -> project.getSamples().containsKey(s)).collect(Collectors.toSet());
    }

    private Set<ACAQAlgorithm> getDataGenerationAlgorithms() {
        Set<ACAQAlgorithm> dataGenerationAlgorithms = new HashSet<>();

        for(ACAQAlgorithm algorithm : graph.getAlgorithmNodes().values()) {
            if(algorithm instanceof ACAQDataSourceFromFile) {
                // Assign project
                ((ACAQDataSourceFromFile) algorithm).setBatchImporter(this);

                for (ACAQDataSlot<?> outputSlot : algorithm.getOutputSlots()) {
                    for (ACAQDataSlot<?> predecessor : GraphUtils.getAllPredecessors(graph.getGraph(), outputSlot)) {
                        dataGenerationAlgorithms.add(predecessor.getAlgorithm());
                    }
                }
            }
        }

        return dataGenerationAlgorithms;
    }

    private void runFilesystemAlgorithms() {
        Set<ACAQAlgorithm> dataGenerationAlgorithms = getDataGenerationAlgorithms();
        Set<ACAQAlgorithm> executedAlgorithms = new HashSet<>();
        for(ACAQDataSlot<?> slot : graph.traverse()) {
            if(!dataGenerationAlgorithms.contains(slot.getAlgorithm()))
                continue;
            if(slot.getAlgorithm() instanceof ACAQDataSourceFromFile)
                continue;
            if(slot.isInput()) {
                // Copy data from source
                ACAQDataSlot<?> sourceSlot = graph.getSourceSlot(slot);
                slot.setData(sourceSlot.getData());
            }
            else if(slot.isOutput()) {
                if(!executedAlgorithms.contains(slot.getAlgorithm())) {
                    slot.getAlgorithm().run();
                    executedAlgorithms.add(slot.getAlgorithm());
                }
            }
        }
        for(ACAQDataSlot<?> slot : graph.traverse()) {
            if(slot.getAlgorithm() instanceof ACAQDataSourceFromFile) {
                if(slot.isInput()) {
                    // Copy data from source
                    ACAQDataSlot<?> sourceSlot = graph.getSourceSlot(slot);
                    slot.setData(sourceSlot.getData());
                }
            }
        }
    }

    @Override
    public void run(Consumer<ACAQRunnerStatus> onProgress, Supplier<Boolean> isCancelled) {

//        Set<ACAQAlgorithm> dataGenerationAlgorithms = getDataGenerationAlgorithms();
//
//        runFilesystemAlgorithms();
//
//        // Find the association batch-sample -> project sample
//        registerSamples();
//
//        // Run data generation
//        for(ACAQAlgorithm algorithm : graph.getAlgorithmNodes().values()) {
//            if (algorithm instanceof ACAQDataSourceFromFile) {
//                ((ACAQDataSourceFromFile) algorithm).setBatchImporter(this);
//                algorithm.run();
//            }
//        }
//
//        // Generate algorithms
//        for(ACAQAlgorithm algorithm : graph.getAlgorithmNodes().values()) {
//            if(algorithm.getCategory() == ACAQAlgorithmCategory.Internal)
//                continue;
//            if(!dataGenerationAlgorithms.contains(algorithm)) {
//                for(ACAQProjectSample sample : currentSampleAssignment.values()) {
//                    ACAQAlgorithm copy = ACAQAlgorithm.clone(algorithm);
//                    copy.setLocation(null);
//                    sample.insertNode(graph.getIdOf(algorithm), copy);
//                }
//            }
//        }
//
//        // Add connections
//        for(ACAQAlgorithm targetAlgorithm : graph.getAlgorithmNodes().values()) {
//            String targetAlgorithmId = graph.getIdOf(targetAlgorithm);
//            if (!dataGenerationAlgorithms.contains(targetAlgorithm)) {
//                for(ACAQProjectSample sample : currentSampleAssignment.values()) {
//                    ACAQAlgorithm targetCopy = sample.getPreprocessingGraph().getAlgorithmNodes().get(targetAlgorithmId);
//
//                    for (ACAQDataSlot<?> targetSlot : targetAlgorithm.getInputSlots()) {
//                        ACAQDataSlot<?> sourceSlot = graph.getSourceSlot(targetSlot);
//                        String sourceAlgorithmId = graph.getIdOf(sourceSlot.getAlgorithm());
//                        ACAQAlgorithm sourceCopy = sample.getPreprocessingGraph().getAlgorithmNodes().get(sourceAlgorithmId);
//
//                        ACAQDataSlot<?> copyTarget = targetCopy.getSlots().get(targetSlot.getName());
//                        ACAQDataSlot<?> copySource = sourceCopy.getSlots().get(sourceSlot.getName());
//
//                        sample.getPreprocessingGraph().connect(copySource, copyTarget);
//                    }
//
//                }
//            }
//        }
    }

    private void registerSamples() {
        currentSampleAssignment.clear();
        Set<String> sampleNames = new HashSet<>();
        for(ACAQAlgorithm algorithm : graph.getAlgorithmNodes().values()) {
            if (algorithm instanceof ACAQDataSourceFromFile) {
                for(ACAQDataSlot<?> inputSlot : algorithm.getInputSlots()) {
                    if(inputSlot instanceof ACAQFilesDataSlot) {
                        ACAQFilesData data = ((ACAQFilesDataSlot) inputSlot).getData();
                        sampleNames.addAll(data.getFiles().stream().map(f -> (String)f.findAnnotation(ProjectSampleTrait.FILESYSTEM_ANNOTATION_SAMPLE)).collect(Collectors.toSet()));
                    }
                    else {
                        throw new RuntimeException("Invalid DataSourceFromFile input slot!");
                    }
                }
            }
        }
        for (String sampleName : sampleNames) {
            registerSampleName(sampleName);
        }
    }

    public void fromJson(JsonNode node) {
        JsonNode projectTypeNode = node.path("acaq:project-type");
        if(projectTypeNode.isMissingNode() || !projectTypeNode.asText().equals("batch-importer"))
            throw new IllegalArgumentException("The JSON data does not contain data for a batch-importer!");
        graph.clear();
        initialize();
        graph.fromJson(node.get("algorithm-graph"));
    }

    public ACAQProject getProject() {
        return project;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Algorithm Graph").report(graph);
        if(getProject().getPreprocessingOutputConfiguration().getSlots().isEmpty())
            report.forCategory("Algorithm Graph").forCategory("Preprocessing output").reportIsInvalid("No data source are generated! Please add data slots to the preprocessing output.");

        boolean foundSampleAnnotation = false;
        for (Set<Class<? extends ACAQTrait>> traits : graph.getAlgorithmTraits().values()) {
            for (Class<? extends ACAQTrait> trait : traits) {
                if(ProjectSampleTrait.class.isAssignableFrom(trait)) {
                    foundSampleAnnotation = true;
                    break;
                }
            }
            if(foundSampleAnnotation)
                break;
        }
        if(!foundSampleAnnotation)
            report.forCategory("Sample generation").reportIsInvalid("No samples are generated! Please add a node that adds the 'Project sample' annotation!");
    }

    public ConflictResolution getConflictResolution() {
        return conflictResolution;
    }

    public void setConflictResolution(ConflictResolution conflictResolution) {
        this.conflictResolution = conflictResolution;
    }

    private void registerSampleName(String sampleName) {
        if(project.getSamples().containsKey(sampleName)) {
            switch (conflictResolution) {
                case Skip:
                    currentSampleAssignment.put(sampleName, null);
                    break;
                case Overwrite:
                    project.removeSample(project.getSamples().get(sampleName));
                    currentSampleAssignment.put(sampleName, project.getOrCreate(sampleName));
                    break;
                case Rename: {
                    String newSampleName = StringUtils.makeUniqueString(sampleName, project.getSamples().keySet());
                    currentSampleAssignment.put(sampleName, project.getOrCreate(newSampleName));
                }
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }
        else {
            currentSampleAssignment.put(sampleName, project.getOrCreate(sampleName));
        }
    }

    /**
     * Returns the sample that will be subject of modification
     * @param sampleName
     * @return null if the sample is skipped
     */
    public ACAQProjectSample getTargetSample(String sampleName) {
        return currentSampleAssignment.get(sampleName);
    }

    /**
     * Returns the samples that have been imported in the last run()
     * @return
     */
    public Set<ACAQProjectSample> getLastImportedSamples() {
        return currentSampleAssignment.values().stream().filter(Objects::nonNull).collect(Collectors.toSet());
    }

    public static class Serializer extends JsonSerializer<ACAQBatchImporter> {
        @Override
        public void serialize(ACAQBatchImporter project, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("acaq:project-type", "batch-importer");
            jsonGenerator.writeObjectField("algorithm-graph", project.graph);
            jsonGenerator.writeEndObject();
        }
    }

    public enum ConflictResolution {
        /**
         * Skip existing samples
         */
        Skip,
        /**
         * Rename new samples
         */
        Rename,
        /**
         * Overwrite existing samples
         */
        Overwrite
    }
}
