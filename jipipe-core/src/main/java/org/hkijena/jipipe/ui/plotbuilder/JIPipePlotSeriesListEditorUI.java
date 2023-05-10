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

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Manages the list of series in a {@link PlotEditor}
 */
public class JIPipePlotSeriesListEditorUI extends JIPipeWorkbenchPanel {
    private PlotEditor plotBuilderUI;
    private FormPanel entriesPanel;

    /**
     * Creates a new instance
     *
     * @param workbench     the workbench
     * @param plotBuilderUI the plot builder
     */
    public JIPipePlotSeriesListEditorUI(JIPipeWorkbench workbench, PlotEditor plotBuilderUI) {
        super(workbench);
        this.plotBuilderUI = plotBuilderUI;
        initialize();
        reloadEntries();
        plotBuilderUI.getEventBus().register(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JToolBar toolBar = new JToolBar();
        JButton addSeriesButton = new JButton("Add series", UIUtils.getIconFromResources("actions/list-add.png"));
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
    @Override
    public void onParametersChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if (event.getKey().equals("series")) {
            reloadEntries();
        }
    }

    /**
     * Reloads entries in this panel
     */
    public void reloadEntries() {
        entriesPanel.clear();
        for (JIPipePlotSeriesBuilder seriesBuilder : plotBuilderUI.getSeriesBuilders()) {
            JIPipePlotSeriesBuilderUI builderUI = new JIPipePlotSeriesBuilderUI(getWorkbench(), seriesBuilder);
            builderUI.setPreferredSize(new Dimension((int) (0.33 * plotBuilderUI.getWidth()),
                    (int) builderUI.getPreferredSize().getHeight()));
            entriesPanel.addWideToForm(builderUI,
                    null);
        }
        entriesPanel.addVerticalGlue();
    }


}
