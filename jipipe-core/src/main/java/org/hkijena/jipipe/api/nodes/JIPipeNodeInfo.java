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

package org.hkijena.jipipe.api.nodes;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeEmptyData;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.nodes.taxonomy.*;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;

import javax.swing.*;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Describes an {@link JIPipeGraphNode}
 */
public interface JIPipeNodeInfo {

    /**
     * Gets a sorted list of algorithms
     *
     * @param entries the algorithms to sort
     * @return sorted list
     */
    static List<JIPipeNodeInfo> getSortedList(Set<JIPipeNodeInfo> entries) {
        return entries.stream().sorted(Comparator.comparing(JIPipeNodeInfo::getName)).collect(Collectors.toList());
    }

    /**
     * Generates an Id for this info
     *
     * @return The ID
     */
    String getId();

    /**
     * If the node is a data source (its function is under the Data sources branch), allows to re-assign them to a new menu
     * If {@link JIPipeEmptyData} is returned, no re-assignment is applied
     *
     * @return the re-assigned data type menu or {@link JIPipeEmptyData}
     */
    default Class<? extends JIPipeData> getDataSourceMenuLocation() {
        return JIPipeEmptyData.class;
    }

    /**
     * The algorithm class that is generated
     *
     * @return The algorithm class
     */
    Class<? extends JIPipeGraphNode> getInstanceClass();

    /**
     * Creates a new node type instance
     *
     * @return Algorithm instance
     */
    JIPipeGraphNode newInstance();

    /**
     * Copies an existing algorithm instance
     *
     * @param algorithm Original algorithm instance
     * @return Copied algorithm instance
     */
    JIPipeGraphNode duplicate(JIPipeGraphNode algorithm);

    /**
     * Returns the algorithm name
     *
     * @return The name
     */
    String getName();

    /**
     * Returns the algorithm description
     *
     * @return The description
     */
    HTMLText getDescription();

    /**
     * Returns general input data.
     * Instances can have a configuration that is different from this configuration.
     *
     * @return List of input slots
     */
    List<AddJIPipeInputSlot> getInputSlots();

    /**
     * Returns general output data.
     * Instances can have a configuration that is different from this configuration.
     *
     * @return List of output slots
     */
    List<AddJIPipeOutputSlot> getOutputSlots();

    /**
     * Returns the utilized environments of this node
     * This may be extended by the node itself during initialization
     *
     * @return the set of utilized environments
     */
    Set<Class<? extends JIPipeEnvironment>> getEnvironments();

    /**
     * If the current node info contains a slot with given name
     *
     * @param slotName the slot name
     * @return if there is an input slot with the name
     */
    default boolean hasInputSlot(String slotName) {
        return getInputSlots().stream().anyMatch(slot -> Objects.equals(slotName, slot.name()));
    }

    /**
     * If the current node info contains a slot with given name
     *
     * @param slotName the slot name
     * @return if there is an input slot with the name
     */
    default boolean hasOutputSlot(String slotName) {
        return getOutputSlots().stream().anyMatch(slot -> Objects.equals(slotName, slot.name()));
    }

    /**
     * Returns all dependencies
     *
     * @return set of dependencies
     */
    Set<JIPipeDependency> getDependencies();

    /**
     * Returns a list of additional citations
     *
     * @return additonal citations
     */
    default List<String> getAdditionalCitations() {
        return Collections.emptyList();
    }

    /**
     * Returns true if this algorithm should not appear in the list of available algorithms.
     * This is useful if it is a structural algorithm
     *
     * @return if this algorithm should not appear in the list of available algorithms
     */
    boolean isHidden();

    /**
     * Returns true if the node is marked as unstable
     *
     * @return if the node is considered unstable
     */
    default boolean isUnstable() {
        return false;
    }

    /**
     * Returns true if this node is deprecated
     *
     * @return if the node is deprecated
     */
    default boolean isDeprecated() {
        return false;
    }

    /**
     * Indicates that the node should carry a workload. Defaults to true.
     *
     * @return if the node carries a workload
     */
    default boolean isRunnable() {
        return true;
    }

    /**
     * Gets the icon for this node info
     *
     * @return the icon
     */
    default Icon getIcon() {
        return JIPipe.getNodes().getIconFor(this);
    }

    /**
     * Gets the icon for this node info
     *
     * @return the icon
     */
    default URL getIconURL() {
        return JIPipe.getNodes().getIconURLFor(this);
    }

    /**
     * Returns the classification of this node, which affects search ranking.
     * Standard nodes are shown at full rank, AutoImport nodes are downranked,
     * and EdgeCase nodes are further downranked.
     *
     * @return the node classification
     */
    default JIPipeNodeClassification getNodeClassification() {
        return JIPipeNodeClassification.Standard;
    }

    // --- Node taxonomy ---

    /**
     * The primary function of this node in the node taxonomy,
     * i.e. the hierarchical function under which the node is primarily
     * grouped.
     * <p>
     * The default implementation returns {@code null} for node infos that
     * are not backed by {@link JIPipeMutableNodeInfo}, meaning the node is
     * not (yet) assigned to a taxonomy function.
     *
     * @return the primary function, or {@code null} if the node has none
     */
    default JIPipeNodeTaxonomyFunction getPrimaryFunction() {
        return null;
    }

    /**
     * The secondary functions of this node in the node taxonomy:
     * additional hierarchical functions under which the node is listed
     * besides its primary function.
     *
     * @return the secondary functions, possibly empty
     */
    default Set<JIPipeNodeTaxonomyFunction> getSecondaryFunctions() {
        return Collections.emptySet();
    }

    /**
     * The spatial dimensions this node operates in.
     *
     * @return the declared spatial dimensions, possibly empty
     */
    default Set<JIPipeNodeTaxonomySpatialDimension> getSpatialDimensions() {
        return Collections.emptySet();
    }

    /**
     * The framework (external software or research project) this node implements.
     *
     * @return the framework, or {@link NoFramework} if the node declares none
     */
    default JIPipeNodeTaxonomyFramework getFramework() {
        return new NoFramework();
    }

    /**
     * The node-level taxonomy tags of this node, declared via
     * {@code @JIPipeNodeTaxonomyTags}.
     * <p>
     * These are distinct from the tags of the taxonomy functions the node
     * appears under; both are combined in {@link #getMergedTags()}.
     *
     * @return the node-level tags, possibly empty
     */
    default Set<String> getTaxonomyTags() {
        return Collections.emptySet();
    }

    /**
     * The merged taxonomy tags of this node: the node-level tags
     * ({@link #getTaxonomyTags()}) plus the tags of every taxonomy function
     * the node appears under.
     *
     * @return the merged tags, possibly empty
     */
    default Set<String> getMergedTags() {
        return Collections.emptySet();
    }

    /**
     * The derived input profile of this node, describing what kind of data it consumes.
     *
     * @return the input profile; defaults to {@link JIPipeNodeTaxonomyInputProfile#NA}
     * when the profile cannot be determined
     */
    default JIPipeNodeTaxonomyInputProfile getInputProfile() {
        return JIPipeNodeTaxonomyInputProfile.NA;
    }
}
