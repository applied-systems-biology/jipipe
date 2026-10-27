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

package org.hkijena.jipipe.api.settings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SettingsBlockPathTest {
    @Test void parsesPrefixedBlockId() {
        SettingsBlockPath p = new SettingsBlockPath("sheet:org.hkijena.jipipe:general-ui");
        assertEquals(SettingsKind.SHEET, p.kind());
        assertEquals("org.hkijena.jipipe:general-ui", p.id());
        assertEquals("sheet:org.hkijena.jipipe:general-ui", p.path());
    }

    @Test void unknownPrefixIsPassThrough() {
        SettingsBlockPath p = new SettingsBlockPath("future-thing:abc");
        assertNull(p.kind());
        assertEquals("future-thing:abc", p.path());
    }

    @Test void subPathAppendsWithSlash() {
        SettingsBlockPath p = new SettingsBlockPath("registry:ui-graph-editor").withSubPath("any/dock/show-labels");
        assertEquals("registry:ui-graph-editor/any/dock/show-labels", p.pathWithSubPath());
    }

    @Test void equalityUsesFullPath() {
        assertEquals(new SettingsBlockPath("state:recent-projects"), new SettingsBlockPath("state:recent-projects"));
        assertNotEquals(new SettingsBlockPath("state:a"), new SettingsBlockPath("state:b"));
    }
}
