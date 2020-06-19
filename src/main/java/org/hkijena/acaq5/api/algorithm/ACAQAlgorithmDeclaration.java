package org.hkijena.acaq5.api.algorithm;

import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.utils.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Describes an {@link ACAQGraphNode}
 */
public interface ACAQAlgorithmDeclaration {

    /**
     * Gets the registered algorithms, grouped by their menu paths
     *
     * @param declarations The declarations to group
     * @return Map from menu path to algorithms with this menu path
     */
    static Map<String, Set<ACAQAlgorithmDeclaration>> groupByMenuPaths(Set<ACAQAlgorithmDeclaration> declarations) {
        Map<String, Set<ACAQAlgorithmDeclaration>> result = new HashMap<>();
        for (ACAQAlgorithmDeclaration declaration : declarations) {
            String menuPath = StringUtils.getCleanedMenuPath(declaration.getMenuPath());
            Set<ACAQAlgorithmDeclaration> group = result.getOrDefault(menuPath, null);
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
    static List<ACAQAlgorithmDeclaration> getSortedList(Set<ACAQAlgorithmDeclaration> entries) {
        return entries.stream().sorted(Comparator.comparing(ACAQAlgorithmDeclaration::getName)).collect(Collectors.toList());
    }

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
    Class<? extends ACAQGraphNode> getAlgorithmClass();

    /**
     * Creates a new algorithm instance
     *
     * @return Algorithm instance
     */
    ACAQGraphNode newInstance();

    /**
     * Copies an existing algorithm instance
     *
     * @param algorithm Original algorithm instance
     * @return Copied algorithm instance
     */
    ACAQGraphNode clone(ACAQGraphNode algorithm);

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
    ACAQAlgorithmCategory getCategory();

    /**
     * Returns general input data.
     * Instances can have a configuration that is different from this configuration.
     *
     * @return List of input slots
     */
    List<AlgorithmInputSlot> getInputSlots();

    /**
     * Returns general output data.
     * Instances can have a configuration that is different from this configuration.
     *
     * @return List of output slots
     */
    List<AlgorithmOutputSlot> getOutputSlots();

    /**
     * Returns all dependencies
     *
     * @return List of dependencies
     */
    Set<ACAQDependency> getDependencies();

    /**
     * Returns true if this algorithm should not appear in the list of available algorithms.
     * This is useful if it is a structural algorithm
     *
     * @return if this algorithm should not appear in the list of available algorithms
     */
    boolean isHidden();
}
