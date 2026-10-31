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
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.settings.JIPipeDefaultProjectSettingsSheet;
import org.hkijena.jipipe.api.settings.JIPipeDefaultProjectSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeProjectSettingsSheet;
import org.hkijena.jipipe.api.settings.SettingsScope;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The three ad-hoc "legacy settings as sheets" views of the settings tab's
 * PROJECT scope — formerly static classes inside the deleted project-settings
 * modal ({@code JIPipeDesktopProjectSettingsUI}, removed in work item 3 of
 * #1345).
 * <p>
 * <b>These are live views, not project settings sheets.</b> Each wrapper
 * exposes a live part of {@link JIPipeProject#getMetadata()} through the
 * parameter UI: the metadata itself (minus the parts owned by the other
 * views), the user paths, and the project-wide parameters. Edits made in the
 * tab's form panel therefore land directly in the project metadata, exactly as
 * they did in the modal — no document, no bridge, no persistence of their own.
 * <p>
 * <b>Never register them on the project.</b> A project settings sheet is
 * persisted into the {@code .jipx} file as a {@code sheet:<id>} block (see
 * {@code JIPipeProject.settingsJsonForWrite()} and the load path that restores
 * registered blocks). The project metadata is <em>also</em> persisted natively
 * (the {@code metadata} member), so registering one of these views would write
 * the same data twice and change the project file format. The tab adds them
 * with {@link JIPipeDesktopSettingsTab} purely as tree sections / form views.
 * {@link #sectionNodes(JIPipeProject)} and {@link #sectionIds()} exist to make
 * that rendering path explicit and testable.
 */
public final class ProjectSettingsViews {

    private ProjectSettingsViews() {
    }

    /**
     * @return the ids of the three live-view sections (deep-link targets at
     * {@link SettingsScope#PROJECT})
     */
    public static List<String> sectionIds() {
        return List.of(ProjectMetadataSettingsSheetWrapper.ID,
                ProjectDirectoriesSettingsSheetWrapper.ID,
                ProjectParametersSettingsSheetWrapper.ID);
    }

    /**
     * Builds the three live-view sections over the given project's metadata,
     * as tree nodes for {@link JIPipeDesktopSettingsTab}'s PROJECT scope.
     * <p>
     * The instances are created over the <em>current</em> project every time the
     * tab reloads its sections, so a view can never hold a stale project after
     * it was closed and another one opened.
     *
     * @param project the open project (must not be {@code null})
     * @return one node per view, in {@link #sectionIds()} order
     */
    public static List<SettingsTreeModel.SheetNode> sectionNodes(JIPipeProject project) {
        List<JIPipeProjectSettingsSheet> sheets = new ArrayList<>();
        sheets.add(new ProjectMetadataSettingsSheetWrapper(project));
        sheets.add(new ProjectDirectoriesSettingsSheetWrapper(project));
        sheets.add(new ProjectParametersSettingsSheetWrapper(project));
        List<SettingsTreeModel.SheetNode> result = new ArrayList<>();
        for (JIPipeProjectSettingsSheet sheet : sheets) {
            result.add(new SettingsTreeModel.SheetNode(sheet.getId(),
                    sheet.getName(),
                    sheet.getDescription(),
                    sheet.getCategory(),
                    SettingsScope.PROJECT,
                    sheet.getId()));
        }
        return result;
    }

    /**
     * Builds the three live-view sections over the given project's metadata,
     * keyed by id, for the tab's right pane.
     *
     * @param project the open project (must not be {@code null})
     * @return the live view instances in {@link #sectionIds()} order
     */
    public static List<JIPipeProjectSettingsSheet> sectionSheets(JIPipeProject project) {
        return List.of(new ProjectMetadataSettingsSheetWrapper(project),
                new ProjectDirectoriesSettingsSheetWrapper(project),
                new ProjectParametersSettingsSheetWrapper(project));
    }

    /**
     * Base of the live views: a project settings sheet that wraps (without
     * copying) a parameter collection of the project metadata. It must never be
     * registered on a project — see the class documentation.
     */
    public abstract static class SettingsSheetWrapper extends JIPipeDefaultProjectSettingsSheet {
        private final JIPipeParameterCollection wrapped;

        private SettingsSheetWrapper(JIPipeParameterCollection wrapped) {
            this.wrapped = wrapped;
        }

        @SetJIPipeDocumentation(name = "General")
        @JIPipeParameter("wrapped")
        public JIPipeParameterCollection getWrapped() {
            return wrapped;
        }
    }

    public static class ProjectDirectoriesSettingsSheetWrapper extends SettingsSheetWrapper {
        public static final String ID = "jipipe:project-directories";
        private final JIPipeProject project;

        public ProjectDirectoriesSettingsSheetWrapper(JIPipeProject project) {
            super(project.getMetadata().getUserPaths());
            this.project = project;
        }

        @Override
        public JIPipeDefaultProjectSettingsSheetCategory getDefaultCategory() {
            return JIPipeDefaultProjectSettingsSheetCategory.General;
        }

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public Icon getIcon() {
            return JIPipe.RESOURCES.getIcon16("actions/folder.png");
        }

        @Override
        public String getName() {
            return "Project user paths";
        }

        @Override
        public String getDescription() {
            return "Allows to setup project user paths";
        }

        @Override
        public void getEnvironmentDependencies(List<JIPipeEnvironment> target) {

        }
    }

    public static class ProjectMetadataSettingsSheetWrapper extends SettingsSheetWrapper {
        public static final String ID = "jipipe:project-metadata";
        private final JIPipeProject project;

        public ProjectMetadataSettingsSheetWrapper(JIPipeProject project) {
            super(project.getMetadata());
            this.project = project;
        }

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public Icon getIcon() {
            return JIPipe.RESOURCES.getIcon16("actions/circle-info.png");
        }

        @Override
        public String getName() {
            return "Project metadata";
        }

        @Override
        public String getDescription() {
            return "General project metadata";
        }

        @Override
        public JIPipeDefaultProjectSettingsSheetCategory getDefaultCategory() {
            return JIPipeDefaultProjectSettingsSheetCategory.General;
        }

        @Override
        public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterCollection subParameter) {
            if (subParameter == project.getMetadata().getPermissions()) {
                return false;
            }
            if (subParameter == project.getMetadata().getUserPaths()) {
                return false;
            }
            if (subParameter == project.getMetadata().getGlobalParameters()) {
                return false;
            }
            return super.isParameterUIVisible(tree, subParameter);
        }

        @Override
        public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
            if ("template-description".equals(access.getKey())) {
                return false;
            }
            if ("node-templates".equals(access.getKey())) {
                return false;
            }
            return super.isParameterUIVisible(tree, access);
        }

        @Override
        public void getEnvironmentDependencies(List<JIPipeEnvironment> target) {

        }
    }

    public static class ProjectParametersSettingsSheetWrapper extends SettingsSheetWrapper {
        public static final String ID = "jipipe:project-global-parameters";
        private final JIPipeProject project;

        public ProjectParametersSettingsSheetWrapper(JIPipeProject project) {
            super(project.getMetadata());
            this.project = project;
        }

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public Icon getIcon() {
            return JIPipe.RESOURCES.getIcon16("actions/configure_toolbars.png");
        }

        @Override
        public String getName() {
            return "Project-wide parameters";
        }

        @Override
        public String getDescription() {
            return "Allows to configure parameters that are accessible project-wide in expressions and nodes";
        }

        @Override
        public JIPipeDefaultProjectSettingsSheetCategory getDefaultCategory() {
            return JIPipeDefaultProjectSettingsSheetCategory.General;
        }

        @Override
        public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterCollection subParameter) {
            if (subParameter == project.getMetadata().getGlobalParameters()) {
                return true;
            }
            return false;
        }

        @Override
        public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
            return super.isParameterUIVisible(tree, access);
        }

        @Override
        public void getEnvironmentDependencies(List<JIPipeEnvironment> target) {

        }
    }
}