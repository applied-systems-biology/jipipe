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

package org.hkijena.jipipe.plugins.plots;

import org.apache.commons.compress.utils.Sets;
import org.hkijena.jipipe.*;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.DataTableImageJDataImporter;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeLegacyDataOperation;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.core.CorePlugin;
import org.hkijena.jipipe.plugins.core.data.OpenInNativeApplicationDataImportOperation;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.plugins.plots.converters.PlotToTableConverter;
import org.hkijena.jipipe.plugins.plots.datatypes.*;
import org.hkijena.jipipe.plugins.plots.nodes.PlotTables2AlgorithmInfo;
import org.hkijena.jipipe.plugins.plots.nodes.PlotTablesAlgorithm;
import org.hkijena.jipipe.plugins.plots.parameters.UIPlotDataSeriesColumnEnum;
import org.hkijena.jipipe.plugins.plots.parameters.UIPlotDataSeriesColumnEnumDesktopParameterEditorUI;
import org.hkijena.jipipe.plugins.plots.ui.resultanalysis.OpenPlotInJIPipeDataDisplayOperation;
import org.hkijena.jipipe.plugins.plots.ui.resultanalysis.PlotDataSlotPreview;
import org.hkijena.jipipe.plugins.plots.utils.ColorMap;
import org.hkijena.jipipe.plugins.plots.viewers.JFreeChartPlotDataViewer;
import org.hkijena.jipipe.plugins.tables.TablesPlugin;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;

/**
 * Provides a standard selection of plots
 */
