package org.hkijena.acaq5.extensions.plots;

import org.hkijena.acaq5.ACAQJavaExtension;
import org.hkijena.acaq5.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.hkijena.acaq5.extensions.plots.datasources.RowIteratorPlotDataSource;
import org.hkijena.acaq5.extensions.plots.datasources.ZeroPlotDataSource;
import org.hkijena.acaq5.extensions.plots.datatypes.*;
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

        // Register generating data sources
        registerPlotDataSource(RowIteratorPlotDataSource.class);
        registerPlotDataSource(ZeroPlotDataSource.class);

        // Register extension so users can create plots
        registerMenuExtension(NewPlotMenuExtension.class);

        // Register the base plot data type for internal usage
        registerDatatype("plot",
                PlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/data-type-plot.png"),
                PlotDataSlotRowUI.class,
                null);

        // Register
        registerDatatype("plot-histogram",
                HistogramPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/bar-plot.png"),
                null,
                null);
        registerDatatype("plot-category-bar",
                BarCategoryPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/bar-plot.png"),
                null,
                null);
        registerDatatype("plot-category-bar-stacked",
                StackedBarCategoryPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/bar-plot.png"),
                null,
                null);
        registerDatatype("plot-category-line",
                LineCategoryPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/line-plot.png"),
                null,
                null);
        registerDatatype("plot-pie-2d",
                Pie2DPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/pie-plot.png"),
                null,
                null);
        registerDatatype("plot-pie-3d",
                Pie3DPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/pie-plot.png"),
                null,
                null);
        registerDatatype("plot-xy-scatter",
                ScatterXYPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/scatter-plot.png"),
                null,
                null);
        registerDatatype("plot-xy-line",
                LineXYPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/line-plot.png"),
                null,
                null);
        registerDatatype("plot-box-and-whisker",
                BarBoxAndWhiskerCategoryPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/bar-plot.png"),
                null,
                null);

        // Register algorithms to create plots
        registerAlgorithm("plot-from-table", PlotGeneratorAlgorithm.class);

        // Register UI-specific parameters
        registerParameterType(UIPlotDataSourceEnum.class, UIPlotDataSourceEnumParameterEditorUI.class,
                "Data column", "A data column to be plot");
    }


}
