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

package org.hkijena.jipipe.ui.components;

import org.hkijena.jipipe.utils.UIUtils;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import javax.swing.*;
import java.awt.BorderLayout;

/**
 * Displays a plot
 */
public class PlotReader extends JPanel {

    private ChartPanel chartPanel;
    private JToolBar toolBar;

    /**
     * Creates a new instance
     */
    public PlotReader() {
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton exportButton = new JButton("Export", UIUtils.getIconFromResources("actions/save.png"));
        exportButton.addActionListener(e -> exportPlot());
        toolBar.add(exportButton);

        add(toolBar, BorderLayout.NORTH);

        chartPanel = new ChartPanel(null);
        add(chartPanel, BorderLayout.CENTER);

        chartPanel.setMinimumDrawWidth(0);
        chartPanel.setMaximumDrawWidth(Integer.MAX_VALUE);
        chartPanel.setMinimumDrawHeight(0);
        chartPanel.setMaximumDrawHeight(Integer.MAX_VALUE);
    }

    private void exportPlot() {
        if (chartPanel.getChart() != null) {
            PlotExporterDialog dialog = new PlotExporterDialog(chartPanel.getChart());
            dialog.pack();
            dialog.setSize(400, 300);
            dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
            dialog.setModal(true);
            dialog.setVisible(true);
        }
    }

    public JToolBar getToolBar() {
        return toolBar;
    }

    public ChartPanel getChartPanel() {
        return chartPanel;
    }

    /**
     * Redraws the plot
     */
    public void redrawPlot() {
        JFreeChart chart = chartPanel.getChart();
        chartPanel.setChart(null);
        chartPanel.revalidate();
        chartPanel.repaint();
        chartPanel.setChart(chart);
        chartPanel.revalidate();
        chartPanel.repaint();
    }
}
