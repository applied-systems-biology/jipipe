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
import org.hkijena.jipipe.JIPipeJavaPluginSplashIcon;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.events.JIPipePluginDiscoveredEvent;
import org.hkijena.jipipe.api.service.events.JIPipePluginDiscoveredEventListener;
import org.hkijena.jipipe.desktop.commons.components.icons.SpinnerIcon;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.ThemeUtils;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JIPipeDesktopSplashScreen extends JWindow implements LogListener, Contextual, JIPipePluginDiscoveredEventListener {

    private static final int ICON_SPACING = 16;
    private static final Object instanceLock = new Object();
    private static volatile JIPipeDesktopSplashScreen instance;
    private final SpinnerIcon spinnerIcon;
    private final JLabel statusLabel;
    private Context context;
    private JPanel poweredByContainer;
    private final JLabel poweredByIconLabel = new JLabel();
    private JIPipeService service;
    private final List<JIPipeJavaPluginSplashIcon> icons = new ArrayList<>();
    private JIPipeJavaPluginSplashIcon currentlyShowcasedIcon;


    public JIPipeDesktopSplashScreen() {
        this.spinnerIcon = new SpinnerIcon(this);
        this.statusLabel = new JLabel("Please wait ...", spinnerIcon, JLabel.LEFT);
        initialize();

        Timer iconTimer = new Timer(3000, e -> updateIconShowcase());
        iconTimer.setRepeats(true);
        iconTimer.start();
    }

    public static void main(String[] args) {
        getInstance().showSplash(null);
        getInstance().addIcon(JIPipeJavaPluginSplashIcon.builder().icon(JIPipe.RESOURCES.getIcon32("apps/jipipe.png")).id("jipipe").name("JIPipe").url("https://jipipe.org/").build());
        getInstance().addIcon(JIPipeJavaPluginSplashIcon.builder().icon(JIPipe.RESOURCES.getIcon32("apps/scijava.png")).id("scijava").name("SciJava").url("https://jipipe.org/").build());
        getInstance().addIcon(JIPipeJavaPluginSplashIcon.builder().icon(JIPipe.RESOURCES.getIcon32("apps/fiji.png")).id("fiji").name("Fiji").url("https://jipipe.org/").build());
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
        setSize(800, 600);
        setContentPane(new ContentPanel());

        final int contentWidth = 759;

        poweredByContainer = new JPanel(new BorderLayout());
        poweredByContainer.setOpaque(false);
        poweredByContainer.setVisible(false);
        poweredByContainer.setLocation(20, 203);
        poweredByContainer.setSize(contentWidth, 200);

        JLabel versionLabel = new JLabel(JIPipe.getJIPipeVersion());
        versionLabel.setOpaque(false);
        versionLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 32));
        versionLabel.setSize(150, 40);
        versionLabel.setLocation(600, 150);
        getContentPane().add(versionLabel);

        statusLabel.setSize(contentWidth, 25);
        statusLabel.setLocation(20, 562);
        getContentPane().add(statusLabel);

        JPanel poweredByContent = UIUtils.boxHorizontal();
        poweredByContent.setOpaque(false);
        poweredByContainer.add(poweredByContent, BorderLayout.CENTER);

        JLabel poweredByLabel = new JLabel("Powered by ...");
        poweredByLabel.setBorder(UIUtils.createEmptyBorder(16));
        poweredByLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        poweredByContent.add(poweredByLabel);

        JPanel separatorPanel = new JPanel();
        separatorPanel.setOpaque(false);
        separatorPanel.setBorder(BorderFactory.createMatteBorder(0,2,0,0, ThemeUtils.getCurrentStyle().getBorderColor()));
        poweredByContent.add(separatorPanel);

        poweredByIconLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        poweredByIconLabel.setBorder(UIUtils.createEmptyBorder(16));
        poweredByContent.add(poweredByIconLabel);

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

    @Override
    public void onJIPipePluginDiscovered(JIPipePluginDiscoveredEvent event) {
        if (event.getExtension() instanceof JIPipeJavaPlugin) {
            SwingUtilities.invokeLater(() -> {
                for (JIPipeJavaPluginSplashIcon icon : ((JIPipeJavaPlugin) event.getExtension()).getSplashIcons()) {
                    addIcon(icon);
                }
            });
        }
    }

    public void addIcon(JIPipeJavaPluginSplashIcon icon) {
        if (icon == null || StringUtils.isNullOrEmpty(icon.getId()) || icon.getIcon() == null) {
            return;
        }

        if(icons.stream().noneMatch(i -> i.getId().equals(icon.getId()))) {

            ImageIcon imageIcon = icon.getIcon();
            if (imageIcon.getIconWidth() != 32 && imageIcon.getIconHeight() != 32) {
                Image scaledInstance = imageIcon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH);
                imageIcon = new ImageIcon(scaledInstance);
            }

            JIPipeJavaPluginSplashIcon copy = new JIPipeJavaPluginSplashIcon(icon);
            copy.setIcon(imageIcon);
            icons.add(copy);

            Collections.shuffle(icons);

            updateIconShowcase();
        }
    }

    private void updateIconShowcase() {

        if(!icons.isEmpty()) {
            int nextIndex;
            if(currentlyShowcasedIcon != null) {
                nextIndex = Math.max(0, icons.indexOf(currentlyShowcasedIcon) + 1) % icons.size();
            }
            else {
                nextIndex = 0;
            }
            currentlyShowcasedIcon = icons.get(nextIndex);
        }

        if(currentlyShowcasedIcon != null) {
            poweredByIconLabel.setIcon(currentlyShowcasedIcon.getIcon());
            poweredByIconLabel.setText("<html><strong>" + currentlyShowcasedIcon.getName() + "</strong><br/>" + currentlyShowcasedIcon.getUrl() + "</html>");
        }

        poweredByContainer.setVisible(currentlyShowcasedIcon != null);
        revalidate();
        repaint();
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

    public JIPipeService getService() {
        return service;
    }

    public void setService(JIPipeService service) {
        this.service = service;
        if (service != null) {
            service.getExtensionDiscoveredEventEmitter().subscribeWeak(this);
        }
    }

    private static class ContentPanel extends JPanel {
        private final BufferedImage backgroundImage;

        public ContentPanel() {
            setOpaque(false);
            setLayout(null);
            try {
                if (ThemeUtils.isUsingDarkTheme())
                    backgroundImage = ImageIO.read(ResourceUtils.getPluginResource("resources/dark/splash-screen.png"));
                else
                    backgroundImage = ImageIO.read(ResourceUtils.getPluginResource("resources/light/splash-screen.png"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void paint(Graphics g) {
            g.drawImage(backgroundImage, 0, 0, null);
            g.setColor(ThemeUtils.getCurrentStyle().getBorderColor());
            g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            super.paint(g);
        }
    }
}
