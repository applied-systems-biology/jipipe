package org.hkijena.acaq5.ui.components;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.extensions.settings.ProjectsSettings;
import org.hkijena.acaq5.ui.ACAQJsonExtensionWindow;

import javax.swing.*;
import java.nio.file.Path;

/**
 * Menu that displays recently opened {@link org.hkijena.acaq5.ACAQJsonExtension}
 */
public class RecentJsonExtensionsMenu extends JMenu {

    private ACAQJsonExtensionWindow workbenchWindow;

    /**
     * @param text            item text
     * @param icon            item icon
     * @param workbenchWindow the workbench
     */
    public RecentJsonExtensionsMenu(String text, Icon icon, ACAQJsonExtensionWindow workbenchWindow) {
        super(text);
        this.setIcon(icon);
        this.workbenchWindow = workbenchWindow;
        reload();
        ProjectsSettings.getInstance().getEventBus().register(this);
    }

    private void reload() {
        removeAll();
        if (ProjectsSettings.getInstance().getRecentJsonExtensionProjects().isEmpty()) {
            JMenuItem noProject = new JMenuItem("No recent extensions");
            noProject.setEnabled(false);
            add(noProject);
        } else {
            for (Path path : ProjectsSettings.getInstance().getRecentJsonExtensionProjects()) {
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
        if ("recent-json-extension-projects".equals(event.getKey())) {
            reload();
        }
    }
}
