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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.settings.SettingsScope;
import org.hkijena.jipipe.plugins.settings.application.JIPipeDefaultEnvironmentsApplicationSettings;
import org.hkijena.jipipe.plugins.settings.project.JIPipeDefaultEnvironmentsProjectSettings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Whole-branch review pin for the settings tab's section model built from the
 * <b>real registries</b> (application sheets + an open project's sheets), which
 * is exactly what {@link JIPipeDesktopSettingsTab#reloadSections()} feeds into
 * {@link SettingsTreeModel}.
 * <p>
 * The regression this pins: {@code org.hkijena.jipipe:default-environments} is
 * both a RUNTIME application sheet (installed environments, machine state) and
 * a PROJECT sheet (the default environment of projects). The bare-id
 * uniqueness rule made the model construction throw, so the settings tab could
 * not be opened in <em>any</em> project.
 * <p>
 * Deliberately no Swing: the tab component is not constructed (AGENTS.md, no
 * Swing structure tests); the model is what threw, and that is what is pinned.
 */
class SettingsTabSectionsWithProjectTest {

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    private static SettingsTreeModel modelWithOpenProject() {
        List<SettingsTreeModel.SheetNode> nodes = new ArrayList<>();
        nodes.addAll(JIPipeDesktopSettingsTab.applicationNodes(
                JIPipe.getSettings().getRegisteredSheets().values(),
                JIPipe.getSettingsService().effectiveScopeByBlock()));
        nodes.addAll(JIPipeDesktopSettingsTab.projectNodes(
                new JIPipeProject().getSettingsSheets().values()));
        return new SettingsTreeModel(nodes);
    }

    @Test
    void sectionModelIsConstructibleWithAnOpenProject() {
        assertDoesNotThrow(SettingsTabSectionsWithProjectTest::modelWithOpenProject);
    }

    @Test
    void bothDefaultEnvironmentsSectionsArePresentEachInItsOwnScope() {
        SettingsTreeModel model = modelWithOpenProject();
        String id = JIPipeDefaultEnvironmentsApplicationSettings.ID;
        assertEquals(id, JIPipeDefaultEnvironmentsProjectSettings.ID, "the two sheets deliberately share the id");

        SettingsTreeModel.SheetNode appSection = model.nodeById(SettingsScope.RUNTIME, id);
        SettingsTreeModel.SheetNode projectSection = model.nodeById(SettingsScope.PROJECT, id);
        assertNotNull(appSection, "the RUNTIME application default-environments section must be present");
        assertNotNull(projectSection, "the PROJECT default-environments section must be present");
        // Distinct sections, not one overwriting the other.
        assertSame(SettingsScope.RUNTIME, appSection.scope());
        assertSame(SettingsScope.PROJECT, projectSection.scope());

        // Global search finds BOTH same-id sections, each in its own scope.
        List<SettingsTreeModel.SheetNode> search = model.search("default-environments");
        List<SettingsTreeModel.SheetNode> sameId = search.stream()
                .filter(node -> node.id().equals(id)).collect(Collectors.toList());
        assertEquals(2, sameId.size(), "the RUNTIME application and the PROJECT section share the id");
        assertSame(SettingsScope.RUNTIME, sameId.stream().filter(n -> n.scope() == SettingsScope.RUNTIME).findFirst().orElseThrow().scope());
        assertSame(SettingsScope.PROJECT, sameId.stream().filter(n -> n.scope() == SettingsScope.PROJECT).findFirst().orElseThrow().scope());

        // Each scope view lists only its own section (no dimmed cross-scope entries).
        List<SettingsTreeModel.SheetNode> projectView = model.categories(SettingsScope.PROJECT).stream()
                .flatMap(category -> category.sheets().stream())
                .filter(node -> node.id().equals(id)).collect(Collectors.toList());
        assertEquals(1, projectView.size(), "the PROJECT tab lists only the project section");
        assertSame(SettingsScope.PROJECT, projectView.get(0).scope());
        assertTrue(model.isEditable(projectView.get(0), SettingsScope.PROJECT));

        List<SettingsTreeModel.SheetNode> applicationView = model.categories(SettingsScope.APPLICATION).stream()
                .flatMap(category -> category.sheets().stream())
                .filter(node -> node.id().equals(id)).collect(Collectors.toList());
        assertTrue(applicationView.isEmpty(), "the APPLICATION tab no longer lists the RUNTIME-scoped section");

        assertFalse(model.isEditable(appSection, SettingsScope.APPLICATION));
    }
}