package org.hkijena.acaq5.extensions.tables;

import org.hkijena.acaq5.ACAQJavaExtension;
import org.hkijena.acaq5.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.hkijena.acaq5.extensions.tables.algorithms.GenerateColumnAlgorithm;
import org.hkijena.acaq5.extensions.tables.algorithms.RemoveColumnAlgorithm;
import org.hkijena.acaq5.extensions.tables.algorithms.RenameColumnAlgorithm;
import org.hkijena.acaq5.extensions.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.RowIndexTableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.StringArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.ZeroTableColumn;
import org.hkijena.acaq5.extensions.tables.operations.*;
import org.hkijena.acaq5.extensions.tables.parameters.TableColumnGeneratorParameter;
import org.hkijena.acaq5.extensions.tables.parameters.TableColumnGeneratorParameterEditorUI;
import org.hkijena.acaq5.extensions.tables.parameters.TableColumnSourceParameter;
import org.hkijena.acaq5.extensions.tables.parameters.TableColumnSourceParameterEditorUI;
import org.hkijena.acaq5.extensions.tables.ui.tableoperations.*;
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
        registerDatatype("table-column-numeric",
                DoubleArrayTableColumn.class,
                ResourceUtils.getPluginResource("icons/data-types/table-column.png"),
                null,
                null);
        registerDatatype("table-column-string",
                StringArrayTableColumn.class,
                ResourceUtils.getPluginResource("icons/data-types/table-column.png"),
                null,
                null);

        // Register generating data sources
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

        registerMenuExtension(NewTableMenuExtension.class);

        registerColumnOperations();

        // Register spreadsheet operations
        registerTableOperation(StatisticsCountVectorOperation.class,
                null,
                "Count",
                "COUNT",
                "Counts all entries",
                UIUtils.getIconFromResources("statistics.png"));
        registerTableOperation(StatisticsCountNonNullVectorOperation.class,
                null,
                "Count Non-Empty",
                "COUNT_NON_EMPTY",
                "Counts all non-empty entries",
                UIUtils.getIconFromResources("statistics.png"));
        registerTableOperation(StatisticsSumVectorOperation.class,
                null,
                "Sum",
                "SUM",
                "Summarizes all entries",
                UIUtils.getIconFromResources("statistics.png"));
        registerTableOperation(StatisticsMinVectorOperation.class,
                null,
                "Minimum",
                "MIN",
                "Minimum value of entries",
                UIUtils.getIconFromResources("statistics.png"));
        registerTableOperation(StatisticsMaxVectorOperation.class,
                null,
                "Maximum",
                "MAX",
                "Maximum value of entries",
                UIUtils.getIconFromResources("statistics.png"));
        registerTableOperation(StatisticsMedianVectorOperation.class,
                null,
                "Median",
                "MEDIAN",
                "Median value of entries",
                UIUtils.getIconFromResources("statistics.png"));
        registerTableOperation(StatisticsAverageVectorOperation.class,
                null,
                "Average",
                "AVG",
                "Average of entries",
                UIUtils.getIconFromResources("statistics.png"));
        registerTableOperation(StatisticsVarianceVectorOperation.class,
                null,
                "Variance",
                "VAR",
                "Variance of entries",
                UIUtils.getIconFromResources("statistics.png"));
        registerTableOperation(ConvertToOccurrencesVectorOperation.class,
                null,
                "Number of entries",
                "COUNT",
                "Returns the number of items",
                UIUtils.getIconFromResources("statistics.png"));

        registerTableOperation(ConvertToNumericVectorOperation.class,
                null,
                "Convert to numbers",
                "TO_NUMBERS",
                "Ensures that all items are numbers. Non-numeric values are set to zero.",
                UIUtils.getIconFromResources("inplace-function.png"));
        registerTableOperation(ConvertToNumericBooleanVectorOperation.class,
                null,
                "Convert to numeric boolean",
                "TO_NUMERIC_BOOLEAN",
                "Ensures that all items are numeric boolean values. Defaults to outputting zero if the value is not valid.",
                UIUtils.getIconFromResources("inplace-function.png"));
        registerTableOperation(ConvertToOccurrencesVectorOperation.class,
                null,
                "Convert to number of occurrences",
                "TO_OCCURENCES",
                "Replaces the items by their number of occurrences within the list of items.",
                UIUtils.getIconFromResources("inplace-function.png"));
        registerTableOperation(ConvertToNumericFactorOperation.class,
                null,
                "Convert to numeric factors",
                "TO_FACTORS",
                "Replaces each item with an ID that uniquely identifies the item.",
                UIUtils.getIconFromResources("inplace-function.png"));

        // Register parameter types
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

        // Register algorithms
        registerAlgorithm("table-add-columns-generate", GenerateColumnAlgorithm.class);
        registerAlgorithm("table-remove-columns", RemoveColumnAlgorithm.class);
        registerAlgorithm("table-rename-columns", RenameColumnAlgorithm.class);
    }

    private void registerColumnOperations() {
        registerTableColumnOperation("to-numeric",
                new ToNumericColumnOperation(),
                "Convert to numeric",
                "numeric",
                "Converts string columns into numeric columns. Attempts to convert a string into a numeric value. If this fails, zero is returned for this item.");
        registerTableColumnOperation("factorize",
                new FactorizeColumnOperation(),
                "Factorize",
                "factors",
                "Converts each item into a numeric factor. The numeric factor is equal for equal items.");
        registerTableColumnOperation("occurrences",
                new OccurrencesColumnOperation(),
                "Occurrences",
                "occ",
                "Converts each item to the count of equal items within the column.");
        registerTableColumnOperation("sort-ascending",
                new SortAscendingColumnOperation(),
                "Sort ascending",
                "sorta",
                "Sorts the numeric items in ascending order. String values are sorted in lexicographic order.");
        registerTableColumnOperation("sort-descending",
                new SortDescendingColumnOperation(),
                "Sort descending",
                "sortd",
                "Sorts the numeric items in descending order. String values are sorted in lexicographic order.");
        registerTableColumnOperation("remove-nan",
                new RemoveNaNColumnOperation(),
                "Remove NaN",
                "rmnan",
                "Replaces all NaN values by a zero. The same applies to string values.");
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
