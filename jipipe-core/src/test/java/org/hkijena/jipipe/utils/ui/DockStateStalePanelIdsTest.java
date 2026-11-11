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

package org.hkijena.jipipe.utils.ui;

import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Dock states persisted by older JIPipe versions can contain panel IDs that
 * are no longer registered, for example the removed "MAP" and "BOOKMARKS"
 * docks of the graph editors. The stored state is just data: deserialization
 * must not reject stale entries, and the runtime ignores panel IDs that have
 * no registered dock panel.
 */
class DockStateStalePanelIdsTest {

    @Test
    void stalePanelIdsAreKeptAsData() throws Exception {
        String json = "{\"visibilities\":{\"MAP\":true,\"BOOKMARKS\":true,\"QUICK_GUIDE\":true}," +
                "\"locations\":{\"MAP\":\"TopLeft\",\"BOOKMARKS\":\"BottomLeft\",\"QUICK_GUIDE\":\"BottomLeft\"}}";

        JIPipeDesktopDockPanel.State state = JsonUtils.getObjectMapper().readValue(json, JIPipeDesktopDockPanel.State.class);

        assertEquals(Boolean.TRUE, state.getVisibilities().get("MAP"));
        assertEquals(Boolean.TRUE, state.getVisibilities().get("BOOKMARKS"));
        assertEquals(Boolean.TRUE, state.getVisibilities().get("QUICK_GUIDE"));
        assertEquals(JIPipeDesktopDockPanel.PanelLocation.TopLeft, state.getLocations().get("MAP"));
        assertEquals(JIPipeDesktopDockPanel.PanelLocation.BottomLeft, state.getLocations().get("BOOKMARKS"));
        assertEquals(JIPipeDesktopDockPanel.PanelLocation.BottomLeft, state.getLocations().get("QUICK_GUIDE"));
    }
}
