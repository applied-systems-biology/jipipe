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

package org.hkijena.jipipe.plugins.plots.nodes;

import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlotRole;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.frameworks.JFreeChartFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.MeasurementsFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.plotting.PlotBarFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.plotting.PlotLineFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.plotting.PlotMiscFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.plotting.PlotScatterFunction;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.plots.datatypes.JFreeChartPlotData;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class PlotTables2AlgorithmInfo implements JIPipeNodeInfo {

    private final JIPipeDataInfo plotDataType;

    public PlotTables2AlgorithmInfo(JIPipeDataInfo plotDataType) {
        this.plotDataType = plotDataType;
    }

    @Override
    public String getId() {
        return "plot-table:" + plotDataType.getId();
    }

    @Override
    public Class<? extends JIPipeGraphNode> getInstanceClass() {
        return PlotTables2Algorithm.class;
    }

    @Override
    public JIPipeNodeTaxonomyFunction getPrimaryFunction() {
        // Runtime-registered "Plot Tables 2" node variants (one per JFreeChart
        // plot data type; no @ConfigureJIPipeNode can reach them). The variant
        // is defined by its plot data type id, so route each to the matching
        // plotting leaf instead of dumping everything into "Other plots":
        //   scatter  -> plot-xy-scatter
        //   line     -> plot-xy-line, plot-category-line, plot-category-statistical-line
        //   bar      -> plot-category-bar, plot-category-bar-stacked,
        //               plot-category-statistical-bar, plot-box-and-whisker
        //   other    -> plot-histogram, plot-pie-2d, plot-pie-3d, and any future type
        String dataTypeId = plotDataType.getId();
        return switch (dataTypeId) {
            case "plot-xy-scatter" -> new PlotScatterFunction();
            case "plot-xy-line", "plot-category-line", "plot-category-statistical-line" -> new PlotLineFunction();
            case "plot-category-bar", "plot-category-bar-stacked",
                 "plot-category-statistical-bar", "plot-box-and-whisker" -> new PlotBarFunction();
            default -> new PlotMiscFunction();
        };
    }

    @Override
    public JIPipeNodeTaxonomyFramework getFramework() {
        return new JFreeChartFramework();
    }

    @Override
    public JIPipeGraphNode newInstance() {
        return new PlotTables2Algorithm(this);
    }

    @Override
    public JIPipeGraphNode duplicate(JIPipeGraphNode algorithm) {
        return new PlotTables2Algorithm((PlotTables2Algorithm) algorithm);
    }

    @Override
    public String getName() {
        return plotDataType.getName();
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Plots the incoming table(s). This node will create a " + plotDataType.getName() + ".");
    }

    @Override
    public Set<JIPipeNodeTaxonomyFunction> getSecondaryFunctions() {
        // The legacy "Analyze" alias (IJ "Analyze\nPlot (JFreeChart)") implied a
        // MeasurementsFunction secondary per the 19.11b alias protocol. The
        // static PlotTablesAlgorithm already declares {PlotMiscFunction,
        // MeasurementsFunction}; the dynamic PlotTables2 variants lacked it
        // (Task 20 refined ij2/clij, not plots), so preserve the taxonomy
        // meaning of the removed alias here.
        return Set.of(new MeasurementsFunction());
    }

    @Override
    public List<AddJIPipeInputSlot> getInputSlots() {
        return Collections.singletonList(new DefaultAddJIPipeInputSlot(ResultsTableData.class, "Input", "The table(s) to be plotted", false, false, JIPipeDataSlotRole.Data));
    }

    public JIPipeDataInfo getPlotDataType() {
        return plotDataType;
    }

    @Override
    public List<AddJIPipeOutputSlot> getOutputSlots() {
        return Collections.singletonList(new DefaultAddJIPipeOutputSlot(JFreeChartPlotData.class, "Output", "The generated plots", null, false, JIPipeDataSlotRole.Data));
    }

    @Override
    public Set<Class<? extends JIPipeEnvironment>> getEnvironments() {
        return Set.of();
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Collections.emptySet();
    }

    @Override
    public boolean isHidden() {
        return false;
    }
}
