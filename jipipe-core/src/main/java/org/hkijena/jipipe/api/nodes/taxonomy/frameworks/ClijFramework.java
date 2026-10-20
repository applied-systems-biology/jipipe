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

package org.hkijena.jipipe.api.nodes.taxonomy.frameworks;

import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFramework;

/**
 * The CLIJ GPU image-processing framework.
 */
public final class ClijFramework implements JIPipeNodeTaxonomyFramework {

    @Override
    public String getId() {
        return "jipipe:framework:clij";
    }

    @Override
    public String getName() {
        return "CLIJ";
    }

    @Override
    public String getAbbreviation() {
        return "CLIJ";
    }

    @Override
    public String getPatternStyleId() {
        return "honeycomb";
    }
}
