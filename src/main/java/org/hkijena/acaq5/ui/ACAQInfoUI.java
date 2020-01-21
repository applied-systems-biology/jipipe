package org.hkijena.acaq5.ui;

import org.hkijena.acaq5.ui.components.ImageLogo;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.jdesktop.swingx.JXImageView;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class ACAQInfoUI extends JPanel {
    public ACAQInfoUI() {
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
