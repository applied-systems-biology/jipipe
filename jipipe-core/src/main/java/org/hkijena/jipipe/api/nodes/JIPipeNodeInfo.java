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
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
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
     * @param category the targeted category
     * @param infos    The infos to group
     * @return Map from menu path to algorithms with this menu path
     */
    static Map<String, Set<JIPipeNodeInfo>> groupByMenuPaths(JIPipeNodeTypeCategory category, Set<JIPipeNodeInfo> infos) {
        Map<String, Set<JIPipeNodeInfo>> result = new HashMap<>();
        for (JIPipeNodeInfo info : infos) {
            if (Objects.equals(info.getCategory().getId(), category.getId())) {
                String menuPath = StringUtils.getCleanedMenuPath(info.getMenuPath());
                Set<JIPipeNodeInfo> group = result.getOrDefault(menuPath, null);
                if (group == null) {
                    group = new HashSet<>();
                    result.put(menuPath, group);
                }
                group.add(info);
            }
            for (JIPipeNodeMenuLocation location : info.getAliases()) {
                if (Objects.equals(location.getCategory().getId(), category.getId())) {
                    String menuPath = StringUtils.getCleanedMenuPath(location.getMenuPath());
                    Set<JIPipeNodeInfo> group = result.getOrDefault(menuPath, null);
                    if (group == null) {
                        group = new HashSet<>();
                        result.put(menuPath, group);
                    }
                    group.add(info);
                }
            }
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
     * If the node is of category {@link org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory}, allows to re-assign them to a new menu
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
    List<AddJIPipeInputSlot> getInputSlots();

    /**
     * Returns general output data.
     * Instances can have a configuration that is different from this configuration.
     *
     * @return List of output slots
     */
    List<AddJIPipeOutputSlot> getOutputSlots();

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
     * A menu location that points towards the primary menu
     *
     * @return the primary menu location
     */
    default JIPipeNodeMenuLocation getPrimaryMenuLocation() {
        return new JIPipeNodeMenuLocation(getCategory(), getMenuPath(), "");
    }

    /**
     * A list of alternative menu locations
     *
     * @return alternative menu locations. might be empty.
     */
    default List<JIPipeNodeMenuLocation> getAliases() {
        return Collections.emptyList();
    }
}
