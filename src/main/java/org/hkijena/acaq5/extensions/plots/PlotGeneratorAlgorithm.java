package org.hkijena.acaq5.extensions.plots;

import ij.measure.ResultsTable;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.events.AlgorithmSlotsChangedEvent;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.events.ParameterStructureChangedEvent;
import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.acaq5.api.parameters.ACAQDynamicParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQMutableParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;
import org.hkijena.acaq5.extensions.parameters.editors.ACAQDataParameterSettings;
import org.hkijena.acaq5.extensions.parameters.filters.StringFilter;
import org.hkijena.acaq5.extensions.parameters.references.ACAQDataDeclarationRef;
import org.hkijena.acaq5.extensions.plots.datatypes.PlotColumn;
import org.hkijena.acaq5.extensions.plots.datatypes.PlotData;
import org.hkijena.acaq5.extensions.plots.datatypes.PlotDataSeries;
import org.hkijena.acaq5.extensions.plots.datatypes.PlotMetadata;
import org.hkijena.acaq5.extensions.tables.ColumnContentType;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;
import org.hkijena.acaq5.extensions.tables.parameters.TableColumnSourceParameter;
import org.hkijena.acaq5.utils.ReflectionUtils;
import org.scijava.Priority;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that creates {@link PlotData} from {@link ResultsTableData}
 */
@ACAQDocumentation(name = "Plot tables", description = "Converts input data tables into plots.")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Analysis, menuPath = "Plot")
@AlgorithmInputSlot(ResultsTableData.class)
@AlgorithmOutputSlot(PlotData.class)
public class PlotGeneratorAlgorithm extends ACAQAlgorithm {

