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
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunExecuteUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.utils.BufferedImageUtils;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class that handles UI for publishing-related tasks.
 * Designed for displaying conditions (fulfilled/unfulfilled/warning) to users
 */
public abstract class JIPipeDesktopPublisherAssistant extends JIPipeDesktopProjectWorkbenchPanel implements JIPipeRunnable.FinishedEventListener {

    private final JIPipeDesktopFormPanel notificationList = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
    private final JButton confirmButton = UIUtils.createButton("Publish now", JIPipe.RESOURCES.getIcon16("actions/share-nodes.png"), this::startPublish);
    private final  JPanel mainPanel = new JPanel();
    private final JIPipeRunnableQueue queue = new JIPipeRunnableQueue("Publish Local");

    private final JButton refreshButton = UIUtils.createButton("Refresh", JIPipe.RESOURCES.getIcon16("actions/view-refresh.png"), this::updateAssistant);
    private final JLabel invalidMessage = new JLabel("Unable to publish. Please review the items below.", JIPipe.RESOURCES.getIcon16("emblems/warning.png"), JLabel.LEFT);
    private final JLabel warningMessage = new JLabel("Some additional checks are recommended. Please review the items below.", JIPipe.RESOURCES.getIcon16("emblems/emblem-important-blue.png"), JLabel.LEFT);
    private final List<JIPipeDesktopPublisherAssistantCondition>  conditions = new ArrayList<>();

    public JIPipeDesktopPublisherAssistant(JIPipeDesktopProjectWorkbench workbench) {
        super(workbench);
        initialize();
        updateAssistant();
        queue.getFinishedEventEmitter().subscribe(this);
    }

    private void initialize() {
        setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());
        setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        setLayout(new BorderLayout(8,8));


        initializeMainPanel();

        switchToSetup();
    }

    private void switchToSetup() {
        removeAll();

        confirmButton.setEnabled(false);
        refreshButton.setEnabled(true);

        add(UIUtils.wrapInIslandPanelIfNeeded(mainPanel), BorderLayout.NORTH);
        add(UIUtils.wrapInIslandPanelIfNeeded(notificationList), BorderLayout.CENTER);

        revalidate();
        repaint(50);

        updateAssistant();
    }

    private void switchToExecution(JIPipeRunnable runnable) {
        removeAll();

        confirmButton.setEnabled(false);
        refreshButton.setEnabled(false);

        JIPipeDesktopRunExecuteUI runExecuteUI = new JIPipeDesktopRunExecuteUI(getDesktopProjectWorkbench(), runnable, queue);

        add(UIUtils.wrapInIslandPanelIfNeeded(mainPanel), BorderLayout.NORTH);
        add(UIUtils.wrapInIslandPanelIfNeeded(runExecuteUI), BorderLayout.CENTER);

        revalidate();
        repaint(50);

        runExecuteUI.startRun();
    }

    private void initializeMainPanel() {
        mainPanel.setLayout(new BorderLayout(8,8));

        mainPanel.add(UIUtils.createJLabel(getAssistantTitle(), JIPipe.RESOURCES.getIcon32("actions/document-export.png"), ThemeUtils.getCurrentStyle().getFontSizeLarge()), BorderLayout.NORTH);
        mainPanel.add(UIUtils.createBorderlessReadonlyTextPane(getAssistantDescription().getHtml(), false), BorderLayout.CENTER);

        // Add logos into the button panel
        JPanel buttonPanel = UIUtils.boxHorizontal();
        List<BufferedImage> logos = getAssistantLogos();
        if(!logos.isEmpty()) {

            for (BufferedImage logo : logos) {
                BufferedImage scaledLogo = BufferedImageUtils.scaleImageToFit(logo, 250, 42);
                JLabel label = new JLabel(new ImageIcon(scaledLogo));
                label.setBorder(BorderFactory.createEmptyBorder(8,8,0,8));
                buttonPanel.add(label);
            }

        }

        buttonPanel.setBorder(BorderFactory.createCompoundBorder(UIUtils.createEmptyBorder(8),
                BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1,0,0,0, ThemeUtils.getCurrentStyle().getBorderColor()),
                        BorderFactory.createEmptyBorder(8,0,0,0))));
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(warningMessage);
        buttonPanel.add(invalidMessage);
        buttonPanel.add(Box.createHorizontalStrut(16));
        buttonPanel.add(refreshButton);
        confirmButton.setBorder(UIUtils.createButtonBorder(ThemeUtils.getCurrentStyle().getSuccessColor()));
        buttonPanel.add(confirmButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    public void addAssistantCondition(JIPipeDesktopPublisherAssistantCondition condition) {
        conditions.add(condition);
        notificationList.addWideToForm(condition);
        updateAssistant();
    }

    private void startPublish() {
        JIPipeRunnable assistantTask = createAssistantTask();
        if(assistantTask != null) {
            switchToExecution(assistantTask);
        }
    }

    public abstract String getAssistantTitle();
    public abstract HTMLText getAssistantDescription();
    public abstract List<BufferedImage> getAssistantLogos();
    public abstract JIPipeRunnable createAssistantTask();
    public abstract void onPublicationFinished(JIPipeRunnable runnable);

    public void updateAssistant() {
        boolean valid = true;
        boolean warning = false;
        for (JIPipeDesktopPublisherAssistantCondition condition : conditions) {
            condition.updateAssistant();
            JIPipeDesktopPublisherAssistantConditionStatus status = condition.getStatus();
            if(status == JIPipeDesktopPublisherAssistantConditionStatus.Invalid) {
                valid = false;
            }
            else if(status == JIPipeDesktopPublisherAssistantConditionStatus.Warning) {
                warning = true;
            }
        }

        confirmButton.setEnabled(valid);
        warningMessage.setVisible(warning && valid);
        invalidMessage.setVisible(!valid);
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        onPublicationFinished(event.getRun());
        closePublisher();
    }

    private void closePublisher() {
        Container tabPane = SwingUtilities.getAncestorOfClass(JIPipeDesktopTabPane.class, this);
        if(tabPane instanceof JIPipeDesktopTabPane) {
            JIPipeDesktopTabPane.DocumentTab tab = ((JIPipeDesktopTabPane) tabPane).findTabFor(this);
            if(tab != null) {
                ((JIPipeDesktopTabPane) tabPane).forceCloseTab(tab);
            }
        }
    }
}
