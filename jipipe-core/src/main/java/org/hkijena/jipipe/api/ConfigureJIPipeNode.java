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

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeEmptyData;
import org.hkijena.jipipe.api.nodes.JIPipeNodeClassification;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension;
import org.hkijena.jipipe.api.nodes.taxonomy.NoFramework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotations used for organizing elements
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ConfigureJIPipeNode {
    /**
     * Only used for data source nodes (their function is under the Data sources branch).
     * Allows to re-assign the data source node into the menu of another data type
     * If set to {@link JIPipeEmptyData}, no re-allocation is applied.
     */
    Class<? extends JIPipeData> dataSourceMenuLocation() default JIPipeEmptyData.class;

    /**
     * Classification of the node that affects search ranking.
     * Standard nodes are shown at full rank, AutoImport nodes are downranked,
     * and EdgeCase nodes are further downranked.
     *
     * @return the node classification
     */
    JIPipeNodeClassification nodeClassification() default JIPipeNodeClassification.Standard;

    /**
     * The taxonomy functions this node appears under.
     * The first entry is the node's primary function; any further entries are
     * secondary locations where the node is additionally listed.
     *
     * @return the functions, in order; empty if the node is not yet assigned to the taxonomy
     */
    Class<? extends JIPipeNodeTaxonomyFunction>[] function() default {};

    /**
     * The spatial dimensions this node operates in.
     *
     * @return the declared spatial dimensions; empty if none are declared
     */
    JIPipeNodeTaxonomySpatialDimension[] spatialDimensions() default {};

    /**
     * The framework (external software or research project) this node implements.
     *
     * @return the framework; defaults to {@link NoFramework} when not set
     */
    Class<? extends JIPipeNodeTaxonomyFramework> framework() default NoFramework.class;
}
