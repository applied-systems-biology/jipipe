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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a node type as a step in one or more workflow families.
 * <p>
 * The annotation is {@linkplain Repeatable repeatable} so that one node
 * class can take part in several families (e.g. a "filter tracks" node
 * that is a step in both the TrackMate and the simple-model families);
 * the annotations are collected into
 * {@link JIPipeNodeTaxonomyFamilyStepAnnotations}.
 * <p>
 * A step without {@link #after()} entries is a <em>start step</em>:
 * nothing has to run before it. Otherwise the listed predecessor node
 * classes are resolved to node ids <em>lazily at query time</em> (see
 * {@link FamilyAssembler#resolveNodeIds(Class)}) against the mapping in
 * effect then, and each resolved node id contributes a directed edge
 * predecessor → this node to the family's workflow graph. A class behind
 * several registered node ids (a shared factory class of generated
 * entries) contributes one edge per node id.
 *
 * @see JIPipeNodeTaxonomyFamily
 * @see FamilyAssembler
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(JIPipeNodeTaxonomyFamilyStepAnnotations.class)
public @interface JIPipeNodeTaxonomyFamilyStepAnnotation {

    /**
     * @return the family this step belongs to
     */
    Class<? extends JIPipeNodeTaxonomyFamily> family();

    /**
     * @return the display label of the step in the workflow
     */
    String label();

    /**
     * @return the predecessor node classes of this step; an empty array
     * marks a start step
     */
    Class<?>[] after() default {};

    /**
     * @return a short description of why this step is part of the workflow;
     * may be empty
     */
    String purpose() default "";
}
