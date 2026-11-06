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

package org.hkijena.jipipe.api.progress;

import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the NDJSON wire contract of {@link JIPipeProgressRecord} (spec §Record types):
 * short keys, discriminator <code>t</code>, single-line records.
 */
class ProgressRecordJsonTest {

    @Test
    void headRecordSerializesWithShortKeys() {
        String json = JsonUtils.toJsonString(JIPipeProgressRecord.head(1, "run-uuid", "Pipeline run", "6.0.0", 123L));
        assertTrue(json.contains("\"t\":\"head\""), json);
        assertTrue(json.contains("\"run\":\"run-uuid\""), json);
        assertTrue(json.contains("\"v\":1"), json);
        assertTrue(json.contains("\"ts\":123"), json);
        assertFalse(json.contains("\n"), "NDJSON records must be single-line");
    }

    @Test
    void levelEnumValuesMatchSpec() {
        assertEquals(10, JIPipeLogLevel.DEBUG.value());
        assertEquals(20, JIPipeLogLevel.INFO.value());
        assertEquals(30, JIPipeLogLevel.WARN.value());
        assertEquals(40, JIPipeLogLevel.ERROR.value());
    }

    @Test
    void allRecordTypesEmitSpecDiscriminators() {
        assertEquals("head", JIPipeProgressRecord.head(1, "r", "l", "6.0.0", 0L).type());
        assertEquals("n", JIPipeProgressRecord.node(2, 1, "Node A", JIPipeProgressNodeKind.ITEM.name(), 0L).type());
        assertEquals("N", JIPipeProgressRecord.nodeFinished(2, "ok", 0L).type());
        assertEquals("l", JIPipeProgressRecord.log(2, JIPipeLogLevel.INFO.value(), "m", 0L).type());
        assertEquals("p", JIPipeProgressRecord.progress(2, 5, 100, 0L).type());
        assertEquals("e", JIPipeProgressRecord.error(2, "java.lang.RuntimeException", "boom", null, 0L).type());
        assertEquals("s", JIPipeProgressRecord.status("running", 0L).type());
        assertEquals("w", JIPipeProgressRecord.notification("h", "d", 0L).type());
    }

    @Test
    void nodeRecordCarriesOptionalLocation() throws IOException {
        // Location URIs are optional (spec §Record types): present when tagged, absent otherwise
        JIPipeProgressRecord.Node withLocation = (JIPipeProgressRecord.Node) JIPipeProgressRecord.node(2, 0, "Node A", JIPipeProgressNodeKind.ITEM.name(), "node://abc", 0L);
        assertEquals("node://abc", withLocation.loc());
        String json = JsonUtils.toJsonString(withLocation);
        assertTrue(json.contains("\"loc\":\"node://abc\""), json);
        assertFalse(json.contains("\n"), "NDJSON records must be single-line");

        // Absent location: the field is omitted (old readers without the field keep working,
        // new readers see null)
        JIPipeProgressRecord.Node withoutLocation = (JIPipeProgressRecord.Node) JIPipeProgressRecord.node(2, 0, "Node A", JIPipeProgressNodeKind.ITEM.name(), 0L);
        assertNull(withoutLocation.loc());
        String plainJson = JsonUtils.toJsonString(withoutLocation);
        assertFalse(plainJson.contains("loc"), plainJson);

        // A line without the loc field deserializes to null (old files)
        JIPipeProgressRecord.Node readBack = JsonUtils.getObjectMapper().readValue(plainJson, JIPipeProgressRecord.Node.class);
        assertNull(readBack.loc());
        assertEquals("Node A", readBack.l());
    }
}
