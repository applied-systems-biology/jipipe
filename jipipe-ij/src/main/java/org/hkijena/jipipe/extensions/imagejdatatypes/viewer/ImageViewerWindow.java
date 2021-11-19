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

package org.hkijena.jipipe.extensions.imagejdatatypes.viewer;

import com.google.common.collect.ImmutableSet;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.HashSet;
import java.util.Set;

public class ImageViewerWindow extends JFrame implements WindowListener {
    private final ImageViewerPanel viewerPanel;

    public ImageViewerWindow() {
        this.viewerPanel = new ImageViewerPanel();
        initialize();
    }

    public ImageViewerWindow(ImageViewerPanel panel) {
        this.viewerPanel = panel;
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

    public ImageViewerPanel getViewerPanel() {
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
}
