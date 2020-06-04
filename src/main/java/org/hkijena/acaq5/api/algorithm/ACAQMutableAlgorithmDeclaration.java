package org.hkijena.acaq5.api.algorithm;

import org.hkijena.acaq5.api.data.traits.ACAQDataSlotTraitConfiguration;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Instantiates {@link ACAQGraphNode} instances
 */
public abstract class ACAQMutableAlgorithmDeclaration implements ACAQAlgorithmDeclaration {

    private Class<? extends ACAQGraphNode> algorithmClass;
    private String id;
    private String name;
    private String description;
    private ACAQAlgorithmCategory category;
    private Set<ACAQTraitDeclaration> preferredTraits = new HashSet<>();
    private Set<ACAQTraitDeclaration> unwantedTraits = new HashSet<>();
    private ACAQDataSlotTraitConfiguration slotTraitConfiguration = new ACAQDataSlotTraitConfiguration();
    private List<AlgorithmInputSlot> inputSlots = new ArrayList<>();
    private List<AlgorithmOutputSlot> outputSlots = new ArrayList<>();
    private String menuPath;

    @Override
    public Class<? extends ACAQGraphNode> getAlgorithmClass() {
        return algorithmClass;
    }

    /**
     * Sets the algorithm class
     *
     * @param algorithmClass The algorithm class
     */
    public void setAlgorithmClass(Class<? extends ACAQGraphNode> algorithmClass) {
        this.algorithmClass = algorithmClass;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Sets the name
     *
     * @param name The name
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description
     *
     * @param description The description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public ACAQAlgorithmCategory getCategory() {
        return category;
    }

    /**
     * Sets the category
     *
     * @param category The category
     */
    public void setCategory(ACAQAlgorithmCategory category) {
        this.category = category;
    }

    @Override
    public Set<ACAQTraitDeclaration> getPreferredTraits() {
        return preferredTraits;
    }

    /**
     * Sets preferred traits
     *
     * @param preferredTraits The preferred traits
     */
    public void setPreferredTraits(Set<ACAQTraitDeclaration> preferredTraits) {
        this.preferredTraits = preferredTraits;
    }

    @Override
    public Set<ACAQTraitDeclaration> getUnwantedTraits() {
        return unwantedTraits;
    }

    /**
     * Sets unwanted traits
     *
     * @param unwantedTraits Unwanted traits
     */
    public void setUnwantedTraits(Set<ACAQTraitDeclaration> unwantedTraits) {
        this.unwantedTraits = unwantedTraits;
    }

    @Override
    public List<AlgorithmInputSlot> getInputSlots() {
        return inputSlots;
    }

    /**
     * Sets the input slots
     *
     * @param inputSlots Input slots
     */
    public void setInputSlots(List<AlgorithmInputSlot> inputSlots) {
        this.inputSlots = inputSlots;
    }

    @Override
    public List<AlgorithmOutputSlot> getOutputSlots() {
        return outputSlots;
    }

    /**
     * Sets the output slots
     *
     * @param outputSlots Output slots
     */
    public void setOutputSlots(List<AlgorithmOutputSlot> outputSlots) {
        this.outputSlots = outputSlots;
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * Sets the ID
     *
     * @param id A unique ID
     */
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public ACAQDataSlotTraitConfiguration getSlotTraitConfiguration() {
        return slotTraitConfiguration;
    }

    /**
     * Sets the slot trait configuration
     *
     * @param slotTraitConfiguration The configuration
     */
    public void setSlotTraitConfiguration(ACAQDataSlotTraitConfiguration slotTraitConfiguration) {
        this.slotTraitConfiguration = slotTraitConfiguration;
    }

    @Override
    public String getMenuPath() {
        return menuPath;
    }

    /**
     * Sets the menu path. Menu items are separated by newlines
     *
     * @param menuPath The menu path
     */
    public void setMenuPath(String menuPath) {
        this.menuPath = menuPath;
    }
}
