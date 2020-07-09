/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.pipelinej.ui.components;

import com.google.common.eventbus.Subscribe;
import org.hkijena.pipelinej.api.events.ParameterChangedEvent;
import org.hkijena.pipelinej.extensions.settings.ProjectsSettings;
import org.hkijena.pipelinej.ui.ACAQProjectWindow;

import javax.swing.*;
import java.nio.file.Path;

/**
 * Menu that displays recently opened {@link org.hkijena.pipelinej.api.ACAQProject}
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
        ProjectsSettings.getInstance().getEventBus().register(this);
    }

    private void reload() {
        removeAll();
        if (ProjectsSettings.getInstance().getRecentProjects().isEmpty()) {
            JMenuItem noProject = new JMenuItem("No recent projects");
            noProject.setEnabled(false);
            add(noProject);
        } else {
            for (Path path : ProjectsSettings.getInstance().getRecentProjects()) {
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
