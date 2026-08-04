package org.hkijena.jipipe.plugins.statistics.ui;

import net.java.balloontip.BalloonTip;
import net.java.balloontip.styles.EdgedBalloonStyle;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchAccess;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;
import org.hkijena.jipipe.plugins.statistics.StatisticsReporter;
import org.hkijena.jipipe.plugins.statistics.settings.JIPipeStatisticsApplicationSettings;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopStatisticsButton extends JButton implements JIPipeDesktopWorkbenchAccess {
    private final JIPipeDesktopProjectWorkbench workbench;
    private BalloonTip balloonTip;

    public JIPipeDesktopStatisticsButton(JIPipeDesktopProjectWorkbench desktopWorkbench) {
        this.workbench = desktopWorkbench;
        initialize();
        initializeBalloon();

        JIPipeStatisticsApplicationSettings settings = JIPipeStatisticsApplicationSettings.getInstance();
        if (settings.isShowFirstTimePrompt() && settings.isEnabled()) {
            showStatisticsBalloon();
        }
    }

    private void initialize() {
        setOpaque(false);
        setText("Statistics");
        setIcon(JIPipe.RESOURCES.getIcon16("actions/chart-bar.png"));
        setToolTipText("Help us improve JIPipe by sending usage statistics");
        addActionListener(e -> showConfigurationDialog());
    }

    private void initializeBalloon() {
        EdgedBalloonStyle style = new EdgedBalloonStyle(UIManager.getColor("TextField.background"), ThemeUtils.getCurrentStyle().getPrimaryColor());
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setOpaque(false);
        content.add(UIUtils.createJLabel("Help us improve JIPipe", 16), BorderLayout.NORTH);
        content.add(new JLabel("<html><strong>JIPipe collects usage statistics to secure funding</strong><br/>" +
                "for NFDI4BIOIMAGE. You can choose what data is shared or opt out entirely.<br/>" +
                "The default setting sends all available statistics.</html>"), BorderLayout.CENTER);
        JPanel buttons = UIUtils.boxHorizontal(
                UIUtils.createButton("Dismiss", JIPipe.RESOURCES.getIcon16("actions/clock.png"), this::dismiss),
                UIUtils.createButton("Configure", JIPipe.RESOURCES.getIcon16("actions/configure.png"), this::showConfigurationDialog)
        );
        buttons.setOpaque(false);
        content.add(buttons, BorderLayout.SOUTH);
        balloonTip = new BalloonTip(
                this,
                content,
                style,
                BalloonTip.Orientation.LEFT_ABOVE,
                BalloonTip.AttachLocation.ALIGNED,
                30, 10,
                true
        );
        balloonTip.setVisible(false);

        JButton closeButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/window-close.png"));
        closeButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        closeButton.setOpaque(false);
        balloonTip.setCloseButton(closeButton, false);
    }

    private void showStatisticsBalloon() {
        UIUtils.invokeMuchLater(2000, () -> {
            balloonTip.refreshLocation();
            balloonTip.setVisible(true);
            workbench.getProjectWindow().registerBalloon(balloonTip);
        });
    }

    private void dismiss() {
        balloonTip.setVisible(false);
        JIPipeStatisticsApplicationSettings settings = JIPipeStatisticsApplicationSettings.getInstance();
        settings.setShowFirstTimePrompt(false);
        JIPipe.getInstance().getApplicationSettings().saveLater();

        if (settings.isEnabled() && settings.getPrivacyLevel() != StatisticsPrivacyLevel.None) {
            StatisticsReporter.sendNow(success -> {});
        }

        removeFromStatusBar();
    }

    private void showConfigurationDialog() {
        balloonTip.setVisible(false);
        JIPipeDesktopStatisticsConfigurationUI dialog = new JIPipeDesktopStatisticsConfigurationUI(workbench.getWindow());
        dialog.setVisible(true);

        if (dialog.isConfigured()) {
            JIPipeStatisticsApplicationSettings settings = JIPipeStatisticsApplicationSettings.getInstance();
            settings.setShowFirstTimePrompt(false);
            JIPipe.getInstance().getApplicationSettings().saveLater();

            if (settings.isEnabled() && settings.getPrivacyLevel() != StatisticsPrivacyLevel.None) {
                StatisticsReporter.sendNow(success -> {});
            }

            removeFromStatusBar();
        }
    }

    private void removeFromStatusBar() {
        Container parent = getParent();
        if (parent != null) {
            parent.remove(this);
            parent.revalidate();
            parent.repaint();
        }
    }

    @Override
    public JIPipeDesktopWorkbench getDesktopWorkbench() {
        return workbench;
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }
}
