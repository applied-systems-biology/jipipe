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

import java.lang.annotation.*;

/**
 * Container annotation for repeating
 * {@link JIPipeNodeTaxonomyFamilyStepAnnotation}s on one node class.
 * Java's {@link java.lang.annotation.Repeatable @Repeatable} mechanism
 * collects repeated annotations into this container; callers should
 * read the steps via
 * {@code Class#getAnnotationsByType(JIPipeNodeTaxonomyFamilyStepAnnotation.class)},
 * which transparently unwraps this container.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JIPipeNodeTaxonomyFamilyStepAnnotations {

    /**
     * @return the repeated family step annotations
     */
    JIPipeNodeTaxonomyFamilyStepAnnotation[] value();
}
