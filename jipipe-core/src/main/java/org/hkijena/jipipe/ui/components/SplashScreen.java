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
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.events.ExtensionDiscoveredEvent;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.Contextual;
import org.scijava.log.LogListener;
import org.scijava.log.LogMessage;
import org.scijava.log.LogService;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class SplashScreen extends JWindow implements LogListener, Contextual {

    private static final Object instanceLock = new Object();
    private static volatile SplashScreen instance;
    private Context context;
    private JPanel poweredByContainer;
    private JPanel poweredByIconContainer;
    private JIPipe jiPipe;
    private JLabel statusLabel = new JLabel("Please wait ...",
            UIUtils.getIconFromResources("actions/hourglass-half.png"), JLabel.LEFT);

    public SplashScreen() {
        initialize();
    }

    private void initialize() {
        setSize(640, 480);
        setContentPane(new ContentPanel());

        poweredByContainer = new JPanel(new BorderLayout());
        poweredByContainer.setOpaque(false);
        poweredByContainer.setVisible(false);
        poweredByContainer.setLocation(20, 203);
        poweredByContainer.setSize(574, 138);

        statusLabel.setSize(574, 25);
        statusLabel.setLocation(20, 450);
        getContentPane().add(statusLabel);

        JPanel poweredByContent = new JPanel(new BorderLayout());
        poweredByContent.setOpaque(false);
        poweredByContainer.add(poweredByContent, BorderLayout.EAST);

        JLabel poweredByLabel = new JLabel("Powered by");
        poweredByLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 14));
        poweredByContent.add(poweredByLabel, BorderLayout.NORTH);

        poweredByIconContainer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        poweredByIconContainer.setOpaque(false);
        poweredByContent.add(poweredByIconContainer, BorderLayout.CENTER);

        getContentPane().add(poweredByContainer);

        // Listen to Esc
        getRootPane().registerKeyboardAction(e -> setVisible(false),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    public void showSplash(Context context) {
        if (context != null)
            context.inject(this);
        setLocationRelativeTo(null);
        setVisible(true);

        if (context != null) {
            LogService logService = context.getService(LogService.class);
            logService.addLogListener(this);
        }
    }

    public void hideSplash() {
        instance = null;
        if (context != null) {
            LogService logService = context.getService(LogService.class);
            logService.removeLogListener(this);
        }
        setVisible(false);
        dispose();
        SwingUtilities.invokeLater(() -> this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
    }

    public JIPipe getJiPipe() {
        return jiPipe;
    }

    public void setJIPipe(JIPipe registry) {
        this.jiPipe = registry;
        if (registry != null) {
            registry.getEventBus().register(this);
        }
    }

    @Subscribe
    public void onExtensionDiscovered(ExtensionDiscoveredEvent event) {
        if (event.getExtension() instanceof JIPipeJavaExtension) {
            SwingUtilities.invokeLater(() -> {
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
            });
        }
    }

    @Override
    public void messageLogged(LogMessage message) {
        statusLabel.setText(message.text());
    }

    @Override
    public Context context() {
        return context;
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public void setContext(Context context) {
        this.context = context;
    }

    public static void main(String[] args) {
        getInstance().showSplash(null);
    }

    public static SplashScreen getInstance() {
        synchronized (instanceLock) {
            if (instance == null) {
                instance = new SplashScreen();
            }
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
            g.drawImage(backgroundImage, 0, 0, null);
            g.setColor(Color.DARK_GRAY);
            g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            super.paint(g);
        }
    }
}
