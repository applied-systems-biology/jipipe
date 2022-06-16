package org.hkijena.jipipe.api.data;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.registries.JIPipeDatatypeRegistry;
import org.hkijena.jipipe.extensions.tables.datatypes.AnnotationTableData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.cache.JIPipeExtendedDataTableInfoUI;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.*;

@JIPipeDocumentation(name = "Data table", description = "A table of data")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Stores a data table in the standard JIPipe format (data-table.json plus numeric slot folders)",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-data-table.schema.json")
public class JIPipeDataTable implements JIPipeData, TableModel {
    private final EventBus eventBus = new EventBus();
    private final List<TableModelListener> listeners = new ArrayList<>();
    // The main data table
    private final ArrayList<JIPipeVirtualData> data = new ArrayList<>();
    // String annotations
    private final List<String> annotationColumns = new ArrayList<>();
    private final Map<String, ArrayList<JIPipeTextAnnotation>> annotations = new HashMap<>();
    // Data annotations
    private final List<String> dataAnnotationColumns = new ArrayList<>();
    private final Map<String, ArrayList<JIPipeVirtualData>> dataAnnotations = new HashMap<>();
    private Class<? extends JIPipeData> acceptedDataType;
    // Metadata
    private boolean newDataVirtual = false;

    public JIPipeDataTable(Class<? extends JIPipeData> acceptedDataType) {
        this.acceptedDataType = acceptedDataType;
    }

    public JIPipeDataTable(JIPipeDataTable other, boolean shallow, JIPipeProgressInfo progressInfo) {

        // Copy properties
        this.acceptedDataType = other.getAcceptedDataType();
        this.newDataVirtual = other.isNewDataVirtual();

        for (int row = 0; row < other.data.size(); row++) {
            JIPipeProgressInfo rowProgress = progressInfo.resolveAndLog("Row", row, other.data.size());
            JIPipeVirtualData virtualData = other.getVirtualData(row);
            if (!shallow) {
                // Copy the main data
                virtualData = virtualData.duplicate(rowProgress);
            }
            List<JIPipeDataAnnotation> dataAnnotations = other.getDataAnnotations(row);
            if (!shallow) {
                // Copy data annotations
                List<JIPipeDataAnnotation> dataAnnotationsCopy = new ArrayList<>();
                for (JIPipeDataAnnotation dataAnnotation : dataAnnotations) {
                    dataAnnotationsCopy.add(dataAnnotation.duplicate(rowProgress));
                }
                dataAnnotations = dataAnnotationsCopy;
            }
            addData(virtualData,
                    other.getTextAnnotations(row),
                    JIPipeTextAnnotationMergeMode.OverwriteExisting,
                    dataAnnotations,
                    JIPipeDataAnnotationMergeMode.OverwriteExisting);
        }
    }

