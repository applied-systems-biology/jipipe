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
 * The Python framework: nodes that run, define or import Python scripts
 * (both the CPython and the Jython runtime - same language, so both are
 * tagged for users filtering for python-related nodes).
 */
public final class PythonFramework implements JIPipeNodeTaxonomyFramework {

    /**
     * The stable registry id of this framework (a string literal per the stable-ID rule).
     */
    public static final String ID = "jipipe:framework:python";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "Python";
    }

    @Override
    public String getAbbreviation() {
        return "PY";
    }

    @Override
    public String getPatternStyleId() {
        return "stripes";
    }
}
