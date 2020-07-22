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

import ij.measure.ResultsTable;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.events.NodeSlotsChangedEvent;
import org.hkijena.jipipe.api.events.ParameterStructureChangedEvent;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.categories.AnalysisNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.extensions.parameters.editors.JIPipeDataParameterSettings;
import org.hkijena.jipipe.extensions.parameters.predicates.StringPredicate;
import org.hkijena.jipipe.extensions.parameters.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.extensions.plots.datatypes.PlotColumn;
import org.hkijena.jipipe.extensions.plots.datatypes.PlotData;
import org.hkijena.jipipe.extensions.plots.datatypes.PlotDataSeries;
import org.hkijena.jipipe.extensions.plots.datatypes.PlotMetadata;
import org.hkijena.jipipe.extensions.tables.ColumnContentType;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;
import org.hkijena.jipipe.extensions.tables.parameters.TableColumnSourceParameter;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.scijava.Priority;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that creates {@link PlotData} from {@link ResultsTableData}
 */
@JIPipeDocumentation(name = "Plot tables", description = "Converts input data tables into plots.")
@JIPipeOrganization(nodeTypeCategory = AnalysisNodeTypeCategory.class, menuPath = "Plot")
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

            ResultsTable seriesTable = new ResultsTable(inputData.getTable().getCounter());

            // Get column headings
            List<String> columnHeadings = new ArrayList<>();
            for (int i = 0; i <= inputData.getTable().getLastColumn(); ++i) {
                columnHeadings.add(inputData.getTable().getColumnHeading(i));
            }

            // First generate real column data
            for (Map.Entry<String, JIPipeParameterAccess> entry : columnAssignments.getParameters().entrySet()) {
                TableColumnSourceParameter parameter = entry.getValue().get(TableColumnSourceParameter.class);
                if (parameter.getMode() == TableColumnSourceParameter.Mode.PickColumn) {
                    String matchingColumn = null;
                    StringPredicate filter = parameter.getColumnSource();
                    for (String columnHeading : columnHeadings) {
                        if (filter.test(columnHeading)) {
                            matchingColumn = columnHeading;
                            break;
                        }
                    }
                    if (matchingColumn == null) {
                        throw new UserFriendlyRuntimeException("Could not find column that matches '" + filter.toString() + "'!",
                                "Could not find column!",
                                "Algorithm '" + getName() + "'",
                                "A plot generator algorithm was instructed to extract a column matching the rule '" + filter.toString() + "' for plotting. The column could note be found. " +
                                        "The table contains only following columns: " + String.join(", ", columnHeadings),
                                "Please check if your input columns are set up with valid filters. Please check the input of the plot generator " +
                                        "via the quick run to see if the input data is correct. You can also select a generator instead of picking a column.");
                    }
                    seriesTable.setColumn(entry.getKey(), inputData.getTable().getColumnAsVariables(matchingColumn));
                }
            }

            if (seriesTable.getCounter() == 0) {
                throw new UserFriendlyRuntimeException("Table has now rows!",
                        "Plot has no real input data!",
                        "Algorithm '" + getName() + "'",
                        "A plot only has column generators. But generators need to know how many rows they should generate.",
                        "Please pick at least one input column from the input table.");
            }

            for (Map.Entry<String, JIPipeParameterAccess> entry : columnAssignments.getParameters().entrySet()) {
                TableColumnSourceParameter parameter = entry.getValue().get(TableColumnSourceParameter.class);
                if (parameter.getMode() == TableColumnSourceParameter.Mode.GenerateColumn) {
                    PlotColumn column = plotColumns.get(entry.getKey());
                    TableColumn generator = (TableColumn) ReflectionUtils.newInstance(parameter.getGeneratorSource().getGeneratorType().getInfo().getDataClass());
                    if (column.isNumeric()) {
                        double[] data = generator.getDataAsDouble(seriesTable.getCounter());
                        int col = seriesTable.getFreeColumn(entry.getKey());
                        for (int i = 0; i < data.length; i++) {
                            seriesTable.setValue(col, i, data[i]);
                        }
                    } else {
                        String[] data = generator.getDataAsString(seriesTable.getCounter());
                        int col = seriesTable.getFreeColumn(entry.getKey());
                        for (int i = 0; i < data.length; i++) {
                            seriesTable.setValue(col, i, data[i]);
                        }
                    }
                }
            }

            plot.addSeries(new PlotDataSeries(seriesTable));

            getFirstOutputSlot().addData(plot, getFirstInputSlot().getAnnotations(row));
        }
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        report.forCategory("Plot type").checkNonNull(getPlotType().getInfo(), this);
        if (plotTypeParameters != null) {
            report.forCategory("Plot parameters").report(plotTypeParameters);
        }
        boolean foundRealDataColumn = false;
        for (Map.Entry<String, JIPipeParameterAccess> entry : columnAssignments.getParameters().entrySet()) {
            TableColumnSourceParameter parameter = entry.getValue().get(TableColumnSourceParameter.class);
            report.forCategory("Input columns").forCategory(entry.getKey()).report(parameter);
            if (parameter.getMode() == TableColumnSourceParameter.Mode.PickColumn) {
                foundRealDataColumn = true;
                break;
            }
        }
        if (!foundRealDataColumn) {
            report.forCategory("Input columns").reportIsInvalid("Plot has no input data!",
                    "A plot only has column generators. But generators need to know how many rows they should generate.",
                    "Please pick at least one input column from the input table.",
                    this);
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
                parameterAccess.setFieldClass(TableColumnSourceParameter.class);
                TableColumnSourceParameter initialValue = new TableColumnSourceParameter();
                initialValue.setMode(TableColumnSourceParameter.Mode.PickColumn);
                initialValue.setColumnSource(new StringPredicate(StringPredicate.Mode.Equals, column.name(), false));
                initialValue.getGeneratorSource().setGeneratedType(column.isNumeric() ? ColumnContentType.NumericColumn : ColumnContentType.StringColumn);
                parameterAccess.set(initialValue);
                parameterAccess.setDescription(column.description() + " " + (column.isNumeric() ? "(Numeric column)" : "(String column)"));
                columnAssignments.addParameter(parameterAccess);
            }
        }
        columnAssignments.endModificationBlock();
    }

    private void updatePlotTypeParameters() {
        if (plotTypeParameters == null || (plotType.getInfo() != null && !Objects.equals(plotType.getInfo().getDataClass(), plotTypeParameters.getClass()))) {
            if (plotType.getInfo() != null) {
                plotTypeParameters = (PlotData) JIPipeData.createInstance(plotType.getInfo().getDataClass());
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
