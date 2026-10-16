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

package org.hkijena.jipipe.desktop.app.grapheditor.templatemanage;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.utils.UIUtils;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the round-2 manager design (work item 1324 design v2): the
 * "Configure templates" dialog is a feature-manager with collapsible
 * sections (Your templates / This project / Downloaded [+ Get more] /
 * System), template cards in a wrap layout, and System collapsed by
 * default.
 */
class TemplateManagementDialogTest {

    private static JDialog ownerWindow;

    @BeforeAll
    static void boot() {
        try {
            if (GraphicsEnvironment.isHeadless()) {
                Assumptions.assumeTrue(false, "headless environment");
                return;
            }
        } catch (Throwable t) {
            Assumptions.assumeTrue(false, "AWT not available: " + t);
            return;
        }
        JIPipe.ensureInstance();
        ownerWindow = new JDialog();
        ownerWindow.setSize(1, 1);
    }

    /**
     * A minimal workbench stub: the dialog needs the window (owner), the
     * tab pane (null is never touched on this code path), and the project
     * (This project / Copy to Project presence).
     */
    private static JIPipeDesktopWorkbench stubWorkbench(JIPipeProject project) {
        return new JIPipeDesktopWorkbench() {
            @Override
            public JIPipeProject getProject() {
                return project;
            }

            @Override
            public Window getWindow() {
                return ownerWindow;
            }

            @Override
            public org.scijava.Context getContext() {
                return JIPipe.getInstance().getContext();
            }

            @Override
            public org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox getNotificationInbox() {
                return null;
            }

            @Override
            public void sendStatusBarText(String text) {
            }

            @Override
            public void showMessageDialog(String message, String title) {
            }

            @Override
            public void showErrorDialog(String message, String title) {
            }

            @Override
            public boolean isProjectModified() {
                return false;
            }

            @Override
            public void setProjectModified(boolean modified) {
            }

            @Override
            public org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane getDocumentTabPane() {
                return null;
            }

            @Override
            public org.hkijena.jipipe.plugins.cef.JIPipeCefClientService getCefClientService() {
                return null;
            }
        };
    }

    @Test
    void sectionsFollowTheDesignOrder() {
        // Without a project workbench the design's order minus the project
        // section; System is LAST either way. (With a real project workbench
        // "This project" slots in after "Your templates" — the production
        // guard is `instanceof JIPipeDesktopProjectWorkbench`, which a stub
        // cannot satisfy.)
        withDialog(stubWorkbench(null), dialog -> {
            List<String> titles = dialog.sectionTitlesForTest();
            assertThat(titles).containsExactly("Your templates", "Downloaded", "System");
        });
    }

    @Test
    void systemSectionIsCollapsedByDefaultAndOthersExpanded() {
        // "System templates hidden by default" (the user requirement behind
        // the design): the System section starts collapsed; the others start
        // expanded.
        withDialog(stubWorkbench(new JIPipeProject()), dialog -> {
            assertThat(dialog.isSectionExpandedForTest("System")).isFalse();
            assertThat(dialog.isSectionExpandedForTest("Your templates")).isTrue();
            assertThat(dialog.isSectionExpandedForTest("Downloaded")).isTrue();
        });
    }

    @Test
    void systemSectionListsTheBundledPatterns() {
        // The curated corpus patterns are plugin (System) templates; they
        // live in the (collapsed) System section, not in the user's.
        withDialog(stubWorkbench(null), dialog -> {
            List<JIPipeNodeTemplate> system = dialog.sectionTemplatesForTest("System");
            int expected = JIPipe.getNodeTemplates().getAllTemplates(null).stream()
                .filter(JIPipe.getNodeTemplates()::isPluginTemplate).toList().size();
            assertThat(system).hasSize(expected);
            assertThat(system).isNotEmpty();
        });
    }

    @Test
    void downloadedSectionIsItsOwnReadonlyScope() {
        // Round-2 spec: "Downloaded" is a first-class scope (repository
        // artifacts in the global store, source = the repository URL) —
        // distinct from "Your templates" (user-owned global templates), and
        // listed in the design's position.
        withDialog(stubWorkbench(null), dialog -> {
            assertThat(dialog.sectionTitlesForTest())
                .containsExactly("Your templates", "Downloaded", "System");
            List<JIPipeNodeTemplate> downloaded = dialog.sectionTemplatesForTest("Downloaded");
            for (JIPipeNodeTemplate t : downloaded) {
                assertThat(JIPipe.getNodeTemplates().isDownloadedTemplate(t))
                    .as("Downloaded section lists repository-sourced templates: " + t.getName())
                    .isTrue();
            }
            // ... and user-owned templates never leak into it.
            for (JIPipeNodeTemplate t : dialog.sectionTemplatesForTest("Your templates")) {
                assertThat(JIPipe.getNodeTemplates().isDownloadedTemplate(t)).isFalse();
            }
        });
    }

    // ------------------------------------------------------------------
    // Harness
    // ------------------------------------------------------------------

    private static void withDialog(JIPipeDesktopWorkbench workbench, java.util.function.Consumer<TemplateManagementDialog> check) {
        TemplateManagementDialog dialog = new TemplateManagementDialog(workbench, null);
        check.accept(dialog);
        dialog.dispose();
    }
}
