package org.hkijena.acaq5.extensions.tables;

import org.hkijena.acaq5.ACAQJavaExtension;
import org.hkijena.acaq5.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.hkijena.acaq5.extensions.imagejdatatypes.resultanalysis.ResultsTableDataSlotRowUI;
import org.hkijena.acaq5.extensions.tables.algorithms.*;
import org.hkijena.acaq5.extensions.tables.datatypes.*;
import org.hkijena.acaq5.extensions.tables.operations.converting.*;
import org.hkijena.acaq5.extensions.tables.operations.integrating.*;
import org.hkijena.acaq5.extensions.tables.parameters.TableColumnSourceParameter;
import org.hkijena.acaq5.extensions.tables.parameters.TableColumnSourceParameterEditorUI;
import org.hkijena.acaq5.extensions.tables.parameters.collections.ConvertingTableColumnProcessorParameterList;
import org.hkijena.acaq5.extensions.tables.parameters.collections.IntegratingTableColumnProcessorParameterList;
import org.hkijena.acaq5.extensions.tables.parameters.collections.TableColumnGeneratorProcessorParameterList;
import org.hkijena.acaq5.extensions.tables.parameters.enums.TableColumnGeneratorParameter;
import org.hkijena.acaq5.extensions.tables.parameters.enums.TableColumnGeneratorParameterEditorUI;
import org.hkijena.acaq5.extensions.tables.parameters.processors.ConvertingTableColumnProcessorParameter;
import org.hkijena.acaq5.extensions.tables.parameters.processors.IntegratingTableColumnProcessorParameter;
import org.hkijena.acaq5.extensions.tables.parameters.processors.TableColumnGeneratorProcessor;
import org.hkijena.acaq5.extensions.tables.traits.ColumnHeaderTrait;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.plugin.Plugin;

/**
 * Standard set of table operations
 */
@Plugin(type = ACAQJavaExtension.class)
public class TablesExtension extends ACAQPrepackagedDefaultJavaExtension {

    @Override
    public String getName() {
        return "Standard table operations";
    }

    @Override
    public String getDescription() {
        return "Common table operations";
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.acaq5:table-operations";
    }

    @Override
    public String getDependencyVersion() {
        return "1.0.0";
    }

    @Override
    public void register() {

        // Register the two base column types
        registerDataTypes();

        // Register generating data sources
        registerColumnSources();
        registerMenuExtension(NewTableMenuExtension.class);
        registerColumnOperations();

        registerParameters();
        registerTraits();
        registerAlgorithms();
    }

    private void registerTraits() {
        registerTrait("column-header", ColumnHeaderTrait.class, ResourceUtils.getPluginResource("icons/table.png"));
        registerTrait("row-filter", ColumnHeaderTrait.class, ResourceUtils.getPluginResource("icons/filter.png"));
    }

    private void registerColumnSources() {
        registerDatatype("table-column-row-index",
                RowIndexTableColumn.class,
                ResourceUtils.getPluginResource("icons/data-types/table-column.png"),
                null,
                null);
        registerDatatype("table-column-zero",
                ZeroTableColumn.class,
                ResourceUtils.getPluginResource("icons/data-types/table-column.png"),
                null,
                null);
    }

    private void registerDataTypes() {
        registerDatatype("table-column",
                TableColumn.class,
                ResourceUtils.getPluginResource("icons/data-types/table-column.png"),
                ResultsTableDataSlotRowUI.class,
                null);
        registerDatatype("table-column-numeric",
                DoubleArrayTableColumn.class,
                ResourceUtils.getPluginResource("icons/data-types/table-column.png"),
                ResultsTableDataSlotRowUI.class,
                null);
        registerDatatype("table-column-string",
                StringArrayTableColumn.class,
                ResourceUtils.getPluginResource("icons/data-types/table-column.png"),
                ResultsTableDataSlotRowUI.class,
                null);
        registerDatatypeConversion(new DoubleArrayColumnToTableConverter());
        registerDatatypeConversion(new StringArrayColumnToTableConverter());
    }

