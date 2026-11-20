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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the preferred replacement nodes of a deprecated node.
 * <p>
 * Put this annotation on a node class that is also annotated with
 * {@link Deprecated}. The node ids are stable string literals exactly as
 * passed to {@code registerNodeType}: they are persisted in saved projects,
 * so they must never be derived from code.
 * <p>
 * The {@link #value()} lists the node ids of the preferred replacements,
 * ordered from most to least preferred. An empty list means the node has no
 * direct replacement; state why in {@link #reason()} so users get guidance
 * anyway.
 * <p>
 * Every declared id must resolve to a registered node at runtime: unknown
 * ids fail the launcher registry gate
 * ({@code NodeSuccessorRegistryTest}), which runs over the entire node
 * registry.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface JIPipeNodeSuccessor {

    /**
     * The node ids (stable string literals) of the preferred replacements,
     * ordered. Empty when the node has no replacement; state the reason in
     * {@link #reason()}.
     *
     * @return the successor node ids
     */
    String[] value() default {};

    /**
     * Why the node is deprecated and what users should do instead. An empty
     * or blank reason is treated as no reason.
     *
     * @return the reason
     */
    String reason() default "";
}
