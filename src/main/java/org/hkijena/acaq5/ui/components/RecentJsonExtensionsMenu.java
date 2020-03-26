package org.hkijena.acaq5.ui.components;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.ui.ACAQJsonExtensionWindow;
import org.hkijena.acaq5.ui.settings.ACAQApplicationSettings;

import javax.swing.*;
import java.nio.file.Path;

public class RecentJsonExtensionsMenu extends JMenu {

    private ACAQJsonExtensionWindow workbenchWindow;

    public RecentJsonExtensionsMenu(String text, Icon icon, ACAQJsonExtensionWindow workbenchWindow) {
        super(text);
        this.setIcon(icon);
        this.workbenchWindow = workbenchWindow;
        reload();
        ACAQApplicationSettings.getInstance().getEventBus().register(this);
    }

    private void reload() {
        removeAll();
        if (ACAQApplicationSettings.getInstance().getRecentJsonExtensions().isEmpty()) {
            JMenuItem noProject = new JMenuItem("No recent extensions");
            noProject.setEnabled(false);
            add(noProject);
        } else {
            for (Path path : ACAQApplicationSettings.getInstance().getRecentJsonExtensions()) {
                JMenuItem openProjectItem = new JMenuItem(path.toString());
                openProjectItem.addActionListener(e -> openProject(path));
                add(openProjectItem);
            }
        }
    }

    private void openProject(Path path) {
        workbenchWindow.openProject(path);
    }

    @Subscribe
    public void onApplicationSettingsChanged(ParameterChangedEvent event) {
        if ("recent-json-extensions".equals(event.getKey())) {
            reload();
        }
    }
}
