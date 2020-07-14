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

import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Describes an {@link JIPipeGraphNode}
 */
public interface JIPipeAlgorithmDeclaration {

    /**
     * Generates an Id for this declaration
     *
     * @return The ID
     */
    String getId();

    /**
     * The algorithm class that is generated
     *
     * @return The algorithm class
     */
    Class<? extends JIPipeGraphNode> getAlgorithmClass();

    /**
     * Creates a new algorithm instance
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
    JIPipeGraphNode clone(JIPipeGraphNode algorithm);

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
    String getDescription();

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
    JIPipeAlgorithmCategory getCategory();

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
     * Returns all dependencies
     *
     * @return List of dependencies
     */
    Set<JIPipeDependency> getDependencies();

    /**
     * Returns true if this algorithm should not appear in the list of available algorithms.
     * This is useful if it is a structural algorithm
     *
     * @return if this algorithm should not appear in the list of available algorithms
     */
    boolean isHidden();

    /**
     * Gets the registered algorithms, grouped by their menu paths
     *
     * @param declarations The declarations to group
     * @return Map from menu path to algorithms with this menu path
     */
    static Map<String, Set<JIPipeAlgorithmDeclaration>> groupByMenuPaths(Set<JIPipeAlgorithmDeclaration> declarations) {
        Map<String, Set<JIPipeAlgorithmDeclaration>> result = new HashMap<>();
        for (JIPipeAlgorithmDeclaration declaration : declarations) {
            String menuPath = StringUtils.getCleanedMenuPath(declaration.getMenuPath());
            Set<JIPipeAlgorithmDeclaration> group = result.getOrDefault(menuPath, null);
            if (group == null) {
                group = new HashSet<>();
                result.put(menuPath, group);
            }
            group.add(declaration);
        }

        return result;
    }

    /**
     * Gets a sorted list of algorithms
     *
     * @param entries the algorithms to sort
     * @return sorted list
     */
    static List<JIPipeAlgorithmDeclaration> getSortedList(Set<JIPipeAlgorithmDeclaration> entries) {
        return entries.stream().sorted(Comparator.comparing(JIPipeAlgorithmDeclaration::getName)).collect(Collectors.toList());
    }
}
