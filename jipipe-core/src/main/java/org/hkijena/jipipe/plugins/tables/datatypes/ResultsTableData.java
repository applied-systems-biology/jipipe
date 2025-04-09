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

package org.hkijena.jipipe.plugins.tables.datatypes;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import ij.IJ;
import ij.macro.Variable;
import ij.measure.ResultsTable;
import ij.util.Tools;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.LabelAsJIPipeCommonData;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeFastThumbnail;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeTextThumbnailData;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeThumbnailData;
import org.hkijena.jipipe.plugins.tables.ConvertingColumnOperation;
import org.hkijena.jipipe.plugins.tables.SummarizingColumnOperation;
import org.hkijena.jipipe.plugins.tables.TableColumnDataReference;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.python.core.PyDictionary;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static ij.measure.ResultsTable.COLUMN_NOT_FOUND;

/**
 * Data containing a {@link ResultsTable}
 */
@SetJIPipeDocumentation(name = "ImageJ table", description = "An ImageJ results table")
@JsonSerialize(using = ResultsTableData.Serializer.class)
@JsonDeserialize(using = ResultsTableData.Deserializer.class)
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains a single *.csv file that contains the table data.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/results-table.schema.json")
@LabelAsJIPipeCommonData
@JIPipeFastThumbnail
public class ResultsTableData implements JIPipeData, TableModel {

    private static final char commaSubstitute = 0x08B3;
    private final List<TableModelListener> listeners = new ArrayList<>();
    private ResultsTable table;

    /**
     * Creates a new instance
     */
    public ResultsTableData() {
        this.table = new ResultsTable();
    }

    /**
     * Creates a table from a map of column name to column data
     *
     * @param columns key is column heading
     */
    public ResultsTableData(Map<String, TableColumnData> columns) {
        importDataColumns(columns);
    }

    /**
     * Creates a table from a list of columns.
     * Column headings are extracted from the column labels
     *
     * @param columns the columns
     */
    public ResultsTableData(Collection<TableColumnData> columns) {
        Map<String, TableColumnData> map = new LinkedHashMap<>();
        for (TableColumnData column : columns) {
            map.put(column.getLabel(), column);
        }
        importDataColumns(map);
    }

