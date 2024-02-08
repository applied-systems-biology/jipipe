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

package org.hkijena.jipipe.extensions.plots.nodes;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHidden;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataParameterSettings;
import org.hkijena.jipipe.extensions.plots.datatypes.PlotColumn;
import org.hkijena.jipipe.extensions.plots.datatypes.PlotData;
import org.hkijena.jipipe.extensions.plots.datatypes.PlotDataSeries;
import org.hkijena.jipipe.extensions.plots.datatypes.PlotMetadata;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.plotbuilder.PlotDataClassFilter;
import org.scijava.Priority;

import java.util.*;

/**
 * Algorithm that creates {@link PlotData} from {@link ResultsTableData}
 *
 * @deprecated Instead, the new dynamically generated plotting should be utilized
 */
@JIPipeDocumentation(name = "Plot tables", description = "Plots incoming tables. First, set the plot type via a parameter. This " +
        "will then show the available settings for this plot type, and a list of input columns for the plot. " +
        "Please ensure to correctly setup these input columns.")
@JIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class)
@JIPipeInputSlot(ResultsTableData.class)
@JIPipeOutputSlot(PlotData.class)
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Analyze", aliasName = "Plot (JFreeChart)")
@JIPipeHidden
@Deprecated
public class PlotTablesAlgorithm extends JIPipeMergingAlgorithm {

