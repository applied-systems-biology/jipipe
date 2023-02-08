package org.hkijena.jipipe.extensions.plots.nodes;

import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlotRole;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.plots.datatypes.PlotData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

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
    public String getMenuPath() {
        return "Plot";
    }

    @Override
    public JIPipeNodeTypeCategory getCategory() {
        return new TableNodeTypeCategory();
    }

    @Override
    public List<JIPipeInputSlot> getInputSlots() {
        return Collections.singletonList(new DefaultJIPipeInputSlot(ResultsTableData.class, "Input", "The table(s) to be plotted", false, false, JIPipeDataSlotRole.Data));
    }

    public JIPipeDataInfo getPlotDataType() {
        return plotDataType;
    }

    @Override
    public List<JIPipeOutputSlot> getOutputSlots() {
        return Collections.singletonList(new DefaultJIPipeOutputSlot(PlotData.class, "Output", "The generated plots", null, false, JIPipeDataSlotRole.Data));
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Collections.emptySet();
    }

    @Override
    public List<JIPipeNodeMenuLocation> getAliases() {
        return Collections.singletonList(new JIPipeNodeMenuLocation(new ImageJNodeTypeCategory(), "Analyze\nPlot (JFreeChart)", null));
    }

    @Override
    public boolean isHidden() {
        return false;
    }
}
