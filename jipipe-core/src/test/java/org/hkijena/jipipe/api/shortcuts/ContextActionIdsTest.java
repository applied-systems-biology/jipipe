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

package org.hkijena.jipipe.api.shortcuts;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ContextActionIdsTest {

    @Test
    void everyCoreContextActionExposesARegisteredId() {
        for (JIPipeShortcutAction action : JIPipeShortcutActions.CORE_ACTIONS) {
            assertNotNull(action.id(), action.name());
            assertFalse(action.id().isBlank(), action.name());
        }
    }

    @Test
    void newNodeActionIdsAreRegisteredAfterRegisterCore() {
        JIPipeShortcutActions.registerCore();
        List<String> ids = List.of(
                "org.hkijena.jipipe.graph.node.save-outputs-enable",
                "org.hkijena.jipipe.graph.node.save-outputs-disable",
                "org.hkijena.jipipe.graph.node.virtual-outputs-enable",
                "org.hkijena.jipipe.graph.node.virtual-outputs-disable",
                "org.hkijena.jipipe.graph.run-and-show-intermediate-results",
                "org.hkijena.jipipe.graph.node.invert-selection",
                "org.hkijena.jipipe.graph.node.add-edge-control-point",
                "org.hkijena.jipipe.graph.node.group",
                "org.hkijena.jipipe.graph.node.collapse-io-interface",
                "org.hkijena.jipipe.graph.compartment.export-json",
                "org.hkijena.jipipe.graph.node.open-cache-browser",
                "org.hkijena.jipipe.graph.node.clear-cache",
                "org.hkijena.jipipe.graph.bookmark.add",
                "org.hkijena.jipipe.graph.bookmark.remove");
        for (String id : ids) {
            assertNotNull(JIPipeShortcutRegistry.getInstance().byId(id), id);
        }
    }
}
