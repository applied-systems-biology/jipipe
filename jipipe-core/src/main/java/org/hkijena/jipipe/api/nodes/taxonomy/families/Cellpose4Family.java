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

package org.hkijena.jipipe.api.nodes.taxonomy.families;

import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFamily;

import java.util.Collections;
import java.util.Set;

/**
 * The Cellpose 4 workflow family: segmenting cells from microscopy
 * images with the Cellpose 4 deep-learning model.
 * <p>
 * Steps are not annotated yet; the family's step annotations and
 * template/example wiring are planned for later phases.
 */
public final class Cellpose4Family implements JIPipeNodeTaxonomyFamily {

    /**
     * The stable id of this family; used verbatim in step ids.
     */
    public static final String ID = "jipipe:family:cellpose4";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "Cellpose 4";
    }

    @Override
    public String getDescription() {
        return "Segment cells from microscopy images using the Cellpose 4 deep-learning model.";
    }

    /**
     * @return the tags of this family, currently none
     */
    @Override
    public Set<String> getTags() {
        return Collections.emptySet();
    }
}
