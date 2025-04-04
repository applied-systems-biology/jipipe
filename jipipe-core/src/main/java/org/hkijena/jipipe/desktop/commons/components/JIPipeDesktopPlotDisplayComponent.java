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

package org.hkijena.jipipe.desktop.commons.components;

import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.ploteditor.JFreeChartPlotEditor;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.CopyImageToClipboard;
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
import java.util.Locale;

/**
 * Displays a plot
 */
public class JIPipeDesktopPlotDisplayComponent extends JPanel {

    private final JFreeChartPlotEditor plotBuilderUI;
    private ChartPanel chartPanel;

    /**
     * Creates a new instance
     *
     * @param plotBuilderUI the plot builder associated to this reader
     */
    public JIPipeDesktopPlotDisplayComponent(JFreeChartPlotEditor plotBuilderUI) {
        this.plotBuilderUI = plotBuilderUI;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        chartPanel = new ChartPanel(null);
        add(chartPanel, BorderLayout.CENTER);

        chartPanel.setMinimumDrawWidth(0);
        chartPanel.setMaximumDrawWidth(Integer.MAX_VALUE);
        chartPanel.setMinimumDrawHeight(0);
        chartPanel.setMaximumDrawHeight(Integer.MAX_VALUE);
    }

    public void rebuildRibbon(JIPipeDesktopRibbon ribbon) {
        JIPipeDesktopRibbon.Band generalViewBand = ribbon.getOrCreateTask("General").getOrCreateBand("View");
        JIPipeDesktopRibbon.Band exportPlotBand = ribbon.getOrCreateTask("Export").getOrCreateBand("Plot");
        generalViewBand.addLargeButton("Refresh plot", "Redraws the plot", UIUtils.getIcon32FromResources("actions/view-refresh.png"), this::redrawPlot);
        exportPlotBand.addLargeMenuButton("As image", "Exports the plot as image", UIUtils.getIcon32FromResources("actions/viewimage.png"),
                UIUtils.createMenuItem("As *.png (current size)", "Exports the plot in the current size", UIUtils.getIconFromResources("actions/filesave.png"), () -> exportPlotToFile(true, FileFormat.PNG)),
                UIUtils.createMenuItem("As *.jpeg (current size)", "Exports the plot in the current size", UIUtils.getIconFromResources("actions/filesave.png"), () -> exportPlotToFile(true, FileFormat.JPEG)),
                UIUtils.createMenuItem("As *.svg (current size)", "Exports the plot in the current size", UIUtils.getIconFromResources("actions/filesave.png"), () -> exportPlotToFile(true, FileFormat.SVG)),
                null,
                UIUtils.createMenuItem("As *.png (exported size)", "Exports the plot in the configured exported size", UIUtils.getIconFromResources("actions/filesave.png"), () -> exportPlotToFile(false, FileFormat.PNG)),
                UIUtils.createMenuItem("As *.jpeg (exported size)", "Exports the plot in the configured exported size", UIUtils.getIconFromResources("actions/filesave.png"), () -> exportPlotToFile(false, FileFormat.JPEG)),
                UIUtils.createMenuItem("As *.svg (exported size)", "Exports the plot in the configured exported size", UIUtils.getIconFromResources("actions/filesave.png"), () -> exportPlotToFile(false, FileFormat.SVG)));
        exportPlotBand.addLargeMenuButton("Copy to clipboard", "Copies the snapshot to the clipboard", UIUtils.getIcon32FromResources("actions/edit-copy.png"),
                UIUtils.createMenuItem("Current size", "Exports the plot in the current size", UIUtils.getIconFromResources("actions/edit-copy.png"), () -> exportPlotToClipboard(true)),
                UIUtils.createMenuItem("Exported size", "Exports the plot in the configured exported size", UIUtils.getIconFromResources("actions/edit-copy.png"), () -> exportPlotToClipboard(false)));

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
        Path path = JIPipeDesktop.saveFile(this,
                plotBuilderUI.getDesktopWorkbench(),
                JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data,
                "Export plot as " + fileFormat,
                HTMLText.EMPTY, filter);
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
                    if (JIPipeFileChooserApplicationSettings.getInstance().isAddFileExtension() && !path.toString().toLowerCase(Locale.ROOT).endsWith(".png")) {
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
                    if (JIPipeFileChooserApplicationSettings.getInstance().isAddFileExtension() && !path.toString().toLowerCase(Locale.ROOT).endsWith(".jpg")) {
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
                    if (JIPipeFileChooserApplicationSettings.getInstance().isAddFileExtension() && !path.toString().toLowerCase(Locale.ROOT).endsWith(".svg")) {
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
            JIPipeDesktopPlotExporterDialog dialog = new JIPipeDesktopPlotExporterDialog(chartPanel.getChart());
            dialog.pack();
            dialog.setSize(400, 300);
            dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
            dialog.setModal(true);
            dialog.setVisible(true);
        }
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
