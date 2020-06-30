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

package org.hkijena.acaq5.ui.plotbuilder;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.ACAQWorkbenchPanel;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * Manages the list of series in a {@link ACAQPlotBuilderUI}
 */
public class ACAQPlotSeriesListEditorUI extends ACAQWorkbenchPanel {
    private ACAQPlotBuilderUI plotBuilderUI;
    private FormPanel entriesPanel;

    /**
     * Creates a new instance
     *
     * @param workbench     the workbench
     * @param plotBuilderUI the plot builder
     */
    public ACAQPlotSeriesListEditorUI(ACAQWorkbench workbench, ACAQPlotBuilderUI plotBuilderUI) {
        super(workbench);
        this.plotBuilderUI = plotBuilderUI;
        initialize();
        reloadEntries();
        plotBuilderUI.getEventBus().register(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JToolBar toolBar = new JToolBar();
        JButton addSeriesButton = new JButton("Add series", UIUtils.getIconFromResources("add.png"));
        addSeriesButton.addActionListener(e -> plotBuilderUI.addSeries());
        toolBar.add(addSeriesButton);
        toolBar.setFloatable(false);

        add(toolBar, BorderLayout.NORTH);

        entriesPanel = new FormPanel(null, FormPanel.WITH_SCROLLING);
        add(entriesPanel, BorderLayout.CENTER);
    }

    /**
     * Triggered when the plot builder's parameters are changed
     *
     * @param event generated event
     */
    @Subscribe
    public void onParametersChanged(ParameterChangedEvent event) {
        if (event.getKey().equals("series")) {
            reloadEntries();
        }
    }

    /**
     * Reloads entries in this panel
     */
    public void reloadEntries() {
        entriesPanel.clear();
        for (ACAQPlotSeriesBuilder seriesBuilder : plotBuilderUI.getSeriesBuilders()) {
            ACAQPlotSeriesBuilderUI builderUI = new ACAQPlotSeriesBuilderUI(getWorkbench(), seriesBuilder);
            builderUI.setPreferredSize(new Dimension((int) (0.33 * plotBuilderUI.getWidth()),
                    (int) builderUI.getPreferredSize().getHeight()));
            entriesPanel.addToForm(builderUI,
                    null);
        }
        entriesPanel.addVerticalGlue();
    }


}
