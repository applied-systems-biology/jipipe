package org.hkijena.acaq5.extension.api.algorithms.macro;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.acaq5.api.ACAQProjectMetadata;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.algorithm.DefaultAlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.DefaultAlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.traits.ACAQDataSlotTraitConfiguration;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.utils.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GraphWrapperAlgorithmDeclaration implements ACAQAlgorithmDeclaration, ACAQValidatable {

    private String id;
    private ACAQProjectMetadata metadata = new ACAQProjectMetadata();
    private ACAQAlgorithmCategory category = ACAQAlgorithmCategory.Miscellaneous;
    private Set<ACAQTraitDeclaration> preferredTraits = new HashSet<>();
    private Set<ACAQTraitDeclaration> unwantedTraits = new HashSet<>();
    private ACAQDataSlotTraitConfiguration dataSlotTraitConfiguration;
    private List<AlgorithmInputSlot> inputSlots = new ArrayList<>();
    private List<AlgorithmOutputSlot> outputSlots = new ArrayList<>();
    private ACAQAlgorithmGraph graph = new ACAQAlgorithmGraph();

    public GraphWrapperAlgorithmDeclaration() {
    }

    @JsonGetter("id")
    @Override
    public String getId() {
        return id;
    }

    @JsonSetter("id")
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public Class<? extends ACAQAlgorithm> getAlgorithmClass() {
        return GraphWrapperAlgorithm.class;
    }

    @Override
    public ACAQAlgorithm newInstance() {
        return new GraphWrapperAlgorithm(this);
    }

    @Override
    public ACAQAlgorithm clone(ACAQAlgorithm algorithm) {
        return new GraphWrapperAlgorithm((GraphWrapperAlgorithm) algorithm);
    }

    @Override
    public String getName() {
        return metadata.getName();
    }

    @Override
    public String getDescription() {
        return metadata.getDescription();
    }

    @Override
    public ACAQAlgorithmCategory getCategory() {
        return category;
    }

    @JsonSetter("category")
    public void setCategory(ACAQAlgorithmCategory category) {
        this.category = category;
    }

    @Override
    public Set<ACAQTraitDeclaration> getPreferredTraits() {
        return preferredTraits;
    }

    public void setPreferredTraits(Set<ACAQTraitDeclaration> preferredTraits) {
        this.preferredTraits = preferredTraits;
    }

    @Override
    public Set<ACAQTraitDeclaration> getUnwantedTraits() {
        return unwantedTraits;
    }

    public void setUnwantedTraits(Set<ACAQTraitDeclaration> unwantedTraits) {
        this.unwantedTraits = unwantedTraits;
    }

    @Override
    public ACAQDataSlotTraitConfiguration getSlotTraitConfiguration() {
        return dataSlotTraitConfiguration;
    }

    public void setSlotTraitConfiguration(ACAQDataSlotTraitConfiguration dataSlotTraitConfiguration) {
        this.dataSlotTraitConfiguration = dataSlotTraitConfiguration;
    }

    @Override
    public List<AlgorithmInputSlot> getInputSlots() {
        return inputSlots;
    }

    @Override
    public List<AlgorithmOutputSlot> getOutputSlots() {
        return outputSlots;
    }

    @JsonGetter("preferred-traits")
    public List<String> getPreferredTraitIds() {
        return preferredTraits.stream().map(ACAQTraitDeclaration::getId).collect(Collectors.toList());
    }

    @JsonGetter("preferred-traits")
    public void setPreferredTraitIds(List<String> ids) {
        for (String id : ids) {
            preferredTraits.add(ACAQTraitRegistry.getInstance().getDeclarationById(id));
        }
    }

    @JsonGetter("unwanted-traits")
    public List<String> getUnwantedTraitIds() {
        return unwantedTraits.stream().map(ACAQTraitDeclaration::getId).collect(Collectors.toList());
    }

    @JsonGetter("unwanted-traits")
    public void setUnwantedTraitIds(List<String> ids) {
        for (String id : ids) {
            unwantedTraits.add(ACAQTraitRegistry.getInstance().getDeclarationById(id));
        }
    }

    @JsonGetter("metadata")
    public ACAQProjectMetadata getMetadata() {
        return metadata;
    }

    @JsonSetter("metadata")
    public void setMetadata(ACAQProjectMetadata metadata) {
        this.metadata = metadata;
    }

    @JsonGetter("graph")
    public ACAQAlgorithmGraph getGraph() {
        return graph;
    }

    @JsonSetter("graph")
    public void setGraph(ACAQAlgorithmGraph graph) {
        this.graph = graph;
        updateSlots();
    }

    private void updateSlots() {
        inputSlots.clear();
        outputSlots.clear();

        Set<String> existingSlots = new HashSet<>();
        for (ACAQDataSlot slot : graph.getUnconnectedSlots()) {
            if(slot.isInput()) {
                String name = StringUtils.makeUniqueString(slot.getName(), existingSlots::contains);
                inputSlots.add(new DefaultAlgorithmInputSlot(slot.getAcceptedDataType(), name, false));
                existingSlots.add(name);
            }
            else if(slot.isOutput()) {
                String name = StringUtils.makeUniqueString(slot.getName(), existingSlots::contains);
                outputSlots.add(new DefaultAlgorithmOutputSlot(slot.getAcceptedDataType(), name, false));
                existingSlots.add(name);
            }
        }
    }

    @JsonGetter("acaq:project-type")
    public String getProjectType() {
        return "graph-wrapper-algorithm";
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if(id == null || id.isEmpty()) {
            report.reportIsInvalid("ID is null or empty! Please provide a valid algorithm ID.");
        }
        else if(ACAQAlgorithmRegistry.getInstance().hasAlgorithmWithId(id)) {
            report.reportIsInvalid("The ID already exists! Please provide a unique ID.");
        }
    }
}