@Plugin(type = JIPipeJavaPlugin.class)
public class PlotsPlugin extends JIPipePrepackagedDefaultJavaPlugin implements JIPipeService.DatatypeRegisteredEventListener {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:plots",
            JIPipe.getJIPipeVersion(),
            "JFreeChart plots");
    public static JIPipeLegacyDataOperation[] STANDARD_DATA_OPERATIONS = {
            new OpenPlotInJIPipeDataDisplayOperation(),
            new OpenInNativeApplicationDataImportOperation("Open *.png", "Opens the rendered PNG image", new String[]{".png"}),
            new OpenInNativeApplicationDataImportOperation("Open *.svg", "Opens the rendered SVG image", new String[]{".svg"})
    };

    public PlotsPlugin() {
        getMetadata().addCategories(PluginCategoriesEnumParameter.CATEGORY_VISUALIZATION, PluginCategoriesEnumParameter.CATEGORY_PLOTTING);
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Sets.newHashSet(CorePlugin.AS_DEPENDENCY, TablesPlugin.AS_DEPENDENCY);
    }

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "JFreeChart plots";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("JFreeChart plot support");
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:plots";
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {

        // Register extension so users can create plots
        registerMenuExtension(NewPlotJIPipeDesktopMenuExtension.class);

        // Register parameter types
        registerEnumParameterType("plot-color-map", ColorMap.class, "Color map", "Determines how plot elements are colored");

        // Register the base plot data type for internal usage
        registerDatatype("plot",
                JFreeChartPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/data-type-plot.png"),
                null,
                PlotDataSlotPreview.class,
                STANDARD_DATA_OPERATIONS);
        registerDefaultDataTypeViewer(JFreeChartPlotData.class, JFreeChartPlotDataViewer.class);
        registerDatatypeConversion(new PlotToTableConverter());

        // Register any existing plot data types
        for (Map.Entry<String, Class<? extends JIPipeData>> entry : jiPipe.getDatatypeRegistry().getRegisteredDataTypes().entrySet()) {
            tryRegisterPlotCreatorNode(entry.getKey());
        }

        // Register listener for future operations
        jiPipe.getDatatypeRegisteredEventEmitter().subscribe(this);


        // Register
        registerDatatype("plot-histogram",
                HistogramJFreeChartPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/bar-plot.png"),
                null,
                PlotDataSlotPreview.class,
                STANDARD_DATA_OPERATIONS);
        registerDatatype("plot-category-bar",
                BarCategoryJFreeChartPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/bar-plot.png"),
                null,
                PlotDataSlotPreview.class,
                STANDARD_DATA_OPERATIONS);
        registerDatatype("plot-category-bar-stacked",
                StackedBarCategoryJFreeChartPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/bar-plot.png"),
                null,
                PlotDataSlotPreview.class,
                STANDARD_DATA_OPERATIONS);
        registerDatatype("plot-category-line",
                LineCategoryJFreeChartPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/line-plot.png"),
                null,
                PlotDataSlotPreview.class,
                STANDARD_DATA_OPERATIONS);
        registerDatatype("plot-pie-2d",
                Pie2DJFreeChartPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/pie-plot.png"),
                null,
                PlotDataSlotPreview.class,
                STANDARD_DATA_OPERATIONS);
        registerDatatype("plot-pie-3d",
                Pie3DJFreeChartPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/pie-plot.png"),
                null,
                PlotDataSlotPreview.class,
                STANDARD_DATA_OPERATIONS);
        registerDatatype("plot-xy-scatter",
                ScatterXYJFreeChartPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/scatter-plot.png"),
                null,
                PlotDataSlotPreview.class,
                STANDARD_DATA_OPERATIONS);
        registerDatatype("plot-xy-line",
                LineXYJFreeChartPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/line-plot.png"),
                null,
                PlotDataSlotPreview.class,
                STANDARD_DATA_OPERATIONS);
        registerDatatype("plot-box-and-whisker",
                BarBoxAndWhiskerCategoryJFreeChartPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/bar-plot.png"),
                null,
                PlotDataSlotPreview.class,
                STANDARD_DATA_OPERATIONS);
        registerDatatype("plot-category-statistical-bar",
                BarStatisticalCategoryJFreeChartPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/bar-plot.png"),
                null,
                PlotDataSlotPreview.class,
                STANDARD_DATA_OPERATIONS);
        registerDatatype("plot-category-statistical-line",
                LineStatisticalCategoryJFreeChartPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/line-plot.png"),
                null,
                PlotDataSlotPreview.class,
                STANDARD_DATA_OPERATIONS);

        // Register algorithms to create plots
        registerNodeType("plot-from-table", PlotTablesAlgorithm.class, UIUtils.getIconURLFromResources("actions/office-chart-area.png"));

        // Register parameters
        registerEnumParameterType("plot-histogram:type",
                HistogramJFreeChartPlotData.HistogramType_.class,
                "Histogram type",
                "Available histogram types");
        registerParameterType("plot-data:series-column",
                UIPlotDataSeriesColumnEnum.class,
                UIPlotDataSeriesColumnEnum::new,
                c -> c,
                "Data column",
                "A data column to be plot",
                UIPlotDataSeriesColumnEnumDesktopParameterEditorUI.class);
    }

    private void tryRegisterPlotCreatorNode(String datatypeId) {
        JIPipeDataInfo dataInfo = JIPipeDataInfo.getInstance(datatypeId);
        if (!JFreeChartPlotData.class.isAssignableFrom(dataInfo.getDataClass()))
            return;
        if (Modifier.isAbstract(dataInfo.getDataClass().getModifiers()))
            return;
        registerNodeType(new PlotTables2AlgorithmInfo(dataInfo), UIUtils.getIconURLFromResources("actions/office-chart-area.png"));
    }

    @Override
    public void postprocess(JIPipeProgressInfo progressInfo) {
        super.postprocess(progressInfo);
        for (Class<? extends JIPipeData> value : getRegistry().getDatatypeRegistry().getRegisteredDataTypes().values()) {
            if (JFreeChartPlotData.class.isAssignableFrom(value)) {
                configureDefaultImageJAdapters(value, DataTableImageJDataImporter.ID, "image-to-imagej-window");
            }
        }
    }

    @Override
    public void onJIPipeDatatypeRegistered(JIPipeService.DatatypeRegisteredEvent event) {
        Class<? extends JIPipeData> dataClass = JIPipe.getDataTypes().getById(event.getId());
        tryRegisterPlotCreatorNode(event.getId());
    }
}
