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

package org.hkijena.jipipe.plugins.parameters.api.options;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.plugins.parameters.library.primitives.DynamicStringSetParameter;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * #1339: the dynamic set parameter's persisted {@code collapsed} field was
 * removed (the editor no longer collapses). Old project files still contain the
 * key; they must load without error and the key must not be re-serialized.
 */
class DynamicSetParameterJsonTest {

    @Test
    void legacyCollapsedKeyIsIgnoredAndNotReserialized() throws Exception {
        String json = "{\"values\":[\"a\",\"b\"],\"collapsed\":true}";
        DynamicStringSetParameter p =
                JsonUtils.getObjectMapper().readerFor(DynamicStringSetParameter.class).readValue(json);

        // Values load fine
        assertEquals(2, p.getValues().size());
        assertTrue(p.getValues().contains("a"));

        // The legacy key is not re-serialized
        String reser = JsonUtils.getObjectMapper().writeValueAsString(p);
        JsonNode node = JsonUtils.getObjectMapper().readTree(reser);
        assertFalse(node.has("collapsed"), "legacy 'collapsed' key must not be written back: " + reser);
        assertTrue(node.has("values"));
    }
}
