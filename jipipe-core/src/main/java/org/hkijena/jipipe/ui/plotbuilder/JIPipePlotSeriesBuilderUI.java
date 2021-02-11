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

package org.hkijena.jipipe.ui.plotbuilder;

import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * UI for {@link JIPipePlotSeriesBuilder}
 */
public class JIPipePlotSeriesBuilderUI extends JIPipeWorkbenchPanel {
    private JIPipePlotSeriesBuilder seriesBuilder;
    private JButton moveDownButton;
    private JButton moveUpButton;
    private JButton removeButton;
    private JButton enableToggleButton;

    /**
     * @param workbench     Parent workbench
     * @param seriesBuilder the series builder
     */
    public JIPipePlotSeriesBuilderUI(JIPipeWorkbench workbench, JIPipePlotSeriesBuilder seriesBuilder) {
        super(workbench);
        this.seriesBuilder = seriesBuilder;
        initialize();
    }

    private void initialize() {
        setBorder(BorderFactory.createLineBorder(Color.GRAY));
        setLayout(new BorderLayout());
        ParameterPanel parameterPanel = new ParameterPanel(getWorkbench(),
                seriesBuilder,
                null,
                ParameterPanel.NO_EMPTY_GROUP_HEADERS);
        add(parameterPanel, BorderLayout.CENTER);

        // Create title panel
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.LINE_AXIS));
        titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        titlePanel.setBackground(Color.LIGHT_GRAY);
        titlePanel.setOpaque(true);

        moveDownButton = new JButton(UIUtils.getIconFromResources("actions/arrow-down.png"));
        UIUtils.makeBorderlessWithoutMargin(moveDownButton);
        moveDownButton.setToolTipText("Move down");
        moveDownButton.addActionListener(e -> seriesBuilder.getPlotBuilderUI().moveSeriesDown(seriesBuilder));
        titlePanel.add(moveDownButton);

        moveUpButton = new JButton(UIUtils.getIconFromResources("actions/arrow-up.png"));
        UIUtils.makeBorderlessWithoutMargin(moveUpButton);
        moveUpButton.setToolTipText("Move up");
        moveUpButton.addActionListener(e -> seriesBuilder.getPlotBuilderUI().moveSeriesUp(seriesBuilder));
        titlePanel.add(moveUpButton);

        titlePanel.add(Box.createHorizontalGlue());
        titlePanel.add(Box.createHorizontalStrut(8));

        removeButton = new JButton(UIUtils.getIconFromResources("actions/delete.png"));
        removeButton.setToolTipText("Remove series");
        removeButton.addActionListener(e -> removeSeries());
        UIUtils.makeBorderlessWithoutMargin(removeButton);
        titlePanel.add(removeButton);

        enableToggleButton = new JButton();
        UIUtils.makeBorderlessWithoutMargin(enableToggleButton);
        enableToggleButton.addActionListener(e -> toggleEnableDisable());
        titlePanel.add(Box.createHorizontalStrut(4));
        titlePanel.add(enableToggleButton);
        updateEnableDisableToggleButton();

        add(titlePanel, BorderLayout.NORTH);
    }

    private void removeSeries() {
        seriesBuilder.getPlotBuilderUI().removeSeries(seriesBuilder);
    }

    private void toggleEnableDisable() {
        seriesBuilder.setEnabled(!seriesBuilder.isEnabled());
        seriesBuilder.getEventBus().post(new JIPipeParameterCollection.ParameterChangedEvent(seriesBuilder, "enabled"));
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
