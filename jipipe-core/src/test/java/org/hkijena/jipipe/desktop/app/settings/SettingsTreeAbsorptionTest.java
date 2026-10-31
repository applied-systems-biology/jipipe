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
 * See the LICENSE file provided with the full license.
 */

package org.hkijena.jipipe.desktop.app.settings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.settings.SettingsScope;
import org.hkijena.jipipe.plugins.settings.application.JIPipeDesktopAccessibilityApplicationSettings;
import org.hkijena.jipipe.plugins.settings.application.JIPipeDesktopAppearanceApplicationSettings;
import org.hkijena.jipipe.plugins.settings.application.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.plugins.settings.application.JIPipeGeneralUIApplicationSettings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pin for the settings-tree absorption (#1345 Phase 2, item 1): the merged
 * Appearance and Accessibility sections own the theme, reduced-motion and
 * file-dialog knobs, so the two plain sheets that also carry them
 * ({@code general-ui}, {@code file-chooser}) must no longer appear as their own
 * tree entries — a user would otherwise see every knob twice.
 * <p>
 * The model is built exactly the way {@link JIPipeDesktopSettingsTab#reloadSections()}
 * builds it ({@code applicationNodes} over the live registry), so this fails if
 * an absorbed sheet ever comes back into the tree.
 */
class SettingsTreeAbsorptionTest {

    private static final String GENERAL_UI = JIPipeGeneralUIApplicationSettings.ID;
    private static final String FILE_CHOOSER = JIPipeFileChooserApplicationSettings.ID;

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    private static SettingsTreeModel treeModel() {
        return new SettingsTreeModel(JIPipeDesktopSettingsTab.applicationNodes(
                JIPipe.getSettings().getRegisteredSheets().values(),
                JIPipe.getSettingsService().effectiveScopeByBlock()));
    }

    @Test
    void absorbedSheetsAreNotTreeSections() {
        SettingsTreeModel model = treeModel();
        assertFalse(model.nodeById(GENERAL_UI) != null,
                "general-ui is absorbed by the merged Appearance/Accessibility sections and must not be its own tree entry");
        assertFalse(model.nodeById(FILE_CHOOSER) != null,
                "file-chooser is absorbed by the merged Appearance section and must not be its own tree entry");
        assertTrue(model.allNodes().stream().noneMatch(node -> node.id().equals(GENERAL_UI)
                        || node.id().equals(FILE_CHOOSER)),
                "no application view lists an absorbed sheet");
        assertTrue(model.search("theme", SettingsScope.APPLICATION).stream()
                        .noneMatch(node -> node.id().equals(GENERAL_UI)),
                "searching does not resurrect an absorbed sheet");
    }

    @Test
    void mergedSectionsRemainInTheTree() {
        SettingsTreeModel model = treeModel();
        assertTrue(model.nodeById(JIPipeDesktopAppearanceApplicationSettings.ID) != null,
                "the merged Appearance section stays");
        assertTrue(model.nodeById(JIPipeDesktopAccessibilityApplicationSettings.ID) != null,
                "the merged Accessibility section stays");
    }

    /**
     * Absorption is not a name match: the merged page hosts are NOT absorbed
     * (they are the tree entries of the merged sections), and an unrelated
     * sheet is untouched.
     */
    @Test
    void onlyTheDeclaredSheetsAreAbsorbed() {
        List<String> absorbed = JIPipeDesktopSettingsTab.absorbedSheetIds();
        assertTrue(absorbed.containsAll(List.of(GENERAL_UI, FILE_CHOOSER)));
        assertFalse(absorbed.contains(JIPipeDesktopAppearanceApplicationSettings.ID));
        assertFalse(absorbed.contains("org.hkijena.jipipe:auto-save"));

        List<String> treeIds = treeModel().allNodes().stream()
                .map(SettingsTreeModel.SheetNode::id).collect(Collectors.toList());
        assertTrue(treeIds.contains("org.hkijena.jipipe:auto-save"),
                "an unabsorbed sheet keeps its tree entry");
    }
}
