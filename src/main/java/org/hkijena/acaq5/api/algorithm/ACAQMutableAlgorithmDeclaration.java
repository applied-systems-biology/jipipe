package org.hkijena.acaq5.api.algorithm;

import org.hkijena.acaq5.api.traits.*;
import org.hkijena.acaq5.api.traits.global.ACAQDataSlotTraitConfiguration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Instantiates {@link ACAQAlgorithm} instances
 */
public abstract class ACAQMutableAlgorithmDeclaration implements ACAQAlgorithmDeclaration {

    private Class<? extends ACAQAlgorithm> algorithmClass;
    private String id;
    private String name;
    private String description;
    private ACAQAlgorithmCategory category;
    private Set<ACAQTraitDeclaration> preferredTraits = new HashSet<>();
    private Set<ACAQTraitDeclaration> unwantedTraits = new HashSet<>();
    private ACAQDataSlotTraitConfiguration slotTraitConfiguration = new ACAQDataSlotTraitConfiguration();
    private List<AlgorithmInputSlot> inputSlots = new ArrayList<>();
    private List<AlgorithmOutputSlot> outputSlots = new ArrayList<>();

    @Override
    public Class<? extends ACAQAlgorithm> getAlgorithmClass() {
        return algorithmClass;
    }

    public void setAlgorithmClass(Class<? extends ACAQAlgorithm> algorithmClass) {
        this.algorithmClass = algorithmClass;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public ACAQAlgorithmCategory getCategory() {
        return category;
    }

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
    public List<AlgorithmInputSlot> getInputSlots() {
        return inputSlots;
    }

    public void setInputSlots(List<AlgorithmInputSlot> inputSlots) {
        this.inputSlots = inputSlots;
    }

    @Override
    public List<AlgorithmOutputSlot> getOutputSlots() {
        return outputSlots;
    }

    public void setOutputSlots(List<AlgorithmOutputSlot> outputSlots) {
        this.outputSlots = outputSlots;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public ACAQDataSlotTraitConfiguration getSlotTraitConfiguration() {
        return slotTraitConfiguration;
    }

    public void setSlotTraitConfiguration(ACAQDataSlotTraitConfiguration slotTraitConfiguration) {
        this.slotTraitConfiguration = slotTraitConfiguration;
    }
}
