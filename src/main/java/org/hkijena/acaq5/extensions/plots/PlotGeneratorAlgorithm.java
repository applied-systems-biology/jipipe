package org.hkijena.acaq5.extensions.plots;

import ij.measure.ResultsTable;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataDeclarationRef;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.events.AlgorithmSlotsChangedEvent;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.events.ParameterStructureChangedEvent;
import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.acaq5.api.parameters.*;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;
import org.hkijena.acaq5.extensions.plots.datatypes.PlotColumn;
import org.hkijena.acaq5.extensions.plots.datatypes.PlotData;
import org.hkijena.acaq5.extensions.plots.datatypes.PlotDataSeries;
import org.hkijena.acaq5.extensions.plots.datatypes.PlotMetadata;
import org.hkijena.acaq5.extensions.standardparametereditors.editors.ACAQDataParameterSettings;
import org.hkijena.acaq5.ui.registries.ACAQUIParametertypeRegistry;
import org.scijava.Priority;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        for(int row = 0; row < getFirstInputSlot().getRowCount(); ++row) {
            ResultsTableData inputData = getFirstInputSlot().getData(row, ResultsTableData.class);
            PlotData plot = (PlotData)plotTypeParameters.duplicate();

            ResultsTable seriesTable = new ResultsTable(inputData.getTable().getCounter());

            // Get column headings
            List<String> columnHeadings = new ArrayList<>();
            for(int i = 0; i <= inputData.getTable().getLastColumn(); ++i) {
                columnHeadings.add(inputData.getTable().getColumnHeading(i));
            }

            // Check if columns exist
            for (Map.Entry<String, ACAQParameterAccess> entry : columnAssignments.getParameters().entrySet()) {
                String matchingColumn = null;
                StringFilter filter = entry.getValue().get();
                for (String columnHeading : columnHeadings) {
                    if(filter.test(columnHeading)) {
                        matchingColumn = columnHeading;
                        break;
                    }
                }
                if(matchingColumn == null) {
                    throw new UserFriendlyRuntimeException("Could not find column that matches '" + filter.toString() + "'!",
                            "Could not find column!",
                            "Algorithm '" + getName() + "'",
                            "A plot generator algorithm was instructed to extract a column matching the rule '" + filter.toString() + "' for plotting. The column could note be found. " +
                                    "The table contains only following columns: " + String.join(", ", columnHeadings),
                            "Please check if your input columns are set up with valid filters. Please check the input of the plot generator " +
                                    "via the testbench to see if the input data is correct.");
                }
                seriesTable.setColumn(entry.getKey(), inputData.getTable().getColumnAsVariables(matchingColumn));
            }

            plot.getSeries().add(new PlotDataSeries(seriesTable));

            getFirstOutputSlot().addData(plot, getFirstInputSlot().getAnnotations(row));
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Plot type").checkNonNull(getPlotType().getDeclaration(), this);
        if(plotTypeParameters != null) {
            report.forCategory("Plot parameters").report(plotTypeParameters);
        }
    }

    @ACAQDocumentation(name = "Plot type", description = "The type of plot to be generated.")
    @ACAQParameter(value = "plot-type", priority = Priority.HIGH)
    @ACAQDataParameterSettings(dataBaseClass = PlotData.class)
    public ACAQDataDeclarationRef getPlotType() {
        if(plotType == null) {
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
        if(plotType.getDeclaration() != null) {
            PlotMetadata plotMetadata = plotType.getDeclaration().getDataClass().getAnnotation(PlotMetadata.class);
            for (PlotColumn column : plotMetadata.columns()) {
                ACAQMutableParameterAccess parameterAccess = new ACAQMutableParameterAccess();
                parameterAccess.setKey(column.name());
                parameterAccess.setName(column.name());
                parameterAccess.setFieldClass(StringFilter.class);
                parameterAccess.set(new StringFilter(StringFilter.Mode.Equals, column.name()));
                parameterAccess.setDescription("A column in the input table of following type: " +
                        ACAQUIParametertypeRegistry.getInstance().getDocumentationFor(column.dataType()).name());
                columnAssignments.addParameter(parameterAccess);
            }
        }
        columnAssignments.endModificationBlock();
    }

    private void updatePlotTypeParameters() {
        if(plotTypeParameters == null || (plotType.getDeclaration() != null && !Objects.equals(plotType.getDeclaration().getDataClass(), plotTypeParameters.getClass()))) {
            if(plotType.getDeclaration() != null) {
                plotTypeParameters = (PlotData) ACAQData.createInstance(plotType.getDeclaration().getDataClass());
                getEventBus().post(new ParameterStructureChangedEvent(this));
            }
        }
        else if(plotType.getDeclaration() == null) {
            plotTypeParameters = null;
            getEventBus().post(new ParameterStructureChangedEvent(this));
        }
    }

    private void updateOutputSlotType() {
        if(plotType.getDeclaration() != null) {
            getFirstOutputSlot().setAcceptedDataType(plotType.getDeclaration().getDataClass());
        }
        else {
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
            "To find out which columns are available, run the testbench on input data.")
    @ACAQParameter("column-assignments")
    public ACAQDynamicParameterCollection getColumnAssignments() {
        return columnAssignments;
    }
}
