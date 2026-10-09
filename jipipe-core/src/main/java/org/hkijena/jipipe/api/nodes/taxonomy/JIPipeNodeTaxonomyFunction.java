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

package org.hkijena.jipipe.api.nodes.taxonomy;

import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Set;

/**
 * A function in the node taxonomy: a hierarchical category that groups
 * algorithm nodes, from top-level groups down to individual functions.
 * <p>
 * Functions form a forest: every function has at most one
 * {@link #getParent() parent}; functions without a parent are top-level.
 * <p>
 * The {@link #getColorHue() color hue} is defined by top-level functions only.
 * Sub-branches return {@code null} and inherit the hue of their nearest
 * ancestor that defines one; {@link FunctionRegistry#baseHue(JIPipeNodeTaxonomyFunction)}
 * resolves it, and {@link FunctionRegistry#shadeHsb(float, float, float, int)}
 * shades it by the tree depth below that ancestor.
 */
public interface JIPipeNodeTaxonomyFunction {

    /**
     * @return the stable, unique identifier of this function
     */
    String getId();

    /**
     * @return the display name of this function
     */
    String getName();

    /**
     * @return the parent function, or {@code null} if this is a top-level function
     */
    @Nullable
    JIPipeNodeTaxonomyFunction getParent();

    /**
     * @return the base hue of this function in HSB hue units ({@code 0..1}),
     * or {@code null} if this function is a sub-branch that inherits the hue
     * from its nearest ancestor
     */
    @Nullable
    Float getColorHue();

    /**
     * @return a human-readable description of this function
     */
    String getDescription();

    /**
     * @return the icon of this function, or {@code null} if it has none
     */
    @Nullable
    Icon getIcon();

    /**
     * @return the search tags associated with this function
     */
    Set<String> getTags();

    /**
     * @return whether this function is shown in the pipeline
     */
    boolean isVisibleInPipeline();

    /**
     * @return whether this function is shown in the compartment view
     */
    boolean isVisibleInCompartments();

    /**
     * Carried over from the node registration contract.
     *
     * @return whether instances of this function can be extracted from a project
     */
    default boolean canExtract() {
        return true;
    }

    /**
     * Carried over from the node registration contract.
     *
     * @return whether users can create instances of this function
     */
    default boolean userCanCreate() {
        return true;
    }

    /**
     * Carried over from the node registration contract.
     *
     * @return whether users can delete instances of this function
     */
    default boolean userCanDelete() {
        return true;
    }

    /**
     * Carried over from the node registration contract.
     *
     * @return whether instances of this function are runnable
     */
    default boolean isRunnable() {
        return true;
    }
}
