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

package org.hkijena.jipipe.desktop.app.ploteditor;

import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernMetalTheme;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * UI for {@link JIPipeDesktopPlotSeriesEditor}
 */
public class JIPipeDesktopPlotSeriesEditorUI extends JIPipeDesktopWorkbenchPanel {
    private JIPipeDesktopPlotSeriesEditor seriesBuilder;
    private JButton moveDownButton;
    private JButton moveUpButton;
    private JButton removeButton;
    private JButton enableToggleButton;

    /**
     * @param workbench     Parent workbench
     * @param seriesBuilder the series builder
     */
    public JIPipeDesktopPlotSeriesEditorUI(JIPipeDesktopWorkbench workbench, JIPipeDesktopPlotSeriesEditor seriesBuilder) {
        super(workbench);
        this.seriesBuilder = seriesBuilder;
        initialize();
    }

    private void initialize() {
        setBorder(UIUtils.createControlBorder());
        setLayout(new BorderLayout());
        JIPipeDesktopParameterFormPanel parameterPanel = new JIPipeDesktopParameterFormPanel(getDesktopWorkbench(),
                seriesBuilder,
                null,
                JIPipeDesktopParameterFormPanel.NO_EMPTY_GROUP_HEADERS);
        add(parameterPanel, BorderLayout.CENTER);

        // Create title panel
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.LINE_AXIS));
        titlePanel.setBorder(UIUtils.createControlBorder());
        titlePanel.setBackground(JIPipeDesktopModernMetalTheme.MEDIUM_GRAY);
        titlePanel.setOpaque(true);

        moveDownButton = new JButton(UIUtils.getIconFromResources("actions/caret-down.png"));
        UIUtils.makeButtonBorderlessWithoutMargin(moveDownButton);
        moveDownButton.setToolTipText("Move down");
        moveDownButton.addActionListener(e -> seriesBuilder.getPlotBuilderUI().moveSeriesDown(seriesBuilder));
        titlePanel.add(moveDownButton);

        moveUpButton = new JButton(UIUtils.getIconFromResources("actions/caret-up.png"));
        UIUtils.makeButtonBorderlessWithoutMargin(moveUpButton);
        moveUpButton.setToolTipText("Move up");
        moveUpButton.addActionListener(e -> seriesBuilder.getPlotBuilderUI().moveSeriesUp(seriesBuilder));
        titlePanel.add(moveUpButton);

        titlePanel.add(Box.createHorizontalGlue());
        titlePanel.add(Box.createHorizontalStrut(8));

        removeButton = new JButton(UIUtils.getIconFromResources("actions/delete.png"));
        removeButton.setToolTipText("Remove series");
        removeButton.addActionListener(e -> removeSeries());
        UIUtils.makeButtonBorderlessWithoutMargin(removeButton);
        titlePanel.add(removeButton);

        enableToggleButton = new JButton();
        UIUtils.makeButtonBorderlessWithoutMargin(enableToggleButton);
        enableToggleButton.addActionListener(e -> toggleEnableDisable());
        titlePanel.add(Box.createHorizontalStrut(4));
        titlePanel.add(enableToggleButton);
        updateEnableDisableToggleButton();

        add(titlePanel, BorderLayout.NORTH);
    }

    private void removeSeries() {
        if (JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(this),
                "Do you really want to remove the series '" + seriesBuilder.getName() + "'?",
                "Delete series",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
            seriesBuilder.getPlotBuilderUI().removeSeries(seriesBuilder);
        }
    }

    private void toggleEnableDisable() {
        seriesBuilder.setEnabled(!seriesBuilder.isEnabled());
        seriesBuilder.emitParameterChangedEvent("enabled");
        updateEnableDisableToggleButton();
    }

    private void updateEnableDisableToggleButton() {
        if (seriesBuilder.isEnabled()) {
            enableToggleButton.setIcon(UIUtils.getIconFromResources("actions/eye.png"));
            enableToggleButton.setToolTipText("Disable series");
        } else {
            enableToggleButton.setIcon(UIUtils.getIconFromResources("actions/eye-slash.png"));
            enableToggleButton.setToolTipText("Enable series");
        }
    }
}
