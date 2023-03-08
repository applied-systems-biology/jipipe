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

package org.hkijena.jipipe.extensions.imageviewer;

import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchAccess;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Collections;

public class JIPipeImageViewerWindow extends JFrame implements WindowListener, JIPipeWorkbenchAccess {
    private final JIPipeWorkbench workbench;
    private final JIPipeImageViewer viewerPanel;

    public JIPipeImageViewerWindow(JIPipeWorkbench workbench) {
        this.workbench = workbench;
        this.viewerPanel = new JIPipeImageViewer(workbench, JIPipeImageViewer.DEFAULT_PLUGINS, Collections.emptyMap());
        initialize();
    }

    public JIPipeImageViewerWindow(JIPipeImageViewer panel) {
        this.viewerPanel = panel;
        this.workbench = panel.getWorkbench();
        initialize();
    }

    private void initialize() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
        setContentPane(viewerPanel);
        pack();
        setSize(1024, 768);

        addWindowListener(this);
    }

    public JIPipeImageViewer getViewerPanel() {
        return viewerPanel;
    }

    @Override
    public void windowOpened(WindowEvent e) {
        viewerPanel.setName(getTitle());
        viewerPanel.addToOpenPanels();
        viewerPanel.setAsActiveViewerPanel();
        SwingUtilities.invokeLater(viewerPanel::fitImageToScreen);
    }

    @Override
    public void setTitle(String title) {
        super.setTitle(title);
        viewerPanel.setName(getTitle());
    }

    @Override
    public void windowClosing(WindowEvent e) {
        viewerPanel.dispose();
    }

    @Override
    public void windowClosed(WindowEvent e) {
        viewerPanel.setImage(null);
    }

    @Override
    public void windowIconified(WindowEvent e) {

    }

    @Override
    public void windowDeiconified(WindowEvent e) {

    }

    @Override
    public void windowActivated(WindowEvent e) {
        viewerPanel.setAsActiveViewerPanel();
    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }
}
