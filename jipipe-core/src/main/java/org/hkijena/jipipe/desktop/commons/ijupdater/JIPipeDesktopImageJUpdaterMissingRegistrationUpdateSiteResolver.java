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

package org.hkijena.jipipe.desktop.commons.ijupdater;

import net.imagej.updater.UpdateSite;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.JIPipeRegistryIssues;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.extensions.JIPipeDesktopActivateAndApplyUpdateSiteRun;
import org.hkijena.jipipe.desktop.app.extensions.JIPipeDesktopExtensionItemActionButton;
import org.hkijena.jipipe.desktop.app.extensions.JIPipeDesktopModernPluginManager;
import org.hkijena.jipipe.desktop.app.extensions.JIPipeDesktopUpdateSitePlugin;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunExecuteUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopMessagePanel;
import org.hkijena.jipipe.desktop.commons.components.icons.JIPipeDesktopAnimatedIcon;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class JIPipeDesktopImageJUpdaterMissingRegistrationUpdateSiteResolver extends JFrame implements JIPipeDesktopWorkbench, JIPipeDesktopModernPluginManager.UpdateSitesReadyEventListener, JIPipeRunnable.FinishedEventListener, JIPipeRunnable.InterruptedEventListener {

    private final Context context;
    private final JIPipeRegistryIssues issues;
    private final JIPipeNotificationInbox notificationInbox = new JIPipeNotificationInbox();

    private final JIPipeDesktopMessagePanel messagePanel = new JIPipeDesktopMessagePanel();
    private final JIPipeDesktopModernPluginManager pluginManager;

    private final JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.WITH_SCROLLING);

    private boolean clickedInstallAll = false;

    public JIPipeDesktopImageJUpdaterMissingRegistrationUpdateSiteResolver(Context context, JIPipeRegistryIssues issues) {
        this.context = context;
        this.issues = issues;
        setSize(1024, 768);
        setTitle("JIPipe - Missing ImageJ dependencies");
        setIconImage(UIUtils.getJIPipeIcon128());
        getContentPane().setLayout(new BorderLayout());
        initialize();

        pluginManager = new JIPipeDesktopModernPluginManager(this, this, messagePanel);
        pluginManager.getUpdateSitesReadyEventEmitter().subscribe(this);
        pluginManager.initializeUpdateSites();

        JIPipeRunnableQueue.getInstance().getFinishedEventEmitter().subscribeWeak(this);
        JIPipeRunnableQueue.getInstance().getInterruptedEventEmitter().subscribeWeak(this);
    }

    private void initialize() {

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(messagePanel, BorderLayout.NORTH);

        formPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        formPanel.addWideToForm(UIUtils.createJLabel("Missing dependencies detected", UIUtils.getIcon32FromResources("dialog-warning.png"), 28));
        formPanel.addWideToForm(UIUtils.createBorderlessReadonlyTextPane("JIPipe detected missing ImageJ dependencies that are required for the functionality of various JIPipe extensions. You can ignore this message if you think that all required software is already installed.", false));
        formPanel.addWideToForm(Box.createVerticalStrut(32));
        if (!issues.getErroneousPlugins().isEmpty()) {
            formPanel.addWideToForm(UIUtils.createJLabel(issues.getErroneousPlugins().size() + " JIPipe extensions reported issues. We strongly recommend the installation of all dependencies.", UIUtils.getIconFromResources("emblems/emblem-important-blue.png")));
        }
        if (!issues.getErroneousNodes().isEmpty()) {
            formPanel.addWideToForm(UIUtils.createJLabel(issues.getErroneousNodes().size() + " nodes could not be registered. We strongly recommend the installation of all dependencies.", UIUtils.getIconFromResources("emblems/emblem-important-blue.png")));
        }
        if (!issues.getErroneousDataTypes().isEmpty()) {
            formPanel.addWideToForm(UIUtils.createJLabel(issues.getErroneousDataTypes().size() + " data types could not be registered. We strongly recommend the installation of all dependencies.", UIUtils.getIconFromResources("emblems/emblem-important-blue.png")));
        }
        formPanel.addWideToForm(UIUtils.createJLabel("You can disable the validation of ImageJ dependencies via Project > Application settings > Extensions > Validate ImageJ dependencies", UIUtils.getIconFromResources("emblems/emblem-information.png")));
        formPanel.addWideToForm(Box.createVerticalStrut(32));

        // Sites will be loaded in later
        if (!issues.getMissingImageJSites().isEmpty()) {
            formPanel.addWideToForm(Box.createVerticalStrut(32));
            JIPipeDesktopAnimatedIcon hourglassAnimation = new JIPipeDesktopAnimatedIcon(this, UIUtils.getIconFromResources("actions/hourglass-half.png"),
                    UIUtils.getIconFromResources("emblems/hourglass-half.png"),
                    100, 0.05);
            hourglassAnimation.start();
            formPanel.addWideToForm(UIUtils.createJLabel("Missing ImageJ update sites", 22));
            formPanel.addWideToForm(UIUtils.createJLabel("Please wait until the update sites are available ...", hourglassAnimation));
        }

        formPanel.addVerticalGlue();
        getContentPane().add(formPanel, BorderLayout.CENTER);

        initializeButtonPanel();
    }

    private void installAllDependencies() {
        if (!pluginManager.isUpdateSitesReady()) {
            JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(this), "ImageJ updates sites are currently not ready/unavailable.",
                    "Activate ImageJ update sites", JOptionPane.ERROR_MESSAGE);
            return;
        }
        clickedInstallAll = true;
        List<UpdateSite> updateSiteSet = new ArrayList<>();
        for (JIPipeImageJUpdateSiteDependency dependency : issues.getMissingImageJSites()) {
            UpdateSite updateSite = pluginManager.getUpdateSites().getUpdateSite(dependency.getName(), true);
            if (updateSite == null) {
                updateSite = pluginManager.getUpdateSites().addUpdateSite(dependency.toUpdateSite());
            }
            updateSiteSet.add(updateSite);
        }
        JIPipeDesktopActivateAndApplyUpdateSiteRun run = new JIPipeDesktopActivateAndApplyUpdateSiteRun(pluginManager, updateSiteSet);
        JIPipeDesktopRunExecuteUI.runInDialog(this, this, run);
    }

    @Override
    public void onPluginManagerUpdateSitesReady(JIPipeDesktopModernPluginManager.UpdateSitesReadyEvent event) {
        if (!issues.getMissingImageJSites().isEmpty()) {
            formPanel.removeLastRow(); //Vertical glue
            formPanel.removeLastRow(); // "Please wait..."
            for (JIPipeImageJUpdateSiteDependency dependency : issues.getMissingImageJSites()) {
                JPanel dependencyPanel = new JPanel(new GridBagLayout());
                dependencyPanel.setBorder(BorderFactory.createCompoundBorder(new RoundedLineBorder(UIManager.getColor("Button.borderColor"), 1, 2), BorderFactory.createEmptyBorder(8, 8, 8, 8)));
                dependencyPanel.add(UIUtils.createJLabel(dependency.getName(), UIUtils.getIcon32FromResources("module-imagej.png"), 16), new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));
                JTextField idField = UIUtils.createReadonlyBorderlessTextField("URL: " + dependency.getUrl());
                idField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
                dependencyPanel.add(idField, new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));
                dependencyPanel.add(UIUtils.createBorderlessReadonlyTextPane(dependency.getDescription(), false), new GridBagConstraints(0, 2, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));

                // Try to find the extension
                JIPipeDesktopUpdateSitePlugin extension = new JIPipeDesktopUpdateSitePlugin(dependency);
                JIPipeDesktopExtensionItemActionButton button = new JIPipeDesktopExtensionItemActionButton(pluginManager, extension);
                button.setFont(new Font(Font.DIALOG, Font.PLAIN, 22));
                dependencyPanel.add(button, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));

                formPanel.addWideToForm(dependencyPanel);
            }
            formPanel.addVerticalGlue();
        }
    }

    private void initializeButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton("Ignore", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.setFont(new Font(Font.DIALOG, Font.PLAIN, 16));
        cancelButton.addActionListener(e -> {
            setVisible(false);
        });
        buttonPanel.add(cancelButton);

        JButton confirmButton = new JButton("Install all dependencies", UIUtils.getIconFromResources("emblems/vcs-normal.png"));
        confirmButton.setFont(new Font(Font.DIALOG, Font.PLAIN, 16));
        confirmButton.addActionListener(e -> {
            installAllDependencies();
        });
        buttonPanel.add(confirmButton);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    }

    @Override
    public Window getWindow() {
        return this;
    }

    @Override
    public void sendStatusBarText(String text) {

    }

    @Override
    public boolean isProjectModified() {
        return false;
    }

    @Override
    public void setProjectModified(boolean modified) {

    }

    @Override
    public void showMessageDialog(String message, String title) {

    }

    @Override
    public void showErrorDialog(String message, String title) {

    }

    @Override
    public JIPipeProject getProject() {
        return null;
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public JIPipeDesktopTabPane getDocumentTabPane() {
        return null;
    }

    @Override
    public JIPipeNotificationInbox getNotificationInbox() {
        return notificationInbox;
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if (event.getRun() instanceof JIPipeDesktopActivateAndApplyUpdateSiteRun && this.clickedInstallAll) {
            if (JOptionPane.showOptionDialog(this, "Please close and restart ImageJ to complete the installation of updates. " +
                    "If you have any issues, please install the necessary dependencies via the ImageJ update manager (Help > Update)", "Dependencies installed", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[]{"Close ImageJ", "Ignore"}, "Close ImageJ") == JOptionPane.YES_OPTION) {
                JIPipe.exitLater(0);
            }
        }
    }

    @Override
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {
        if (event.getRun() instanceof JIPipeDesktopActivateAndApplyUpdateSiteRun) {
            clickedInstallAll = false;
        }
    }
}
