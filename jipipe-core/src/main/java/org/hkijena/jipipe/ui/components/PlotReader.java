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

import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.plotbuilder.PlotEditor;
import org.hkijena.jipipe.utils.CopyImageToClipboard;
import org.hkijena.jipipe.utils.UIUtils;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.jfree.graphics2d.svg.SVGUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Displays a plot
 */
public class PlotReader extends JPanel {

    private final PlotEditor plotBuilderUI;
    private ChartPanel chartPanel;
    private JToolBar toolBar;

    /**
     * Creates a new instance
     *
     * @param plotBuilderUI the plot builder associated to this reader
     */
    public PlotReader(PlotEditor plotBuilderUI) {
        this.plotBuilderUI = plotBuilderUI;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton exportButton = new JButton("Export", UIUtils.getIconFromResources("actions/save.png"));
        JPopupMenu exportMenu = UIUtils.addPopupMenuToComponent(exportButton);

        JMenuItem copyCurrentPlot = new JMenuItem("Copy to clipboard (current size)",
                UIUtils.getIconFromResources("actions/edit-copy.png"));
        copyCurrentPlot.addActionListener(e -> exportPlotToClipboard(true));
        exportMenu.add(copyCurrentPlot);

        JMenuItem copyPlot = new JMenuItem("Copy to clipboard (exported size)",
                UIUtils.getIconFromResources("actions/edit-copy.png"));
        copyPlot.addActionListener(e -> exportPlotToClipboard(false));
        exportMenu.add(copyPlot);

        exportMenu.addSeparator();

        JMenuItem exportCurrentPlotAsPNG = new JMenuItem("as *.png (current size)",
                UIUtils.getIconFromResources("actions/viewimage.png"));
        exportCurrentPlotAsPNG.addActionListener(e -> exportPlotToFile(true, FileFormat.PNG));
        exportMenu.add(exportCurrentPlotAsPNG);

        JMenuItem exportCurrentPlotAsJPEG = new JMenuItem("as *.jpeg (current size)",
                UIUtils.getIconFromResources("actions/viewimage.png"));
        exportCurrentPlotAsJPEG.addActionListener(e -> exportPlotToFile(true, FileFormat.JPEG));
        exportMenu.add(exportCurrentPlotAsJPEG);

        JMenuItem exportCurrentPlotAsSVG = new JMenuItem("as *.svg (current size)",
                UIUtils.getIconFromResources("actions/viewimage.png"));
        exportCurrentPlotAsSVG.addActionListener(e -> exportPlotToFile(true, FileFormat.SVG));
        exportMenu.add(exportCurrentPlotAsSVG);

        exportMenu.addSeparator();

        JMenuItem exportPlotAsPNG = new JMenuItem("as *.png (exported size)",
                UIUtils.getIconFromResources("actions/viewimage.png"));
        exportPlotAsPNG.addActionListener(e -> exportPlotToFile(false, FileFormat.PNG));
        exportMenu.add(exportPlotAsPNG);

        JMenuItem exportPlotAsJPEG = new JMenuItem("as *.jpeg (exported size)",
                UIUtils.getIconFromResources("actions/viewimage.png"));
        exportPlotAsJPEG.addActionListener(e -> exportPlotToFile(false, FileFormat.JPEG));
        exportMenu.add(exportPlotAsJPEG);

        JMenuItem exportPlotAsSVG = new JMenuItem("as *.svg (exported size)",
                UIUtils.getIconFromResources("actions/viewimage.png"));
        exportPlotAsSVG.addActionListener(e -> exportPlotToFile(false, FileFormat.SVG));
        exportMenu.add(exportPlotAsSVG);

        toolBar.add(exportButton);

        add(toolBar, BorderLayout.NORTH);

        chartPanel = new ChartPanel(null);
        add(chartPanel, BorderLayout.CENTER);

        chartPanel.setMinimumDrawWidth(0);
        chartPanel.setMaximumDrawWidth(Integer.MAX_VALUE);
        chartPanel.setMinimumDrawHeight(0);
        chartPanel.setMaximumDrawHeight(Integer.MAX_VALUE);
    }

    private void exportPlotToClipboard(boolean currentSize) {
        Dimension size;
        if (currentSize) {
            size = getSize();
        } else {
            size = new Dimension(plotBuilderUI.getCurrentPlot().getExportWidth(), plotBuilderUI.getCurrentPlot().getExportHeight());
        }
        JFreeChart chart = chartPanel.getChart();
        BufferedImage image = chart.createBufferedImage(size.width, size.height, null);
        CopyImageToClipboard copyImageToClipboard = new CopyImageToClipboard();
        copyImageToClipboard.copyImage(image);
    }

    private void exportPlotToFile(boolean currentSize, FileFormat fileFormat) {
        FileNameExtensionFilter filter;
        switch (fileFormat) {
            case PNG:
                filter = UIUtils.EXTENSION_FILTER_PNG;
                break;
            case SVG:
                filter = UIUtils.EXTENSION_FILTER_SVG;
                break;
            case JPEG:
                filter = UIUtils.EXTENSION_FILTER_JPEG;
                break;
            default:
                throw new UnsupportedOperationException();
        }
        Path path = FileChooserSettings.saveFile(this, FileChooserSettings.KEY_DATA, "Export plot as " + fileFormat, filter);
        Dimension size;
        if (currentSize) {
            size = getSize();
        } else {
            size = new Dimension(plotBuilderUI.getCurrentPlot().getExportWidth(), plotBuilderUI.getCurrentPlot().getExportHeight());
        }
        if (path != null) {
            JFreeChart chart = chartPanel.getChart();
            switch (fileFormat) {
                case PNG:
                    if (FileChooserSettings.getInstance().isAddFileExtension() && !path.toString().toLowerCase().endsWith(".png")) {
                        path = path.getParent().resolve(path.getFileName() + ".png");
                    }
                    try {
                        ChartUtils.saveChartAsPNG(path.toFile(),
                                chart,
                                size.width,
                                size.height);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case JPEG:
                    if (FileChooserSettings.getInstance().isAddFileExtension() && !path.toString().toLowerCase().endsWith(".jpg")) {
                        path = path.getParent().resolve(path.getFileName() + ".jpg");
                    }
                    try {
                        ChartUtils.saveChartAsJPEG(path.toFile(),
                                chart,
                                size.width,
                                size.height);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case SVG: {
                    if (FileChooserSettings.getInstance().isAddFileExtension() && !path.toString().toLowerCase().endsWith(".svg")) {
                        path = path.getParent().resolve(path.getFileName() + ".svg");
                    }
                    int w = size.width;
                    int h = size.height;
                    SVGGraphics2D g2 = new SVGGraphics2D(w, h);
                    Rectangle r = new Rectangle(0, 0, w, h);
                    chart.draw(g2, r);
                    try {
                        SVGUtils.writeToSVG(path.toFile(), g2.getSVGElement());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                break;
            }
        }
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

    /**
     * Available file formats
     */
    enum FileFormat {
        PNG,
        JPEG,
        SVG;

        @Override
        public String toString() {
            switch (this) {
                case PNG:
                    return "PNG (*.png)";
                case JPEG:
                    return "JPEG (*.jpeg)";
                case SVG:
                    return "SVG (*.svg)";
                default:
                    throw new UnsupportedOperationException();
            }
        }

        public Icon toIcon() {
            return UIUtils.getIconFromResources("actions/viewimage.png");
        }
    }
}
