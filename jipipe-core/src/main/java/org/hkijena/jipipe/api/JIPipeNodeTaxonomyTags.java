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

package org.hkijena.jipipe.api;

import java.lang.annotation.*;

/**
 * Declares node-level search tags for the taxonomy of an annotated node class.
 * <p>
 * This annotation is {@link Repeatable}: several {@code @JIPipeNodeTaxonomyTags}
 * may be applied to one node class. Repeated instances are collected into the
 * {@link JIPipeNodeTaxonomyTagsContainer} container; read all tags via
 * {@code Class#getAnnotationsByType(JIPipeNodeTaxonomyTags.class)}, which
 * transparently unwraps that container.
 * <p>
 * These tags are node-specific. They are distinct from the tags that belong
 * to taxonomy functions; both are merged when a node is registered.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(JIPipeNodeTaxonomyTagsContainer.class)
public @interface JIPipeNodeTaxonomyTags {

    /**
     * @return the tags for this node
     */
    String[] value();
}
