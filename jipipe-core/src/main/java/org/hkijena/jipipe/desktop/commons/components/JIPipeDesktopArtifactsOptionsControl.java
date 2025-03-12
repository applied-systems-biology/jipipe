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

import net.java.balloontip.BalloonTip;
import net.java.balloontip.styles.EdgedBalloonStyle;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifactRepositoryReference;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifactRepositoryType;
import org.hkijena.jipipe.api.registries.JIPipeArtifactsRegistry;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.plugins.artifactsmanager.JIPipeDesktopArtifactManagerUI;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernMetalTheme;
import org.hkijena.jipipe.plugins.artifacts.JIPipeArtifactApplicationSettings;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

public class JIPipeDesktopArtifactsOptionsControl extends JButton implements JIPipeArtifactsRegistry.UpdatedEventListener {

    private final JIPipeDesktopProjectWorkbench workbench;
    private final JPopupMenu popupMenu = new JPopupMenu();
    private final JIPipeArtifactApplicationSettings settings = JIPipeArtifactApplicationSettings.getInstance();
    private BalloonTip balloonTip;
    private static boolean balloonTipDismissed;

    public JIPipeDesktopArtifactsOptionsControl(JIPipeDesktopProjectWorkbench workbench) {
        this.workbench = workbench;
        initialize();
        initializeBalloon();
        updateText();
        JIPipe.getArtifacts().getUpdatedEventEmitter().subscribeWeak(this);

        showBalloonIfNeeded();
    }

    private void initialize() {
        UIUtils.makeButtonFlat(this);
        setIcon(UIUtils.getIconFromResources("actions/run-install.png"));
        UIUtils.addReloadablePopupMenuToButton(this, popupMenu, this::reloadMenu);
    }

    private void initializeBalloon() {
        EdgedBalloonStyle style = new EdgedBalloonStyle(UIManager.getColor("TextField.background"), JIPipeDesktopModernMetalTheme.PRIMARY5);
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setOpaque(false);
        content.add(UIUtils.createJLabel("No connection to artifacts repository", 16), BorderLayout.NORTH);
        content.add(new JLabel("<html><strong>JIPipe was unable to query the list of available artifacts.</strong><br/>" +
                "Pipelines that rely on artifacts (Cellpose, Python, R, ...) that are not installed may fail.<br/><br/>" +
                "Please check your internet connection and try again by clicking <strong>Try again</strong>. <br/>" +
                "If no internet is available, you can also connect JIPipe to a local repository.</html>"), BorderLayout.CENTER);
        JPanel buttons = UIUtils.boxHorizontal(
                UIUtils.createButton("Never show this again", UIUtils.getIconFromResources("actions/cancel.png"), this::disableWarning),
                Box.createHorizontalStrut(16),
                UIUtils.createButton("Try again", UIUtils.getIconFromResources("actions/view-refresh.png"), this::refreshArtifacts),
                UIUtils.createButton("Add local repository", UIUtils.getIconFromResources("actions/add-folder-to-archive.png"), this::addLocalRepository),
                UIUtils.createButton("Dismiss", UIUtils.getIconFromResources("actions/clock.png"), this::closeBalloon)
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

        JButton closeButton = new JButton(UIUtils.getIconFromResources("actions/window-close.png"));
        closeButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        closeButton.setOpaque(false);
        balloonTip.setCloseButton(closeButton, false);
        workbench.getProjectWindow().registerBalloon(balloonTip);
    }

    private void closeBalloon() {
        balloonTipDismissed = true;
        balloonTip.setVisible(false);
    }

    private void disableWarning() {
        settings.setShowConnectionIssueBallon(false);
        JIPipe.getSettings().saveLater();
        closeBalloon();
    }

    private void reloadMenu() {
        popupMenu.removeAll();

        popupMenu.add(UIUtils.createMenuItem("Manage project artifacts ...", "Manages the artifact settings for this project", UIUtils.getIconFromResources("actions/configure.png"), this::openProjectSettings));
        popupMenu.addSeparator();
        popupMenu.add(UIUtils.createMenuItem("Install/uninstall ...", "Manage installed artifacts", UIUtils.getIconFromResources("actions/run-install.png"), this::manageArtifacts));
        popupMenu.add(UIUtils.createMenuItem("Refresh", "Refreshes the list of installed and available artifacts", UIUtils.getIconFromResources("actions/view-refresh.png"), this::refreshArtifacts));
        popupMenu.addSeparator();
        popupMenu.add(UIUtils.createMenuItem("Add local directory ...", "Adds a local artifacts repository for offline use", UIUtils.getIconFromResources("actions/add-folder-to-archive.png"), this::addLocalRepository));
        popupMenu.add(UIUtils.createMenuItem("More settings ...", "Opens the application settings", UIUtils.getIconFromResources("actions/configure.png"), this::openApplicationSettings));
    }

    private void openProjectSettings() {
        workbench.openProjectSettings("/Plugins");
    }

    private void addLocalRepository() {
        closeBalloon();
        Path selectedPath = JIPipeDesktop.openDirectory(workbench.getWindow(), workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey.External, "Add local repository", new HTMLText("Please select the root directory of the local repository. It should contain directories like 'org', 'com', and 'sc'."));
        settings.getRepositories().add(new JIPipeArtifactRepositoryReference(selectedPath.toString(), "", JIPipeArtifactRepositoryType.LocalDirectory));
        JIPipe.getSettings().saveLater();

        JOptionPane.showMessageDialog(workbench.getWindow(),
                "<html>The local directory " + selectedPath + " was added to the list of repositories.<br/>" +
                        "If you want to remove/change the repositories, navigate to the application settings.</html>",
                "Add local repository",
                JOptionPane.INFORMATION_MESSAGE);

        refreshArtifacts();
    }

    private void manageArtifacts() {
        JIPipeDesktopArtifactManagerUI.show(workbench);
    }

    private void refreshArtifacts() {
        JIPipe.getArtifacts().enqueueUpdateCachedArtifacts();
    }

    private void openApplicationSettings() {
        workbench.openApplicationSettings("/General/Artifacts");
    }

    private void updateText() {
        setToolTipText("Artifacts (" + JIPipe.getArtifacts().getCachedRemoteArtifacts().size() + " available, " + JIPipe.getArtifacts().getCachedLocalArtifacts().size() + " installed)");
       if(JIPipe.getArtifacts().getCachedRemoteArtifacts().isEmpty()) {
           setIcon(UIUtils.getIconFromResources("actions/gtk-disconnect.png"));
           setText("Artifacts unavailable");
       }
       else {
           setIcon(UIUtils.getIconFromResources("actions/run-install.png"));
           setText("Artifacts");
       }
    }

    @Override
    public void onArtifactsRegistryUpdated(JIPipeArtifactsRegistry.UpdatedEvent event) {
        updateText();
        showBalloonIfNeeded();

        // Hide the balloon tip
        if(!JIPipe.getArtifacts().getCachedRemoteArtifacts().isEmpty()) {
            balloonTip.setVisible(false);
        }
    }

    private void showBalloonIfNeeded() {
        if(!balloonTipDismissed && settings.isShowConnectionIssueBallon() && JIPipe.getArtifacts().getCachedRemoteArtifacts().isEmpty()) {
            UIUtils.invokeMuchLater(1000, () -> balloonTip.setVisible(true));
        }
    }
}
