package org.hkijena.acaq5.extensions.standardplots;

import org.hkijena.acaq5.ACAQExtensionService;
import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.extensions.standardplots.ui.plots.*;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

@Plugin(type = ACAQExtensionService.class)
public class StandardPlotsExtension extends AbstractService implements ACAQExtensionService {
    @Override
    public String getName() {
        return "ACAQ5 standard plots";
    }

    @Override
    public String getDescription() {
        return "Provides some plot types for the ACAQ5 plot generator";
    }

    @Override
    public List<String> getAuthors() {
        return Arrays.asList("Zoltán Cseresnyés", "Ruman Gerst");
    }

    @Override
    public String getURL() {
        return "https://applied-systems-biology.github.io/acaq5/";
    }

    @Override
    public String getLicense() {
        return "BSD-2";
    }

    @Override
    public URL getIconURL() {
        return ResourceUtils.getPluginResource("logo-400.png");
    }

    @Override
    public void register(ACAQRegistryService registryService) {
        registryService.getPlotBuilderRegistry().register(DefaultBoxAndWhiskerBarCategoryPlot.class,
                CategoryPlotSettingsUI.class,
                "Box Plot",
                UIUtils.getIconFromResources("bar-chart.png"));
        registryService.getPlotBuilderRegistry().register(DefaultStatisticalLineCategoryPlot.class,
                CategoryPlotSettingsUI.class,
                "Statistical Line Plot",
                UIUtils.getIconFromResources("line-chart.png"));
        registryService.getPlotBuilderRegistry().register(DefaultStatisticalBarCategoryPlot.class,
                CategoryPlotSettingsUI.class,
                "Statistical Bar Plot",
                UIUtils.getIconFromResources("bar-chart.png"));
        registryService.getPlotBuilderRegistry().register(LineCategoryPlot.class,
                CategoryPlotSettingsUI.class,
                "Line Plot",
                UIUtils.getIconFromResources("line-chart.png"));
        registryService.getPlotBuilderRegistry().register(BarCategoryPlot.class,
                CategoryPlotSettingsUI.class,
                "Bar Plot",
                UIUtils.getIconFromResources("bar-chart.png"));
        registryService.getPlotBuilderRegistry().register(StackedBarCategoryPlot.class,
                CategoryPlotSettingsUI.class,
                "Stacked Bar Plot",
                UIUtils.getIconFromResources("bar-chart.png"));
        registryService.getPlotBuilderRegistry().register(Pie2DPlot.class,
                PiePlotSettingsUI.class,
                "2D Pie Plot",
                UIUtils.getIconFromResources("pie-chart.png"));
        registryService.getPlotBuilderRegistry().register(Pie3DPlot.class,
                PiePlotSettingsUI.class,
                "3D Pie Plot",
                UIUtils.getIconFromResources("pie-chart.png"));
        registryService.getPlotBuilderRegistry().register(LineXYPlot.class,
                XYPlotSettingsUI.class,
                "XY Line Plot",
                UIUtils.getIconFromResources("line-chart.png"));
        registryService.getPlotBuilderRegistry().register(ScatterXYPlot.class,
                XYPlotSettingsUI.class,
                "XY Scatter Plot",
                UIUtils.getIconFromResources("scatter-chart.png"));
        registryService.getPlotBuilderRegistry().register(HistogramPlot.class,
                HistogramPlotSettingsUI.class,
                "Histogram Plot",
                UIUtils.getIconFromResources("bar-chart.png"));
    }
}
