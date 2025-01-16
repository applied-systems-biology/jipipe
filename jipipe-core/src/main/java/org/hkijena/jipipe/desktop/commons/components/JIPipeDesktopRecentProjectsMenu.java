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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.registries.JIPipeRecentProjectsRegistry;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.desktop.app.documentation.JIPipeDesktopRecentProjectsListPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.nio.file.Path;

/**
 * Menu that displays recently opened {@link JIPipeProject}
 */
public class JIPipeDesktopRecentProjectsMenu extends JMenu implements JIPipeRecentProjectsRegistry.ChangedEventListener {

    private final JIPipeDesktopProjectWindow workbenchWindow;

    /**
     * @param text            item text
     * @param icon            item icon
     * @param workbenchWindow the workbench
     */
    public JIPipeDesktopRecentProjectsMenu(String text, Icon icon, JIPipeDesktopProjectWindow workbenchWindow) {
        super(text);
        this.setIcon(icon);
        this.workbenchWindow = workbenchWindow;
        reload();
        JIPipe.getInstance().getRecentProjectsRegistry().getChangedEventEmitter().subscribeWeak(this);
    }

    private void reload() {
        removeAll();
        if (JIPipe.getInstance().getRecentProjectsRegistry().getRecentProjects().isEmpty()) {
            JMenuItem noProject = new JMenuItem("No recent projects");
            noProject.setEnabled(false);
            add(noProject);
        } else {
            JMenuItem searchItem = new JMenuItem("Search ...", UIUtils.getIconFromResources("actions/search.png"));
            searchItem.addActionListener(e -> openProjectSearch());
            add(searchItem);

            for (Path path : JIPipe.getInstance().getRecentProjectsRegistry().getRecentProjects()) {
                JMenuItem openProjectItem = new JMenuItem(path.toString());
                openProjectItem.addActionListener(e -> openProject(path));
                add(openProjectItem);
            }
        }
    }

    private void openProjectSearch() {
        JDialog dialog = new JDialog(workbenchWindow);
        dialog.setTitle("Open project");
        JIPipeDesktopRecentProjectsListPanel panel = new JIPipeDesktopRecentProjectsListPanel(workbenchWindow.getProjectUI());
        panel.getProjectOpenedEventEmitter().subscribeLambda((emitter, lambda) -> {
            dialog.setVisible(false);
        });
        dialog.setContentPane(panel);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setIconImage(UIUtils.getJIPipeIcon128());
        dialog.pack();
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(workbenchWindow);
        dialog.setModal(true);
        dialog.setVisible(true);
    }

    private void openProject(Path path) {
        workbenchWindow.openProject(path, false);
    }

    @Override
    public void onRecentProjectsChanged(JIPipeRecentProjectsRegistry.ChangedEvent event) {
        reload();
    }
}
