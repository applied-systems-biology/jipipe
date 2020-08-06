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

package org.hkijena.jipipe.ui.components;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipeDefaultRegistry;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.events.ExtensionDiscoveredEvent;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class SplashScreen extends JWindow {

    private static SplashScreen instance;
    private JPanel poweredByContainer;
    private JPanel poweredByIconContainer;
    private JIPipeDefaultRegistry registry;

    public SplashScreen() {
        initialize();
    }

    private void initialize() {
        setSize(640, 480);
        setContentPane(new ContentPanel());

        poweredByContainer = new JPanel(new BorderLayout());
        poweredByContainer.setOpaque(false);
        poweredByContainer.setVisible(false);
        poweredByContainer.setLocation(20,203);
        poweredByContainer.setSize(574,138);

        JPanel poweredByContent = new JPanel(new BorderLayout());
        poweredByContent.setOpaque(false);
        poweredByContainer.add(poweredByContent, BorderLayout.EAST);

        JLabel poweredByLabel = new JLabel("Powered by");
        poweredByLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 14));
        poweredByContent.add(poweredByLabel, BorderLayout.NORTH);

        poweredByIconContainer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        poweredByIconContainer.setOpaque(false);
//        for (int i = 0; i < 5; i++) {
//            ImageIcon icon = UIUtils.getIcon32FromResources("apps/clij.png");
//            if(icon.getIconWidth() != 32 && icon.getIconHeight() != 32) {
//                Image scaledInstance = icon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH);
//                icon = new ImageIcon(scaledInstance);
//            }
//            JLabel label = new JLabel(icon);
//            poweredByIconContainer.add(label);
//        }
        poweredByContent.add(poweredByIconContainer, BorderLayout.CENTER);

        getContentPane().add(poweredByContainer);
    }

    public void showSplash() {
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public void hideSplash() {
        setVisible(false);
    }

    public JIPipeDefaultRegistry getRegistry() {
        return registry;
    }

    public void setRegistry(JIPipeDefaultRegistry registry) {
        this.registry = registry;
        if(registry != null) {
            registry.getEventBus().register(this);
        }
    }

    @Subscribe
    public void onExtensionDiscovered(ExtensionDiscoveredEvent event) {
        if(event.getExtension() instanceof JIPipeJavaExtension) {
            for (ImageIcon icon : ((JIPipeJavaExtension) event.getExtension()).getSplashIcons()) {
                if (icon.getIconWidth() != 32 && icon.getIconHeight() != 32) {
                    Image scaledInstance = icon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH);
                    icon = new ImageIcon(scaledInstance);
                }
                JLabel label = new JLabel(icon);
                poweredByIconContainer.add(label);
                revalidate();
                repaint();
            }
            poweredByContainer.setVisible(poweredByIconContainer.getComponentCount() > 0);
        }
    }

    public static void main(String[] args) {
        getInstance().showSplash();
    }

    public static SplashScreen getInstance() {
        if (instance == null) {
            instance = new SplashScreen();
        }
        return instance;
    }

    private static class ContentPanel extends JPanel {
        private final BufferedImage backgroundImage;

        public ContentPanel() {
            setOpaque(false);
            setLayout(null);
            try {
                backgroundImage = ImageIO.read(ResourceUtils.getPluginResource("splash-screen.png"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void paint(Graphics g) {
            g.drawImage(backgroundImage, 0,0,null);
            g.setColor(Color.DARK_GRAY);
            g.drawRect(0,0,getWidth() - 1, getHeight() - 1);
            super.paint(g);
        }
    }
}
