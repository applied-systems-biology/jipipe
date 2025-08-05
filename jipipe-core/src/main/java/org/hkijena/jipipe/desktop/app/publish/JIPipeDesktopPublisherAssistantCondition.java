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
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * A condition that is checked by the {@link JIPipeDesktopPublisherAssistant}
 */
public abstract class JIPipeDesktopPublisherAssistantCondition extends JIPipeDesktopProjectWorkbenchPanel {

    private final JLabel titleLabel = new  JLabel();
    private final JTextPane descriptionLabel = UIUtils.createBorderlessReadonlyTextPane("", false);
    private final JPanel buttonPanel = UIUtils.boxHorizontal();
    private final JIPipeDesktopPublisherAssistant assistant;

    public JIPipeDesktopPublisherAssistantCondition(JIPipeDesktopPublisherAssistant assistant) {
        super(assistant.getDesktopProjectWorkbench());
        this.assistant = assistant;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout(8,8));
        setBorder(UIUtils.createControlBorder());
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, ThemeUtils.getCurrentStyle().getFontSizeLarge()));
        buttonPanel.setBorder(BorderFactory.createCompoundBorder(UIUtils.createEmptyBorder(8),
                BorderFactory.createMatteBorder(1,0,0,0, ThemeUtils.getCurrentStyle().getBorderColor())));
        buttonPanel.add(Box.createHorizontalGlue());

        add(titleLabel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.SOUTH);
        add(descriptionLabel, BorderLayout.CENTER);
    }

    public abstract JIPipeDesktopPublisherAssistantConditionStatus getStatus();

    public abstract String getAssistantTitle(JIPipeDesktopPublisherAssistantConditionStatus  status);

    public abstract HTMLText getAssistantDescription(JIPipeDesktopPublisherAssistantConditionStatus status);

    public void updateAssistant() {
        JIPipeDesktopPublisherAssistantConditionStatus status = getStatus();
        switch (status) {
            case Valid -> {
                buttonPanel.setVisible(false);
                titleLabel.setIcon(JIPipe.RESOURCES.getIcon32("emblems/vcs-normal.png"));
            }
            case Invalid -> {
                if(buttonPanel.getComponentCount() > 1) {
                    buttonPanel.setVisible(true);
                }
                titleLabel.setIcon(JIPipe.RESOURCES.getIcon32("emblems/vcs-conflicting.png"));
            }
            case Warning -> {
                if(buttonPanel.getComponentCount() > 1) {
                    buttonPanel.setVisible(true);
                }
                titleLabel.setIcon(JIPipe.RESOURCES.getIcon32("emblems/emblem-warning-orange.png"));
            }
        }
        titleLabel.setText(getAssistantTitle(status));
        descriptionLabel.setText(getAssistantDescription(status).getHtml());
    }

    public void addButton(JButton button) {
        buttonPanel.add(button);
        button.addActionListener(e -> assistant.updateAssistant());
    }
}
