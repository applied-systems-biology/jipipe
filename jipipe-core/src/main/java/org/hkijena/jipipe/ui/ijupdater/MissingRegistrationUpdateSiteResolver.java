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

package org.hkijena.jipipe.ui.ijupdater;

import com.google.common.eventbus.Subscribe;
import net.imagej.updater.UpdateSite;
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.JIPipeRegistryIssues;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.MessagePanel;
import org.hkijena.jipipe.ui.components.icons.AnimatedIcon;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.extensions.ActivateAndApplyUpdateSiteRun;
import org.hkijena.jipipe.ui.extensions.ExtensionItemActionButton;
import org.hkijena.jipipe.ui.extensions.JIPipeModernPluginManager;
import org.hkijena.jipipe.ui.extensions.UpdateSiteExtension;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MissingRegistrationUpdateSiteResolver extends JDialog implements JIPipeWorkbench {

    private final Context context;
    private final JIPipeRegistryIssues issues;
    private final JIPipeNotificationInbox notificationInbox = new JIPipeNotificationInbox();

    private final MessagePanel messagePanel = new MessagePanel();
    private final JIPipeModernPluginManager pluginManager;

    private final FormPanel formPanel = new FormPanel(null, FormPanel.WITH_SCROLLING);

    private boolean clickedInstallAll = false;

    public MissingRegistrationUpdateSiteResolver(Context context, JIPipeRegistryIssues issues) {
        this.context = context;
        this.issues = issues;
        setSize(1024, 768);
        setTitle("Missing ImageJ dependencies");
        setModal(true);
        getContentPane().setLayout(new BorderLayout());
        initialize();

        pluginManager = new JIPipeModernPluginManager(this, messagePanel);
        pluginManager.getEventBus().register(this);
        pluginManager.initializeUpdateSites();

        JIPipeRunnerQueue.getInstance().getEventBus().register(this);
    }

    private void initialize() {

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(messagePanel, BorderLayout.NORTH);

        formPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        formPanel.addWideToForm(UIUtils.createJLabel("Missing dependencies detected", UIUtils.getIcon32FromResources("dialog-warning.png"), 28));
        formPanel.addWideToForm(UIUtils.makeBorderlessReadonlyTextPane("JIPipe detected missing ImageJ dependencies that are required for the functionality of various JIPipe extensions. You can ignore this message if you think that all required software is already installed.", false));
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
            AnimatedIcon hourglassAnimation = new AnimatedIcon(this, UIUtils.getIconFromResources("actions/hourglass-half.png"),
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
        ActivateAndApplyUpdateSiteRun run = new ActivateAndApplyUpdateSiteRun(pluginManager, updateSiteSet);
        JIPipeRunExecuterUI.runInDialog(this, run);
    }

    @Subscribe
    public void onUpdateSitesReady(JIPipeModernPluginManager.UpdateSitesReadyEvent event) {
        if (!issues.getMissingImageJSites().isEmpty()) {
            formPanel.removeLastRow(); //Vertical glue
            formPanel.removeLastRow(); // "Please wait..."
            for (JIPipeImageJUpdateSiteDependency dependency : issues.getMissingImageJSites()) {
                JPanel dependencyPanel = new JPanel(new GridBagLayout());
                dependencyPanel.setBorder(BorderFactory.createCompoundBorder(new RoundedLineBorder(UIManager.getColor("Button.borderColor"), 1, 2), BorderFactory.createEmptyBorder(8, 8, 8, 8)));
                dependencyPanel.add(UIUtils.createJLabel(dependency.getName(), UIUtils.getIcon32FromResources("module-imagej.png"), 16), new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));
                JTextField idField = UIUtils.makeReadonlyBorderlessTextField("URL: " + dependency.getUrl());
                idField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
                dependencyPanel.add(idField, new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));
                dependencyPanel.add(UIUtils.makeBorderlessReadonlyTextPane(dependency.getDescription(), false), new GridBagConstraints(0, 2, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));

                // Try to find the extension
                UpdateSiteExtension extension = new UpdateSiteExtension(dependency);
                ExtensionItemActionButton button = new ExtensionItemActionButton(pluginManager, extension);
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
    public Context getContext() {
        return context;
    }

    @Override
    public DocumentTabPane getDocumentTabPane() {
        return null;
    }

    @Override
    public JIPipeNotificationInbox getNotificationInbox() {
        return notificationInbox;
    }

    @Subscribe
    public void onUpdateSiteActivated(JIPipeRunnable.FinishedEvent event) {
        if (event.getRun() instanceof ActivateAndApplyUpdateSiteRun && this.clickedInstallAll) {
            if (JOptionPane.showOptionDialog(this, "Please close and restart ImageJ to complete the installation of updates. " +
                    "If you have any issues, please install the necessary dependencies via the ImageJ update manager (Help > Update)", "Dependencies installed", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[]{"Close ImageJ", "Ignore"}, "Close ImageJ") == JOptionPane.YES_OPTION) {
                System.exit(0);
            }
        }
    }

    @Subscribe
    public void onUpdateSiteInstallationInterrupted(JIPipeRunnable.InterruptedEvent event) {
        if (event.getRun() instanceof ActivateAndApplyUpdateSiteRun) {
            clickedInstallAll = false;
        }
    }
}
