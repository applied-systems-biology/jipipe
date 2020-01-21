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

        addLogo();
        

        this.setBorder(BorderFactory.createLineBorder(Color.CYAN));
    }

    private void addLogo() {
//        JLabel logo = new JLabel();
//        try {
//            logo.setIcon(new ImageIcon(ImageIO.read(ResourceUtils.getPluginResource("logo.png"))));
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        logo.setHorizontalAlignment(SwingConstants.CENTER);
        ImageLogo logo = new ImageLogo();
        try {
            logo.setImage(ImageIO.read(ResourceUtils.getPluginResource("logo.png")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        logo.setAlignmentX(CENTER_ALIGNMENT);
        add(logo);
    }
}
