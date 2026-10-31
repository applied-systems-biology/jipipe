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

package org.hkijena.jipipe.desktop.app.settings;

import org.hkijena.jipipe.api.settings.SettingsScope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests of {@link SettingsTreeModel}, the pure (non-Swing) view model behind
 * the settings tab: search matching and the cross-scope editability rule.
 */
class SettingsTreeModelTest {

    @Test
    void searchIsCaseInsensitiveAndMatchesBlockPath() {
        SettingsTreeModel m = model();
        assertEquals(List.of("org.hkijena.jipipe:general-ui"), ids(m.search("GENERAL-UI", SettingsScope.APPLICATION)));
        assertEquals(List.of("org.hkijena.jipipe:general-ui"), ids(m.search("theme", SettingsScope.APPLICATION)));
    }

    @Test
    void sectionsOfOtherScopesArePresentButNotEditable() {
        SettingsTreeModel m = model();
        SettingsTreeModel.SheetNode runtimeSheet = m.nodeById("org.hkijena.jipipe:runtime");
        assertFalse(m.isEditable(runtimeSheet, SettingsScope.PROJECT));
    }

    private static SettingsTreeModel model() {
        SettingsTreeModel.SheetNode generalUi = new SettingsTreeModel.SheetNode(
                "org.hkijena.jipipe:general-ui",
                "General UI",
                "Contains theme and general user interface settings",
                "General",
                SettingsScope.APPLICATION,
                "org.hkijena.jipipe:general-ui");
        SettingsTreeModel.SheetNode runtime = new SettingsTreeModel.SheetNode(
                "org.hkijena.jipipe:runtime",
                "Runtime",
                "Threads, temporary directories and caches",
                "Execution",
                SettingsScope.RUNTIME,
                "org.hkijena.jipipe:runtime");
        SettingsTreeModel.SheetNode projectDefaults = new SettingsTreeModel.SheetNode(
                "org.hkijena.jipipe:project-defaults",
                "Project defaults",
                "Defaults applied to newly created projects",
                "General",
                SettingsScope.PROJECT,
                "org.hkijena.jipipe:project-defaults");
        return new SettingsTreeModel(List.of(generalUi, runtime, projectDefaults));
    }

    private static List<String> ids(List<SettingsTreeModel.SheetNode> nodes) {
        return nodes.stream().map(SettingsTreeModel.SheetNode::id).collect(Collectors.toList());
    }
}