    /**
     * Imports this data from the path
     *
     * @param storage the storage
     * @return the data
     */
    public static JIPipeDataTable importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        Path storagePath = storage.getFileSystemPath();
        JIPipeDataTableMetadata dataTable = JIPipeDataTableMetadata.loadFromJson(storagePath.resolve("data-table.json"));
        Class<? extends JIPipeData> acceptedDataType = JIPipe.getDataTypes().getById(dataTable.getAcceptedDataTypeId());
        JIPipeDataTable slot = new JIPipeDataTable(acceptedDataType);
        for (int i = 0; i < dataTable.getRowCount(); i++) {
            JIPipeProgressInfo rowProgress = progressInfo.resolveAndLog("Row", i, dataTable.getRowCount());
            JIPipeDataTableMetadataRow row = dataTable.getRowList().get(i);
            Path rowStorage = storagePath.resolve("" + row.getIndex());
            Class<? extends JIPipeData> rowDataType = JIPipe.getDataTypes().getById(row.getTrueDataType());
            JIPipeData data = JIPipe.importData(new JIPipeFileSystemReadDataStorage(progressInfo, rowStorage), rowDataType, rowProgress);
            slot.addData(data, row.getTextAnnotations(), JIPipeTextAnnotationMergeMode.OverwriteExisting, rowProgress);

            for (JIPipeExportedDataAnnotation dataAnnotation : row.getDataAnnotations()) {
                Path dataAnnotationRowStorage = storagePath.resolve(dataAnnotation.getRowStorageFolder());
                Class<? extends JIPipeData> dataAnnotationDataType = JIPipe.getDataTypes().getById(dataAnnotation.getTrueDataType());
                JIPipeData dataAnnotationData = JIPipe.importData(new JIPipeFileSystemReadDataStorage(progressInfo, dataAnnotationRowStorage), dataAnnotationDataType, progressInfo);
                slot.setDataAnnotation(i, dataAnnotation.getName(), dataAnnotationData);
            }
        }
        return slot;
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return annotationColumns.size() + dataAnnotationColumns.size() + 1;
    }

    @Override
    public String getColumnName(int columnIndex) {
        if (columnIndex == 0) {
            return "Data";
        } else if (columnIndex < dataAnnotationColumns.size() + 1) {
            return dataAnnotationColumns.get(columnIndex - 1);
        } else {
            return annotationColumns.get(columnIndex - 1 - dataAnnotationColumns.size());
        }
    }

    /**
     * The column class. Columns are ordered as following: data column, data annotation columns, text annotation columns.
     * Adapt your indices accordingly.
     * Returns {@link JIPipeVirtualData} for the first column.
     * For data annotation columns, returns {@link JIPipeDataAnnotation}.
     * For text annotation columns, returns {@link JIPipeTextAnnotation}.
     *
     * @param columnIndex the column. 0 is the data column. (0, #data columns + 1) are data columns. (#data columns, #data columns + #text columns + 1) are text columns.
     * @return the column class (see description)
     */
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0) {
            return JIPipeVirtualData.class;
        } else if (columnIndex < dataAnnotationColumns.size() + 1) {
            return JIPipeDataAnnotation.class;
        } else {
            return JIPipeTextAnnotation.class;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    /**
     * Returns the data. Columns are ordered as following: data column, data annotation columns, text annotation columns.
     * Adapt your indices accordingly.
     * For column index 0, returns the {@link JIPipeVirtualData}
     * For data annotation columns, returns the {@link JIPipeDataAnnotation}
     * For text annotation columns, returns the {@link JIPipeTextAnnotation}
     *
     * @param rowIndex    the row
     * @param columnIndex the column. 0 is the data column. (0, #data columns + 1) are data columns. (#data columns, #data columns + #text columns + 1) are text columns.
     * @return the value (see description)
     */
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            return getVirtualData(rowIndex);
        } else if (columnIndex < dataAnnotationColumns.size()) {
            return getVirtualDataAnnotation(rowIndex, dataAnnotationColumns.get(columnIndex - 1));
        } else {
            return getTextAnnotation(rowIndex, annotationColumns.get(columnIndex - dataAnnotationColumns.size() - 1));
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        // Not supported
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

    /**
     * Removes all rows from this table
     */
    public void clearData() {
        for (JIPipeVirtualData item : data) {
            item.removeUser(this);
            if(item.canClose()) {
                try {
                    item.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        for (Map.Entry<String, ArrayList<JIPipeVirtualData>> entry : dataAnnotations.entrySet()) {
            for (JIPipeVirtualData item : entry.getValue()) {
                item.removeUser(this);
                if(item.canClose()) {
                    try {
                        item.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        data.clear();
        annotationColumns.clear();
        annotations.clear();
        dataAnnotations.clear();
        dataAnnotationColumns.clear();
//        System.gc();
    }

    /**
     * Removes all rows from this table
     */
    public void clear() {
        clearData();
    }

    /**
     * @return The event bus
     */
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * @return Immutable list of all string annotation columns
     */
    public List<String> getAnnotationColumns() {
        return Collections.unmodifiableList(annotationColumns);
    }

    /**
     * @return Immutable list of all data annotation columns
     */
    public List<String> getDataAnnotationColumns() {
        return Collections.unmodifiableList(dataAnnotationColumns);
    }

    /**
     * @return the slot's data type
     */
    public Class<? extends JIPipeData> getAcceptedDataType() {
        return acceptedDataType;
    }

    /**
     * Sets the accepted slot type
     * Please note that this method can cause issues when running the graph
     *
     * @param slotDataType the new data type
     */
    public void setAcceptedDataType(Class<? extends JIPipeData> slotDataType) {
        acceptedDataType = slotDataType;
    }

    /**
     * Returns true if the slot can carry the provided data.
     * This will also look up if the data can be converted
     *
     * @param data Data
     * @return True if the slot accepts the data
     */
    public boolean accepts(JIPipeData data) {
        if (data == null)
            throw new NullPointerException("Data slots cannot accept null data!");
        return JIPipe.getDataTypes().isConvertible(data.getClass(), getAcceptedDataType());
    }

    /**
     * Returns true if the slot can carry the provided data.
     * This will also look up if the data can be converted
     *
     * @param klass Data class
     * @return True if the slot accepts the data
     */
    public boolean accepts(Class<? extends JIPipeData> klass) {
        if (data == null)
            throw new NullPointerException("Data slots cannot accept null data!");
        return JIPipe.getDataTypes().isConvertible(klass, getAcceptedDataType());
    }

    /**
     * Returns true if the slot can carry the provided data.
     * This will only consider trivial conversions
     *
     * @param data Data
     * @return True if the slot accepts the data
     */
    public boolean acceptsTrivially(JIPipeData data) {
        if (data == null)
            throw new NullPointerException("Data slots cannot accept null data!");
        return JIPipeDatatypeRegistry.isTriviallyConvertible(data.getClass(), getAcceptedDataType());
    }

    /**
     * Returns true if the slot can carry the provided data.
     * This will also look up if the data can be converted
     *
     * @param klass Data class
     * @return True if the slot accepts the data
     */
    public boolean acceptsTrivially(Class<? extends JIPipeData> klass) {
        if (data == null)
            throw new NullPointerException("Data slots cannot accept null data!");
        return JIPipeDatatypeRegistry.isTriviallyConvertible(klass, getAcceptedDataType());
    }

    /**
     * Gets the virtual data container at given row
     *
     * @param row the row
     * @return the virtual data container
     */
    public JIPipeVirtualData getVirtualData(int row) {
        return data.get(row);
    }

    /**
     * Gets the data stored in a specific row.
     * Please note that this will allocate virtual data
     *
     * @param <T>          Data type
     * @param row          The row
     * @param dataClass    the class to return
     * @param progressInfo progress for data loading
     * @return Data at row
     */
    public <T extends JIPipeData> T getData(int row, Class<T> dataClass, JIPipeProgressInfo progressInfo) {
        return (T) JIPipe.getDataTypes().convert(data.get(row).getData(progressInfo), dataClass);
    }

    /**
     * Gets all the data stored in a specific row.
     * Please note that this will allocate virtual data
     *
     * @param <T>          Data type
     * @param dataClass    the class to return
     * @param progressInfo progress for data loading
     * @return Data at row
     */
    public <T extends JIPipeData> List<T> getAllData(Class<T> dataClass, JIPipeProgressInfo progressInfo) {
        List<T> result = new ArrayList<>();
        for (int row = 0; row < getRowCount(); row++) {
            result.add((T) JIPipe.getDataTypes().convert(data.get(row).getData(progressInfo), dataClass));
        }
        return result;
    }

    /**
     * Gets a data annotation as {@link JIPipeVirtualData}
     *
     * @param row    the row
     * @param column the data annotation column
     * @return the data or null if there is no annotation
     */
    public JIPipeVirtualData getVirtualDataAnnotation(int row, String column) {
        List<JIPipeVirtualData> data = getOrCreateDataAnnotationColumnData(column);
        return data.get(row);
    }

    /**
     * Sets a virtual data annotation
     *
     * @param row         the row
     * @param column      the data annotation column
     * @param virtualData the data. can be null.
     */
    public void setVirtualDataAnnotation(int row, String column, JIPipeVirtualData virtualData) {
        List<JIPipeVirtualData> data = getOrCreateDataAnnotationColumnData(column);
        if(virtualData != null)
            virtualData.addUser(this);
        JIPipeVirtualData existing = data.get(row);
        if(existing != null) {
            existing.removeUser(this);
            if(existing.canClose()) {
                try {
                    existing.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        data.set(row, virtualData);
    }

    /**
     * Sets the data of a specific row
     * @param row the row
     * @param data the data
     */
    public void setData(int row, JIPipeData data) {
        if (!accepts(data))
            throw new IllegalArgumentException("Tried to add data of type " + data.getClass() + ", but slot only accepts " + acceptedDataType + ". A converter could not be found.");
        JIPipeVirtualData existing = this.data.get(row);
        if(existing != null) {
            existing.removeUser(this);
            if(existing.canClose()) {
                try {
                    existing.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        JIPipeVirtualData virtualData = new JIPipeVirtualData(JIPipe.getDataTypes().convert(data, getAcceptedDataType()));
        virtualData.addUser(this);
        this.data.set(row, virtualData);
    }

    /**
     * Sets the data of a specific row
     * @param row the row
     * @param virtualData the data
     */
    public void setVirtualData(int row, JIPipeVirtualData virtualData) {
        if (!accepts(virtualData.getDataClass()))
            throw new IllegalArgumentException("Tried to add data of type " + virtualData.getDataClass() + ", but slot only accepts " + acceptedDataType + ". A converter could not be found.");
        JIPipeVirtualData existing = this.data.get(row);
        if(existing != null) {
            existing.removeUser(this);
            if(existing.canClose()) {
                try {
                    existing.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        this.data.set(row, virtualData);
        virtualData.addUser(this);
    }

    /**
     * Sets a data annotation
     *
     * @param row    the row
     * @param column the column
     * @param data   the data. Can be null
     */
    public void setDataAnnotation(int row, String column, JIPipeData data) {
        if (data == null)
            setVirtualDataAnnotation(row, column, null);
        else
            setVirtualDataAnnotation(row, column, new JIPipeVirtualData(data));
    }

    /**
     * Returns annotations of a row as map
     *
     * @param row the row
     * @return map from annotation name to annotation instance. Non-existing annotations are not present.
     */
    public Map<String, JIPipeTextAnnotation> getAnnotationMap(int row) {
        Map<String, JIPipeTextAnnotation> result = new HashMap<>();
        for (JIPipeTextAnnotation annotation : getTextAnnotations(row)) {
            result.put(annotation.getName(), annotation);
        }
        return result;
    }

    /**
     * Returns a map of all data annotations as {@link JIPipeVirtualData}
     *
     * @param row the row
     * @return map from data annotation column to instance. Non-existing annotations are not present.
     */
    public Map<String, JIPipeVirtualData> getVirtualDataAnnotationMap(int row) {
        Map<String, JIPipeVirtualData> result = new HashMap<>();
        for (String column : getDataAnnotationColumns()) {
            JIPipeVirtualData virtualData = getVirtualDataAnnotation(row, column);
            if (virtualData != null)
                result.put(column, virtualData);
        }
        return result;
    }

    /**
     * Gets the list of all data annotations in the specified row
     *
     * @param row the row
     * @return list of data annotations
     */
    public List<JIPipeDataAnnotation> getDataAnnotations(int row) {
        List<JIPipeDataAnnotation> dataAnnotations = new ArrayList<>();
        for (String dataAnnotationColumn : getDataAnnotationColumns()) {
            JIPipeVirtualData virtualDataAnnotation = getVirtualDataAnnotation(row, dataAnnotationColumn);
            if (virtualDataAnnotation != null) {
                dataAnnotations.add(new JIPipeDataAnnotation(dataAnnotationColumn, virtualDataAnnotation));
            }
        }
        return dataAnnotations;
    }

    /**
     * Gets the list of all data annotations in the specified row
     *
     * @param rows the rows
     * @return list of data annotations
     */
    public List<JIPipeDataAnnotation> getDataAnnotations(Collection<Integer> rows) {
        List<JIPipeDataAnnotation> dataAnnotations = new ArrayList<>();
        for (Integer row : rows) {
            dataAnnotations.addAll(getDataAnnotations(row));
        }
        return dataAnnotations;
    }

    /**
     * Gets a data annotation
     *
     * @param row    the row
     * @param column the data annotation column
     * @return the data or null if there is no annotation
     */
    public JIPipeDataAnnotation getDataAnnotation(int row, String column) {
        JIPipeVirtualData virtualData = getVirtualDataAnnotation(row, column);
        if (virtualData == null)
            return null;
        else
            return new JIPipeDataAnnotation(column, virtualData);
    }

    /**
     * Gets the list of annotations for a specific data row
     *
     * @param row The row
     * @return Annotations at row
     */
    public synchronized List<JIPipeTextAnnotation> getTextAnnotations(int row) {
        List<JIPipeTextAnnotation> result = new ArrayList<>();
        for (String info : annotationColumns) {
            JIPipeTextAnnotation annotation = getOrCreateTextAnnotationColumnData(info).get(row);
            if (annotation != null)
                result.add(annotation);
        }
        return result;
    }

    /**
     * Gets the list of annotations for specific data rows
     *
     * @param rows The set of rows
     * @return Annotations at row
     */
    public synchronized List<JIPipeTextAnnotation> getTextAnnotations(Collection<Integer> rows) {
        List<JIPipeTextAnnotation> result = new ArrayList<>();
        for (String info : annotationColumns) {
            for (int row : rows) {
                JIPipeTextAnnotation annotation = getOrCreateTextAnnotationColumnData(info).get(row);
                if (annotation != null)
                    result.add(annotation);
            }
        }
        return result;
    }

    /**
     * Returns the annotation of specified type or the alternative value.
     *
     * @param row    data row
     * @param name   annotation name
     * @param orElse alternative value
     * @return annotation of type 'type' or 'orElse'
     */
    public JIPipeTextAnnotation getTextAnnotationOr(int row, String name, JIPipeTextAnnotation orElse) {
        return getTextAnnotations(row).stream().filter(a -> a != null && Objects.equals(a.getName(), name)).findFirst().orElse(orElse);
    }

    /**
     * Gets the annotation column or creates it
     * Ensures that the output size is equal to getRowCount()
     *
     * @param columnName Annotation type
     * @return All annotation instances of the provided type. Size is getRowCount()
     */
    private synchronized List<JIPipeTextAnnotation> getOrCreateTextAnnotationColumnData(String columnName) {
        ArrayList<JIPipeTextAnnotation> arrayList = annotations.getOrDefault(columnName, null);
        if (arrayList == null) {
            annotationColumns.add(columnName);
            arrayList = new ArrayList<>();
            annotations.put(columnName, arrayList);
        }
        while (arrayList.size() < getRowCount()) {
            arrayList.add(null);
        }
        return arrayList;
    }

    /**
     * Gets the annotation data column or creates it
     * Ensures that the output size is equal to getRowCount()
     *
     * @param columnName Annotation type
     * @return All annotation instances of the provided type. Size is getRowCount()
     */
    private synchronized List<JIPipeVirtualData> getOrCreateDataAnnotationColumnData(String columnName) {
        ArrayList<JIPipeVirtualData> arrayList = dataAnnotations.getOrDefault(columnName, null);
        if (arrayList == null) {
            dataAnnotationColumns.add(columnName);
            arrayList = new ArrayList<>();
            dataAnnotations.put(columnName, arrayList);
        }
        while (arrayList.size() < getRowCount()) {
            arrayList.add(null);
        }
        return arrayList;
    }

    /**
     * Adds a data row
     *
     * @param value        The data
     * @param annotations  Optional annotations
     * @param progressInfo progress for data storage
     */
    public synchronized void addData(JIPipeData value, List<JIPipeTextAnnotation> annotations, JIPipeTextAnnotationMergeMode mergeStrategy, JIPipeProgressInfo progressInfo) {
        if (!accepts(value))
            throw new IllegalArgumentException("Tried to add data of type " + value.getClass() + ", but slot only accepts " + acceptedDataType + ". A converter could not be found.");
        if (!annotations.isEmpty()) {
            annotations = mergeStrategy.merge(annotations);
        }
        JIPipeVirtualData virtualData = new JIPipeVirtualData(JIPipe.getDataTypes().convert(value, getAcceptedDataType()));
        virtualData.addUser(this);
        data.add(virtualData);
        for (JIPipeTextAnnotation annotation : annotations) {
            List<JIPipeTextAnnotation> annotationArray = getOrCreateTextAnnotationColumnData(annotation.getName());
            annotationArray.set(getRowCount() - 1, annotation);
        }
        if (isNewDataVirtual())
            virtualData.makeVirtual(progressInfo, false);
        fireChangedEvent(new TableModelEvent(this));
    }

    /**
     * Adds an annotation to all existing data
     *
     * @param annotation The annotation instance
     * @param overwrite  If false, existing annotations of the same type are not overwritten
     */
    public synchronized void addAnnotationToAllData(JIPipeTextAnnotation annotation, boolean overwrite) {
        List<JIPipeTextAnnotation> annotationArray = getOrCreateTextAnnotationColumnData(annotation.getName());
        for (int i = 0; i < getRowCount(); ++i) {
            if (!overwrite && annotationArray.get(i) != null)
                continue;
            annotationArray.set(i, annotation);
        }
        fireChangedEvent(new TableModelEvent(this));
    }

    /**
     * Removes an annotation column from the data
     *
     * @param column Annotation type
     */
    public synchronized void removeAllAnnotationsFromData(String column) {
        int columnIndex = annotationColumns.indexOf(column);
        if (columnIndex != -1) {
            annotationColumns.remove(columnIndex);
            annotations.remove(column);
        }
    }

    /**
     * Adds a data row
     *
     * @param value        Data
     * @param progressInfo progress for data storage
     */
    public synchronized void addData(JIPipeData value, JIPipeProgressInfo progressInfo) {
        addData(value, Collections.emptyList(), JIPipeTextAnnotationMergeMode.Merge, progressInfo);
    }

    /**
     * Merges the data from the source table into the current one
     * @param table the source table
     * @param progressInfo the progress
     */
    public synchronized void addFromTable(JIPipeDataTable table, JIPipeProgressInfo progressInfo) {
        for (int row = 0; row < table.getRowCount(); row++) {
            progressInfo.resolveAndLog("Add data from table", row, table.getRowCount());
            addData(table.getVirtualData(row), table.getTextAnnotations(row), JIPipeTextAnnotationMergeMode.OverwriteExisting, table.getDataAnnotations(row), JIPipeDataAnnotationMergeMode.OverwriteExisting);
        }
    }

    /**
     * Finds the row that matches the given annotations
     *
     * @param annotations A valid annotation list with size equals to getRowCount()
     * @return row index greater or equal to 0 if found, otherwise -1
     */
    public int findRowWithAnnotations(List<JIPipeTextAnnotation> annotations) {
        String[] infoMap = new String[annotations.size()];
        for (int i = 0; i < annotations.size(); ++i) {
            int infoIndex = annotationColumns.indexOf(annotations.get(i).getName());
            if (infoIndex == -1)
                return -1;
            infoMap[i] = annotationColumns.get(infoIndex);
        }
        for (int row = 0; row < data.size(); ++row) {
            boolean equal = true;
            for (int i = 0; i < annotations.size(); ++i) {
                String info = infoMap[i];
                JIPipeTextAnnotation rowAnnotation = this.annotations.get(info).get(row);
                if (!JIPipeTextAnnotation.nameEquals(annotations.get(i), rowAnnotation)) {
                    equal = false;
                }
            }
            if (equal)
                return row;
        }
        return -1;
    }

    public String getDisplayName() {
        return toString();
    }

    /**
     * Saves the data contained in this slot into the storage path.
     *
     * @param storage      storage that contains the data
     * @param saveProgress save progress
     */
    public void exportData(JIPipeWriteDataStorage storage, JIPipeProgressInfo saveProgress) {
        JIPipeDataTableMetadata dataTableMetadata = new JIPipeDataTableMetadata();
        dataTableMetadata.setAcceptedDataTypeId(JIPipe.getDataTypes().getIdOf(getAcceptedDataType()));

        // We need to create unique and filesystem-safe mappings for data annotation column names
        Map<String, String> dataAnnotationColumnNameMapping = new HashMap<>();
        for (String dataAnnotationColumn : getDataAnnotationColumns()) {
            String mappedName = StringUtils.makeUniqueString(StringUtils.makeFilesystemCompatible(dataAnnotationColumn),
                    "_",
                    dataAnnotationColumnNameMapping.values());
            dataAnnotationColumnNameMapping.put(dataAnnotationColumn, mappedName);
        }

        // Save data
        for (int row = 0; row < getRowCount(); ++row) {
            JIPipeDataTableMetadataRow rowMetadata = new JIPipeDataTableMetadataRow();
            rowMetadata.setIndex(row);
            rowMetadata.setTrueDataType(JIPipe.getDataTypes().getIdOf(getVirtualData(row).getDataClass()));
            rowMetadata.setTextAnnotations(getTextAnnotations(row));
            JIPipeProgressInfo rowProgress = saveProgress.resolveAndLog("Row", row, getRowCount());
            saveDataRow(storage, row, rowProgress);
            for (JIPipeDataAnnotation dataAnnotation : getDataAnnotations(row)) {
                JIPipeProgressInfo dataAnnotationProgress = rowProgress.resolveAndLog("Data annotation '" + dataAnnotation.getName() + "'");
                JIPipeWriteDataStorage dataAnnotationStore = saveDataAnnotationRow(storage, dataAnnotationProgress, row, rowProgress, dataAnnotation, dataAnnotationColumnNameMapping);
                JIPipeExportedDataAnnotation dataAnnotationMetadata = new JIPipeExportedDataAnnotation(dataAnnotation.getName(),
                        dataAnnotationStore.getInternalPath(),
                        JIPipe.getDataTypes().getIdOf(dataAnnotation.getDataClass()),
                        rowMetadata);
                rowMetadata.getDataAnnotations().add(dataAnnotationMetadata);
            }
            dataTableMetadata.add(rowMetadata);
        }

        try {
            dataTableMetadata.saveAsJson(storage.getFileSystemPath().resolve("data-table.json"));
            dataTableMetadata.saveAsCSV(storage.getFileSystemPath().resolve("data-table.csv"));
        } catch (IOException e) {
            throw new UserFriendlyRuntimeException(e, "Unable to save data table!",
                    "Data slot '" + getDisplayName() + "'", "JIPipe tried to write files into '" + storage + "'.",
                    "Check if you have permissions to write into the path, and if there is enough disk space.");
        }
    }

    private JIPipeWriteDataStorage saveDataAnnotationRow(JIPipeWriteDataStorage storage, JIPipeProgressInfo saveProgress, int row, JIPipeProgressInfo rowProgress, JIPipeDataAnnotation dataAnnotation, Map<String, String> dataAnnotationColumnNameMapping) {
        JIPipeWriteDataStorage dataAnnotationsStore = storage.resolve("data-annotations").resolve("" + row).resolve(dataAnnotationColumnNameMapping.get(dataAnnotation.getName()));
        dataAnnotation.getData(JIPipeData.class, saveProgress.resolve("Load virtual data")).exportData(dataAnnotationsStore,
                dataAnnotationColumnNameMapping.get(dataAnnotation.getName()),
                false,
                rowProgress);
        return dataAnnotationsStore;
    }

    private void saveDataRow(JIPipeWriteDataStorage storage, int row, JIPipeProgressInfo rowProgress) {
        JIPipeWriteDataStorage rowStorage = storage.resolve("" + row);
        data.get(row).getData(rowProgress.resolve("Load virtual data")).exportData(rowStorage, "data", false, rowProgress);
    }

    /**
     * Copies the source slot into this slot.
     * This will only add data and not clear it beforehand.
     * Data is copied without duplication.
     *
     * @param sourceSlot   The other slot
     * @param progressInfo the progress
     */
    public void addData(JIPipeDataSlot sourceSlot, JIPipeProgressInfo progressInfo) {
        String text = "Copying data from " + sourceSlot.getDisplayName() + " to " + getDisplayName();
        for (int row = 0; row < sourceSlot.getRowCount(); ++row) {
            progressInfo.resolveAndLog(text, row, sourceSlot.getRowCount());
            addData(sourceSlot.getVirtualData(row), sourceSlot.getTextAnnotations(row), JIPipeTextAnnotationMergeMode.Merge);

            // Copy data annotations
            for (Map.Entry<String, JIPipeVirtualData> entry : sourceSlot.getVirtualDataAnnotationMap(row).entrySet()) {
                setVirtualDataAnnotation(row, entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Adds data as virtual data reference
     *
     * @param virtualData   the virtual data
     * @param annotations   the annotations
     * @param mergeStrategy merge strategy
     */
    public void addData(JIPipeVirtualData virtualData, List<JIPipeTextAnnotation> annotations, JIPipeTextAnnotationMergeMode mergeStrategy) {
        if (!accepts(virtualData.getDataClass()))
            throw new IllegalArgumentException("Tried to add data of type " + virtualData.getDataClass() + ", but slot only accepts "
                    + acceptedDataType + ". A converter could not be found.");
        if (!annotations.isEmpty()) {
            annotations = mergeStrategy.merge(annotations);
        }
        data.add(virtualData);
        for (JIPipeTextAnnotation annotation : annotations) {
            List<JIPipeTextAnnotation> annotationArray = getOrCreateTextAnnotationColumnData(annotation.getName());
            annotationArray.set(getRowCount() - 1, annotation);
        }
    }

    /**
     * Returns whether a row is virtual (unloaded) or non-virtual (present in memory)
     *
     * @param row the row
     * @return if the data at given row is virtual
     */
    public boolean rowIsVirtual(int row) {
        return data.get(row) == null;
    }

    /**
     * Loads all virtual data into memory
     *
     * @param progressInfo             the progress
     * @param removeVirtualDataStorage if true, the folder containing the cache is deleted
     */
    public void makeDataNonVirtual(JIPipeProgressInfo progressInfo, boolean removeVirtualDataStorage) {
        JIPipeProgressInfo subProgress = progressInfo.resolve("Loading virtual data to memory");
        for (int row = 0; row < getRowCount(); row++) {
            JIPipeVirtualData virtualData = getVirtualData(row);
            if (virtualData.isVirtual()) {
                virtualData.makeNonVirtual(subProgress.resolveAndLog("Row", row, getRowCount()), removeVirtualDataStorage);
            }
            for (Map.Entry<String, JIPipeVirtualData> entry : getVirtualDataAnnotationMap(row).entrySet()) {
                if (entry.getValue().isVirtual()) {
                    entry.getValue().makeNonVirtual(subProgress.resolveAndLog("Row", row,
                            getRowCount()).resolveAndLog("Data annotation " + entry.getKey()), removeVirtualDataStorage);
                }
            }
        }
    }

    /**
     * Unloads all data into the virtual cache
     *
     * @param progressInfo the progress
     */
    public void makeDataVirtual(JIPipeProgressInfo progressInfo) {
        JIPipeProgressInfo subProgress = progressInfo.resolve("Unloading data to virtual cache");
        for (int row = 0; row < getRowCount(); row++) {
            JIPipeVirtualData virtualData = getVirtualData(row);
            if (!virtualData.isVirtual()) {
                virtualData.makeVirtual(subProgress.resolveAndLog("Row", row, getRowCount()), false);
            }
            for (Map.Entry<String, JIPipeVirtualData> entry : getVirtualDataAnnotationMap(row).entrySet()) {
                if (!entry.getValue().isVirtual()) {
                    entry.getValue().makeVirtual(subProgress.resolveAndLog("Row", row,
                            getRowCount()).resolveAndLog("Data annotation " + entry.getKey()), false);
                }
            }
        }
    }

    /**
     * Gets an annotation at a specific index
     *
     * @param row    the data row
     * @param column the column
     * @return the annotation
     */
    public JIPipeTextAnnotation getTextAnnotation(int row, int column) {
        String annotation = annotationColumns.get(column);
        return annotations.get(annotation).get(row);
    }

    /**
     * Gets an annotation at a specific column
     *
     * @param row    the data row
     * @param column the column
     * @return the annotation
     */
    public JIPipeTextAnnotation getTextAnnotation(int row, String column) {
        return annotations.get(column).get(row);
    }

    /**
     * Converts this data table in-place and sets the accepted data type
     *
     * @param dataClass the target data type
     */
    public void convert(Class<? extends JIPipeData> dataClass, JIPipeProgressInfo progressInfo) {
        for (int row = 0; row < getRowCount(); row++) {
            JIPipeProgressInfo rowProgress = progressInfo.resolveAndLog("Convert", row, getRowCount());
            JIPipeVirtualData virtualData = getVirtualData(row);
            if (!dataClass.isAssignableFrom(virtualData.getDataClass())) {
                JIPipeData converted = JIPipe.getDataTypes().convert(virtualData.getData(rowProgress), dataClass);
                virtualData = new JIPipeVirtualData(converted);
                data.set(row, virtualData);
            }
        }
        this.acceptedDataType = dataClass;
    }

    /**
     * Closes all data and data annotations and replaces their values in the table by null. Text annotations are left unchanged.
     * This method is internally used for exporting data. Please be careful, as {@link JIPipeVirtualData} items that might be present in
     * other tables might be destroyed.
     * Use clearData() as alternative
     */
    public void destroyData() {
        for (int i = 0; i < data.size(); ++i) {
            try {
                data.get(i).close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            data.set(i, null);
        }
        for (ArrayList<JIPipeVirtualData> list : dataAnnotations.values()) {
            for (int i = 0; i < list.size(); i++) {
                try {
                    list.get(i).close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                list.set(i, null);
            }
        }
    }

    public boolean isEmpty() {
        return getRowCount() <= 0;
    }

    /**
     * Creates a new {@link JIPipeDataSlot} instance that contains only the selected rows.
     * All other attributes are copied.
     *
     * @param rows the rows
     * @return the sliced slot
     */
    public JIPipeDataTable slice(Collection<Integer> rows) {
        JIPipeDataTable result = new JIPipeDataTable(getAcceptedDataType());
        for (Integer row : rows) {
            result.addData(getVirtualData(row), getTextAnnotations(row), JIPipeTextAnnotationMergeMode.OverwriteExisting);
            for (Map.Entry<String, JIPipeVirtualData> entry : getVirtualDataAnnotationMap(row).entrySet()) {
                result.setVirtualDataAnnotation(result.getRowCount() - 1, entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * Converts the slot into an annotation table
     *
     * @param withDataAsString if the string representation should be included
     * @return the table
     */
    public AnnotationTableData toAnnotationTable(boolean withDataAsString) {
        AnnotationTableData output = new AnnotationTableData();
        int dataColumn = withDataAsString ? output.addColumn(StringUtils.makeUniqueString("String representation", "_", getAnnotationColumns()), true) : -1;
        int row = 0;
        for (int sourceRow = 0; sourceRow < getRowCount(); ++sourceRow) {
            output.addRow();
            if (dataColumn >= 0)
                output.setValueAt(getVirtualData(row).getStringRepresentation(), row, dataColumn);
            for (JIPipeTextAnnotation annotation : getTextAnnotations(sourceRow)) {
                if (annotation != null) {
                    int col = output.addAnnotationColumn(annotation.getName());
                    output.setValueAt(annotation.getValue(), row, col);
                }
            }
            ++row;
        }
        return output;
    }

    /**
     * Adds data as virtual data reference
     *
     * @param virtualData   the virtual data
     * @param annotations   the annotations
     * @param mergeStrategy merge strategy
     */
    public void addData(JIPipeVirtualData virtualData, List<JIPipeTextAnnotation> annotations, JIPipeTextAnnotationMergeMode mergeStrategy,
                        List<JIPipeDataAnnotation> dataAnnotations, JIPipeDataAnnotationMergeMode dataAnnotationMergeStrategy) {
        if (!accepts(virtualData.getDataClass()))
            throw new IllegalArgumentException("Tried to add data of type " + virtualData.getDataClass() + ", but slot only accepts "
                    + acceptedDataType + ". A converter could not be found.");
        if (!annotations.isEmpty()) {
            annotations = mergeStrategy.merge(annotations);
        }
        data.add(virtualData);
        for (JIPipeTextAnnotation annotation : annotations) {
            List<JIPipeTextAnnotation> annotationArray = getOrCreateTextAnnotationColumnData(annotation.getName());
            annotationArray.set(getRowCount() - 1, annotation);
        }
        for (JIPipeDataAnnotation annotation : dataAnnotationMergeStrategy.merge(dataAnnotations)) {
            setVirtualDataAnnotation(getRowCount() - 1, annotation.getName(), annotation.getVirtualData());
        }
    }

    /**
     * Adds data as virtual data reference
     *
     * @param data          the data
     * @param annotations   the annotations
     * @param mergeStrategy merge strategy
     */
    public void addData(JIPipeData data, List<JIPipeTextAnnotation> annotations, JIPipeTextAnnotationMergeMode mergeStrategy,
                        List<JIPipeDataAnnotation> dataAnnotations, JIPipeDataAnnotationMergeMode dataAnnotationMergeStrategy) {
        if (!accepts(data))
            throw new IllegalArgumentException("Tried to add data of type " + data.getClass() + ", but slot only accepts "
                    + acceptedDataType + ". A converter could not be found.");
        if (!annotations.isEmpty()) {
            annotations = mergeStrategy.merge(annotations);
        }
        this.data.add(new JIPipeVirtualData(data));
        for (JIPipeTextAnnotation annotation : annotations) {
            List<JIPipeTextAnnotation> annotationArray = getOrCreateTextAnnotationColumnData(annotation.getName());
            annotationArray.set(getRowCount() - 1, annotation);
        }
        for (JIPipeDataAnnotation annotation : dataAnnotationMergeStrategy.merge(dataAnnotations)) {
            setVirtualDataAnnotation(getRowCount() - 1, annotation.getName(), annotation.getVirtualData());
        }
    }

    /**
     * Gets the true type of the data at given row
     *
     * @param row the row
     * @return the true data type
     */
    public Class<? extends JIPipeData> getDataClass(int row) {
        return data.get(row).getDataClass();
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        if (forceName) {
            exportData(storage.resolve(name), progressInfo);
        } else {
            exportData(storage, progressInfo);
        }
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new JIPipeDataTable(this, false, new JIPipeProgressInfo());
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        JIPipeExtendedDataTableInfoUI tableUI = new JIPipeExtendedDataTableInfoUI(workbench, this, true);
        JFrame frame = new JFrame(displayName);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
        frame.setContentPane(tableUI);
        frame.pack();
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(workbench.getWindow());
        frame.setVisible(true);
    }

    @Override
    public Component preview(int width, int height) {
        if (isEmpty()) {
            return JIPipeData.super.preview(width, height);
        } else {
            JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
            JPanel panel = new JPanel(new GridBagLayout());
            for (int i = 0; i < Math.min(9, getRowCount()); i++) {
                JIPipeVirtualData virtualData = getVirtualData(i);

                GridBagConstraints constraints = new GridBagConstraints();
                constraints.gridx = i % 3;
                constraints.gridy = i / 3;
                constraints.weightx = 1;
                constraints.weighty = 1;
                constraints.anchor = GridBagConstraints.CENTER;

                if (!virtualData.isVirtual()) {
                    Component preview = virtualData.getData(progressInfo).preview(width / 3, height / 3);
                    if (preview == null)
                        preview = new JLabel("N/A");
                    panel.add(preview, constraints);
                } else {
                    panel.add(new JLabel(JIPipe.getDataTypes().getIconFor(virtualData.getDataClass())), constraints);
                }
            }
            return panel;
        }
    }

    @Override
    public String toString() {
        return getRowCount() + " rows of " + JIPipeData.getNameOf(getAcceptedDataType());
    }

    /**
     * Determines if newly added data should be virtual
     *
     * @return true if newly added data will be virtual
     */
    public boolean isNewDataVirtual() {
        return newDataVirtual;
    }

    /**
     * Determines if newly added data should be virtual
     *
     * @param newDataVirtual if newly added data will be virtual
     */
    public void setNewDataVirtual(boolean newDataVirtual) {
        this.newDataVirtual = newDataVirtual;
    }

    /**
     * Returns info about the location of this data table.
     * Returns null if no info with given key exists.
     *
     * @param key the key
     * @return the location. null if no info is available
     */
    public String getLocation(String key) {
        return getLocation(key, null);
    }

    /**
     * Returns info about the location of this data table.
     * Returns the default value if no info with given key exists.
     *
     * @param key          the key
     * @param defaultValue returned if no info about the key exists
     * @return the location. default value if no info is available
     */
    public String getLocation(String key, String defaultValue) {
        return defaultValue;
    }
}
