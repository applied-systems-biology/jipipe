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

package org.hkijena.jipipe.plugins.parameters.library.taxonomy;

import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JIPipeTaxonomyFunctionRefTest {
    @Test
    void serializesToIdString() throws Exception {
        String json = JsonUtils.toPrettyJsonString(new JIPipeTaxonomyFunctionRef("jipipe:function:images"));
        assertThat(json).contains("jipipe:function:images");
    }

    @Test
    void deserializesFromIdString() throws Exception {
        JIPipeTaxonomyFunctionRef ref =
                JsonUtils.readFromString("\"jipipe:function:tables\"", JIPipeTaxonomyFunctionRef.class);
        assertThat(ref.getId()).isEqualTo("jipipe:function:tables");
    }

    @Test
    void nullRoundTripsToUnset() {
        JIPipeTaxonomyFunctionRef ref = JsonUtils.readFromString("null", JIPipeTaxonomyFunctionRef.class);
        assertThat(ref.getId()).isNull();
        assertThat(ref.isResolved()).isFalse();
    }

    @Test
    void unresolvedIdDoesNotThrow() {
        JIPipeTaxonomyFunctionRef ref = new JIPipeTaxonomyFunctionRef("jipipe:function:does-not-exist");
        assertThat(ref.isResolved()).isFalse();   // registry not booted in a unit test → unresolved
        assertThat(ref.getFunction()).isNull();
    }

    @Test
    void equalsAndHashCodeOnId() {
        assertThat(new JIPipeTaxonomyFunctionRef("x")).isEqualTo(new JIPipeTaxonomyFunctionRef("x"));
        assertThat(new JIPipeTaxonomyFunctionRef("x")).isNotEqualTo(new JIPipeTaxonomyFunctionRef("y"));
    }
}
