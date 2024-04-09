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

package org.hkijena.jipipe.desktop.app.documentation;

import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopRecentProjectListCellRenderer;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.plugins.settings.ProjectsSettings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;

public class JIPipeDesktopRecentProjectsListPanel extends JIPipeDesktopProjectWorkbenchPanel implements JIPipeParameterCollection.ParameterChangedEventListener {
    private final ProjectOpenedEventEmitter projectOpenedEventEmitter = new ProjectOpenedEventEmitter();
    private final JIPipeDesktopSearchTextField recentProjectsSearch = new JIPipeDesktopSearchTextField();
    private final JList<Path> recentProjectsList = new JList<>();

    public JIPipeDesktopRecentProjectsListPanel(JIPipeDesktopProjectWorkbench workbench) {
        super(workbench);
        initialize();
        ProjectsSettings.getInstance().getParameterChangedEventEmitter().subscribeWeak(this);
        refreshRecentProjects();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        recentProjectsList.setCellRenderer(new JIPipeDesktopRecentProjectListCellRenderer());
        JScrollPane recentProjectsScrollPane = new JScrollPane(recentProjectsList);
        recentProjectsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Path value = recentProjectsList.getSelectedValue();
                    if (value != null) {
                        projectOpenedEventEmitter.emit(new ProjectOpenedEvent(value));
                        ((JIPipeDesktopProjectWindow) getDesktopProjectWorkbench().getWindow()).openProject(value, false);
                    }
                } else {
                    if (recentProjectsList.getMousePosition().x > recentProjectsList.getWidth() - 50) {
                        Path value = recentProjectsList.getSelectedValue();
                        if (value != null) {
                            projectOpenedEventEmitter.emit(new ProjectOpenedEvent(value));
                            ((JIPipeDesktopProjectWindow) getDesktopProjectWorkbench().getWindow()).openProject(value, false);
                        }
                    }
                }
            }
        });

        // Init search
        recentProjectsSearch.addActionListener(e -> refreshRecentProjects());

        add(recentProjectsScrollPane, BorderLayout.CENTER);
        add(recentProjectsSearch, BorderLayout.NORTH);
    }

    private void refreshRecentProjects() {
        DefaultListModel<Path> model = new DefaultListModel<>();
        for (Path path : ProjectsSettings.getInstance().getRecentProjects()) {
            if (recentProjectsSearch.test(path.toString())) {
                model.addElement(path);
            }
        }
        if (model.getSize() == 0 && recentProjectsSearch.getSearchStrings().length == 0) {
            model.addElement(null);
        }
        recentProjectsList.setModel(model);
    }

    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if ("recent-projects".equals(event.getKey())) {
            refreshRecentProjects();
        }
    }

    public ProjectOpenedEventEmitter getProjectOpenedEventEmitter() {
        return projectOpenedEventEmitter;
    }

    public interface ProjectOpenedEventListener {
        void onRecentProjectListProjectOpened(ProjectOpenedEvent event);
    }

    public static class ProjectOpenedEvent extends AbstractJIPipeEvent {
        private final Path projectPath;

        public ProjectOpenedEvent(Path projectPath) {
            super(null);
            this.projectPath = projectPath;
        }

        public Path getProjectPath() {
            return projectPath;
        }
    }

    public static class ProjectOpenedEventEmitter extends JIPipeEventEmitter<ProjectOpenedEvent, ProjectOpenedEventListener> {

        @Override
        protected void call(ProjectOpenedEventListener projectOpenedEventListener, ProjectOpenedEvent event) {
            projectOpenedEventListener.onRecentProjectListProjectOpened(event);
        }
    }
}
