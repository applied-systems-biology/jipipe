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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.project.JIPipeProjectSettingsDocument;
import org.hkijena.jipipe.api.settings.JIPipeProjectSettingsSheet;
import org.hkijena.jipipe.api.settings.SettingsScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins the three live-view sections of the settings tab's PROJECT scope — the
 * former project-settings modal's ad-hoc "legacy settings as sheets" wrappers,
 * moved to {@link ProjectSettingsViews}.
 * <p>
 * The contract these tests protect is not cosmetic: the wrappers are
 * <b>non-persisting views</b> over the project metadata. Registering them on
 * the project would write {@code sheet:jipipe:project-metadata} blocks next to
 * the natively persisted {@code metadata} member — a silent project-file
 * format change. The tests therefore pin (a) the stable ids/names/
 * descriptions the tab and the deep links address, (b) the exact visibility
 * exclusions the modal had, (c) that each wrapper reads straight through to
 * the live metadata, and (d) that the project persists no block for them.
 * <p>
 * Deliberately no Swing: the wrappers are plain parameter collections and the
 * form panel is the user's eye-check (AGENTS.md, no Swing structure tests).
 */
class ProjectSettingsViewsTest {

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    /**
     * @param sheet a sheet to build a parameter tree over
     * @return the tree rooted at the sheet (its wrapped collections are nested nodes)
     */
    private static JIPipeParameterTree tree(JIPipeProjectSettingsSheet sheet) {
        return new JIPipeParameterTree(sheet);
    }

    /**
     * @param tree  the project metadata's parameter tree
     * @param sheet the live view under test
     * @return the keys of the metadata that the view hides
     */
    private static List<String> hiddenKeys(JIPipeParameterTree tree, JIPipeProjectSettingsSheet sheet) {
        List<String> result = new ArrayList<>();
        for (JIPipeParameterAccess access : tree.getRoot().getChildren().get("wrapped").getParameters().values()) {
            if (!sheet.isParameterUIVisible(tree, access)) {
                result.add(access.getKey());
            }
        }
        return result;
    }

    @Test
    void idsNamesAndDescriptionsAreUnchanged() {
        // These ids are the deep-link targets (openSettingsSection(PROJECT, id)); the
        // names/descriptions are what the modal showed.
        assertEquals("jipipe:project-metadata", ProjectSettingsViews.ProjectMetadataSettingsSheetWrapper.ID);
        assertEquals("jipipe:project-directories", ProjectSettingsViews.ProjectDirectoriesSettingsSheetWrapper.ID);
        assertEquals("jipipe:project-global-parameters", ProjectSettingsViews.ProjectParametersSettingsSheetWrapper.ID);

        JIPipeProject project = new JIPipeProject();

        ProjectSettingsViews.ProjectMetadataSettingsSheetWrapper metadata =
                new ProjectSettingsViews.ProjectMetadataSettingsSheetWrapper(project);
        assertEquals("jipipe:project-metadata", metadata.getId());
        assertEquals("Project metadata", metadata.getName());
        assertEquals("General project metadata", metadata.getDescription());
        assertEquals("General", metadata.getCategory());

        ProjectSettingsViews.ProjectDirectoriesSettingsSheetWrapper directories =
                new ProjectSettingsViews.ProjectDirectoriesSettingsSheetWrapper(project);
        assertEquals("jipipe:project-directories", directories.getId());
        assertEquals("Project user paths", directories.getName());
        assertEquals("Allows to setup project user paths", directories.getDescription());
        assertEquals("General", directories.getCategory());

        ProjectSettingsViews.ProjectParametersSettingsSheetWrapper parameters =
                new ProjectSettingsViews.ProjectParametersSettingsSheetWrapper(project);
        assertEquals("jipipe:project-global-parameters", parameters.getId());
        assertEquals("Project-wide parameters", parameters.getName());
        assertEquals("Allows to configure parameters that are accessible project-wide in expressions and nodes",
                parameters.getDescription());
        assertEquals("General", parameters.getCategory());
    }

    @Test
    void metadataViewHidesPermissionsUserPathsGlobalParametersAndTemplateKeys() {
        JIPipeProject project = new JIPipeProject();
        ProjectSettingsViews.ProjectMetadataSettingsSheetWrapper metadata =
                new ProjectSettingsViews.ProjectMetadataSettingsSheetWrapper(project);
        JIPipeParameterTree tree = tree(metadata);

        // Sub-collections owned by the other live views (and by the permissions view
        // that the modal never showed) are hidden.
        assertFalse(metadata.isParameterUIVisible(tree, project.getMetadata().getPermissions()));
        assertFalse(metadata.isParameterUIVisible(tree, project.getMetadata().getUserPaths()));
        assertFalse(metadata.isParameterUIVisible(tree, project.getMetadata().getGlobalParameters()));

        // Template authoring keys are hidden; the metadata's own keys stay visible.
        List<String> hidden = hiddenKeys(tree, metadata);
        assertTrue(hidden.contains("template-description"), "template-description must be hidden, got " + hidden);
        assertTrue(hidden.contains("node-templates"), "node-templates must be hidden, got " + hidden);
        assertFalse(hidden.contains("name"), "the metadata's own keys must stay visible, hidden=" + hidden);
    }

