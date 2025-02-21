package org.hkijena.jipipe.desktop.app.components;

import net.java.balloontip.BalloonTip;
import net.java.balloontip.styles.EdgedBalloonStyle;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.registries.JIPipeApplicationSettingsRegistry;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchAccess;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernMetalTheme;
import org.hkijena.jipipe.plugins.settings.JIPipeProjectAuthorsApplicationSettings;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopAuthorProfileButton extends JButton implements JIPipeDesktopWorkbenchAccess, JIPipeApplicationSettingsRegistry.ChangedEventListener {
    private final JIPipeDesktopProjectWorkbench workbench;
    private BalloonTip balloonTip;
    private final JIPipeProjectAuthorsApplicationSettings settings;

    public JIPipeDesktopAuthorProfileButton(JIPipeDesktopProjectWorkbench desktopWorkbench) {
        this.workbench = desktopWorkbench;
        this.settings = JIPipeProjectAuthorsApplicationSettings.getInstance();
        initialize();
        updateStatus();
        if(settings.isWarnNoAuthors() && settings.getProjectAuthors().isEmpty()) {
            showMissingAuthorBalloon();
        }
        JIPipe.getSettings().getChangedEventEmitter().subscribe(this);
    }

    private void updateStatus() {
        if(settings.getProjectAuthors().isEmpty()) {
            setText("Unknown author");
            setIcon(UIUtils.getIconFromResources("actions/im-kick-user.png"));
        }
        else if(settings.getProjectAuthors().size() == 1) {
            JIPipeAuthorMetadata authorMetadata = settings.getProjectAuthors().get(0);
            setText(authorMetadata.getFirstName() + " " + authorMetadata.getLastName());
            setIcon(UIUtils.getIconFromResources("actions/icon_user.png"));
        }
        else {
            JIPipeAuthorMetadata authorMetadata = settings.getProjectAuthors().get(0);
            setText("<html>" + authorMetadata.getFirstName() + " " + authorMetadata.getLastName() + " <i>et al.</i></html>");
            setIcon(UIUtils.getIconFromResources("actions/user-group.png"));
        }

    }

    private void initialize() {
//        JPopupMenu popupMenu = UIUtils.addPopupMenuToButton(this);
//        popupMenu.add(UIUtils.createMenuItem("Configure ...", "Configures the application-wide authors", UIUtils.getIconFromResources("actions/configure.png"), this::showSettings));

        // Initialize tooltip
        initializeBalloon();
        addActionListener(e -> { showSettings(); });
    }

    private void showSettings() {
        balloonTip.setVisible(false);
        workbench.openApplicationSettings("/General/Project authors");
    }

    private void initializeBalloon() {
        EdgedBalloonStyle style = new EdgedBalloonStyle(UIManager.getColor("TextField.background"), JIPipeDesktopModernMetalTheme.PRIMARY5);
        JPanel content = new JPanel(new BorderLayout(8,8));
        content.setOpaque(false);
        content.add(new JLabel("<html><strong>Please click this button to configure your author information.</strong><br/>" +
                "You only have to do this once and JIPipe will automatically add you<br/>" +
                "as author to your projects with appropriate affiliations.</html>"), BorderLayout.CENTER);
        JPanel buttons = UIUtils.boxHorizontal(
                UIUtils.createButton("Ask never", UIUtils.getIconFromResources("actions/cancel.png"), this::disableWarning),
                UIUtils.createButton("Dismiss", UIUtils.getIconFromResources("actions/clock.png"), this::closeBalloon),
                UIUtils.createButton("Configure", UIUtils.getIconFromResources("actions/configure.png"), this::showSettings)
        );
        buttons.setOpaque(false);
        content.add(buttons , BorderLayout.SOUTH);
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

        JButton closeButton = new JButton(UIUtils.getIconFromResources("actions/window-close.png"));
        closeButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        closeButton.setOpaque(false);
        balloonTip.setCloseButton(closeButton, false);
    }

    private void closeBalloon() {
        balloonTip.setVisible(false);
    }

    private void disableWarning() {
        balloonTip.setVisible(false);
        settings.setWarnNoAuthors(false);
        JIPipe.getInstance().getApplicationSettingsRegistry().saveLater();
    }

    public void showMissingAuthorBalloon() {
        UIUtils.invokeMuchLater(2000, () -> {
            balloonTip.refreshLocation();
            balloonTip.setVisible(true);
        });
    }

    @Override
    public JIPipeDesktopWorkbench getDesktopWorkbench() {
        return workbench;
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }

    @Override
    public void onApplicationSettingsChanged() {
        updateStatus();
    }
}