    private void registerAlgorithms() {
        registerAlgorithm("table-add-columns-generate", GenerateColumnAlgorithm.class, UIUtils.getAlgorithmIconURL("edit-table-insert-column-right.png"));
        registerAlgorithm("table-remove-columns", RemoveColumnAlgorithm.class, UIUtils.getAlgorithmIconURL("edit-table-delete-column.png"));
        registerAlgorithm("table-rename-columns", RenameTableColumnsAlgorithm.class, UIUtils.getAlgorithmIconURL("pen.png"));
        registerAlgorithm("table-rename-single-columns", RenameColumnsAlgorithm.class, UIUtils.getAlgorithmIconURL("pen.png"));
        registerAlgorithm("table-integrate-columns", IntegrateColumnsAlgorithm.class, UIUtils.getAlgorithmIconURL("statistics.png"));
        registerAlgorithm("table-convert-columns", ConvertColumnsAlgorithm.class, UIUtils.getAlgorithmIconURL("formula.png"));
        registerAlgorithm("table-split-into-columns", SplitTableIntoColumnsAlgorithm.class, UIUtils.getAlgorithmIconURL("split.png"));
        registerAlgorithm("table-merge-from-columns", MergeColumnsAlgorithm.class, UIUtils.getAlgorithmIconURL("merge.png"));
        registerAlgorithm("table-merge-tables", MergeTablesAlgorithm.class, UIUtils.getAlgorithmIconURL("merge.png"));
        registerAlgorithm("table-split-by-columns", SplitTableByColumnsAlgorithm.class, UIUtils.getAlgorithmIconURL("split.png"));
        registerAlgorithm("table-filter", FilterTableAlgorithm.class, UIUtils.getAlgorithmIconURL("filter.png"));
        registerAlgorithm("table-sort", SortTableAlgorithm.class, UIUtils.getAlgorithmIconURL("sort-amount-up-alt.png"));
    }

    private void registerParameters() {
        registerEnumParameterType("table-column-content-type",
                ColumnContentType.class,
                "Column content type",
                "Determines if the column is numeric or contains text values");
        registerParameterType("table-column-generator",
                TableColumnGeneratorParameter.class,
                TableColumnGeneratorParameter::new,
                p -> new TableColumnGeneratorParameter((TableColumnGeneratorParameter) p),
                "Column generator",
                "Defines a column generator",
                TableColumnGeneratorParameterEditorUI.class);
        registerParameterType("table-column-source",
                TableColumnSourceParameter.class,
                TableColumnSourceParameter::new,
                p -> new TableColumnSourceParameter((TableColumnSourceParameter) p),
                "Column source",
                "Defines a column source",
                TableColumnSourceParameterEditorUI.class);

        // Processors
        registerParameterType("integrating-table-column-processor",
                IntegratingTableColumnProcessorParameter.class,
                IntegratingTableColumnProcessorParameter::new,
                p -> new IntegratingTableColumnProcessorParameter((IntegratingTableColumnProcessorParameter) p),
                "Column integration processor",
                "Defines a processor that integrates a column",
                null);
        registerParameterType("integrating-table-column-processor-list",
                IntegratingTableColumnProcessorParameterList.class,
                IntegratingTableColumnProcessorParameterList::new,
                p -> new IntegratingTableColumnProcessorParameterList((IntegratingTableColumnProcessorParameterList) p),
                "Column integration processor list",
                "Defines processors that integrate columns",
                null);

        registerParameterType("converting-table-column-processor",
                ConvertingTableColumnProcessorParameter.class,
                ConvertingTableColumnProcessorParameter::new,
                p -> new ConvertingTableColumnProcessorParameter((ConvertingTableColumnProcessorParameter) p),
                "Column integration processor",
                "Defines a processor that apply a function to each cell",
                null);
        registerParameterType("converting-table-column-processor-list",
                ConvertingTableColumnProcessorParameterList.class,
                ConvertingTableColumnProcessorParameterList::new,
                p -> new ConvertingTableColumnProcessorParameterList((ConvertingTableColumnProcessorParameterList) p),
                "Column conversion processor list",
                "Defines processors that apply a function to each cell",
                null);

        registerParameterType("table-column-generator-column-processor",
                TableColumnGeneratorProcessor.class,
                TableColumnGeneratorProcessor::new,
                p -> new TableColumnGeneratorProcessor((TableColumnGeneratorProcessor) p),
                "Column generator processor",
                "Defines a processor that generates a column",
                null);
        registerParameterType("table-column-generator-column-processor-list",
                TableColumnGeneratorProcessorParameterList.class,
                TableColumnGeneratorProcessorParameterList::new,
                p -> new TableColumnGeneratorProcessorParameterList((TableColumnGeneratorProcessorParameterList) p),
                "Column generator processor list",
                "Defines multiple columns to be generated",
                null);
    }

