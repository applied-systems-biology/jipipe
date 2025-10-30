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

package org.hkijena.jipipe.desktop.app.publish;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.parameters.JIPipeDummyParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableLogEntry;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunExecuteUI;
import org.hkijena.jipipe.desktop.app.running.logs.JIPipeDesktopRunnableLogsCollection;
import org.hkijena.jipipe.desktop.commons.components.panels.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.parameters.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.BufferedImageUtils;
import org.hkijena.jipipe.utils.JIPipeDesktopSplitPane;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class that handles UI for publishing-related tasks.
 * Designed for displaying conditions (fulfilled/unfulfilled/warning) to users
 */
public abstract class JIPipeDesktopPublisherAssistant extends JIPipeDesktopProjectWorkbenchPanel implements JIPipeRunnable.FinishedEventListener, JIPipeRunnable.InterruptedEventListener {

    private final JIPipeDesktopFormPanel notificationList = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
    private final JIPipeDesktopSplitPane splitPane = new JIPipeDesktopSplitPane(JIPipeDesktopSplitPane.LEFT_RIGHT, new JIPipeDesktopSplitPane.DynamicSidebarRatio(350, false));
    private final JPanel setupPanel = new JPanel();
    private final JIPipeDesktopParameterFormPanel parameterPanel = new JIPipeDesktopParameterFormPanel(getDesktopWorkbench(), new JIPipeDummyParameterCollection(), MarkdownText.EMPTY, JIPipeDesktopFormPanel.WITH_SCROLLING | JIPipeDesktopFormPanel.WITH_DOCUMENTATION | JIPipeDesktopParameterFormPanel.DOCUMENTATION_NO_UI);
    private final JIPipeRunnableQueue queue = new JIPipeRunnableQueue("Publish Local");
    private final JButton confirmButton = UIUtils.createButton("Publish now", JIPipe.RESOURCES.getIcon16("actions/share-nodes.png"), this::startPublish);
    private final JLabel invalidMessage = new JLabel("Unable to publish. Please review the items on the left.", JIPipe.RESOURCES.getIcon16("emblems/warning.png"), JLabel.LEFT);
    private final JLabel warningMessage = new JLabel("Some additional checks are recommended. Please review the items on the left.", JIPipe.RESOURCES.getIcon16("emblems/emblem-important-blue.png"), JLabel.LEFT);
    private final JButton refreshButton = UIUtils.createButton("Refresh", JIPipe.RESOURCES.getIcon16("actions/view-refresh.png"), this::updateAssistant);
    private final List<JIPipeDesktopPublisherAssistantCondition> conditions = new ArrayList<>();
    private JIPipeDesktopPublisherAssistantConditionStatus currentStatus = JIPipeDesktopPublisherAssistantConditionStatus.Invalid;

    public JIPipeDesktopPublisherAssistant(JIPipeDesktopProjectWorkbench workbench) {
        super(workbench);
        initialize();
        queue.getFinishedEventEmitter().subscribe(this);
        queue.getInterruptedEventEmitter().subscribe(this);
    }

    private void initialize() {
        setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        setLayout(new BorderLayout(8, 8));
        add(splitPane, BorderLayout.CENTER);

        initializeSetupPanel();
    }

    public void postInit() {
        switchToSetup();
    }

    private void switchToSetup() {

        confirmButton.setEnabled(false);
        refreshButton.setEnabled(true);

        splitPane.setLeftComponent(UIUtils.wrapInIslandPanelIfNeeded(notificationList));
        splitPane.setRightComponent(UIUtils.wrapInIslandPanelIfNeeded(setupPanel));

        revalidate();
        repaint(50);

        updateAssistant();

        splitPane.applyRatio();
    }


    private void switchToExecution(JIPipeRunnable runnable) {

        confirmButton.setEnabled(false);
        refreshButton.setEnabled(false);

        JIPipeDesktopRunExecuteUI runExecuteUI = new JIPipeDesktopRunExecuteUI(getDesktopProjectWorkbench(), runnable, queue);

        splitPane.setLeftComponent(UIUtils.wrapInIslandPanelIfNeeded(runExecuteUI));
        splitPane.setRightComponent(UIUtils.wrapInIslandPanelIfNeeded(setupPanel));

        revalidate();
        repaint(50);

        runExecuteUI.startRun();

        splitPane.applyRatio();
    }

