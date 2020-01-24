package org.hkijena.acaq5.ui;

import ij.IJ;
import ij.ImagePlus;
import org.hkijena.acaq5.ui.components.ImageLogo;
import org.hkijena.acaq5.ui.components.ImagePlusExternalPreviewer;
import org.hkijena.acaq5.utils.ResourceUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class ACAQInfoUI extends JPanel {

    private ACAQWorkbenchUI workbenchUI;

    public ACAQInfoUI(ACAQWorkbenchUI workbenchUI) {
        this.workbenchUI = workbenchUI;
        initialize();
    }

    private void initialize() {
        BoxLayout layout = new BoxLayout(this, BoxLayout.PAGE_AXIS);
        setLayout(layout);

        add(Box.createVerticalGlue());
        JPanel contentPanel = new JPanel();
        contentPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1, true));
        contentPanel.setSize(new Dimension(500, 400));
        contentPanel.setMaximumSize(new Dimension(500, 400));
        add(contentPanel);
        add(Box.createVerticalGlue());

        BoxLayout contentPanelLayout = new BoxLayout(contentPanel, BoxLayout.PAGE_AXIS);
        contentPanel.setLayout(contentPanelLayout);

        addLogo(contentPanel);

        ImagePlus img0 = IJ.openImage("/data/ACAQ5/example1.tif");
        ImagePlus img1 = IJ.openImage("/data/ACAQ5/example2.tif");
        ImagePlusExternalPreviewer previewer = new ImagePlusExternalPreviewer();

        JButton button0 = new JButton("Image0");
        button0.addActionListener(x -> previewer.setCurrentImage(img0));
        add(button0);

        JButton button1 = new JButton("Image1");
        button1.addActionListener(x -> previewer.setCurrentImage(img1));
        add(button1);
    }

    private void addLogo(JPanel target) {
        ImageLogo logo = new ImageLogo();
        logo.setPreferredSize(new Dimension(500, 100));
        try {
            logo.setImage(ImageIO.read(ResourceUtils.getPluginResource("logo.png")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        target.add(logo);
    }
}
