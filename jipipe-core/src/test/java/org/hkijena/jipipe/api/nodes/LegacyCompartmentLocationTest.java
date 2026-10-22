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

package org.hkijena.jipipe.api.nodes;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies that a node whose {@code jipipe:graph-compartment} holds a legacy
 * (non-UUID) name is demoted to the default compartment AND that its stored
 * UI location, which is keyed under that legacy name
 * ({@code location/<legacyName>/x|y}), is re-keyed to the default compartment
 * at load time so it remains resolvable (instead of stranding the node on
 * auto-placement).
 */
class LegacyCompartmentLocationTest {

    @BeforeAll
    static void boot() {
        JIPipe.ensureInstance();
    }

    @Test
    void legacyCompartmentLocationIsRekeyedToDefaultAtLoad() throws Exception {
        String json = "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"parameters-define\","
            + "\"jipipe:graph-compartment\":\"derived\","
            + "\"jipipe:metadata-v1\":{\"location/derived/x\":12,\"location/derived/y\":5}}},"
            + "\"edges\":[]}";
        JIPipeGraph graph = JsonUtils.readFromString(json, JIPipeGraph.class);
        JIPipeGraphNode node = graph.getGraphNodes().iterator().next();
        assertNull(graph.getCompartmentUUIDOf(node), "legacy name demotes to the default compartment");
        assertEquals(new Point(12, 5), node.getNodeUILocationWithin(""),
            "location must resolve under the default compartment key");
    }
}