    private void initializeSetupPanel() {
        setupPanel.setLayout(new BorderLayout(8, 8));

        // Add title
        JPanel titlePanel = new JPanel(new BorderLayout(8, 8));
        titlePanel.add(UIUtils.createJLabel(getAssistantTitle(), JIPipe.RESOURCES.getIcon32("actions/document-export.png"), ThemeUtils.getCurrentStyle().getFontSizeLarge()), BorderLayout.NORTH);
        titlePanel.add(UIUtils.createBorderlessReadonlyTextPane(getAssistantDescription().getHtml(), false), BorderLayout.CENTER);

        List<BufferedImage> logos = getAssistantLogos();
        JPanel logoPanel = UIUtils.boxHorizontal();
        if (!logos.isEmpty()) {
            for (BufferedImage logo : logos) {
                BufferedImage scaledLogo = BufferedImageUtils.scaleImageToFit(logo, 250, 42);
                JLabel label = new JLabel(new ImageIcon(scaledLogo));
                label.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
                logoPanel.add(label);
            }
        }
        titlePanel.add(logoPanel, BorderLayout.SOUTH);
        setupPanel.add(titlePanel, BorderLayout.NORTH);

        // Add settings
        setupPanel.add(parameterPanel, BorderLayout.CENTER);

        // Create button panel
        JPanel buttonPanel = UIUtils.boxVertical();
        buttonPanel.setBorder(BorderFactory.createCompoundBorder(UIUtils.createEmptyBorder(8),
                BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeUtils.getCurrentStyle().getBorderColor()),
                        BorderFactory.createEmptyBorder(8, 0, 0, 0))));
        buttonPanel.add(UIUtils.wrapInCenterPanel(warningMessage));
        buttonPanel.add(UIUtils.wrapInCenterPanel(invalidMessage));
        buttonPanel.add(Box.createVerticalStrut(16));
        buttonPanel.add(UIUtils.boxHorizontal(Box.createHorizontalGlue(), refreshButton, confirmButton));
        confirmButton.setBorder(UIUtils.createButtonBorder(ThemeUtils.getCurrentStyle().getSuccessColor()));
        setupPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    public void addAssistantCondition(JIPipeDesktopPublisherAssistantCondition condition) {
        conditions.add(condition);
        notificationList.addWideToForm(condition);
        updateAssistant();
    }

    private void startPublish() {
        updateAssistant();
        if (currentStatus == JIPipeDesktopPublisherAssistantConditionStatus.Warning) {
            if (JOptionPane.showConfirmDialog(this, "Potential issues with were detected. Do you still want to continue?",
                    getAssistantTitle(), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.NO_OPTION) {
                return;
            }
        }
        if (currentStatus == JIPipeDesktopPublisherAssistantConditionStatus.Invalid) {
            JOptionPane.showMessageDialog(this, "The project cannot be published in the current state.",
                    getAssistantTitle(), JOptionPane.ERROR_MESSAGE);
            return;
        }
        JIPipeRunnable assistantTask = createAssistantTask();
        if (assistantTask != null) {
            switchToExecution(assistantTask);
        }
    }

    public abstract String getAssistantTitle();

    public abstract HTMLText getAssistantDescription();

    public abstract List<BufferedImage> getAssistantLogos();

    public abstract JIPipeRunnable createAssistantTask();

    public abstract void onPublicationFinished(JIPipeRunnable runnable);

    public abstract JIPipeParameterCollection getAssistantParameters();

    public void updateAssistant() {
        boolean valid = true;
        boolean warning = false;
        for (JIPipeDesktopPublisherAssistantCondition condition : conditions) {
            condition.updateAssistant();
            JIPipeDesktopPublisherAssistantConditionStatus status = condition.getStatus();
            if (status == JIPipeDesktopPublisherAssistantConditionStatus.Invalid) {
                valid = false;
            } else if (status == JIPipeDesktopPublisherAssistantConditionStatus.Warning) {
                warning = true;
            }
        }

        if (valid && warning) {
            this.currentStatus = JIPipeDesktopPublisherAssistantConditionStatus.Warning;
        } else if (valid) {
            this.currentStatus = JIPipeDesktopPublisherAssistantConditionStatus.Valid;
        } else {
            this.currentStatus = JIPipeDesktopPublisherAssistantConditionStatus.Invalid;
        }
        confirmButton.setEnabled(valid);
        warningMessage.setVisible(warning && valid);
        invalidMessage.setVisible(!valid);
        parameterPanel.setDisplayedParameters(getAssistantParameters());
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        JIPipeDesktopRunnableLogsCollection.getInstance().pushToLog(new JIPipeRunnableLogEntry(getAssistantTitle(),
                LocalDateTime.now(),
                event.getRun().getProgressInfo().getLog().toString(),
                new JIPipeNotificationInbox(),
                true));
        onPublicationFinished(event.getRun());
        closePublisher();
    }

    private void closePublisher() {
        Container tabPane = SwingUtilities.getAncestorOfClass(JIPipeDesktopTabPane.class, this);
        if (tabPane instanceof JIPipeDesktopTabPane) {
            JIPipeDesktopTabPane.DocumentTab tab = ((JIPipeDesktopTabPane) tabPane).findTabFor(this);
            if (tab != null) {
                ((JIPipeDesktopTabPane) tabPane).forceCloseTab(tab);
            }
        }
    }

    public JIPipeDesktopPublisherAssistantConditionStatus getCurrentStatus() {
        return currentStatus;
    }


    @Override
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {
        JOptionPane.showMessageDialog(this, "There were errors during the export process.\n" +
                        "Please open the JIPipe log to review them.",
                getAssistantTitle(), JOptionPane.ERROR_MESSAGE);
        JIPipeDesktopRunnableLogsCollection.getInstance().pushToLog(new JIPipeRunnableLogEntry(getAssistantTitle(),
                LocalDateTime.now(),
                event.getRun().getProgressInfo().getLog().toString(),
                new JIPipeNotificationInbox(),
                false));
        switchToSetup();
    }
}
