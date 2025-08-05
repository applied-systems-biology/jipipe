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
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
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
public abstract class JIPipeDesktopPublisherAssistant extends JIPipeDesktopProjectWorkbenchPanel {

    private final JIPipeDesktopFormPanel notificationList = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
    private final JButton confirmButton = new JButton("Publish now", JIPipe.RESOURCES.getIcon16("actions/share-nodes.png"));
    private final JLabel invalidMessage = new JLabel("Unable to publish. Please review the items below.", JIPipe.RESOURCES.getIcon16("emblems/warning.png"), JLabel.LEFT);
    private final JLabel warningMessage = new JLabel("Some additional checks are recommended. Please review the items below.", JIPipe.RESOURCES.getIcon16("emblems/emblem-important-blue.png"), JLabel.LEFT);
    private final List<JIPipeDesktopPublisherAssistantCondition>  conditions = new ArrayList<>();

    public JIPipeDesktopPublisherAssistant(JIPipeDesktopProjectWorkbench workbench) {
        super(workbench);
        initialize();
        updateAssistant();
    }

    private void initialize() {
        setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());
        setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        JPanel mainPanel = new JPanel();

        setLayout(new BorderLayout(8,8));
        add(UIUtils.wrapInIslandPanelIfNeeded(mainPanel), BorderLayout.NORTH);
        add(UIUtils.wrapInIslandPanelIfNeeded(notificationList), BorderLayout.CENTER);

        initializeMainPanel(mainPanel);
    }

    private void initializeMainPanel(JPanel mainPanel) {
        mainPanel.setLayout(new BorderLayout(8,8));

        // Add Header and description
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
                BorderFactory.createMatteBorder(1,0,0,0, ThemeUtils.getCurrentStyle().getBorderColor())));
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(invalidMessage);
        buttonPanel.add(Box.createHorizontalStrut(16));
        buttonPanel.add(UIUtils.createButton("Refresh", JIPipe.RESOURCES.getIcon16("actions/view-refresh.png"), this::updateAssistant));
        buttonPanel.add(confirmButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    public void addAssistantCondition(JIPipeDesktopPublisherAssistantCondition condition) {
        conditions.add(condition);
        notificationList.addWideToForm(condition);
        updateAssistant();
    }

    public abstract String getAssistantTitle();
    public abstract HTMLText getAssistantDescription();
    public abstract List<BufferedImage> getAssistantLogos();

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
        warningMessage.setVisible(warning);
        invalidMessage.setVisible(!valid);
    }
}
