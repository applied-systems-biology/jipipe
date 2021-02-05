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

package org.hkijena.jipipe.ui.grapheditor.contextmenu;

import org.hkijena.jipipe.api.JIPipeGraphType;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.extensions.settings.GeneralUISettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.cache.JIPipeAlgorithmCacheBrowserUI;
import org.hkijena.jipipe.ui.components.AlwaysOnTopToggle;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipeNodeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.Set;

public class OpenCacheBrowserInWindowUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeNodeUI> selection) {
        return !selection.isEmpty() && selection.stream().allMatch(ui -> ui.getNode().getInfo().isRunnable() &&
                ui.getNode() instanceof JIPipeAlgorithm &&
                ui.getNode().getGraph().getAttachment(JIPipeGraphType.class) == JIPipeGraphType.Project);
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeNodeUI> selection) {
        for (JIPipeNodeUI ui : selection) {
            JIPipeAlgorithmCacheBrowserUI browserUI = new JIPipeAlgorithmCacheBrowserUI((JIPipeProjectWorkbench) ui.getWorkbench(), ui.getNode());
            JFrame frame = new JFrame("Cache browser: " + ui.getNode().getName());
            frame.setAlwaysOnTop(GeneralUISettings.getInstance().isOpenUtilityWindowsAlwaysOnTop());
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setContentPane(browserUI);
            frame.setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
            frame.pack();
            frame.setSize(640, 480);
            frame.setLocationRelativeTo(null);

            AlwaysOnTopToggle alwaysOnTopToggle = new AlwaysOnTopToggle(frame);
            alwaysOnTopToggle.addActionListener(e -> GeneralUISettings.getInstance().setOpenUtilityWindowsAlwaysOnTop(alwaysOnTopToggle.isSelected()));
            browserUI.getToolBar().add(alwaysOnTopToggle);

            frame.setVisible(true);
        }
    }

    @Override
    public String getName() {
        return "Open cache browser window";
    }

    @Override
    public String getDescription() {
        return "Opens the cache browser tab in a new window";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/window-new.png");
    }

    @Override
    public boolean isShowingInOverhang() {
        return true;
    }

    @Override
    public boolean disableOnNonMatch() {
        return false;
    }
}
