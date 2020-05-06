package org.hkijena.acaq5.ui.plotbuilder;

import org.hkijena.acaq5.ui.plotbuilder.datasources.DoubleArrayPlotDataSource;
import org.hkijena.acaq5.ui.plotbuilder.datasources.StringArrayPlotDataSource;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Renders entries
 */
public class PlotDataSourceListCellRenderer extends JLabel implements ListCellRenderer<PlotDataSource> {

    /**
     * Creates a new renderer
     */
    public PlotDataSourceListCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends PlotDataSource> list, PlotDataSource value, int index, boolean isSelected, boolean cellHasFocus) {

        if (value instanceof DoubleArrayPlotDataSource) {
            DoubleArrayPlotDataSource data = (DoubleArrayPlotDataSource) value;
            setText(data.getName() + " (" + data.getData().length + " rows)");
            setIcon(UIUtils.getIconFromResources("table.png"));
        } else if (value instanceof StringArrayPlotDataSource) {
            StringArrayPlotDataSource data = (StringArrayPlotDataSource) value;
            setText(data.getName() + " (" + data.getData().length + " rows)");
            setIcon(UIUtils.getIconFromResources("table.png"));
        } else if (value != null) {
            setText(value.getName());
            setIcon(UIUtils.getIconFromResources("cog.png"));
        } else {
            setText("<Null>");
            setIcon(null);
        }

        if (isSelected) {
            setBackground(new Color(184, 207, 229));
        } else {
            setBackground(new Color(255, 255, 255));
        }

        return this;
    }
}
