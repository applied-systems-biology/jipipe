/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.nodes.infos;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeEmptyData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;

import java.util.ArrayList;
import java.util.List;

/**
 * Instantiates {@link JIPipeGraphNode} instances
 */
public abstract class JIPipeMutableNodeInfo implements JIPipeNodeInfo {

    private Class<? extends JIPipeGraphNode> nodeClass;
    private String id;
    private String name;
    private HTMLText description = new HTMLText();
    private JIPipeNodeTypeCategory category;
    private List<AddJIPipeInputSlot> inputSlots = new ArrayList<>();
    private List<AddJIPipeOutputSlot> outputSlots = new ArrayList<>();
    private String menuPath;

    private Class<? extends JIPipeData> dataSourceMenuLocation = JIPipeEmptyData.class;
    private boolean hidden = false;
    private boolean runnable = true;
    private boolean deprecated = false;
    private List<String> additionalCitations = new ArrayList<>();

    private List<JIPipeNodeMenuLocation> aliases = new ArrayList<>();

    @Override
    public Class<? extends JIPipeData> getDataSourceMenuLocation() {
        return dataSourceMenuLocation;
    }

    public void setDataSourceMenuLocation(Class<? extends JIPipeData> dataSourceMenuLocation) {
        this.dataSourceMenuLocation = dataSourceMenuLocation;
    }

    @Override
    public Class<? extends JIPipeGraphNode> getInstanceClass() {
        return nodeClass;
    }

    /**
     * Sets the algorithm class
     *
     * @param nodeClass The algorithm class
     */
    public void setnodeClass(Class<? extends JIPipeGraphNode> nodeClass) {
        this.nodeClass = nodeClass;
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
    public HTMLText getDescription() {
        return description;
    }

    /**
     * Sets the description
     *
     * @param description The description
     */
    public void setDescription(HTMLText description) {
        this.description = description;
    }

    @Override
    public JIPipeNodeTypeCategory getCategory() {
        return category;
    }

    /**
     * Sets the category
     *
     * @param category The category
     */
    public void setCategory(JIPipeNodeTypeCategory category) {
        this.category = category;
    }


    @Override
    public List<AddJIPipeInputSlot> getInputSlots() {
        return inputSlots;
    }

    /**
     * Sets the input slots
     *
     * @param inputSlots Input slots
     */
    public void setInputSlots(List<AddJIPipeInputSlot> inputSlots) {
        this.inputSlots = inputSlots;
    }

    @Override
    public List<AddJIPipeOutputSlot> getOutputSlots() {
        return outputSlots;
    }

    /**
     * Sets the output slots
     *
     * @param outputSlots Output slots
     */
    public void setOutputSlots(List<AddJIPipeOutputSlot> outputSlots) {
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

    @Override
    public boolean isRunnable() {
        return runnable;
    }

    public void setRunnable(boolean runnable) {
        this.runnable = runnable;
    }

    @Override
    public List<String> getAdditionalCitations() {
        return additionalCitations;
    }

    public void setAdditionalCitations(List<String> additionalCitations) {
        this.additionalCitations = additionalCitations;
    }

    @Override
    public List<JIPipeNodeMenuLocation> getAliases() {
        return aliases;
    }

    public void setAliases(List<JIPipeNodeMenuLocation> aliases) {
        this.aliases = aliases;
    }

    @Override
    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }
}
