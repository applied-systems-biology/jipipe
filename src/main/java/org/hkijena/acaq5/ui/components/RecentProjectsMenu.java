package org.hkijena.acaq5.ui.components;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.ui.ACAQProjectWindow;
import org.hkijena.acaq5.ui.settings.ACAQApplicationSettings;

import javax.swing.*;
import java.nio.file.Path;

/**
 * Menu that displays recently opened {@link org.hkijena.acaq5.api.ACAQProject}
 */
public class RecentProjectsMenu extends JMenu {

    private ACAQProjectWindow workbenchWindow;

    /**
     * @param text            item text
     * @param icon            item icon
     * @param workbenchWindow the workbench
     */
    public RecentProjectsMenu(String text, Icon icon, ACAQProjectWindow workbenchWindow) {
        super(text);
        this.setIcon(icon);
        this.workbenchWindow = workbenchWindow;
        reload();
        ACAQApplicationSettings.getInstance().getEventBus().register(this);
    }

    private void reload() {
        removeAll();
        if (ACAQApplicationSettings.getInstance().getRecentProjects().isEmpty()) {
            JMenuItem noProject = new JMenuItem("No recent projects");
            noProject.setEnabled(false);
            add(noProject);
        } else {
            for (Path path : ACAQApplicationSettings.getInstance().getRecentProjects()) {
                JMenuItem openProjectItem = new JMenuItem(path.toString());
                openProjectItem.addActionListener(e -> openProject(path));
                add(openProjectItem);
            }
        }
    }

    private void openProject(Path path) {
        workbenchWindow.openProject(path);
    }

    /**
     * Triggered when the list should be changed
     *
     * @param event generated event
     */
    @Subscribe
    public void onApplicationSettingsChanged(ParameterChangedEvent event) {
        if ("recent-projects".equals(event.getKey())) {
            reload();
        }
    }
}