    /**
     * Wraps a results table
     *
     * @param table wrapped table
     */
    public ResultsTableData(ResultsTable table) {
        this.table = table;
        cleanupTable();
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ResultsTableData(ResultsTableData other) {
        this.table = (ResultsTable) other.table.clone();
    }

    public static ResultsTableData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        try {
            return new ResultsTableData(ResultsTable.open(PathUtils.findFileByExtensionIn(storage.getFileSystemPath(), ".csv").toString()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Converts a Python dictionary of column name to row data list to a results table
     *
     * @param tableDict the dictionary
     * @return equivalent results table
     */
    public static ResultsTableData fromPython(PyDictionary tableDict) {
        Map<String, TableColumnData> columns = new HashMap<>();
        for (Object key : tableDict.keys()) {
            String columnKey = "" + key;
            List<Object> rows = (List<Object>) tableDict.get(key);
            boolean isNumeric = true;
            for (Object row : rows) {
                isNumeric &= row instanceof Number;
            }
            if (isNumeric) {
                double[] data = new double[rows.size()];
                for (int i = 0; i < rows.size(); i++) {
                    data[i] = ((Number) rows.get(i)).doubleValue();
                }
                columns.put(columnKey, new DoubleArrayTableColumnData(data, columnKey));
            } else {
                String[] data = new String[rows.size()];
                for (int i = 0; i < rows.size(); i++) {
                    data[i] = "" + rows.get(i);
                }
                columns.put(columnKey, new StringArrayTableColumnData(data, columnKey));
            }
        }
        return new ResultsTableData(columns);
    }

    public static ResultsTableData fromCSV(Path path) {
        return fromCSV(path, ",");
    }

    private static String replaceQuotedCommas(String text) {
        char[] c = text.toCharArray();
        boolean inQuotes = false;
        for (int i = 0; i < c.length; i++) {
            if (c[i] == '"')
                inQuotes = !inQuotes;
            if (inQuotes && c[i] == ',')
                c[i] = commaSubstitute;
        }
        return new String(c);
    }

    private static int getTableType(String[] lines, String cellSeparator) {
        if (lines.length < 2) return 0;
        String[] items = lines[1].split(cellSeparator);
        int nonNumericCount = 0;
        int nonNumericIndex = 0;
        for (int i = 0; i < items.length; i++) {
            if (!items[i].equals("NaN") && Double.isNaN(Tools.parseDouble(items[i]))) {
                nonNumericCount++;
                nonNumericIndex = i;
            }
        }
        if (nonNumericCount == 0)
            return 0; // assume this is all-numeric table
        if (nonNumericCount == 1 && nonNumericIndex == 1)
            return 1; // assume this is an ImageJ Results table with row numbers and row labels
        if (nonNumericCount == 1 && nonNumericIndex == 0)
            return 2; // assume this is an ImageJ Results table without row numbers and with row labels
        return 3;
    }

    public static ResultsTableData fromCSV(Path path, String cellSeparator) {
        final String lineSeparator = "\n";
        String text = IJ.openAsString(path.toString());
        if (text == null)
            return null;
        if (text.length() == 0)
            return new ResultsTableData();
        if (text.startsWith("Error:"))
            throw new RuntimeException(new IOException("Error opening " + path));
        boolean commasReplaced = false;
        if (text.contains("\"")) {
            text = replaceQuotedCommas(text);
            commasReplaced = true;
        }
        String commaSubstitute2 = "" + commaSubstitute;
        String[] lines = text.split(lineSeparator);
        if (lines.length == 0 || (lines.length == 1 && lines[0].length() == 0))
            throw new RuntimeException(new IOException("Table is empty or invalid"));
        String[] headings = lines[0].split(cellSeparator);
        if (headings.length < 1)
            throw new RuntimeException(new IOException("This is not a tab or comma delimited text file."));
        int numbersInHeadings = 0;
        for (String heading : headings) {
            if (heading.equals("NaN") || !Double.isNaN(Tools.parseDouble(heading)))
                numbersInHeadings++;
        }
        boolean allNumericHeadings = numbersInHeadings == headings.length;
        if (allNumericHeadings) {
            for (int i = 0; i < headings.length; i++)
                headings[i] = "C" + (i + 1);
        }
        int firstColumn = headings.length > 0 && headings[0].equals(" ") ? 1 : 0;
        for (int i = 0; i < headings.length; i++) {
            headings[i] = headings[i].trim();
            if (commasReplaced) {
                if (headings[i].startsWith("\"") && headings[i].endsWith("\""))
                    headings[i] = headings[i].substring(1, headings[i].length() - 1);
            }
        }
        int firstRow = allNumericHeadings ? 0 : 1;
        boolean labels = firstColumn == 1 && headings[1].equals("Label");
        int type = getTableType(lines, cellSeparator);
        //if (!labels && (type==1||type==2))
        //	labels = true;
        int labelsIndex = (type == 2) ? 0 : 1;
        if (lines[0].startsWith("\t")) {
            String[] headings2 = new String[headings.length + 1];
            headings2[0] = " ";
            System.arraycopy(headings, 0, headings2, 1, headings.length);
            headings = headings2;
            firstColumn = 1;
        }
        ResultsTable rt = new ResultsTable();
        if (firstRow >= lines.length) { //empty table?
            for (String heading : headings) {
                if (heading == null) continue;
                int col = rt.getColumnIndex(heading);
                if (col == COLUMN_NOT_FOUND)
                    col = rt.getFreeColumn(heading);
            }
            return new ResultsTableData(rt);
        }
        for (int i = firstRow; i < lines.length; i++) {
            rt.incrementCounter();
            String[] items = lines[i].split(cellSeparator);
            for (int j = firstColumn; j < headings.length; j++) {
                if (j == labelsIndex && labels)
                    rt.addLabel(headings[labelsIndex], items[labelsIndex]);
                else {
                    double value = j < items.length ? Tools.parseDouble(items[j]) : Double.NaN;
                    if (Double.isNaN(value)) {
                        String item = j < items.length ? items[j] : "";
                        if (commasReplaced) {
                            item = item.replaceAll(commaSubstitute2, ",");
                            if (item.startsWith("\"") && item.endsWith("\""))
                                item = item.substring(1, item.length() - 1);
                        }
                        rt.addValue(headings[j], item);
                    } else
                        rt.addValue(headings[j], value);
                }
            }
        }
        return new ResultsTableData(rt);
    }

    /**
     * Converts a table model into a string results table
     *
     * @param model the model
     * @return the results table
     */
    public static ResultsTableData stringModelFromTableModel(TableModel model) {
        ResultsTableData resultsTableData = new ResultsTableData();
        for (int col = 0; col < model.getColumnCount(); col++) {
            String newColumnName = StringUtils.makeUniqueString(model.getColumnName(col), " ", resultsTableData.getColumnNames());
            resultsTableData.addColumn(newColumnName, true);
        }
        for (int row = 0; row < model.getRowCount(); row++) {
            resultsTableData.addRow();
            for (int col = 0; col < model.getColumnCount(); col++) {
                resultsTableData.setValueAt("" + model.getValueAt(row, col), row, col);
            }
        }
        return resultsTableData;
    }

    /**
     * Imports a table from a table model and column model
     *
     * @param model               the table model
     * @param columnModel         the column model (can be null)
     * @param stripColumnNameHtml if enabled, remove HTML from column names
     * @return the table
     */
    public static ResultsTableData fromTableModel(TableModel model, TableColumnModel columnModel, boolean stripColumnNameHtml) {
        ResultsTableData resultsTableData = new ResultsTableData();
        for (int col = 0; col < model.getColumnCount(); col++) {
            String requestedName;
            if (columnModel != null) {
                requestedName = StringUtils.nullToEmpty(columnModel.getColumn(col).getIdentifier());
                if (requestedName.isEmpty()) {
                    requestedName = model.getColumnName(col);
                }
            } else {
                requestedName = model.getColumnName(col);
            }
            if (stripColumnNameHtml) {
                requestedName = Jsoup.clean(requestedName, new Whitelist());
            }
            String newColumnName = StringUtils.makeUniqueString(requestedName, " ", resultsTableData.getColumnNames());
            resultsTableData.addColumn(newColumnName, false);
        }
        for (int row = 0; row < model.getRowCount(); row++) {
            resultsTableData.addRow();
            for (int col = 0; col < model.getColumnCount(); col++) {
                resultsTableData.setValueAt(model.getValueAt(row, col), row, col);
            }
        }
        return resultsTableData;
    }

    public static Map<String, ResultsTableData> fromXLSX(Path xlsxFile) {
        Map<String, ResultsTableData> output = new HashMap<>();
        try (Workbook workbook = new XSSFWorkbook(xlsxFile.toFile())) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                ResultsTableData tableData = new ResultsTableData();
                boolean hasHeader = false;
                for (Row row : sheet) {
                    if (hasHeader) {
                        for (Cell cell : row) {
                            if (cell.getCellType() == CellType.NUMERIC) {
                                tableData.setValueAt(cell.getNumericCellValue(), row.getRowNum() - 1, cell.getColumnIndex());
                            } else {
                                tableData.setValueAt(cell.getStringCellValue(), row.getRowNum() - 1, cell.getColumnIndex());
                            }
                        }
                    } else {
                        for (Cell cell : row) {
                            String columnName = StringUtils.makeUniqueString(cell.getStringCellValue(), "-", tableData.getColumnNames());
                            tableData.addNumericColumn(columnName);
                        }
                        hasHeader = true;
                    }
                }

                output.put(sheet.getSheetName(), tableData);
            }
        } catch (IOException | InvalidFormatException e) {
            throw new RuntimeException(e);
        }
        return output;
    }

    /**
     * Creates a valid XLSX sheet name from a string
     *
     * @param name     the string. if null or empty, will be assumed to be "Sheet"
     * @param existing existing names
     * @return valid sheet name
     */
    public static String createXLSXSheetName(String name, Collection<String> existing) {
        if (StringUtils.isNullOrEmpty(name))
            name = "Sheet";
        name = name.replace('\0', ' ');
        name = name.replace('\3', ' ');
        name = name.replace(':', ' ');
        name = name.replace('\\', ' ');
        name = name.replace('*', ' ');
        name = name.replace('?', ' ');
        name = name.replace('/', ' ');
        name = name.replace('[', ' ');
        name = name.replace(']', ' ');
        name = name.trim();
        while (name.startsWith("'"))
            name = name.substring(1);
        while (name.endsWith("'"))
            name = name.substring(0, name.length() - 1);
        name = name.trim();
        if (StringUtils.isNullOrEmpty(name))
            name = "Sheet";

        // Shorten to 31-character limit
        if (name.length() > 31)
            name = name.substring(0, 32);

        // Make unique
        String uniqueName = StringUtils.makeUniqueString(name, " ", existing);
        while (uniqueName.length() > 31) {
            name = name.substring(0, name.length() - 1);
            uniqueName = StringUtils.makeUniqueString(name, " ", existing);
        }
        return uniqueName;
    }

    /**
     * Adds missing columns from the other table
     *
     * @param other the other table
     */
    public void copyColumnSchemaFrom(ResultsTableData other) {
        for (int col = 0; col < other.getColumnCount(); col++) {
            String name = other.getColumnName(col);
            boolean stringColumn = other.isStringColumn(col);
            if (!getColumnNames().contains(name)) {
                addColumn(name, stringColumn);
            }
        }
    }

    private void importDataColumns(Map<String, TableColumnData> columns) {
        this.table = new ResultsTable();

        // Collect map column name -> index
        Map<String, Integer> columnIndexMap = new HashMap<>();
        for (String column : columns.keySet()) {
            columnIndexMap.put(column, table.getFreeColumn(column));
        }

        // Collect the number of rows
        int rows = 0;
        for (TableColumnData column : columns.values()) {
            rows = Math.max(rows, column.getRows());
        }

        // Create table
        for (int row = 0; row < rows; row++) {
            table.incrementCounter();
            for (Map.Entry<String, TableColumnData> entry : columns.entrySet()) {
                int col = columnIndexMap.get(entry.getKey());
                if (entry.getValue().isNumeric()) {
                    table.setValue(col, row, entry.getValue().getRowAsDouble(row));
                } else {
                    table.setValue(col, row, entry.getValue().getRowAsString(row));
                }
            }
        }
    }

    /**
     * Converts this table into its equivalent Python form (as dictionary of columns)
     *
     * @return a dictionary of column name to list of row data
     */
    public PyDictionary toPython() {
        PyDictionary tableDict = new PyDictionary();
        for (int col = 0; col < getColumnCount(); col++) {
            String colName = getColumnName(col);
            List<Object> copy = new ArrayList<>();

            if (isNumericColumn(col)) {
                for (int row = 0; row < getRowCount(); row++) {
                    copy.add(getValueAsDouble(row, col));
                }
            } else {
                for (int row = 0; row < getRowCount(); row++) {
                    copy.add(getValueAsString(row, col));
                }
            }

            tableDict.put(colName, copy);
        }
        return tableDict;
    }

    @Override
    public JIPipeThumbnailData createThumbnail(int width, int height, JIPipeProgressInfo progressInfo) {
        String text = String.format("<html><strong>%d rows<br/>%d columns</strong><br/>%s</html>", getRowCount(), getColumnCount(), String.join(", ", getColumnNames()));
        JLabel label = new JLabel(text);
        label.setSize(label.getPreferredSize());
        return new JIPipeTextThumbnailData(text);
    }

    /**
     * Saves the table as Excel file
     *
     * @param path the path
     */
    public void saveAsXLSX(Path path) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Data");
            saveToXLSXSheet(sheet);
            workbook.write(Files.newOutputStream(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Saves the table data to an Excel sheet
     *
     * @param sheet the sheet
     */
    public void saveToXLSXSheet(Sheet sheet) {
        {
            Row xlsxRow = sheet.createRow(0);
            for (int col = 0; col < getColumnCount(); col++) {
                Cell xlsxCell = xlsxRow.createCell(col, CellType.STRING);
                xlsxCell.setCellValue(getColumnName(col));
            }
        }
        for (int row = 0; row < getRowCount(); row++) {
            Row xlsxRow = sheet.createRow(row + 1);
            for (int col = 0; col < getColumnCount(); col++) {
                Cell xlsxCell = xlsxRow.createCell(col, isNumericColumn(col) ? CellType.NUMERIC : CellType.STRING);
                if (isNumericColumn(col))
                    xlsxCell.setCellValue(getValueAsDouble(row, col));
                else
                    xlsxCell.setCellValue(getValueAsString(row, col));
            }
        }
        for (int i = 0; i < getColumnCount(); i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Saves the table as CSV
     *
     * @param path the output file
     */
    public void saveAsCSV(Path path) {
        try {
            table.saveAs(path.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Applies an operation to the selected cells
     *
     * @param selectedCells the selected cells
     * @param operation     the operation
     */
    public void applyOperation(List<Index> selectedCells, ConvertingColumnOperation operation) {
        Map<Integer, List<Index>> byColumn = selectedCells.stream().collect(Collectors.groupingBy(Index::getColumn));
        for (Map.Entry<Integer, List<Index>> entry : byColumn.entrySet()) {
            int col = entry.getKey();
            TableColumnData buffer;
            boolean numeric = isNumericColumn(col);
            if (numeric) {
                double[] data = new double[entry.getValue().size()];
                for (int i = 0; i < entry.getValue().size(); i++) {
                    int row = entry.getValue().get(i).row;
                    data[i] = getValueAsDouble(row, col);
                }
                buffer = new DoubleArrayTableColumnData(data, "buffer");
            } else {
                String[] data = new String[entry.getValue().size()];
                for (int i = 0; i < entry.getValue().size(); i++) {
                    int row = entry.getValue().get(i).row;
                    data[i] = getValueAsString(row, col);
                }
                buffer = new StringArrayTableColumnData(data, "buffer");
            }

            TableColumnData result = operation.apply(buffer);
            for (int i = 0; i < entry.getValue().size(); i++) {
                int row = entry.getValue().get(i).row;
                Object value = numeric ? result.getRowAsDouble(i) : result.getRowAsString(i);
                setValueAt(value, row, col);
            }
        }
    }

    /**
     * Returns a column that contains all values of the selected columns
     *
     * @param columns   the columns to merge
     * @param separator the separator character
     * @param equals    the equals character
     * @return a column that contains all values of the selected columns
     */
    public TableColumnData getMergedColumn(Set<String> columns, String separator, String equals) {
        String[] values = new String[getRowCount()];
        List<String> sortedColumns = columns.stream().sorted().collect(Collectors.toList());
        for (int row = 0; row < getRowCount(); row++) {
            int finalRow = row;
            values[row] = sortedColumns.stream().map(col -> col + equals + getValueAsString(finalRow, col)).collect(Collectors.joining(separator));
        }
        return new StringArrayTableColumnData(values, String.join(separator, columns));
    }

    /**
     * Returns the row indices grouped by the values of the provided columns
     *
     * @param columns the columns to group by
     * @return a map from column name to column value to the rows that share the same values
     */
    public Map<Map<String, Object>, List<Integer>> getEquivalentRows(Set<String> columns) {
        Map<Map<String, Object>, List<Integer>> result = new HashMap<>();
        Set<Integer> columnIndices = columns.stream().map(this::getColumnIndex).collect(Collectors.toSet());
        for (int row = 0; row < getRowCount(); row++) {
            Map<String, Object> columnValues = new HashMap<>();
            for (Integer columnIndex : columnIndices) {
                String columnName = getColumnName(columnIndex);
                columnValues.put(columnName, getValueAt(row, columnIndex));
            }
            List<Integer> rowList = result.getOrDefault(columnValues, null);
            if (rowList == null) {
                rowList = new ArrayList<>();
                result.put(columnValues, rowList);
            }
            rowList.add(row);
        }
        return result;
    }

    /**
     * Generates a new table that contains statistics.
     * Optionally, statistics can be created for each category (based on the category column)
     *
     * @param operations the operations
     * @param categories columns considered as categorical. They are added to the output without changes.
     * @return integrated table
     */
    public ResultsTableData getStatistics(List<IntegratingColumnOperationEntry> operations, Set<String> categories) {
        ResultsTableData result = new ResultsTableData();

        if (categories == null || categories.isEmpty()) {
            result.addRow();
            for (IntegratingColumnOperationEntry operation : operations) {
                TableColumnData inputColumn = getColumnReference(getColumnIndex(operation.getSourceColumnName()));
                TableColumnData outputColumn = operation.getOperation().apply(inputColumn);
                if (outputColumn.isNumeric()) {
                    result.setValueAt(outputColumn.getRowAsDouble(0), 0, result.addColumn(operation.getTargetColumnName(), false));
                } else {
                    result.setValueAt(outputColumn.getRowAsString(0), 0, result.addColumn(operation.getTargetColumnName(), true));
                }
            }
        } else {
            Map<String, Integer> targetColumnIndices = new HashMap<>();
            // Create category columns first
            for (String category : categories) {
                targetColumnIndices.put(category, result.addColumn(category, isNumericColumn(getColumnIndex(category))));
            }
            Map<Map<String, Object>, List<Integer>> equivalentRows = getEquivalentRows(categories);
            int row = 0;
            for (Map.Entry<Map<String, Object>, List<Integer>> entry : equivalentRows.entrySet()) {
                result.addRow();
                // Write category columns
                for (Map.Entry<String, Object> categoryEntry : entry.getKey().entrySet()) {
                    result.setValueAt(categoryEntry.getValue(), row, result.getColumnIndex(categoryEntry.getKey()));
                }

                // Apply statistics
                for (IntegratingColumnOperationEntry operation : operations) {
                    TableColumnData inputColumn = TableColumnData.getSlice(getColumnReference(getColumnIndex(operation.getSourceColumnName())), entry.getValue());
                    TableColumnData outputColumn = operation.getOperation().apply(inputColumn);
                    int col = targetColumnIndices.getOrDefault(operation.getTargetColumnName(), -1);
                    if (col == -1) {
                        col = result.addColumn(operation.getTargetColumnName(), !outputColumn.isNumeric());
                    }
                    if (result.isNumericColumn(col)) {
                        result.setValueAt(outputColumn.getRowAsDouble(0), row, col);
                    } else {
                        result.setValueAt(outputColumn.getRowAsString(0), row, col);
                    }
                }

                ++row;
            }
        }
        return result;
    }

    /**
     * Adds multiple rows
     *
     * @param rows the number of rows to add
     */
    public void addRows(int rows) {
        for (int row = 0; row < rows; row++) {
            table.incrementCounter();
        }
    }

    /**
     * ImageJ tables break many assumptions about tables, as they lazy-delete their columns without ensuring consecutive IDs
     * This breaks too many algorithms, so re-create the column
     */
    private void cleanupTable() {
        if (table.columnDeleted() || table.getHeadings().length != getColumnNames().size()) {
            ResultsTable original = table;
            table = new ResultsTable(original.getCounter());

            for (String column : original.getHeadings()) {
                Variable[] columnAsVariables = original.getColumnAsVariables(column);
                table.setColumn(column, columnAsVariables);
            }
        }
    }

    /**
     * Returns a column as vector.
     * This is a reference.
     *
     * @param columnName the column name
     * @return the column
     */
    public TableColumnData getColumnReference(String columnName) {
        return new TableColumnDataReference(this, getColumnIndex(columnName));
    }

    /**
     * Returns a column as vector.
     * This is a reference.
     *
     * @param index the column index
     * @return the column
     */
    public TableColumnData getColumnReference(int index) {
        return new TableColumnDataReference(this, index);
    }

    /**
     * Returns a column as vector.
     * This is a copy.
     *
     * @param index the column index
     * @return the column
     */
    public TableColumnData getColumnCopy(int index) {
        if (isNumericColumn(index)) {
            return new DoubleArrayTableColumnData(getColumnReference(index).getDataAsDouble(getRowCount()), getColumnName(index));
        } else {
            return new StringArrayTableColumnData(getColumnReference(index).getDataAsString(getRowCount()), getColumnName(index));
        }
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        try {
            Path path = PathUtils.ensureExtension(storage.getFileSystemPath().resolve(name), ".csv");
            if (Files.isRegularFile(path)) {
                Files.delete(path);
            }
            table.saveAs(path.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a new column that consists of a combination of the selected values.
     * They will be formatted like col1=col1row1,col2=col2row1, ...
     *
     * @param sourceColumns the source columns
     * @param newColumn     the new column
     * @param separator     separator character e.g. ", "
     * @param equals        equals character e.g. "="
     */
    public void mergeColumns(List<Integer> sourceColumns, String newColumn, String separator, String equals) {
        String[] value = new String[getRowCount()];
        for (int row = 0; row < getRowCount(); row++) {
            int finalRow = row;
            value[row] = sourceColumns.stream().map(col -> getColumnName(col) + equals + getValueAsString(finalRow, col)).collect(Collectors.joining(separator));
        }
        int col = addColumn(newColumn, true);
        for (int row = 0; row < getRowCount(); row++) {
            setValueAt(value[row], row, col);
        }
    }

    /**
     * Duplicates a column
     *
     * @param column    the input column
     * @param newColumn the new column name
     */
    public void duplicateColumn(int column, String newColumn) {
        if (containsColumn(newColumn))
            throw new IllegalArgumentException("Column '" + newColumn + "' already exists!");
        int newColumnIndex = addColumn(newColumn, !isNumericColumn(column));
        for (int row = 0; row < getRowCount(); row++) {
            setValueAt(getValueAt(row, column), row, newColumnIndex);
        }
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new ResultsTableData((ResultsTable) table.clone());
    }

    public ResultsTable getTable() {
        return table;
    }

    public void setTable(ResultsTable table) {
        this.table = table;
    }

    /**
     * Returns the index of an existing column
     *
     * @param id the column ID
     * @return the index. -1 if the column does not exist
     */
    public int getColumnIndex(String id) {
        for (int i = 0; i <= table.getLastColumn(); ++i) {
            if (id.equals(table.getColumnHeading(i)))
                return i;
        }
        return -1;
    }

    /**
     * Returns the index of an existing column or creates a new column if it does not exist
     *
     * @param id           the column ID
     * @param stringColumn if a new column is a string column
     * @return the column index
     */
    public int getOrCreateColumnIndex(String id, boolean stringColumn) {
        int existing = getColumnIndex(id);
        if (existing == -1) {
            existing = table.getFreeColumn(id);
            if (stringColumn) {
                for (int row = 0; row < getRowCount(); row++) {
                    table.setValue(existing, row, "");
                }
            }
        }
        return existing;
    }

    /**
     * Adds the table to an existing table
     *
     * @param destination Target table
     */
    public void addToTable(ResultsTable destination) {
        Set<String> existingColumns = new HashSet<>(Arrays.asList(destination.getHeadings()));
        TIntIntMap columnMap = new TIntIntHashMap();
        for (int col = 0; col < getColumnCount(); col++) {
            if (!existingColumns.contains(getColumnName(col))) {
                columnMap.put(col, destination.getFreeColumn(getColumnName(col)));
            } else {
                columnMap.put(col, destination.getColumnIndex(getColumnName(col)));
            }
        }
        int startRow = destination.getCounter();
        for (int row = 0; row < table.size(); ++row) {
            destination.incrementCounter();
            for (int col = 0; col < getColumnCount(); col++) {
                if (isNumericColumn(col)) {
                    destination.setValue(columnMap.get(col), row + startRow, getValueAsDouble(row, col));
                } else {
                    destination.setValue(columnMap.get(col), row + startRow, getValueAsString(row, col));
                }
            }
        }
    }

    @Override
    public int getRowCount() {
        return table.getCounter();
    }

    @Override
    public int getColumnCount() {
        return table.getLastColumn() + 1;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return table.getColumnHeading(columnIndex);
    }

    public List<String> getColumnNames() {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < getColumnCount(); i++) {
            result.add(getColumnName(i));
        }
        return result;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        Hashtable<Integer, ArrayList<Object>> stringColumnsTable = getStringColumnsTable();
        if (stringColumnsTable != null && stringColumnsTable.containsKey(columnIndex)) {
            return String.class;
        } else {
            return Double.class;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    /**
     * @return The table's internal string column table
     */
    private Hashtable<Integer, ArrayList<Object>> getStringColumnsTable() {
        try {
            Field stringColumns = ResultsTable.class.getDeclaredField("stringColumns");
            stringColumns.setAccessible(true);
            return (Hashtable<Integer, ArrayList<Object>>) stringColumns.get(table);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns true if a column is numeric
     *
     * @param columnIndex the column
     * @return if the column is numeric (type {@link Double}). Otherwise it is a {@link String}
     */
    public boolean isNumericColumn(int columnIndex) {
        return getColumnClass(columnIndex) == Double.class;
    }

    public boolean isNumericColumn(String columnName) {
        return isNumericColumn(getColumnIndex(columnName));
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex >= getColumnCount() || rowIndex >= getRowCount())
            return null;
        if (isNumericColumn(columnIndex)) {
            return table.getValueAsDouble(columnIndex, rowIndex);
        } else {
            return table.getStringValue(columnIndex, rowIndex);
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (aValue instanceof Number) {
            table.setValue(columnIndex, rowIndex, ((Number) aValue).doubleValue());
        } else {
            table.setValue(columnIndex, rowIndex, "" + aValue);
        }
    }

    public void setValueAt(Object aValue, int rowIndex, String column) {
        if (aValue instanceof Number) {
            int columnIndex = getOrCreateColumnIndex(column, false);
            table.setValue(columnIndex, rowIndex, ((Number) aValue).doubleValue());
        } else {
            int columnIndex = getOrCreateColumnIndex(column, true);
            table.setValue(columnIndex, rowIndex, "" + aValue);
        }
    }

    public void setLastValue(Object aValue, String column) {
        int rowIndex = getRowCount() - 1;
        if (aValue instanceof Number) {
            int columnIndex = getOrCreateColumnIndex(column, false);
            table.setValue(columnIndex, rowIndex, ((Number) aValue).doubleValue());
        } else {
            int columnIndex = getOrCreateColumnIndex(column, true);
            table.setValue(columnIndex, rowIndex, "" + aValue);
        }
    }

    /**
     * Returns a table value as double. Attempts conversion via a Double parser if the value is a string
     *
     * @param rowIndex    the row
     * @param columnIndex the column
     * @return the table value. NaN if it is not a double or convertible into one
     */
    public double getValueAsDouble(int rowIndex, int columnIndex) {
        double value = table.getValueAsDouble(columnIndex, rowIndex);
        if (Double.isNaN(value)) {
            String string = getValueAsString(rowIndex, columnIndex);
            if (NumberUtils.isCreatable(string)) {
                return NumberUtils.createDouble(string);
            }
        }
        return value;
    }

    /**
     * Returns a table value as string
     *
     * @param rowIndex    the row
     * @param columnIndex the column
     * @return the table value
     */
    public String getValueAsString(int rowIndex, int columnIndex) {
        return table.getStringValue(columnIndex, rowIndex);
    }

    /**
     * Returns a table value as double
     *
     * @param rowIndex   the row
     * @param columnName the column
     * @return the table value
     */
    public double getValueAsDouble(int rowIndex, String columnName) {
        return table.getValue(columnName, rowIndex);
    }

    /**
     * Returns a table value as string
     *
     * @param rowIndex   the row
     * @param columnName the column
     * @return the table value
     */
    public String getValueAsString(int rowIndex, String columnName) {
        return table.getStringValue(columnName, rowIndex);
    }

    /**
     * Renames a column
     *
     * @param column  the column
     * @param newName the new name
     */
    public void renameColumn(String column, String newName) {
        if (getColumnIndex(column) == -1)
            throw new NullPointerException("Column '" + column + "' does not exist!");
        if (column.equals(newName))
            return;
        if (getColumnIndex(newName) != -1)
            throw new NullPointerException("Column '" + newName + "' already exists!");

        table.renameColumn(column, newName);
        cleanupTable();
    }

    /**
     * Fires a {@link TableModelEvent} to all listeners
     *
     * @param event the event
     */
    public void fireChangedEvent(TableModelEvent event) {
        for (TableModelListener listener : listeners) {
            listener.tableChanged(event);
        }
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }

    /**
     * Removes the column with the specified index
     *
     * @param col column index
     */
    public void removeColumnAt(int col) {
        table.deleteColumn(getColumnName(col));
        cleanupTable();
    }

    /**
     * Merges another results table into this one
     *
     * @param other the other data
     */
    public void addRows(ResultsTableData other) {
        Map<String, Boolean> inputColumnsNumeric = new HashMap<>();
        for (int col = 0; col < other.getColumnCount(); col++) {
            inputColumnsNumeric.put(other.getColumnName(col), other.isNumericColumn(col));
        }

        // For some reason ImageJ can create tables with missing columns
        Set<String> allowedColumns = new HashSet<>(Arrays.asList(other.getTable().getHeadings()));

        int localRow = getRowCount();
        for (int row = 0; row < other.getRowCount(); row++) {
            addRow();
            for (int col = 0; col < other.getColumnCount(); col++) {
                String colName = other.getColumnName(col);
                if (!allowedColumns.contains(colName))
                    continue;
                if (inputColumnsNumeric.get(colName)) {
                    table.setValue(colName, localRow, other.getTable().getValueAsDouble(col, row));
                } else {
                    table.setValue(colName, localRow, other.getTable().getStringValue(col, row));
                }
            }

            ++localRow;
        }
    }

    /**
     * Adds columns from another table
     *
     * @param others     the other tables
     * @param makeUnique if true, the columns will be made unique. Otherwise, existing columns will be skipped.
     */
    public void addColumns(Collection<ResultsTableData> others, boolean makeUnique, TableColumnNormalization normalization) {
        List<TableColumnData> columnList = new ArrayList<>();
        int nRow = getRowCount();
        for (ResultsTableData tableData : others) {
            nRow = Math.max(nRow, tableData.getRowCount());
            for (int col = 0; col < tableData.getColumnCount(); col++) {
                TableColumnData column = tableData.getColumnReference(col);
                columnList.add(column);
            }
        }
        columnList = normalization.normalize(columnList, nRow);
        Set<String> existing = new HashSet<>();
        for (TableColumnData column : columnList) {
            if (containsColumn(column.getLabel()) && !makeUnique)
                continue;
            String name = StringUtils.makeUniqueString(column.getLabel(), ".", existing);
            existing.add(name);
            addColumn(name, column, true);
        }
    }

    /**
     * Splits this table into multiple tables according to an internal or external column
     *
     * @param externalColumn the column
     * @return output table map. The map key contains the group.
     */
    public Map<String, ResultsTableData> splitBy(TableColumnData externalColumn) {
        String[] groupColumn = externalColumn.getDataAsString(getRowCount());

        Map<String, ResultsTableData> result = new HashMap<>();
        for (int row = 0; row < getRowCount(); row++) {
            String group = groupColumn[row];
            if (group == null)
                group = "";
            ResultsTableData target = result.getOrDefault(group, null);
            if (target == null) {
                target = new ResultsTableData();
                result.put(group, target);
            }
            target.addRow();
            for (int col = 0; col < getColumnCount(); col++) {
                if (isNumericColumn(col)) {
                    target.getTable().setValue(getColumnName(col), row, table.getValueAsDouble(col, row));
                } else {
                    target.getTable().setValue(getColumnName(col), row, table.getStringValue(col, row));
                }
            }
        }

        return result;
    }

    /**
     * Adds a string column with the given name.
     * If the column already exists, the method returns the existing index
     *
     * @param name column name. cannot be empty.
     * @return the column index (this includes any existing column) or -1 if the creation was not possible
     */
    public int addStringColumn(String name) {
        return addColumn(name, true);
    }

    /**
     * Adds a string column with the given name.
     * If the column already exists, the method returns the existing index
     *
     * @param name column name. cannot be empty.
     * @return the column index (this includes any existing column) or -1 if the creation was not possible
     */
    public int addNumericColumn(String name) {
        return addColumn(name, false);
    }

    /**
     * Adds a column with the given name.
     * If the column already exists, the method returns the existing index
     *
     * @param name         column name. cannot be empty.
     * @param stringColumn if the new column is a string column
     * @return the column index (this includes any existing column) or -1 if the creation was not possible
     */
    public int addColumn(String name, boolean stringColumn) {
        if (StringUtils.isNullOrEmpty(name))
            return -1;
        if (getColumnIndex(name) != -1)
            return getColumnIndex(name);
        int index = table.getFreeColumn(name);
        if (stringColumn) {
            for (int row = 0; row < getRowCount(); row++) {
                table.setValue(index, row, "");
            }
        }
        return index;
    }

    /**
     * Adds a column with the given name.
     * If the column already exists, the method returns the existing index
     *
     * @param name       column name. cannot be empty.
     * @param data       the data
     * @param extendRows if true, add rows if needed to contain all non-generated information in the column
     * @return the column index (this includes any existing column) or -1 if the creation was not possible
     */
    public int addColumn(String name, TableColumnData data, boolean extendRows) {
        int col = addColumn(name, !data.isNumeric());
        if (extendRows && data.getRows() > getRowCount()) {
            addRows(data.getRows() - getRowCount());
        }
//        System.out.println(name + ": " + col + " / " + getColumnCount());
        for (int row = 0; row < getRowCount(); row++) {
            if (data.isNumeric()) {
                setValueAt(data.getRowAsDouble(row), row, col);
            } else {
                setValueAt(data.getRowAsString(row), row, col);
            }
        }
        return col;
    }

    /**
     * Sets a column to a value.
     * Creates the column if necessary
     *
     * @param name  the column name
     * @param value the value. Can be numeric or string
     * @return the column index
     */
    public int setColumnToValue(String name, Object value) {
        int col = addColumn(name, !(value instanceof Number));
        for (int row = 0; row < getRowCount(); row++) {
            setValueAt(value, row, col);
        }
        return col;
    }

    /**
     * Converts the column into a string column. Silently ignores columns that are already string columns.
     *
     * @param column column index
     */
    public void convertToStringColumn(int column) {
        if (!isNumericColumn(column))
            return;
        String[] values = new String[getRowCount()];
        for (int row = 0; row < getRowCount(); row++) {
            values[row] = getValueAsString(row, column);
        }
        String columnName = getColumnName(column);
        table.deleteColumn(columnName);
        column = table.getFreeColumn(columnName);
        for (int row = 0; row < getRowCount(); row++) {
            table.setValue(column, row, values[row]);
        }
        cleanupTable();
    }

    /**
     * Converts the column into a numeric column. Silently ignores columns that are already numeric columns.
     * Attempts to convert string values into their numeric representation. If this fails, defaults to zero.
     *
     * @param column column index
     */
    public void convertToNumericColumn(int column) {
        if (isNumericColumn(column))
            return;
        double[] values = new double[getRowCount()];
        for (int row = 0; row < getRowCount(); row++) {
            String s = getValueAsString(row, column);
            try {
                values[row] = Double.parseDouble(s);
            } catch (NumberFormatException e) {
            }
        }
        String columnName = getColumnName(column);
        table.deleteColumn(columnName);
        column = table.getFreeColumn(columnName);
        for (int row = 0; row < getRowCount(); row++) {
            table.setValue(column, row, values[row]);
        }
        cleanupTable();
    }

    /**
     * Returns true if the column exists
     *
     * @param columnName the column name
     * @return if the column exists
     */
    public boolean containsColumn(String columnName) {
        return getColumnIndex(columnName) != -1;
    }

    /**
     * Adds a new row and writes the provided values into the table
     *
     * @param values map of column name and value. Automatically creates columns if needed
     * @return inserted row index
     */
    public int addRow(Map<String, Object> values) {
        int row = addRow();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            int columnIndex = getOrCreateColumnIndex(entry.getKey(), !(entry.getValue() instanceof Number));
            setValueAt(entry.getValue(), row, columnIndex);
        }
        return row;
    }

    /**
     * Adds a new row and returns a {@link RowBuilder} for setting values conveniently
     *
     * @return the row builder
     * @deprecated Use addAndModifyRow() instead
     */
    @Deprecated
    public RowBuilder addRowBuilder() {
        return new RowBuilder(this, addRow());
    }

    /**
     * Adds a new row and returns a {@link RowBuilder} for setting values conveniently
     *
     * @return the row builder
     */
    public RowBuilder addAndModifyRow() {
        return new RowBuilder(this, addRow());
    }

    /**
     * Adds a new row
     *
     * @return the newly created row id
     */
    public int addRow() {
        table.incrementCounter();
        int row = getRowCount() - 1;
        for (int col = 0; col < getColumnCount(); col++) {
            if (isNumericColumn(col))
                setValueAt(0.0, row, col);
            else
                setValueAt("", row, col);
        }
        return getRowCount() - 1;
    }

    /**
     * Removes the columns with given headings
     *
     * @param removedColumns the columns to remove
     */
    public void removeColumns(Collection<String> removedColumns) {
        for (String removedColumn : removedColumns) {
            if (containsColumn(removedColumn))
                table.deleteColumn(removedColumn);
        }
        cleanupTable();
    }

    @Override
    public String toString() {
        return "Table (" + getRowCount() + " rows, " + getColumnCount() + " columns): " + String.join(", ", getColumnNames());
    }

    /**
     * Sets a column from another table column.
     * If the types do not match, the old column is deleted
     *
     * @param columnName the column name
     * @param column     the column data
     */
    public void setColumn(String columnName, TableColumnData column) {
        int columnIndex = getColumnIndex(columnName);
        if (columnIndex != -1) {
            if (column.isNumeric() != isNumericColumn(columnIndex)) {
                removeColumnAt(columnIndex);
                columnIndex = -1;
            }
        }
        if (columnIndex == -1) {
            columnIndex = addColumn(columnName, !column.isNumeric());
        }
        if (column.isNumeric()) {
            for (int row = 0; row < getRowCount(); row++) {
                setValueAt(column.getRowAsDouble(row), row, columnIndex);
            }
        } else {
            for (int row = 0; row < getRowCount(); row++) {
                setValueAt(column.getRowAsString(row), row, columnIndex);
            }
        }
    }

    /**
     * Sets a column from another table column.
     * The column type is forced by the third parameter
     *
     * @param columnName the column name
     * @param column     the column data
     * @param numeric    if the table column should be numeric
     */
    public void setColumn(String columnName, TableColumnData column, boolean numeric) {
        int columnIndex = getColumnIndex(columnName);
        if (columnIndex != -1) {
            if (numeric != isNumericColumn(columnIndex)) {
                removeColumnAt(columnIndex);
                columnIndex = -1;
            }
        }
        if (columnIndex == -1) {
            columnIndex = addColumn(columnName, !column.isNumeric());
        }
        if (numeric) {
            for (int row = 0; row < getRowCount(); row++) {
                setValueAt(column.getRowAsDouble(row), row, columnIndex);
            }
        } else {
            for (int row = 0; row < getRowCount(); row++) {
                setValueAt(column.getRowAsString(row), row, columnIndex);
            }
        }
    }

    public void removeRow(int removedRow) {
        // This function is buggy, so we instead replace the backend table
//        table.deleteRow(row);
        int targetRow = 0;
        ResultsTable newData = new ResultsTable(getRowCount() - 1);
        for (int col = 0; col < getColumnCount(); col++) {
            newData.getFreeColumn(getColumnName(col));
        }
        for (int row = 0; row < getRowCount(); row++) {
            if (row == removedRow)
                continue;
            for (int col = 0; col < getColumnCount(); col++) {
                if (isNumericColumn(col))
                    newData.setValue(col, targetRow, getValueAsDouble(row, col));
                else
                    newData.setValue(col, targetRow, getValueAsString(row, col));
            }
            ++targetRow;
        }
        this.table = newData;
    }

    public void removeRows(Collection<Integer> rows) {
        TIntSet removedRows = new TIntHashSet(rows);
        int targetRow = 0;
        ResultsTable newData = new ResultsTable(getRowCount() - removedRows.size());
        for (int col = 0; col < getColumnCount(); col++) {
            newData.getFreeColumn(getColumnName(col));
        }
        for (int row = 0; row < getRowCount(); row++) {
            if (removedRows.contains(row))
                continue;
            for (int col = 0; col < getColumnCount(); col++) {
                if (isNumericColumn(col))
                    newData.setValue(col, targetRow, getValueAsDouble(row, col));
                else
                    newData.setValue(col, targetRow, getValueAsString(row, col));
            }
            ++targetRow;
        }
        this.table = newData;
    }

    public ResultsTableData getRows(Collection<Integer> rows) {
        ResultsTableData result = new ResultsTableData();
        for (int col = 0; col < getColumnCount(); col++) {
            result.addColumn(getColumnName(col), !isNumericColumn(col));
        }
        int targetRow = 0;
        for (Integer row : rows) {
            result.addRow();
            for (int col = 0; col < getColumnCount(); col++) {
                result.setValueAt(getValueAt(row, col), targetRow, col);
            }
            ++targetRow;
        }
        return result;
    }

    /**
     * Extracts rows
     *
     * @param start the first row index (inclusive)
     * @param end   the last row index (exclusive)
     * @return table with rows within [start, end)
     */
    public ResultsTableData getRows(int start, int end) {
        ResultsTableData result = new ResultsTableData();
        for (int col = 0; col < getColumnCount(); col++) {
            result.addColumn(getColumnName(col), !isNumericColumn(col));
        }
        for (int row = start; row < end; row++) {
            int targetRow = result.addRow();
            for (int col = 0; col < getColumnCount(); col++) {
                result.setValueAt(getValueAt(row, col), targetRow, col);
            }
        }
        return result;
    }

    public ResultsTableData getRow(int row) {
        return getRows(Collections.singleton(row));
    }

    public boolean isStringColumn(String columnName) {
        return !isNumericColumn(getColumnIndex(columnName));
    }

    public boolean isStringColumn(int col) {
        return !isNumericColumn(col);
    }

    /**
     * Removes a column with given name
     *
     * @param columnName the column name
     * @return if a column was removed
     */
    public boolean removeColumn(String columnName) {
        int columnIndex = getColumnIndex(columnName);
        if (columnIndex >= 0) {
            removeColumnAt(columnIndex);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Helper class for adding a row into the table
     */
    public static class RowBuilder {
        private final ResultsTableData tableData;
        private final int row;

        public RowBuilder(ResultsTableData tableData, int row) {
            this.tableData = tableData;
            this.row = row;
        }

        public RowBuilder set(String columnName, Object value) {
            int columnIndex = tableData.getOrCreateColumnIndex(columnName, !(value instanceof Number));
            return set(columnIndex, value);
        }

        public RowBuilder set(int columnIndex, Object value) {
            tableData.setValueAt(value, row, columnIndex);
            return this;
        }

        /**
         * Does nothing
         */
        public void build() {

        }

        public ResultsTableData getTableData() {
            return tableData;
        }

        public int getRow() {
            return row;
        }
    }

    /**
     * An entry for obtaining statistics/integrated values
     */
    public static class IntegratingColumnOperationEntry {
        private String sourceColumnName;
        private String targetColumnName;
        private SummarizingColumnOperation operation;

        /**
         * Creates a new entry
         *
         * @param sourceColumnName the source column
         * @param targetColumnName the target column
         * @param operation        the operation
         */
        public IntegratingColumnOperationEntry(String sourceColumnName, String targetColumnName, SummarizingColumnOperation operation) {
            this.sourceColumnName = sourceColumnName;
            this.targetColumnName = targetColumnName;
            this.operation = operation;
        }

        public String getSourceColumnName() {
            return sourceColumnName;
        }

        public String getTargetColumnName() {
            return targetColumnName;
        }

        public SummarizingColumnOperation getOperation() {
            return operation;
        }
    }

    /**
     * Points to a cell in the table
     */
    public static class Index {
        public int row;
        public int column;

        /**
         * Creates a new instance. Initialized to zero.
         */
        public Index() {
        }

        /**
         * Creates a new instance and initializes it
         *
         * @param row    the row
         * @param column the column
         */
        public Index(int row, int column) {
            this.row = row;
            this.column = column;
        }

        public int getRow() {
            return row;
        }

        public int getColumn() {
            return column;
        }
    }

    public static class Serializer extends JsonSerializer<ResultsTableData> {
        @Override
        public void serialize(ResultsTableData resultsTableData, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            List<SerializedColumn> columns = new ArrayList<>();
            List<Map<String, Object>> rows = new ArrayList<>();
            for (int col = 0; col < resultsTableData.getColumnCount(); col++) {
                SerializedColumn column = new SerializedColumn();
                column.setName(resultsTableData.getColumnName(col));
                column.setNumeric(resultsTableData.isNumericColumn(col));
                columns.add(column);
            }
            for (int row = 0; row < resultsTableData.getRowCount(); row++) {
                Map<String, Object> rowData = new HashMap<>();
                for (int col = 0; col < resultsTableData.getColumnCount(); col++) {
                    rowData.put(resultsTableData.getColumnName(col), resultsTableData.getValueAt(row, col));
                }
                rows.add(rowData);
            }
            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectField("columns", columns);
            jsonGenerator.writeObjectField("rows", rows);
            jsonGenerator.writeEndObject();
        }
    }

    public static class Deserializer extends JsonDeserializer<ResultsTableData> {
        @Override
        public ResultsTableData deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.readValueAsTree();
            ResultsTableData resultsTableData = new ResultsTableData();

            {
                TypeReference<List<SerializedColumn>> typeReference = new TypeReference<List<SerializedColumn>>() {
                };
                List<SerializedColumn> columns = JsonUtils.getObjectMapper().readerFor(typeReference).readValue(node.get("columns"));
                for (SerializedColumn column : columns) {
                    resultsTableData.addColumn(column.getName(), !column.isNumeric());
                }
            }
            {
                TypeReference<List<Map<String, Object>>> typeReference = new TypeReference<List<Map<String, Object>>>() {
                };
                List<Map<String, Object>> rows = JsonUtils.getObjectMapper().readerFor(typeReference).readValue(node.get("rows"));
                int rowIndex = 0;
                for (Map<String, Object> rowData : rows) {
                    resultsTableData.addRow();
                    for (int col = 0; col < resultsTableData.getColumnCount(); col++) {
                        Object value = rowData.get(resultsTableData.getColumnName(col));
                        resultsTableData.setValueAt(value, rowIndex, col);
                    }
                    ++rowIndex;
                }
            }

            return resultsTableData;
        }
    }

    private static class SerializedColumn {
        private String name;
        private boolean numeric;

        @JsonGetter("name")
        public String getName() {
            return name;
        }

        @JsonSetter("name")
        public void setName(String name) {
            this.name = name;
        }

        @JsonGetter("is-numeric")
        public boolean isNumeric() {
            return numeric;
        }

        @JsonSetter("is-numeric")
        public void setNumeric(boolean numeric) {
            this.numeric = numeric;
        }
    }
}
