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

import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Manages the list of series in a {@link JFreeChartPlotEditor}
 */
public class JIPipePlotSeriesListEditorUI extends JIPipeDesktopWorkbenchPanel implements JIPipeParameterCollection.ParameterChangedEventListener {
    private final JFreeChartPlotEditor plotBuilderUI;
    private JIPipeDesktopFormPanel entriesPanel;

    /**
     * Creates a new instance
     *
     * @param workbench     the workbench
     * @param plotBuilderUI the plot builder
     */
    public JIPipePlotSeriesListEditorUI(JIPipeDesktopWorkbench workbench, JFreeChartPlotEditor plotBuilderUI) {
        super(workbench);
        this.plotBuilderUI = plotBuilderUI;
        initialize();
        reloadEntries();
        plotBuilderUI.getParameterChangedEventEmitter().subscribe(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JToolBar toolBar = new JToolBar();
        JButton addSeriesButton = new JButton("Add series", UIUtils.getIconFromResources("actions/list-add.png"));
        addSeriesButton.addActionListener(e -> plotBuilderUI.addSeries());
        toolBar.add(addSeriesButton);
        toolBar.setFloatable(false);

        add(toolBar, BorderLayout.NORTH);

        entriesPanel = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.WITH_SCROLLING);
        add(entriesPanel, BorderLayout.CENTER);
    }

    /**
     * Triggered when the plot builder's parameters are changed
     *
     * @param event generated event
     */
    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if (event.getKey().equals("series")) {
            reloadEntries();
        }
    }

    /**
     * Reloads entries in this panel
     */
    public void reloadEntries() {
        entriesPanel.clear();
        for (JIPipeDesktopPlotSeriesEditor seriesBuilder : plotBuilderUI.getSeriesBuilders()) {
            JIPipeDesktopPlotSeriesEditorUI builderUI = new JIPipeDesktopPlotSeriesEditorUI(getDesktopWorkbench(), seriesBuilder);
            builderUI.setPreferredSize(new Dimension((int) (0.33 * plotBuilderUI.getWidth()),
                    (int) builderUI.getPreferredSize().getHeight()));
            entriesPanel.addWideToForm(builderUI,
                    null);
        }
        entriesPanel.addVerticalGlue();
    }


}
