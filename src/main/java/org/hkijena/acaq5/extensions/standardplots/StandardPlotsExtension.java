package org.hkijena.acaq5.extensions.standardplots;

import org.hkijena.acaq5.ACAQJavaExtension;
import org.hkijena.acaq5.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.hkijena.acaq5.extensions.standardplots.ui.plots.*;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.plugin.Plugin;

@Plugin(type = ACAQJavaExtension.class)
public class StandardPlotsExtension extends ACAQPrepackagedDefaultJavaExtension {

    @Override
    public String getName() {
        return "Standard plots";
    }

    @Override
    public String getDescription() {
        return "Commonly used plot types";
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.acaq5:plots";
    }

    @Override
    public String getDependencyVersion() {
        return "1.0.0";
    }

    @Override
    public void register() {
        registerPlot(DefaultBoxAndWhiskerBarCategoryPlot.class,
                CategoryPlotSettingsUI.class,
                "Box Plot",
                UIUtils.getIconFromResources("bar-chart.png"));
        registerPlot(DefaultStatisticalLineCategoryPlot.class,
                CategoryPlotSettingsUI.class,
                "Statistical Line Plot",
                UIUtils.getIconFromResources("line-chart.png"));
        registerPlot(DefaultStatisticalBarCategoryPlot.class,
                CategoryPlotSettingsUI.class,
                "Statistical Bar Plot",
                UIUtils.getIconFromResources("bar-chart.png"));
        registerPlot(LineCategoryPlot.class,
                CategoryPlotSettingsUI.class,
                "Line Plot",
                UIUtils.getIconFromResources("line-chart.png"));
        registerPlot(BarCategoryPlot.class,
                CategoryPlotSettingsUI.class,
                "Bar Plot",
                UIUtils.getIconFromResources("bar-chart.png"));
        registerPlot(StackedBarCategoryPlot.class,
                CategoryPlotSettingsUI.class,
                "Stacked Bar Plot",
                UIUtils.getIconFromResources("bar-chart.png"));
        registerPlot(Pie2DPlot.class,
                PiePlotSettingsUI.class,
                "2D Pie Plot",
                UIUtils.getIconFromResources("pie-chart.png"));
        registerPlot(Pie3DPlot.class,
                PiePlotSettingsUI.class,
                "3D Pie Plot",
                UIUtils.getIconFromResources("pie-chart.png"));
        registerPlot(LineXYPlot.class,
                XYPlotSettingsUI.class,
                "XY Line Plot",
                UIUtils.getIconFromResources("line-chart.png"));
        registerPlot(ScatterXYPlot.class,
                XYPlotSettingsUI.class,
                "XY Scatter Plot",
                UIUtils.getIconFromResources("scatter-chart.png"));
        registerPlot(HistogramPlot.class,
                HistogramPlotSettingsUI.class,
                "Histogram Plot",
                UIUtils.getIconFromResources("bar-chart.png"));
    }

}
