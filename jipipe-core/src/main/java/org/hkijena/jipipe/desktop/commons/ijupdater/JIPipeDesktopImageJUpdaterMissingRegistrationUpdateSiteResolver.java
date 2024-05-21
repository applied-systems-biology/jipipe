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

import net.imagej.ui.swing.updater.ImageJUpdater;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.JIPipeRegistryIssues;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopMessagePanel;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopImageJUpdaterMissingRegistrationUpdateSiteResolver extends JFrame implements JIPipeDesktopWorkbench {

    private final Context context;
    private final JIPipeRegistryIssues issues;
    private final JIPipeNotificationInbox notificationInbox = new JIPipeNotificationInbox();

    private final JIPipeDesktopMessagePanel messagePanel = new JIPipeDesktopMessagePanel();

    private final JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.WITH_SCROLLING);

    public JIPipeDesktopImageJUpdaterMissingRegistrationUpdateSiteResolver(Context context, JIPipeRegistryIssues issues) {
        this.context = context;
        this.issues = issues;
        setSize(1024, 768);
        setTitle("JIPipe - Missing ImageJ dependencies");
        setIconImage(UIUtils.getJIPipeIcon128());
        getContentPane().setLayout(new BorderLayout());
        initialize();
    }

    private void initialize() {

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(messagePanel, BorderLayout.NORTH);

        formPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        formPanel.addWideToForm(UIUtils.createJLabel("Missing dependencies detected", UIUtils.getIcon32FromResources("dialog-warning.png"), 28));
        formPanel.addWideToForm(UIUtils.createBorderlessReadonlyTextPane("JIPipe detected missing ImageJ dependencies that are required for the functionality of various JIPipe extensions. You can ignore this message if you think that all required software is already installed.", false));
        formPanel.addWideToForm(Box.createVerticalStrut(32));
        if (!issues.getErroneousPlugins().isEmpty()) {
            formPanel.addWideToForm(UIUtils.createJLabel(issues.getErroneousPlugins().size() + " JIPipe plugins reported issues. We strongly recommend the installation of all dependencies.", UIUtils.getIconFromResources("emblems/emblem-important-blue.png")));
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
            formPanel.addWideToForm(UIUtils.createJLabel("Missing ImageJ update sites", 22));
            formPanel.addWideToForm(UIUtils.createJLabel(issues.getMissingImageJSites().size() + " update sites were found to be not activated. Please click the 'Start ImageJ updater' button and use the interface to activate and install the following update sites:", UIUtils.getIconFromResources("emblems/emblem-important-blue.png")));
            formPanel.addWideToForm(Box.createVerticalStrut(16));
            for (JIPipeImageJUpdateSiteDependency siteDependency : issues.getMissingImageJSites()) {
                formPanel.addWideToForm(UIUtils.createJLabel("<html><strong>" + siteDependency.getName() + "</strong> (" + siteDependency.getUrl() + ")</html>", UIUtils.getIconFromResources("actions/run-build-install.png")));
            }
        }

        formPanel.addVerticalGlue();
        getContentPane().add(formPanel, BorderLayout.CENTER);

        initializeButtonPanel();
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

        JButton confirmButton = new JButton("Start ImageJ updater", UIUtils.getIconFromResources("apps/imagej.png"));
        confirmButton.setFont(new Font(Font.DIALOG, Font.PLAIN, 16));
        confirmButton.addActionListener(e -> {
            ImageJUpdater updater = new ImageJUpdater();
            JIPipe.getInstance().getContext().inject(updater);
            updater.run();
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
}
