package org.hkijena.acaq5.extensions.plots;

import org.hkijena.acaq5.ACAQJavaExtension;
import org.hkijena.acaq5.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.hkijena.acaq5.extensions.plots.datatypes.HistogramPlotData;
import org.hkijena.acaq5.extensions.plots.datatypes.PlotData;
import org.hkijena.acaq5.extensions.plots.parameters.UIPlotDataSourceEnum;
import org.hkijena.acaq5.extensions.plots.parameters.UIPlotDataSourceEnumParameterEditorUI;
import org.hkijena.acaq5.extensions.plots.ui.plots.*;
import org.hkijena.acaq5.extensions.plots.ui.resultanalysis.PlotDataSlotRowUI;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.plugin.Plugin;

/**
 * Provides a standard selection of plots
 */
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

        registerMenuExtension(NewPlotMenuExtension.class);

        registerDatatype("plot",
                PlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/data-type-plot.png"),
                PlotDataSlotRowUI.class,
                null);

        registerAlgorithm("plot-from-table", PlotGeneratorAlgorithm.class);


        registerDatatype("plot-histogram",
                HistogramPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/data-type-plot.png"),
                null,
                null);

        registerPlot(DefaultBoxAndWhiskerBarCategoryPlot.class,
                "Box Plot",
                UIUtils.getIconFromResources("bar-chart.png"));
        registerPlot(DefaultStatisticalLineCategoryPlot.class,
                "Statistical Line Plot",
                UIUtils.getIconFromResources("line-chart.png"));
        registerPlot(DefaultStatisticalBarCategoryPlot.class,
                "Statistical Bar Plot",
                UIUtils.getIconFromResources("bar-chart.png"));
        registerPlot(LineCategoryPlot.class,
                "Line Plot",
                UIUtils.getIconFromResources("line-chart.png"));
        registerPlot(BarCategoryPlot.class,
                "Bar Plot",
                UIUtils.getIconFromResources("bar-chart.png"));
        registerPlot(StackedBarCategoryPlot.class,
                "Stacked Bar Plot",
                UIUtils.getIconFromResources("bar-chart.png"));
        registerPlot(Pie2DPlot.class,
                "2D Pie Plot",
                UIUtils.getIconFromResources("pie-chart.png"));
        registerPlot(Pie3DPlot.class,
                "3D Pie Plot",
                UIUtils.getIconFromResources("pie-chart.png"));
        registerPlot(LineXYPlot.class,
                "XY Line Plot",
                UIUtils.getIconFromResources("line-chart.png"));
        registerPlot(ScatterXYPlot.class,
                "XY Scatter Plot",
                UIUtils.getIconFromResources("scatter-chart.png"));
        registerPlot(HistogramPlot.class,
                "Histogram Plot",
                UIUtils.getIconFromResources("bar-chart.png"));

        // Register UI-specific parameters
        registerParameterType(UIPlotDataSourceEnum.class, UIPlotDataSourceEnumParameterEditorUI.class,
                "Data column", "A data column to be plot");
    }

}
