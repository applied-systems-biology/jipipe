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

import org.apache.commons.compress.utils.Sets;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.DataTableImageJDataImporter;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataOperation;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.core.CoreExtension;
import org.hkijena.jipipe.extensions.core.data.OpenInNativeApplicationDataImportOperation;
import org.hkijena.jipipe.extensions.parameters.library.enums.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.extensions.plots.converters.PlotToTableConverter;
import org.hkijena.jipipe.extensions.plots.datatypes.*;
import org.hkijena.jipipe.extensions.plots.nodes.PlotTablesAlgorithm;
import org.hkijena.jipipe.extensions.plots.parameters.UIPlotDataSeriesColumnEnum;
import org.hkijena.jipipe.extensions.plots.parameters.UIPlotDataSeriesColumnEnumParameterEditorUI;
import org.hkijena.jipipe.extensions.plots.ui.resultanalysis.OpenPlotInJIPipeDataOperation;
import org.hkijena.jipipe.extensions.plots.ui.resultanalysis.PlotDataSlotPreview;
import org.hkijena.jipipe.extensions.plots.utils.ColorMap;
import org.hkijena.jipipe.extensions.tables.TablesExtension;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import java.util.Set;

/**
 * Provides a standard selection of plots
 */
@Plugin(type = JIPipeJavaExtension.class)
public class PlotsExtension extends JIPipePrepackagedDefaultJavaExtension {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:plots",
            JIPipe.getJIPipeVersion(),
            "Standard plots");
    public static JIPipeDataOperation[] STANDARD_DATA_OPERATIONS = {
            new OpenPlotInJIPipeDataOperation(),
            new OpenInNativeApplicationDataImportOperation("Open *.png", "Opens the rendered PNG image", new String[]{".png"}),
            new OpenInNativeApplicationDataImportOperation("Open *.svg", "Opens the rendered SVG image", new String[]{".svg"})
    };

    public PlotsExtension() {
        getMetadata().addCategories(PluginCategoriesEnumParameter.CATEGORY_VISUALIZATION, PluginCategoriesEnumParameter.CATEGORY_PLOTTING);
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Sets.newHashSet(CoreExtension.AS_DEPENDENCY, TablesExtension.AS_DEPENDENCY);
    }

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "Standard plots";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Commonly used plot types");
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:plots";
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {

        // Register extension so users can create plots
        registerMenuExtension(NewPlotJIPipeMenuExtension.class);

        // Register parameter types
        registerEnumParameterType("plot-color-map", ColorMap.class, "Color map", "Determines how plot elements are colored");

        // Register the base plot data type for internal usage
        registerDatatype("plot",
                PlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/data-type-plot.png"),
                null,
                PlotDataSlotPreview.class,
                STANDARD_DATA_OPERATIONS);
        registerDatatypeConversion(new PlotToTableConverter());

        // Register
        registerDatatype("plot-histogram",
                HistogramPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/bar-plot.png"),
                null,
                PlotDataSlotPreview.class,
                STANDARD_DATA_OPERATIONS);
        registerDatatype("plot-category-bar",
                BarCategoryPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/bar-plot.png"),
                null,
                PlotDataSlotPreview.class,
                STANDARD_DATA_OPERATIONS);
        registerDatatype("plot-category-bar-stacked",
                StackedBarCategoryPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/bar-plot.png"),
                null,
                PlotDataSlotPreview.class,
                STANDARD_DATA_OPERATIONS);
        registerDatatype("plot-category-line",
                LineCategoryPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/line-plot.png"),
                null,
                PlotDataSlotPreview.class,
                STANDARD_DATA_OPERATIONS);
        registerDatatype("plot-pie-2d",
                Pie2DPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/pie-plot.png"),
                null,
                PlotDataSlotPreview.class,
                STANDARD_DATA_OPERATIONS);
        registerDatatype("plot-pie-3d",
                Pie3DPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/pie-plot.png"),
                null,
                PlotDataSlotPreview.class,
                STANDARD_DATA_OPERATIONS);
        registerDatatype("plot-xy-scatter",
                ScatterXYPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/scatter-plot.png"),
                null,
                PlotDataSlotPreview.class,
                STANDARD_DATA_OPERATIONS);
        registerDatatype("plot-xy-line",
                LineXYPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/line-plot.png"),
                null,
                PlotDataSlotPreview.class,
                STANDARD_DATA_OPERATIONS);
        registerDatatype("plot-box-and-whisker",
                BarBoxAndWhiskerCategoryPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/bar-plot.png"),
                null,
                PlotDataSlotPreview.class,
                STANDARD_DATA_OPERATIONS);
        registerDatatype("plot-category-statistical-bar",
                BarStatisticalCategoryPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/bar-plot.png"),
                null,
                PlotDataSlotPreview.class,
                STANDARD_DATA_OPERATIONS);
        registerDatatype("plot-category-statistical-line",
                LineStatisticalCategoryPlotData.class,
                ResourceUtils.getPluginResource("icons/data-types/line-plot.png"),
                null,
                PlotDataSlotPreview.class,
                STANDARD_DATA_OPERATIONS);

        // Register algorithms to create plots
        registerNodeType("plot-from-table", PlotTablesAlgorithm.class, UIUtils.getIconURLFromResources("actions/office-chart-area.png"));

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

    @Override
    public void postprocess() {
        super.postprocess();
        for (Class<? extends JIPipeData> value : getRegistry().getDatatypeRegistry().getRegisteredDataTypes().values()) {
            if (PlotData.class.isAssignableFrom(value)) {
                configureDefaultImageJAdapters(value, DataTableImageJDataImporter.ID, "image-to-imagej-window");
            }
        }
    }
}
