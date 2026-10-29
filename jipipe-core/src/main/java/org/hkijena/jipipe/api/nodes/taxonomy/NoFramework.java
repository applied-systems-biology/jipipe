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

/**
 * Sentinel {@link JIPipeNodeTaxonomyFramework} used when a node declares no
 * external framework. It is the default for {@link
 * org.hkijena.jipipe.api.ConfigureJIPipeNode#framework()}.
 * <p>
 * The name intentionally does not carry the {@code JIPipeNodeTaxonomy} prefix
 * because it is a short, well-known sentinel rather than a named taxonomy
 * member. Its display name and abbreviation are both a dash ("—") to render
 * as an empty slot in node labels.
 */
public class NoFramework implements JIPipeNodeTaxonomyFramework {

    /**
     * The stable id of the no-framework sentinel (see the stable-ID rule).
     */
    public static final String ID = "jipipe:framework:none";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "—";
    }

    @Override
    public String getAbbreviation() {
        return "—";
    }

    @Override
    public String getPatternStyleId() {
        return null;
    }
}
