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

package org.hkijena.jipipe.desktop.commons.components;

import org.hkijena.jipipe.JIPipeJsonPlugin;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.desktop.jsonextensionbuilder.JIPipeDesktopJsonExtensionWindow;
import org.hkijena.jipipe.plugins.settings.ProjectsSettings;

import javax.swing.*;
import java.nio.file.Path;

/**
 * Menu that displays recently opened {@link JIPipeJsonPlugin}
 */
public class JIPipeDesktopRecentJsonExtensionsMenu extends JMenu implements JIPipeParameterCollection.ParameterChangedEventListener {

    private final JIPipeDesktopJsonExtensionWindow workbenchWindow;

    /**
     * @param text            item text
     * @param icon            item icon
     * @param workbenchWindow the workbench
     */
    public JIPipeDesktopRecentJsonExtensionsMenu(String text, Icon icon, JIPipeDesktopJsonExtensionWindow workbenchWindow) {
        super(text);
        this.setIcon(icon);
        this.workbenchWindow = workbenchWindow;
        reload();
        ProjectsSettings.getInstance().getParameterChangedEventEmitter().subscribeWeak(this);
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
    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if ("recent-json-extension-projects".equals(event.getKey())) {
            reload();
        }
    }
}
