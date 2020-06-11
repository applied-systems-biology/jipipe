package org.hkijena.acaq5.api.algorithm;

import java.util.ArrayList;
import java.util.List;

/**
 * Instantiates {@link ACAQGraphNode} instances
 */
public abstract class ACAQMutableAlgorithmDeclaration implements ACAQAlgorithmDeclaration {

    private Class<? extends ACAQGraphNode> algorithmClass;
    private String id;
    private String name;
    private String description;
    private ACAQAlgorithmCategory category;
    private List<AlgorithmInputSlot> inputSlots = new ArrayList<>();
    private List<AlgorithmOutputSlot> outputSlots = new ArrayList<>();
    private String menuPath;
    private boolean hidden = false;

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

    @Override
    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
}
