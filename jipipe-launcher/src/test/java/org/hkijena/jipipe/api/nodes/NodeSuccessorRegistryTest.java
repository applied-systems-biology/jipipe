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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Registry-wide gate for the node successor API.
 * <p>
 * Lives in <b>jipipe-launcher</b>, the only module that aggregates all
 * {@code jipipe-plugin-*} modules plus {@code jipipe-core} on its classpath,
 * so after {@code JIPipe.ensureInstance()} every node of every plugin is
 * registered. Follows the registry-aware test design: presence assertions,
 * never exact counts (plugins may add nodes).
 */
class NodeSuccessorRegistryTest {

    @BeforeAll
    static void init() {
        // The ImageJLegacyPatcherLauncherSessionListener registered in this
        // module preinits the ImageJ1 legacy patcher before any test class
        // loads, so ensureInstance() here is safe. Idempotent.
        JIPipe.ensureInstance();
    }

    @Test
    void everyDeclaredSuccessorResolves() {
        // Presence, not exact counts: plugins may add nodes.
        Map<String, JIPipeNodeInfo> infos = JIPipe.getNodes().getRegisteredNodeInfos();
        for (JIPipeNodeInfo info : infos.values()) {
            for (String successorId : info.getSuccessorNodeIds()) {
                assertTrue(infos.containsKey(successorId),
                        "Node '" + info.getId() + "' declares successor '" + successorId
                                + "' which is not registered");
            }
        }
    }

    @Test
    void successorImpliesDeprecatedAndNeverSelfReferences() {
        for (JIPipeNodeInfo info : JIPipe.getNodes().getRegisteredNodeInfos().values()) {
            if (!info.getSuccessorNodeIds().isEmpty()) {
                assertTrue(info.isDeprecated(),
                        "Node '" + info.getId() + "' declares a successor but is not deprecated");
                assertFalse(info.getSuccessorNodeIds().contains(info.getId()),
                        "Node '" + info.getId() + "' lists itself as successor");
            }
        }
    }

    @Test
    void loopHelpersAreAnnotatedExplicitly() {
        // The known deprecated node classes carry the annotation with an empty
        // successor list, so the "deprecated but no successor" state is explicit
        // rather than defaulted.
        JIPipeNodeInfo loopStart = JIPipe.getNodes().getRegisteredNodeInfos().get("loop:start");
        if (loopStart != null) {
            assertNotNull(loopStart.getSuccessorReason(),
                    "LoopStartNode must carry an explicit successor note");
        }
    }
}