    private ACAQDataDeclarationRef plotType = new ACAQDataDeclarationRef();
    private PlotData plotTypeParameters;
    private ACAQDynamicParameterCollection columnAssignments = new ACAQDynamicParameterCollection(false);

    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public PlotGeneratorAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().addInputSlot("Input", ResultsTableData.class)
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
        this.plotType = new ACAQDataDeclarationRef(other.plotType);
        this.plotTypeParameters = (PlotData) other.plotTypeParameters.duplicate();
        this.columnAssignments = new ACAQDynamicParameterCollection(other.columnAssignments);
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        PlotMetadata plotMetadata = plotType.getDeclaration().getDataClass().getAnnotation(PlotMetadata.class);
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
            for (Map.Entry<String, ACAQParameterAccess> entry : columnAssignments.getParameters().entrySet()) {
                TableColumnSourceParameter parameter = entry.getValue().get(TableColumnSourceParameter.class);
                if (parameter.getMode() == TableColumnSourceParameter.Mode.PickColumn) {
                    String matchingColumn = null;
                    StringFilter filter = parameter.getColumnSource();
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
                                        "via the testbench to see if the input data is correct. You can also select a generator instead of picking a column.");
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

            for (Map.Entry<String, ACAQParameterAccess> entry : columnAssignments.getParameters().entrySet()) {
                TableColumnSourceParameter parameter = entry.getValue().get(TableColumnSourceParameter.class);
                if (parameter.getMode() == TableColumnSourceParameter.Mode.GenerateColumn) {
                    PlotColumn column = plotColumns.get(entry.getKey());
                    TableColumn generator = (TableColumn) ReflectionUtils.newInstance(parameter.getGeneratorSource().getGeneratorType().getDeclaration().getDataClass());
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
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Plot type").checkNonNull(getPlotType().getDeclaration(), this);
        if (plotTypeParameters != null) {
            report.forCategory("Plot parameters").report(plotTypeParameters);
        }
        boolean foundRealDataColumn = false;
        for (Map.Entry<String, ACAQParameterAccess> entry : columnAssignments.getParameters().entrySet()) {
            TableColumnSourceParameter parameter = entry.getValue().get(TableColumnSourceParameter.class);
            report.forCategory("Input columns").forCategory(entry.getKey()).report(parameter);
            if(parameter.getMode() == TableColumnSourceParameter.Mode.PickColumn) {
                foundRealDataColumn = true;
                break;
            }
        }
        if(!foundRealDataColumn) {
            report.forCategory("Input columns").reportIsInvalid("Plot has no input data!",
                    "A plot only has column generators. But generators need to know how many rows they should generate.",
                    "Please pick at least one input column from the input table.",
                    this);
        }

    }

    @ACAQDocumentation(name = "Plot type", description = "The type of plot to be generated.")
    @ACAQParameter(value = "plot-type", priority = Priority.HIGH)
    @ACAQDataParameterSettings(dataBaseClass = PlotData.class)
    public ACAQDataDeclarationRef getPlotType() {
        if (plotType == null) {
            plotType = new ACAQDataDeclarationRef();
        }
        return plotType;
    }

    @ACAQParameter("plot-type")
    public void setPlotType(ACAQDataDeclarationRef plotType) {
        this.plotType = plotType;
        getEventBus().post(new ParameterChangedEvent(this, "plot-type"));

        updateOutputSlotType();
        updatePlotTypeParameters();
        updateColumnAssignment();
    }

    private void updateColumnAssignment() {
        columnAssignments.beginModificationBlock();
        columnAssignments.clear();
        if (plotType.getDeclaration() != null) {
            PlotMetadata plotMetadata = plotType.getDeclaration().getDataClass().getAnnotation(PlotMetadata.class);
            for (PlotColumn column : plotMetadata.columns()) {
                ACAQMutableParameterAccess parameterAccess = new ACAQMutableParameterAccess();
                parameterAccess.setKey(column.name());
                parameterAccess.setName(column.name());
                parameterAccess.setFieldClass(TableColumnSourceParameter.class);
                TableColumnSourceParameter initialValue = new TableColumnSourceParameter();
                initialValue.setMode(TableColumnSourceParameter.Mode.PickColumn);
                initialValue.setColumnSource(new StringFilter(StringFilter.Mode.Equals, column.name()));
                initialValue.getGeneratorSource().setGeneratedType(column.isNumeric() ? ColumnContentType.NumericColumn : ColumnContentType.StringColumn);
                parameterAccess.set(initialValue);
                parameterAccess.setDescription(column.description() + " " + (column.isNumeric() ? "(Numeric column)" : "(String column)"));
                columnAssignments.addParameter(parameterAccess);
            }
        }
        columnAssignments.endModificationBlock();
    }

    private void updatePlotTypeParameters() {
        if (plotTypeParameters == null || (plotType.getDeclaration() != null && !Objects.equals(plotType.getDeclaration().getDataClass(), plotTypeParameters.getClass()))) {
            if (plotType.getDeclaration() != null) {
                plotTypeParameters = (PlotData) ACAQData.createInstance(plotType.getDeclaration().getDataClass());
                getEventBus().post(new ParameterStructureChangedEvent(this));
            }
        } else if (plotType.getDeclaration() == null) {
            plotTypeParameters = null;
            getEventBus().post(new ParameterStructureChangedEvent(this));
        }
    }

    private void updateOutputSlotType() {
        if (plotType.getDeclaration() != null) {
            getFirstOutputSlot().setAcceptedDataType(plotType.getDeclaration().getDataClass());
        } else {
            getFirstOutputSlot().setAcceptedDataType(PlotData.class);
        }
        getEventBus().post(new AlgorithmSlotsChangedEvent(this));
    }

    @ACAQDocumentation(name = "Plot parameters")
    @ACAQParameter("plot-parameters")
    public PlotData getPlotTypeParameters() {
        return plotTypeParameters;
    }

    @ACAQDocumentation(name = "Input columns", description = "Please define which input table columns are copied into the plot. " +
            "To find out which columns are available, run the testbench on input data. You can also generate missing columns.")
    @ACAQParameter("column-assignments")
    public ACAQDynamicParameterCollection getColumnAssignments() {
        return columnAssignments;
    }
}