    @Test
    void parametersViewShowsOnlyTheGlobalParameters() {
        JIPipeProject project = new JIPipeProject();
        ProjectSettingsViews.ProjectParametersSettingsSheetWrapper parameters =
                new ProjectSettingsViews.ProjectParametersSettingsSheetWrapper(project);
        JIPipeParameterTree tree = tree(parameters);

        assertTrue(parameters.isParameterUIVisible(tree, project.getMetadata().getGlobalParameters()),
                "the project-wide parameters are this view's content");
        assertFalse(parameters.isParameterUIVisible(tree, project.getMetadata().getUserPaths()),
                "user paths belong to the directories view");
        assertFalse(parameters.isParameterUIVisible(tree, project.getMetadata().getPermissions()),
                "permissions have no live view in the tab");
        // The view still exposes the metadata sub-parameter (the form walks into it);
        // its keys are filtered by the metadata's own visibility rule, which is the
        // modal's behaviour — the wrapper only picks the sub-collection.
        assertNotNull(tree.getRoot().getChildren().get("wrapped"));
    }

    @Test
    void wrappersAreLiveViewsOverTheProjectMetadata() {
        JIPipeProject project = new JIPipeProject();

        ProjectSettingsViews.ProjectDirectoriesSettingsSheetWrapper directories =
                new ProjectSettingsViews.ProjectDirectoriesSettingsSheetWrapper(project);
        // Same instance as the project's live user-path collection: an edit through
        // the tab's form panel lands in project.getMetadata().getUserPaths() directly.
        assertSame(project.getMetadata().getUserPaths(), directories.getWrapped());

        ProjectSettingsViews.ProjectMetadataSettingsSheetWrapper metadata =
                new ProjectSettingsViews.ProjectMetadataSettingsSheetWrapper(project);
        assertSame(project.getMetadata(), metadata.getWrapped());

        ProjectSettingsViews.ProjectParametersSettingsSheetWrapper parameters =
                new ProjectSettingsViews.ProjectParametersSettingsSheetWrapper(project);
        assertSame(project.getMetadata(), parameters.getWrapped());

        // A change on the live collection is reflected by the view (no copy, no cache).
        int before = project.getMetadata().getUserPaths().getUserPathsAsInstance().size();
        project.getMetadata().getUserPaths().setUserPath("TEST-ROLE", Path.of("/tmp/somewhere"));
        assertNotEquals(before, project.getMetadata().getUserPaths().getUserPathsAsInstance().size());
        assertSame(project.getMetadata().getUserPaths(), directories.getWrapped());
    }

    @Test
    void viewsAreTabSectionsButNeverRegisterOrPersistOnTheProject() throws Exception {
        JIPipeProject project = new JIPipeProject();

        // The tab shows them as PROJECT sections (this is the rendering path
        // reloadSections() uses).
        SettingsTreeModel model = new SettingsTreeModel(ProjectSettingsViews.sectionNodes(project));
        assertEquals(ProjectSettingsViews.sectionIds().size(), ProjectSettingsViews.sectionNodes(project).size());
        for (String id : ProjectSettingsViews.sectionIds()) {
            SettingsTreeModel.SheetNode node = model.nodeById(SettingsScope.PROJECT, id);
            assertNotNull(node, "the tab must show the live view '" + id + "' as a PROJECT section");
            assertSame(SettingsScope.PROJECT, node.scope());
        }

        // Persistence safety: a view section is a tree entry, never a project sheet.
        assertTrue(project.getSettingsSheets().values().stream()
                        .noneMatch(sheet -> ProjectSettingsViews.sectionIds().contains(sheet.getId())),
                "the live views must not be registered as project settings sheets, got "
                        + project.getSettingsSheets().keySet());
        // ... and the project's settings member must not get a block for them.
        ObjectNode written = project.settingsJsonForWrite();
        for (String id : ProjectSettingsViews.sectionIds()) {
            assertTrue(written.path(JIPipeProjectSettingsDocument.blockPathOf(id)).isMissingNode(),
                    "no persisted block may be written for the live view '" + id + "'");
        }
    }
}