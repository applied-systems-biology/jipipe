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

import javax.swing.*;
import java.util.List;
import java.util.Set;

/**
 * A function in the node taxonomy: a hierarchical category that groups
 * algorithm nodes, from top-level groups down to individual functions.
 * <p>
 * Functions form a forest: every function has at most one
 * {@link #getParent() parent}; functions without a parent are top-level.
 * <p>
 * The {@link #getColorHue() color hue} is defined by top-level functions only.
 * A top-level function declares a {@link #getHueCategory() hue category} and
 * gets a deterministic in-cell hue from the default {@link #getColorHue()}
 * (see {@link TaxonomyHue#autoHue(HueCategory, String)}); without a category
 * it is {@code null}, i.e. <em>transparent</em> (rendered with the theme's
 * panel background). An explicit {@link #getColorHue()} override always wins
 * over the category default. Sub-branches always return {@code null} and
 * inherit the hue of their nearest ancestor that defines one;
 * {@link FunctionRegistry#baseHue(JIPipeNodeTaxonomyFunction)}
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
     * Conceptual hue family used by the deterministic auto-picker when this
     * function is top-level and does not override {@link #getColorHue()}.
     * Null category = transparent (no auto hue).
     *
     * @return the hue category of this function, or {@code null}
     */
    @Nullable
    default HueCategory getHueCategory() {
        return null;
    }

    /**
     * The base hue of this function in HSB hue units ({@code 0..1}),
     * or {@code null} if it does not define one.
     * <p>
     * By default: top-level functions with a
     * {@link #getHueCategory() hue category} get a deterministic in-cell hue
     * ({@link TaxonomyHue#autoHue(HueCategory, String)}); functions without a
     * category are {@code null} (transparent); sub-branches are always
     * {@code null} (they inherit + depth-shade from their nearest ancestor).
     * Override this method for a designed hue; an explicit override always
     * wins over the category default.
     *
     * @return the hue in {@code 0..1}, or {@code null}
     */
    @Nullable
    default Float getColorHue() {
        if (getParent() != null || getHueCategory() == null) return null;
        return TaxonomyHue.autoHue(getHueCategory(), getId());
    }

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
     * Node behavior flag read by the UI and graph operations via `TaxonomyView.behaviorFlags`.
     *
     * @return whether instances of this function can be extracted from a project
     */
    default boolean canExtract() {
        return true;
    }

    /**
     * Node behavior flag read by the UI and graph operations via `TaxonomyView.behaviorFlags`.
     *
     * @return whether instances of this function are runnable
     */
    default boolean isRunnable() {
        return true;
    }

    /**
     * Name-inference patterns of this function: weighted keywords matched as
     * case-insensitive substrings of a discovered node's display name. The
     * aggregate score is the sum of the weights of all matched keywords; the
     * function with the highest total wins. Functions that declare no patterns
     * cannot be inferred into. Empty by default.
     *
     * @return the inference patterns of this function; never {@code null}
     */
    default List<JIPipeNodeTaxonomyInferencePattern> getInferencePatterns() {
        return List.of();
    }
}
