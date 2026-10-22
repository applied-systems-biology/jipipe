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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation for repeating {@link JIPipeNodeTaxonomyTags} on one
 * node class.
 * <p>
 * Java's {@link java.lang.annotation.Repeatable @Repeatable} mechanism
 * collects repeated tags into this container; callers should read them via
 * {@code Class#getAnnotationsByType(JIPipeNodeTaxonomyTags.class)}, which
 * transparently unwraps this container.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JIPipeNodeTaxonomyTagsContainer {

    /**
     * @return the repeated taxonomy tag annotations
     */
    JIPipeNodeTaxonomyTags[] value();
}