    private void registerColumnOperations() {
        // Converters
        registerTableColumnOperation("convert-to-numeric",
                new ToNumericColumnOperation(),
                "Convert to numeric",
                "numeric",
                "Converts string columns into numeric columns. Attempts to convert a string into a numeric value. If this fails, zero is returned for this item.");
        registerTableColumnOperation("convert-factorize",
                new FactorizeColumnOperation(),
                "Factorize",
                "factors",
                "Converts each item into a numeric factor. The numeric factor is equal for equal items.");
        registerTableColumnOperation("convert-occurrences",
                new OccurrencesColumnOperation(),
                "Occurrences",
                "occ",
                "Converts each item to the count of equal items within the column.");
        registerTableColumnOperation("convert-sort-ascending",
                new SortAscendingColumnOperation(),
                "Sort ascending",
                "sorta",
                "Sorts the numeric items in ascending order. String values are sorted in lexicographic order.");
        registerTableColumnOperation("convert-sort-descending",
                new SortDescendingColumnOperation(),
                "Sort descending",
                "sortd",
                "Sorts the numeric items in descending order. String values are sorted in lexicographic order.");
        registerTableColumnOperation("convert-remove-nan",
                new RemoveNaNColumnOperation(),
                "Remove NaN",
                "rmnan",
                "Replaces all NaN values by a zero. The same applies to string values.");
        registerTableColumnOperation("convert-absolute",
                new AbsoluteColumnOperation(),
                "Absolute value",
                "abs",
                "Calculates the absolute values. String values are converted to numbers or zero.");
        registerTableColumnOperation("convert-ceiling",
                new CeilingColumnOperation(),
                "Ceiling",
                "ceil",
                "Rounds the values to the higher integer value. String values are converted to numbers or zero.");
        registerTableColumnOperation("convert-floor",
                new FloorColumnOperation(),
                "Floor",
                "floor",
                "Rounds the values to the lower integer value. String values are converted to numbers or zero.");
        registerTableColumnOperation("convert-clamp-negative",
                new ClampNegativeColumnOperation(),
                "Set positive to zero",
                "clampn",
                "Sets positives numbers to zero. String values are converted to numbers or zero.");
        registerTableColumnOperation("convert-clamp-positive",
                new ClampPositiveColumnOperation(),
                "Set negative to zero",
                "clampp",
                "Sets negative numbers to zero. String values are converted to numbers or zero.");
        registerTableColumnOperation("convert-degree-to-radians",
                new DegreeToRadiansColumnOperation(),
                "Degree to radians",
                "deg2rad",
                "Converts the value from degree to radians. String values are converted to numbers or zero.");
        registerTableColumnOperation("convert-radians-to-degree",
                new RadiansToDegreeColumnOperation(),
                "Radians to degree",
                "rad2deg",
                "Converts the value from radians to degree. String values are converted to numbers or zero.");
        registerTableColumnOperation("convert-exp",
                new ExpColumnOperation(),
                "Exponent (base e)",
                "exp",
                "Calculates e^x for each value x. String values are converted to numbers or zero.");
        registerTableColumnOperation("convert-ln",
                new LnColumnOperation(),
                "Logarithm (base e)",
                "ln",
                "Calculates ln(x) for each value x. String values are converted to numbers or zero.");
        registerTableColumnOperation("convert-sqrt",
                new SqrtColumnOperation(),
                "Square root",
                "sqrt",
                "Calculates sqrt(x) for each value x. String values are converted to numbers or zero.");
        registerTableColumnOperation("convert-square",
                new SquareColumnOperation(),
                "Square",
                "sqr",
                "Calculates x^2 for each value x. String values are converted to numbers or zero.");

        // Integrating functions
        registerTableColumnOperation("statistics-average",
                new StatisticsAverageIntegratingColumnOperation(),
                "Average",
                "avg",
                "Calculates the average value of all numeric values. String values are converted to numbers or zero.");
        registerTableColumnOperation("statistics-count",
                new StatisticsCountIntegratingColumnOperation(),
                "Count",
                "count",
                "Outputs the number of input rows.");
        registerTableColumnOperation("statistics-count-non-zero",
                new StatisticsCountNonZeroIntegratingColumnOperation(),
                "Count non-zero",
                "countn0",
                "Counts all values that are not zero. String values are converted to numbers or zero.");
        registerTableColumnOperation("statistics-cumulative-sum",
                new StatisticsCumulativeSumColumnOperation(),
                "Cumulative sum",
                "cumsum",
                "Calculates the cumulative sum of the rows in their order.");
        registerTableColumnOperation("statistics-geometric-mean",
                new StatisticsGeometricMeanIntegratingColumnOperation(),
                "Geometric mean",
                "geomean",
                "Calculates the geometric mean of all numeric values. String values are converted to numbers or zero.");
        registerTableColumnOperation("statistics-kurtosis",
                new StatisticsKurtosisIntegratingColumnOperation(),
                "Kurtosis",
                "kurt",
                "Calculates the kurtosis of all numeric values. String values are converted to numbers or zero.");
        registerTableColumnOperation("statistics-min",
                new StatisticsMinIntegratingColumnOperation(),
                "Minimum",
                "min",
                "Calculates the minimum of all numeric values. String values are converted to numbers or zero.");
        registerTableColumnOperation("statistics-max",
                new StatisticsMaxIntegratingColumnOperation(),
                "Maximum",
                "max",
                "Calculates the maximum of all numeric values. String values are converted to numbers or zero.");
        registerTableColumnOperation("statistics-median",
                new StatisticsMedianIntegratingColumnOperation(),
                "Median",
                "median",
                "Calculates the median of all numeric values. String values are converted to numbers or zero.");
        registerTableColumnOperation("statistics-skewness",
                new StatisticsSkewnessIntegratingColumnOperation(),
                "Skewness",
                "skewness",
                "Calculates the skewness of all numeric values. String values are converted to numbers or zero.");
        registerTableColumnOperation("statistics-standard-deviation",
                new StatisticsStandardDeviationIntegratingColumnOperation(),
                "Standard deviation",
                "stdev",
                "Calculates the standard deviation of all numeric values. String values are converted to numbers or zero.");
        registerTableColumnOperation("statistics-variance",
                new StatisticsVarianceIntegratingColumnOperation(),
                "Variance",
                "var",
                "Calculates the variance of all numeric values. String values are converted to numbers or zero.");
        registerTableColumnOperation("statistics-weighted-sum",
                new StatisticsWeightedSumColumnOperation(),
                "Weighted sum",
                "wsum",
                "Calculates the weighted sum of all values. The weight is the row index (beginning with 1). String values are converted to numbers or zero.");
    }
}
