/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Insitute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.acaq5.ui.components;

import org.hkijena.acaq5.utils.UIUtils;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.jfree.graphics2d.svg.SVGUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class PlotExporterDialog extends JDialog {

    private JFreeChart chart;
    private FileSelection fileSelection;
    private JComboBox<FileFormat> plotExportFormat;
    private JSpinner plotWidth;
    private JSpinner plotHeight;

    public PlotExporterDialog(JFreeChart chart) {
        this.chart = chart;
        initialize();
    }

    private void initialize() {
        setLayout(new GridBagLayout());
        setTitle("Export plot");

        {
            add(new JLabel("Export path"), new GridBagConstraints() {
                {
                    gridx = 0;
                    gridy = 0;
                    anchor = GridBagConstraints.WEST;
                    insets = UIUtils.UI_PADDING;
                }
            });
            fileSelection = new FileSelection(FileSelection.IOMode.Save, FileSelection.PathMode.FilesOnly);
            add(fileSelection, new GridBagConstraints() {
                {
                    gridx = 1;
                    gridy = 0;
                    fill = GridBagConstraints.HORIZONTAL;
                    gridwidth = 1;
                    insets = UIUtils.UI_PADDING;
                }
            });
        }
        {
            add(new JLabel("File format"), new GridBagConstraints() {
                {
                    gridx = 0;
                    gridy = 1;
                    anchor = GridBagConstraints.WEST;
                    insets = UIUtils.UI_PADDING;
                }
            });
            plotExportFormat = new JComboBox<>(FileFormat.values());
            add(plotExportFormat, new GridBagConstraints() {
                {
                    gridx = 1;
                    gridy = 1;
                    fill = GridBagConstraints.HORIZONTAL;
                    gridwidth = 1;
                    insets = UIUtils.UI_PADDING;
                }
            });
        }
        {
            add(new JLabel("Width"), new GridBagConstraints() {
                {
                    gridx = 0;
                    gridy = 2;
                    anchor = GridBagConstraints.WEST;
                    insets = UIUtils.UI_PADDING;
                }
            });
            plotWidth = new JSpinner(new SpinnerNumberModel(800, 1, Integer.MAX_VALUE, 1));
            add(plotWidth, new GridBagConstraints() {
                {
                    gridx = 1;
                    gridy = 2;
                    fill = GridBagConstraints.HORIZONTAL;
                    gridwidth = 1;
                    insets = UIUtils.UI_PADDING;
                }
            });
        }
        {
            add(new JLabel("Height"), new GridBagConstraints() {
                {
                    gridx = 0;
                    gridy = 3;
                    anchor = GridBagConstraints.WEST;
                    insets = UIUtils.UI_PADDING;
                }
            });
            plotHeight = new JSpinner(new SpinnerNumberModel(600, 1, Integer.MAX_VALUE, 1));
            add(plotHeight, new GridBagConstraints() {
                {
                    gridx = 1;
                    gridy = 3;
                    fill = GridBagConstraints.HORIZONTAL;
                    gridwidth = 1;
                    insets = UIUtils.UI_PADDING;
                }
            });
        }

        UIUtils.addFillerGridBagComponent(getContentPane(), 4, 1);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("remove.png"));
        cancelButton.addActionListener(e -> setVisible(false));
        buttonPanel.add(cancelButton);

        JButton exportButton = new JButton("Export", UIUtils.getIconFromResources("save.png"));
        exportButton.setDefaultCapable(true);
        exportButton.addActionListener(e -> exportPlot());
        buttonPanel.add(exportButton);

        add(buttonPanel, new GridBagConstraints() {
            {
                gridx = 0;
                gridy = 5;
                gridwidth = 2;
                fill = GridBagConstraints.HORIZONTAL;
                insets = UIUtils.UI_PADDING;
            }
        });
    }

    private void exportPlot() {
        if (fileSelection.getPath() == null)
            return;
        switch ((FileFormat) plotExportFormat.getSelectedItem()) {
            case PNG:
                try {
                    ChartUtils.saveChartAsPNG(fileSelection.getPath().toFile(),
                            chart,
                            ((Number) plotWidth.getValue()).intValue(),
                            ((Number) plotHeight.getValue()).intValue());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            case JPEG:
                try {
                    ChartUtils.saveChartAsJPEG(fileSelection.getPath().toFile(),
                            chart,
                            ((Number) plotWidth.getValue()).intValue(),
                            ((Number) plotHeight.getValue()).intValue());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            case SVG: {
                int w = ((Number) plotWidth.getValue()).intValue();
                int h = ((Number) plotHeight.getValue()).intValue();
                SVGGraphics2D g2 = new SVGGraphics2D(w, h);
                Rectangle r = new Rectangle(0, 0, w, h);
                chart.draw(g2, r);
                try {
                    SVGUtils.writeToSVG(fileSelection.getPath().toFile(), g2.getSVGElement());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            break;
        }

        setVisible(false);
    }

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
            return UIUtils.getIconFromResources("filetype-image.png");
        }
    }
}
