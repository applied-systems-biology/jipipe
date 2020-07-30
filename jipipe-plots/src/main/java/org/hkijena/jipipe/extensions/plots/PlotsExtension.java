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

package org.hkijena.jipipe.extensions.plots;

import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.extensions.plots.datatypes.*;
import org.hkijena.jipipe.extensions.plots.parameters.UIPlotDataSeriesColumnEnum;
import org.hkijena.jipipe.extensions.plots.parameters.UIPlotDataSeriesColumnEnumParameterEditorUI;
import org.hkijena.jipipe.extensions.plots.ui.resultanalysis.PlotDataSlotPreviewUI;
import org.hkijena.jipipe.extensions.plots.ui.resultanalysis.PlotDataSlotRowUI;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.plugin.Plugin;

/**
 * Provides a standard selection of plots
 */
@Plugin(type = JIPipeJavaExtension.class)
public class PlotsExtension extends JIPipePrepackagedDefaultJavaExtension {

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

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
        return "org.hkijena.jipipe:plots";
    }

    @Override
    public String getDependencyVersion() {
        return "1.0.0";
    }

    @Override
    public void register() {

        // Register extension so users can create plots
        registerMenuExtension(NewPlotMenuExtension.class);

        // Register the base plot data type for internal usage
        registerDatatype("plot",
                PlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/data-type-plot.png"),
                PlotDataSlotRowUI.class,
                PlotDataSlotPreviewUI.class);

        // Register
        registerDatatype("plot-histogram",
                HistogramPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/bar-plot.png"),
                PlotDataSlotRowUI.class,
                PlotDataSlotPreviewUI.class);
        registerDatatype("plot-category-bar",
                BarCategoryPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/bar-plot.png"),
                PlotDataSlotRowUI.class,
                PlotDataSlotPreviewUI.class);
        registerDatatype("plot-category-bar-stacked",
                StackedBarCategoryPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/bar-plot.png"),
                PlotDataSlotRowUI.class,
                PlotDataSlotPreviewUI.class);
        registerDatatype("plot-category-line",
                LineCategoryPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/line-plot.png"),
                PlotDataSlotRowUI.class,
                PlotDataSlotPreviewUI.class);
        registerDatatype("plot-pie-2d",
                Pie2DPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/pie-plot.png"),
                PlotDataSlotRowUI.class,
                PlotDataSlotPreviewUI.class);
        registerDatatype("plot-pie-3d",
                Pie3DPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/pie-plot.png"),
                PlotDataSlotRowUI.class,
                PlotDataSlotPreviewUI.class);
        registerDatatype("plot-xy-scatter",
                ScatterXYPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/scatter-plot.png"),
                PlotDataSlotRowUI.class,
                PlotDataSlotPreviewUI.class);
        registerDatatype("plot-xy-line",
                LineXYPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/line-plot.png"),
                PlotDataSlotRowUI.class,
                PlotDataSlotPreviewUI.class);
        registerDatatype("plot-box-and-whisker",
                BarBoxAndWhiskerCategoryPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/bar-plot.png"),
                PlotDataSlotRowUI.class,
                PlotDataSlotPreviewUI.class);
        registerDatatype("plot-category-statistical-bar",
                BarStatisticalCategoryPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/bar-plot.png"),
                PlotDataSlotRowUI.class,
                PlotDataSlotPreviewUI.class);
        registerDatatype("plot-category-statistical-line",
                LineStatisticalCategoryPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/line-plot.png"),
                PlotDataSlotRowUI.class,
                PlotDataSlotPreviewUI.class);

        // Register algorithms to create plots
        registerNodeType("plot-from-table", PlotGeneratorAlgorithm.class, UIUtils.getIconURLFromResources("actions/office-chart-area.png"));

        // Register parameters
        registerEnumParameterType("plot-histogram:type",
                HistogramPlotData.HistogramType_.class,
                "Histogram type",
                "Available histogram types");
        registerParameterType("plot-data:series-column",
                UIPlotDataSeriesColumnEnum.class,
                UIPlotDataSeriesColumnEnum::new,
                c -> c,
                "Data column",
                "A data column to be plot",
                UIPlotDataSeriesColumnEnumParameterEditorUI.class);
    }


}