    private JIPipeDataInfoRef plotType = new JIPipeDataInfoRef();
    private PlotData plotTypeParameters;
    private JIPipeDynamicParameterCollection inputColumns = new JIPipeDynamicParameterCollection(false);
    private StringQueryExpression seriesName = new StringQueryExpression("SUMMARIZE_VARIABLES()");

    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public PlotTablesAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", "Tables that will be plotted", ResultsTableData.class)
                .addOutputSlot("Output", "Generated plots", PlotData.class)
                .seal()
                .build());
        registerSubParameter(inputColumns);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public PlotTablesAlgorithm(PlotTablesAlgorithm other) {
        super(other);
        this.plotType = new JIPipeDataInfoRef(other.plotType);
        if (other.plotTypeParameters != null)
            this.plotTypeParameters = (PlotData) other.plotTypeParameters.duplicate(new JIPipeProgressInfo());
        this.inputColumns = new JIPipeDynamicParameterCollection(other.inputColumns);
        this.seriesName = new StringQueryExpression(other.seriesName);
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        PlotMetadata plotMetadata = plotType.getInfo().getDataClass().getAnnotation(PlotMetadata.class);
        Map<String, PlotColumn> plotColumns = new HashMap<>();
        for (PlotColumn column : plotMetadata.columns()) {
            plotColumns.put(column.name(), column);
        }

        PlotData plot = (PlotData) plotTypeParameters.duplicate(progressInfo);
        int seriesCounter = 0;
        for (int row : iterationStep.getInputRows(getFirstInputSlot())) {

            ResultsTableData inputData = getFirstInputSlot().getData(row, ResultsTableData.class, progressInfo);
            ResultsTableData seriesTable = new ResultsTableData();
            seriesTable.addRows(inputData.getRowCount());

            JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
            List<JIPipeTextAnnotation> originalAnnotations = getFirstInputSlot().getTextAnnotations(row);
            for (JIPipeTextAnnotation annotation : originalAnnotations) {
                variables.set(annotation.getName(), annotation.getValue());
            }

            // Generate series
            for (Map.Entry<String, JIPipeParameterAccess> entry : inputColumns.getParameters().entrySet()) {
                TableColumnSourceExpressionParameter parameter = entry.getValue().get(TableColumnSourceExpressionParameter.class);
                seriesTable.setColumn(entry.getKey(), parameter.pickOrGenerateColumn(inputData), plotColumns.get(entry.getKey()).isNumeric());
            }

            PlotDataSeries series = new PlotDataSeries(seriesTable.getTable());
            series.setName(seriesName.generate(variables));
            plot.addSeries(series);

            // Increment the series counter
            seriesCounter += 1;
            if (seriesCounter >= plotMetadata.maxSeriesCount()) {
                progressInfo.log("Maximum number of series was reached (maximum is " + plotMetadata.maxSeriesCount() + "!). Creating a new plot.");
                iterationStep.addOutputData(getFirstOutputSlot(), plot, progressInfo);
                plot = (PlotData) plotTypeParameters.duplicate(progressInfo);
                seriesCounter = 0;
            }
        }
        if (!plot.getSeries().isEmpty()) {
            iterationStep.addOutputData(getFirstOutputSlot(), plot, progressInfo);
        }
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        if (getPlotType().getInfo() == null) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new ParameterValidationReportContext(context, this, "Plot type", "plot-type"),
                    "Plot type not set!",
                    "Please choose a plot type"));
        }
        if (plotTypeParameters != null) {
            report.report(new ParameterValidationReportContext(context, this, "Plot parameters", "plot-parameters"), plotTypeParameters);
        }
    }

    @JIPipeDocumentation(name = "Plot type", description = "The type of plot to be generated.")
    @JIPipeParameter(value = "plot-type", priority = Priority.HIGH, important = true)
    @JIPipeDataParameterSettings(dataBaseClass = PlotData.class, dataClassFilter = PlotDataClassFilter.class)
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
        inputColumns.beginModificationBlock();
        inputColumns.clear();
        if (plotType.getInfo() != null) {
            PlotMetadata plotMetadata = plotType.getInfo().getDataClass().getAnnotation(PlotMetadata.class);
            for (PlotColumn column : plotMetadata.columns()) {
                JIPipeMutableParameterAccess parameterAccess = new JIPipeMutableParameterAccess();
                parameterAccess.setKey(column.name());
                parameterAccess.setName(column.name());
                parameterAccess.setImportant(true);
                parameterAccess.setFieldClass(TableColumnSourceExpressionParameter.class);
                TableColumnSourceExpressionParameter initialValue = new TableColumnSourceExpressionParameter();
                parameterAccess.set(initialValue);
                parameterAccess.setDescription(column.description() + " " + (column.isNumeric() ? "(Numeric column)" : "(String column)"));
                inputColumns.addParameter(parameterAccess);
            }
        }
        inputColumns.endModificationBlock();
    }

    private void updatePlotTypeParameters() {
        if (plotTypeParameters == null || (plotType.getInfo() != null && !Objects.equals(plotType.getInfo().getDataClass(), plotTypeParameters.getClass()))) {
            if (plotType.getInfo() != null) {
                plotTypeParameters = (PlotData) JIPipe.createData(plotType.getInfo().getDataClass());
                emitParameterStructureChangedEvent();
            }
        } else if (plotType.getInfo() == null) {
            plotTypeParameters = null;
            emitParameterStructureChangedEvent();
        }
    }

    private void updateOutputSlotType() {
        if (plotType.getInfo() != null) {
            getFirstOutputSlot().setAcceptedDataType(plotType.getInfo().getDataClass());
        } else {
            getFirstOutputSlot().setAcceptedDataType(PlotData.class);
        }
        emitNodeSlotsChangedEvent();
    }

    @JIPipeDocumentation(name = "Plot parameters")
    @JIPipeParameter("plot-parameters")
    public PlotData getPlotTypeParameters() {
        return plotTypeParameters;
    }

    @JIPipeDocumentation(name = "Input columns", description = "Please define which input table columns are copied into the plot. " +
            "To find out which columns are available, run the quick run on input data. You can also generate missing columns.")
    @JIPipeParameter(value = "input-columns", persistence = JIPipeParameterPersistence.NestedCollection)
    public JIPipeDynamicParameterCollection getInputColumns() {
        return inputColumns;
    }

    @JIPipeDocumentation(name = "Series name", description = "Expression that is used to generate the series name")
    @JIPipeParameter("series-name")
    @JIPipeExpressionParameterSettings(variableSource = VariablesInfo.class)
    public StringQueryExpression getSeriesName() {
        return seriesName;
    }

    @JIPipeParameter("series-name")
    public void setSeriesName(StringQueryExpression seriesName) {
        this.seriesName = seriesName;
    }

    public static class VariablesInfo implements ExpressionParameterVariablesInfo {

        public static final Set<ExpressionParameterVariable> VARIABLES;

        static {
            VARIABLES = new HashSet<>();
            VARIABLES.add(ExpressionParameterVariable.ANNOTATIONS_VARIABLE);
        }

        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }
}
