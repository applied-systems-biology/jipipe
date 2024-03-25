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
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.plots.datatypes.PlotData;
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
    public List<AddJIPipeInputSlot> getInputSlots() {
        return Collections.singletonList(new DefaultAddJIPipeInputSlot(ResultsTableData.class, "Input", "The table(s) to be plotted", false, false, JIPipeDataSlotRole.Data));
    }

    public JIPipeDataInfo getPlotDataType() {
        return plotDataType;
    }

    @Override
    public List<AddJIPipeOutputSlot> getOutputSlots() {
        return Collections.singletonList(new DefaultAddJIPipeOutputSlot(PlotData.class, "Output", "The generated plots", null, false, JIPipeDataSlotRole.Data));
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
