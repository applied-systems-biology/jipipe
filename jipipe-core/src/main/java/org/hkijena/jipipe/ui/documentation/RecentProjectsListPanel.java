package org.hkijena.jipipe.ui.documentation;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.settings.ProjectsSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWindow;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.components.RecentProjectListCellRenderer;
import org.hkijena.jipipe.ui.components.search.SearchTextField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;

public class RecentProjectsListPanel extends JIPipeProjectWorkbenchPanel {
    private final SearchTextField recentProjectsSearch = new SearchTextField();
    private final JList<Path> recentProjectsList = new JList<>();
    private final EventBus eventBus = new EventBus();

    public RecentProjectsListPanel(JIPipeProjectWorkbench workbench) {
        super(workbench);
        initialize();
        ProjectsSettings.getInstance().getEventBus().register(this);
        refreshRecentProjects();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        recentProjectsList.setCellRenderer(new RecentProjectListCellRenderer());
        JScrollPane recentProjectsScrollPane = new JScrollPane(recentProjectsList);
        recentProjectsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Path value = recentProjectsList.getSelectedValue();
                    if (value != null) {
                        eventBus.post(new ProjectOpenedEvent(value));
                        ((JIPipeProjectWindow) getProjectWorkbench().getWindow()).openProject(value);
                    }
                } else {
                    if (recentProjectsList.getMousePosition().x > recentProjectsList.getWidth() - 50) {
                        Path value = recentProjectsList.getSelectedValue();
                        if (value != null) {
                            eventBus.post(new ProjectOpenedEvent(value));
                            ((JIPipeProjectWindow) getProjectWorkbench().getWindow()).openProject(value);
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

    public EventBus getEventBus() {
        return eventBus;
    }

    @Subscribe
    public void onRecentProjectsChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if ("recent-projects".equals(event.getKey())) {
            refreshRecentProjects();
        }
    }

    public static class ProjectOpenedEvent {
        private final Path projectPath;

        public ProjectOpenedEvent(Path projectPath) {

            this.projectPath = projectPath;
        }

        public Path getProjectPath() {
            return projectPath;
        }
    }
}
