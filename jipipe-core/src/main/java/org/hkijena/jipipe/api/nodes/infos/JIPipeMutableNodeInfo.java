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
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyInputProfile;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension;
import org.hkijena.jipipe.api.nodes.taxonomy.NoFramework;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Instantiates {@link JIPipeGraphNode} instances
 */
public abstract class JIPipeMutableNodeInfo implements JIPipeNodeInfo {

    private Class<? extends JIPipeGraphNode> nodeClass;
    private String id;
    private String name;
    private HTMLText description = new HTMLText();
    private List<AddJIPipeInputSlot> inputSlots = new ArrayList<>();
    private List<AddJIPipeOutputSlot> outputSlots = new ArrayList<>();
    private Set<Class<? extends JIPipeEnvironment>> environments = new HashSet<>();

    private Class<? extends JIPipeData> dataSourceMenuLocation = JIPipeEmptyData.class;
    private boolean hidden = false;
    private boolean runnable = true;
    private boolean deprecated = false;
    private boolean unstable = false;
    private JIPipeNodeClassification nodeClassification = JIPipeNodeClassification.Standard;
    private List<String> additionalCitations = new ArrayList<>();

    private JIPipeNodeTaxonomyFunction primaryFunction = null;
    private Set<JIPipeNodeTaxonomyFunction> secondaryFunctions = new HashSet<>();
    private Set<JIPipeNodeTaxonomySpatialDimension> spatialDimensions = new HashSet<>();
    private JIPipeNodeTaxonomyFramework framework = new NoFramework();
    private Set<String> taxonomyTags = new HashSet<>();
    private Set<String> mergedTags = new HashSet<>();
    private JIPipeNodeTaxonomyInputProfile inputProfile = JIPipeNodeTaxonomyInputProfile.NA;

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
    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    @Override
    public Set<Class<? extends JIPipeEnvironment>> getEnvironments() {
        return environments;
    }

    public void setEnvironments(Set<Class<? extends JIPipeEnvironment>> environments) {
        this.environments = environments;
    }

    @Override
    public boolean isUnstable() {
        return unstable;
    }

    public void setUnstable(boolean unstable) {
        this.unstable = unstable;
    }

    @Override
    public JIPipeNodeClassification getNodeClassification() {
        return nodeClassification;
    }

    public void setNodeClassification(JIPipeNodeClassification nodeClassification) {
        this.nodeClassification = nodeClassification;
    }

    @Override
    public JIPipeNodeTaxonomyFunction getPrimaryFunction() {
        return primaryFunction;
    }

    /**
     * Sets the primary taxonomy function of this node
     *
     * @param primaryFunction the primary function, or null if the node has none
     */
    public void setPrimaryFunction(JIPipeNodeTaxonomyFunction primaryFunction) {
        this.primaryFunction = primaryFunction;
    }

    @Override
    public Set<JIPipeNodeTaxonomyFunction> getSecondaryFunctions() {
        return secondaryFunctions;
    }

    /**
     * Sets the secondary taxonomy functions of this node
     *
     * @param secondaryFunctions the secondary functions
     */
    public void setSecondaryFunctions(Set<JIPipeNodeTaxonomyFunction> secondaryFunctions) {
        this.secondaryFunctions = secondaryFunctions;
    }

    @Override
    public Set<JIPipeNodeTaxonomySpatialDimension> getSpatialDimensions() {
        return spatialDimensions;
    }

    /**
     * Sets the spatial dimensions this node operates in
     *
     * @param spatialDimensions the declared spatial dimensions
     */
    public void setSpatialDimensions(Set<JIPipeNodeTaxonomySpatialDimension> spatialDimensions) {
        this.spatialDimensions = spatialDimensions;
    }

    @Override
    public JIPipeNodeTaxonomyFramework getFramework() {
        return framework;
    }

    /**
     * Sets the framework this node implements
     *
     * @param framework the framework, or {@link NoFramework} if the node declares none
     */
    public void setFramework(JIPipeNodeTaxonomyFramework framework) {
        this.framework = framework;
    }

    @Override
    public Set<String> getTaxonomyTags() {
        return taxonomyTags;
    }

    /**
     * Sets the node-level taxonomy tags of this node
     *
     * @param taxonomyTags the node-level tags
     */
    public void setTaxonomyTags(Set<String> taxonomyTags) {
        this.taxonomyTags = taxonomyTags;
    }

    @Override
    public Set<String> getMergedTags() {
        return mergedTags;
    }

    /**
     * Sets the merged taxonomy tags of this node
     * (node-level tags plus the tags of every taxonomy function the node appears under)
     *
     * @param mergedTags the merged tags
     */
    public void setMergedTags(Set<String> mergedTags) {
        this.mergedTags = mergedTags;
    }

    @Override
    public JIPipeNodeTaxonomyInputProfile getInputProfile() {
        return inputProfile;
    }

    /**
     * Sets the derived input profile of this node
     *
     * @param inputProfile the input profile
     */
    public void setInputProfile(JIPipeNodeTaxonomyInputProfile inputProfile) {
        this.inputProfile = inputProfile;
    }
}
