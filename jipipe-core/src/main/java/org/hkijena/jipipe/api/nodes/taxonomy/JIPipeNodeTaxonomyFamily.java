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

import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.nodes.JIPipeNodeExample;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * A workflow family in the node taxonomy: a group of node types that
 * form a setup together, i.e. the steps a user performs in sequence to
 * carry out one workflow (e.g. TrackMate: detect spots → track spots
 * → filter tracks → analyze tracks).
 * <p>
 * A family declares its steps through
 * {@link JIPipeNodeTaxonomyFamilyStepAnnotation}s on the node classes
 * (see {@link FamilyAssembler}). Each family may be associated with a
 * ready-to-run {@link JIPipeNodeTemplate template} and a
 * {@link JIPipeNodeExample example} that demonstrate the workflow; both
 * are optional and default to {@code null}.
 */
public interface JIPipeNodeTaxonomyFamily {

    /**
     * @return the stable, unique identifier of this family
     */
    String getId();

    /**
     * @return the display name of this family
     */
    String getName();

    /**
     * @return the description of this family
     */
    String getDescription();

    /**
     * @return the template that sets up this family's workflow as a
     * ready-to-run subgraph, or {@code null} if the family has no template
     */
    @Nullable
    default JIPipeNodeTemplate getTemplate() {
        return null;
    }

    /**
     * @return the example that demonstrates this family's workflow,
     * or {@code null} if the family has no example
     */
    @Nullable
    default JIPipeNodeExample getExample() {
        return null;
    }

    /**
     * @return the tags of this family, used for searching and grouping
     */
    default Set<String> getTags() {
        return Set.of();
    }
}
