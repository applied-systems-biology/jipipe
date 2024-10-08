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
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.JIPipeService;
import org.hkijena.jipipe.desktop.commons.components.icons.SpinnerIcon;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.Contextual;
import org.scijava.log.LogListener;
import org.scijava.log.LogMessage;
import org.scijava.log.LogService;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class JIPipeDesktopSplashScreen extends JWindow implements LogListener, Contextual, JIPipeService.PluginDiscoveredEventListener {

    private static final Object instanceLock = new Object();
    private static volatile JIPipeDesktopSplashScreen instance;
    private final SpinnerIcon spinnerIcon;
    private final JLabel statusLabel;
    private Context context;
    private JPanel poweredByContainer;
    private JPanel poweredByIconContainer;
    private JIPipe jiPipe;

    public JIPipeDesktopSplashScreen() {
        this.spinnerIcon = new SpinnerIcon(this);
        this.statusLabel = new JLabel("Please wait ...", spinnerIcon, JLabel.LEFT);
        initialize();
    }

    public static void main(String[] args) {
        getInstance().showSplash(null);
    }

    public static JIPipeDesktopSplashScreen getInstance() {
        synchronized (instanceLock) {
            if (instance == null) {
                instance = new JIPipeDesktopSplashScreen();
            }
        }
        return instance;
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
        spinnerIcon.start();

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
        spinnerIcon.stop();
        setVisible(false);
        dispose();
        SwingUtilities.invokeLater(() -> this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
    }

    public JIPipe getJIPipe() {
        return jiPipe;
    }

    public void setJIPipe(JIPipe registry) {
        this.jiPipe = registry;
        if (registry != null) {
            registry.getExtensionDiscoveredEventEmitter().subscribeWeak(this);
        }
    }

    @Override
    public void onJIPipePluginDiscovered(JIPipe.ExtensionDiscoveredEvent event) {
        if (event.getExtension() instanceof JIPipeJavaPlugin) {
            SwingUtilities.invokeLater(() -> {
                for (ImageIcon icon : ((JIPipeJavaPlugin) event.getExtension()).getSplashIcons()) {
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

    private static class ContentPanel extends JPanel {
        private final BufferedImage backgroundImage;

        public ContentPanel() {
            setOpaque(false);
            setLayout(null);
            try {
                if (UIUtils.getThemeFromRawSettings().isDark())
                    backgroundImage = ImageIO.read(ResourceUtils.getPluginResource("splash-screen-dark.png"));
                else
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
