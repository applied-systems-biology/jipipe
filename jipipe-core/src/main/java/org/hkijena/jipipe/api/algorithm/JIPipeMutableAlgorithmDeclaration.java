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

package org.hkijena.jipipe.api.algorithm;

import java.util.ArrayList;
import java.util.List;

/**
 * Instantiates {@link JIPipeGraphNode} instances
 */
public abstract class JIPipeMutableAlgorithmDeclaration implements JIPipeAlgorithmDeclaration {

    private Class<? extends JIPipeGraphNode> algorithmClass;
    private String id;
    private String name;
    private String description;
    private JIPipeAlgorithmCategory category;
    private List<JIPipeInputSlot> inputSlots = new ArrayList<>();
    private List<JIPipeOutputSlot> outputSlots = new ArrayList<>();
    private String menuPath;
    private boolean hidden = false;

    @Override
    public Class<? extends JIPipeGraphNode> getAlgorithmClass() {
        return algorithmClass;
    }

    /**
     * Sets the algorithm class
     *
     * @param algorithmClass The algorithm class
     */
    public void setAlgorithmClass(Class<? extends JIPipeGraphNode> algorithmClass) {
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
    public JIPipeAlgorithmCategory getCategory() {
        return category;
    }

    /**
     * Sets the category
     *
     * @param category The category
     */
    public void setCategory(JIPipeAlgorithmCategory category) {
        this.category = category;
    }


    @Override
    public List<JIPipeInputSlot> getInputSlots() {
        return inputSlots;
    }

    /**
     * Sets the input slots
     *
     * @param inputSlots Input slots
     */
    public void setInputSlots(List<JIPipeInputSlot> inputSlots) {
        this.inputSlots = inputSlots;
    }

    @Override
    public List<JIPipeOutputSlot> getOutputSlots() {
        return outputSlots;
    }

    /**
     * Sets the output slots
     *
     * @param outputSlots Output slots
     */
    public void setOutputSlots(List<JIPipeOutputSlot> outputSlots) {
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
