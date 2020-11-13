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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.events.NodeSlotsChangedEvent;
import org.hkijena.jipipe.api.events.ParameterStructureChangedEvent;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeMutableParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.parameters.editors.JIPipeDataParameterSettings;
import org.hkijena.jipipe.extensions.parameters.expressions.TableColumnSourceExpressionParameter;
import org.hkijena.jipipe.extensions.parameters.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.extensions.plots.datatypes.PlotColumn;
import org.hkijena.jipipe.extensions.plots.datatypes.PlotData;
import org.hkijena.jipipe.extensions.plots.datatypes.PlotDataSeries;
import org.hkijena.jipipe.extensions.plots.datatypes.PlotMetadata;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.scijava.Priority;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that creates {@link PlotData} from {@link ResultsTableData}
 */
@JIPipeDocumentation(name = "Plot tables", description = "Converts input data tables into plots.")
@JIPipeOrganization(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Plot")
@JIPipeInputSlot(ResultsTableData.class)
@JIPipeOutputSlot(PlotData.class)
public class PlotGeneratorAlgorithm extends JIPipeAlgorithm {

    private JIPipeDataInfoRef plotType = new JIPipeDataInfoRef();
    private PlotData plotTypeParameters;
    private JIPipeDynamicParameterCollection columnAssignments = new JIPipeDynamicParameterCollection(false);

    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public PlotGeneratorAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ResultsTableData.class)
                .addOutputSlot("Output", PlotData.class, null)
                .seal()
                .build());
        registerSubParameter(columnAssignments);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public PlotGeneratorAlgorithm(PlotGeneratorAlgorithm other) {
        super(other);
        this.plotType = new JIPipeDataInfoRef(other.plotType);
        if (other.plotTypeParameters != null)
            this.plotTypeParameters = (PlotData) other.plotTypeParameters.duplicate();
        this.columnAssignments = new JIPipeDynamicParameterCollection(other.columnAssignments);
    }

    @Override
    public void run(JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        PlotMetadata plotMetadata = plotType.getInfo().getDataClass().getAnnotation(PlotMetadata.class);
        Map<String, PlotColumn> plotColumns = new HashMap<>();
        for (PlotColumn column : plotMetadata.columns()) {
            plotColumns.put(column.name(), column);
        }

        for (int row = 0; row < getFirstInputSlot().getRowCount(); ++row) {
            ResultsTableData inputData = getFirstInputSlot().getData(row, ResultsTableData.class);
            PlotData plot = (PlotData) plotTypeParameters.duplicate();

            ResultsTableData seriesTable = new ResultsTableData();
            seriesTable.addRows(inputData.getRowCount());

            // First generate real column data
            for (Map.Entry<String, JIPipeParameterAccess> entry : columnAssignments.getParameters().entrySet()) {
                TableColumnSourceExpressionParameter parameter = entry.getValue().get(TableColumnSourceExpressionParameter.class);
                seriesTable.setColumn(entry.getKey(), parameter.pickColumn(inputData), plotColumns.get(entry.getKey()).isNumeric());
            }

            if (seriesTable.getRowCount() == 0) {
                throw new UserFriendlyRuntimeException("Table has now rows!",
                        "Plot has no real input data!",
                        "Algorithm '" + getName() + "'",
                        "A plot only has column generators. But generators need to know how many rows they should generate.",
                        "Please pick at least one input column from the input table.");
            }

            plot.addSeries(new PlotDataSeries(seriesTable.getTable()));
            getFirstOutputSlot().addData(plot, getFirstInputSlot().getAnnotations(row), JIPipeAnnotationMergeStrategy.Merge);
        }
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        report.forCategory("Plot type").checkNonNull(getPlotType().getInfo(), this);
        if (plotTypeParameters != null) {
            report.forCategory("Plot parameters").report(plotTypeParameters);
        }
    }

    @JIPipeDocumentation(name = "Plot type", description = "The type of plot to be generated.")
    @JIPipeParameter(value = "plot-type", priority = Priority.HIGH)
    @JIPipeDataParameterSettings(dataBaseClass = PlotData.class)
    public JIPipeDataInfoRef getPlotType() {
        if (plotType == null) {
            plotType = new JIPipeDataInfoRef();
        }
        return plotType;
    }

    @JIPipeParameter("plot-type")
    public void setPlotType(JIPipeDataInfoRef plotType) {
        this.plotType = plotType;


        updateOutputSlotType();
        updatePlotTypeParameters();
        updateColumnAssignment();
    }

    private void updateColumnAssignment() {
        columnAssignments.beginModificationBlock();
        columnAssignments.clear();
        if (plotType.getInfo() != null) {
            PlotMetadata plotMetadata = plotType.getInfo().getDataClass().getAnnotation(PlotMetadata.class);
            for (PlotColumn column : plotMetadata.columns()) {
                JIPipeMutableParameterAccess parameterAccess = new JIPipeMutableParameterAccess();
                parameterAccess.setKey(column.name());
                parameterAccess.setName(column.name());
                parameterAccess.setFieldClass(TableColumnSourceExpressionParameter.class);
                TableColumnSourceExpressionParameter initialValue = new TableColumnSourceExpressionParameter();
                parameterAccess.set(initialValue);
                parameterAccess.setDescription(column.description() + " " + (column.isNumeric() ? "(Numeric column)" : "(String column)") + " " + TableColumnSourceExpressionParameter.DOCUMENTATION_DESCRIPTION);
                columnAssignments.addParameter(parameterAccess);
            }
        }
        columnAssignments.endModificationBlock();
    }

    private void updatePlotTypeParameters() {
        if (plotTypeParameters == null || (plotType.getInfo() != null && !Objects.equals(plotType.getInfo().getDataClass(), plotTypeParameters.getClass()))) {
            if (plotType.getInfo() != null) {
                plotTypeParameters = (PlotData) JIPipe.createData(plotType.getInfo().getDataClass());
                getEventBus().post(new ParameterStructureChangedEvent(this));
            }
        } else if (plotType.getInfo() == null) {
            plotTypeParameters = null;
            getEventBus().post(new ParameterStructureChangedEvent(this));
        }
    }

    private void updateOutputSlotType() {
        if (plotType.getInfo() != null) {
            getFirstOutputSlot().setAcceptedDataType(plotType.getInfo().getDataClass());
        } else {
            getFirstOutputSlot().setAcceptedDataType(PlotData.class);
        }
        getEventBus().post(new NodeSlotsChangedEvent(this));
    }

    @JIPipeDocumentation(name = "Plot parameters")
    @JIPipeParameter("plot-parameters")
    public PlotData getPlotTypeParameters() {
        return plotTypeParameters;
    }

    @JIPipeDocumentation(name = "Input columns", description = "Please define which input table columns are copied into the plot. " +
            "To find out which columns are available, run the quick run on input data. You can also generate missing columns.")
    @JIPipeParameter(value = "column-assignments", persistence = JIPipeParameterPersistence.Object)
    public JIPipeDynamicParameterCollection getColumnAssignments() {
        return columnAssignments;
    }
}
