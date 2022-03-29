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

package org.hkijena.jipipe.api.nodes;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Describes an {@link JIPipeGraphNode}
 */
public interface JIPipeNodeInfo {

    /**
     * Gets the registered algorithms, grouped by their menu paths
     *
     * @param infos The infos to group
     * @return Map from menu path to algorithms with this menu path
     */
    static Map<String, Set<JIPipeNodeInfo>> groupByMenuPaths(Set<JIPipeNodeInfo> infos) {
        Map<String, Set<JIPipeNodeInfo>> result = new HashMap<>();
        for (JIPipeNodeInfo info : infos) {
            String menuPath = StringUtils.getCleanedMenuPath(info.getMenuPath());
            Set<JIPipeNodeInfo> group = result.getOrDefault(menuPath, null);
            if (group == null) {
                group = new HashSet<>();
                result.put(menuPath, group);
            }
            group.add(info);
        }

        return result;
    }

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
     * Gets the menu path of this algorithm
     *
     * @return String containing menu entries separated by new-lines
     */
    String getMenuPath();

    /**
     * Returns the algorithm category
     *
     * @return The category
     */
    JIPipeNodeTypeCategory getCategory();

    /**
     * Returns general input data.
     * Instances can have a configuration that is different from this configuration.
     *
     * @return List of input slots
     */
    List<JIPipeInputSlot> getInputSlots();

    /**
     * Returns general output data.
     * Instances can have a configuration that is different from this configuration.
     *
     * @return List of output slots
     */
    List<JIPipeOutputSlot> getOutputSlots();

    /**
     * If the current node info contains a slot with given name
     *
     * @param slotName the slot name
     * @return if there is an input slot with the name
     */
    default boolean hasInputSlot(String slotName) {
        return getInputSlots().stream().anyMatch(slot -> Objects.equals(slotName, slot.slotName()));
    }

    /**
     * If the current node info contains a slot with given name
     *
     * @param slotName the slot name
     * @return if there is an input slot with the name
     */
    default boolean hasOutputSlot(String slotName) {
        return getOutputSlots().stream().anyMatch(slot -> Objects.equals(slotName, slot.slotName()));
    }

    /**
     * Returns all dependencies
     *
     * @return List of dependencies
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
}
