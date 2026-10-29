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
 * The TrackMate workflow family: detecting spots in a time series,
 * linking them into tracks, and filtering and analyzing the tracks.
 * <p>
 * The family's steps are declared as
 * {@link org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFamilyStepAnnotation}s
 * on the TrackMate node types.
 */
public final class TrackMateFamily implements JIPipeNodeTaxonomyFamily {

    /**
     * The stable id of this family; used verbatim in step ids.
     */
    public static final String ID = "jipipe:family:trackmate";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "TrackMate";
    }

    @Override
    public String getDescription() {
        return "Track objects through a time series with TrackMate: detect spots, track them, filter and analyze the resulting tracks.";
    }

    /**
     * @return the tags of this family, currently none
     */
    @Override
    public Set<String> getTags() {
        return Collections.emptySet();
    }
}
