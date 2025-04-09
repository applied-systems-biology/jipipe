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

package org.hkijena.jipipe.api.data;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.LabelAsJIPipeHeavyData;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.context.JIPipeDataContext;
import org.hkijena.jipipe.api.data.context.JIPipeMutableDataContext;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataAnnotationInfo;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableInfo;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableRowInfo;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeGridThumbnailData;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeThumbnailData;
import org.hkijena.jipipe.api.data.utils.JIPipeWeakDataReferenceData;
import org.hkijena.jipipe.api.registries.JIPipeDatatypeRegistry;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.plugins.parameters.library.pairs.IntegerAndIntegerPairParameter;
import org.hkijena.jipipe.plugins.settings.JIPipeGeneralDataApplicationSettings;
import org.hkijena.jipipe.plugins.tables.datatypes.AnnotationTableData;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.locks.StampedLock;
import java.util.function.BiPredicate;

@SetJIPipeDocumentation(name = "Data table", description = "A table of data")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Stores a data table in the standard JIPipe format (data-table.json plus numeric slot folders)",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-data-table.schema.json")
@LabelAsJIPipeHeavyData
public class JIPipeDataTable implements JIPipeData, TableModel {
    private final StampedLock stampedLock = new StampedLock();
    private final List<TableModelListener> listeners = new ArrayList<>();
    private final ArrayList<JIPipeDataItemStore> dataArray = new ArrayList<>();
    private final ArrayList<JIPipeDataContext> dataContextsArray = new ArrayList<>();
    private final List<String> textAnnotationColumnNames = new ArrayList<>();
    private final Map<String, ArrayList<JIPipeTextAnnotation>> textAnnotationArrays = new HashMap<>();
    private final List<String> dataAnnotationColumnNames = new ArrayList<>();
    private final Map<String, ArrayList<JIPipeDataItemStore>> dataAnnotationsArrays = new HashMap<>();
    private Class<? extends JIPipeData> acceptedDataType;

    public JIPipeDataTable() {
        this.acceptedDataType = JIPipeData.class;
    }

    public JIPipeDataTable(Class<? extends JIPipeData> acceptedDataType) {
        this.acceptedDataType = acceptedDataType;
    }

    public JIPipeDataTable(JIPipeDataTable other, boolean shallow, JIPipeProgressInfo progressInfo) {

        // Copy properties
        this.acceptedDataType = other.getAcceptedDataType();

        for (int row = 0; row < other.dataArray.size(); row++) {
            JIPipeProgressInfo rowProgress = progressInfo.resolveAndLog("Row", row, other.dataArray.size());
            JIPipeDataItemStore virtualData = other.getDataItemStore(row);
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
                    JIPipeDataAnnotationMergeMode.OverwriteExisting,
                    other.getDataContext(row),
                    rowProgress);
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
        JIPipeDataTableInfo dataTableMetadata = JIPipeDataTableInfo.loadFromJson(storagePath.resolve("data-table.json"));
        Class<? extends JIPipeData> acceptedDataType = JIPipe.getDataTypes().getById(dataTableMetadata.getAcceptedDataTypeId());
        int rowCount = dataTableMetadata.getRowCount();
        JIPipeDataTable dataTable = new JIPipeDataTable(acceptedDataType);
        for (int i = 0; i < rowCount; i++) {
            JIPipeProgressInfo rowProgress = progressInfo.resolveAndLog("Row", i, rowCount);
            JIPipeDataTableRowInfo row = dataTableMetadata.getRowList().get(i);
            Path rowStorage = storagePath.resolve("" + row.getIndex());
            Class<? extends JIPipeData> rowDataType = JIPipe.getDataTypes().getById(row.getTrueDataType());
            JIPipeData data = JIPipe.importData(new JIPipeFileSystemReadDataStorage(progressInfo, rowStorage), rowDataType, rowProgress);
            dataTable.addData(data, row.getTextAnnotations(), JIPipeTextAnnotationMergeMode.OverwriteExisting, row.getDataContext(), rowProgress);

            for (JIPipeDataAnnotationInfo dataAnnotation : row.getDataAnnotations()) {
                Path dataAnnotationRowStorage = storagePath.resolve(dataAnnotation.getRowStorageFolder());
                Class<? extends JIPipeData> dataAnnotationDataType = JIPipe.getDataTypes().getById(dataAnnotation.getTrueDataType());
                JIPipeData dataAnnotationData = JIPipe.importData(new JIPipeFileSystemReadDataStorage(progressInfo, dataAnnotationRowStorage), dataAnnotationDataType, progressInfo);
                dataTable.setDataAnnotation(i, dataAnnotation.getName(), dataAnnotationData);
            }
        }
        return dataTable;
    }

    /**
     * Creates a copy of this data table that contains only {@link JIPipeWeakDataReferenceData} data and data annotations
     * (If any data is already weak, it is left as-is)
     *
     * @return weakly referencing shallow copy of the data as table
     */
    public JIPipeDataTable toWeakCopy() {
        JIPipeDataTable result = new JIPipeDataTable(JIPipeData.class);
        JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
        for (int row = 0; row < dataArray.size(); row++) {
            JIPipeProgressInfo rowProgress = progressInfo.resolveAndLog("Row", row, dataArray.size());
            JIPipeDataItemStore virtualData = getDataItemStore(row);
            JIPipeData data_ = virtualData.getData(rowProgress);
            if (!(data_ instanceof JIPipeWeakDataReferenceData)) {
                // Copy the main data
                virtualData = new JIPipeDataItemStore(new JIPipeWeakDataReferenceData(data_));
            }
            List<JIPipeDataAnnotation> dataAnnotations = new ArrayList<>();
            for (JIPipeDataAnnotation dataAnnotation : getDataAnnotations(row)) {
                JIPipeData dataAnnotation_ = dataAnnotation.getData(JIPipeData.class, rowProgress);
                if (!(dataAnnotation_ instanceof JIPipeWeakDataReferenceData)) {
                    dataAnnotation_ = new JIPipeWeakDataReferenceData(dataAnnotation_);
                }
                dataAnnotations.add(new JIPipeDataAnnotation(dataAnnotation.getName(), dataAnnotation_));
            }
            result.addData(virtualData,
                    getTextAnnotations(row),
                    JIPipeTextAnnotationMergeMode.OverwriteExisting,
                    dataAnnotations,
                    JIPipeDataAnnotationMergeMode.OverwriteExisting,
                    getDataContext(row),
                    rowProgress);
        }
        return result;
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
        long stamp = stampedLock.readLock();
        try {
            return dataArray.size();
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * NOT THREAD-SAFE
     *
     * @return the number of rows
     */
    protected int getRowCount_() {
        return dataArray.size();
    }

    @Override
    public int getColumnCount() {
        long stamp = stampedLock.readLock();
        try {
            return textAnnotationColumnNames.size() + dataAnnotationColumnNames.size() + 1;
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    @Override
    public String getColumnName(int columnIndex) {
        long stamp = stampedLock.readLock();
        try {
            if (columnIndex == 0) {
                return "Data";
            } else if (columnIndex < dataAnnotationColumnNames.size() + 1) {
                return dataAnnotationColumnNames.get(columnIndex - 1);
            } else {
                return textAnnotationColumnNames.get(columnIndex - 1 - dataAnnotationColumnNames.size());
            }
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * The column class. Columns are ordered as following: data column, data annotation columns, text annotation columns.
     * Adapt your indices accordingly.
     * Returns {@link JIPipeDataItemStore} for the first column.
     * For data annotation columns, returns {@link JIPipeDataAnnotation}.
     * For text annotation columns, returns {@link JIPipeTextAnnotation}.
     *
     * @param columnIndex the column. 0 is the data column. (0, #data columns + 1) are data columns. (#data columns, #data columns + #text columns + 1) are text columns.
     * @return the column class (see description)
     */
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        long stamp = stampedLock.readLock();
        try {
            if (columnIndex == 0) {
                return JIPipeDataItemStore.class;
            } else if (columnIndex < dataAnnotationColumnNames.size() + 1) {
                return JIPipeDataAnnotation.class;
            } else {
                return JIPipeTextAnnotation.class;
            }
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    /**
     * Returns the data. Columns are ordered as following: data column, data annotation columns, text annotation columns.
     * Adapt your indices accordingly.
     * For column index 0, returns the {@link JIPipeDataItemStore}
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
            return getDataItemStore(rowIndex);
        } else if (columnIndex < dataAnnotationColumnNames.size()) {
            return getDataAnnotationItemStore(rowIndex, dataAnnotationColumnNames.get(columnIndex - 1));
        } else {
            return getTextAnnotation(rowIndex, textAnnotationColumnNames.get(columnIndex - dataAnnotationColumnNames.size() - 1));
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
     *
     * @param force        if true, close all data stores
     * @param progressInfo the progress info
     */
    public void clearData(boolean force, JIPipeProgressInfo progressInfo) {
        long stamp = stampedLock.writeLock();
        try {
            for (int i = 0; i < dataArray.size(); i++) {
                JIPipeDataItemStore item = dataArray.get(i);
                if (item == null)
                    continue;
                item.removeUser(this);
                if (force || item.canClose()) {
                    try {
                        progressInfo.log("Closing data item store row=" + i + " in " + hashCode() + " (force=" + force + ")");
                        item.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            for (Map.Entry<String, ArrayList<JIPipeDataItemStore>> entry : dataAnnotationsArrays.entrySet()) {
                ArrayList<JIPipeDataItemStore> value = entry.getValue();
                for (int i = 0; i < value.size(); i++) {
                    JIPipeDataItemStore item = value.get(i);
                    if (item == null)
                        continue;
                    item.removeUser(this);
                    if (force || item.canClose()) {
                        try {
                            progressInfo.log("Closing data item store index=" + i + " of data annotation " + entry.getKey() + " in " + hashCode() + " (force=" + force + ")");
                            item.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
            dataArray.clear();
            dataContextsArray.clear();
            textAnnotationColumnNames.clear();
            textAnnotationArrays.clear();
            dataAnnotationsArrays.clear();
            dataAnnotationColumnNames.clear();
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * Removes all rows from this table
     *
     * @param force        if true, force closing the data storage
     * @param progressInfo the progress info
     */
    public void clear(boolean force, JIPipeProgressInfo progressInfo) {
        clearData(force, progressInfo);
    }

    /**
     * @return Immutable list of all string annotation columns
     */
    public List<String> getTextAnnotationColumnNames() {
        long stamp = stampedLock.readLock();
        try {
            return Collections.unmodifiableList(textAnnotationColumnNames);
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * @return Immutable list of all string annotation columns
     * @deprecated use getTextAnnotationColumns()
     */
    @Deprecated
    public List<String> getAnnotationColumns() {
        return getTextAnnotationColumnNames();
    }

    /**
     * @return Immutable list of all data annotation columns
     */
    public List<String> getDataAnnotationColumnNames() {
        long stamp = stampedLock.readLock();
        try {
            return Collections.unmodifiableList(dataAnnotationColumnNames);
        } finally {
            stampedLock.unlock(stamp);
        }
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
     * Return the info about the slot's data type
     *
     * @return the info
     */
    public JIPipeDataInfo getAcceptedDataTypeInfo() {
        return JIPipeDataInfo.getInstance(acceptedDataType);
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
        if (dataArray == null)
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
        if (dataArray == null)
            throw new NullPointerException("Data slots cannot accept null data!");
        return JIPipeDatatypeRegistry.isTriviallyConvertible(klass, getAcceptedDataType());
    }

    /**
     * Gets the virtual data container at given row
     *
     * @param row the row
     * @return the virtual data container
     * @deprecated use getDataItemStore
     */
    @Deprecated
    public JIPipeDataItemStore getVirtualData(int row) {
        long stamp = stampedLock.readLock();
        try {
            return dataArray.get(row);
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * Gets the virtual data container at given row
     *
     * @param row the row
     * @return the virtual data container
     */
    public JIPipeDataItemStore getDataItemStore(int row) {
        long stamp = stampedLock.readLock();
        try {
            return dataArray.get(row);
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * Gets the context of the data in the row
     *
     * @param row the row
     * @return the data context
     */
    public JIPipeDataContext getDataContext(int row) {
        long stamp = stampedLock.readLock();
        try {
            return dataContextsArray.get(row);
        } finally {
            stampedLock.unlock(stamp);
        }
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
        long stamp = stampedLock.readLock();
        try {
            return JIPipe.getDataTypes().convert(dataArray.get(row).getData(progressInfo), dataClass, progressInfo);
        } finally {
            stampedLock.unlock(stamp);
        }
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
        long stamp = stampedLock.readLock();
        try {
            List<T> result = new ArrayList<>();
            for (int row = 0; row < getRowCount_(); row++) {
                result.add(JIPipe.getDataTypes().convert(dataArray.get(row).getData(progressInfo), dataClass, progressInfo));
            }
            return result;
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * Gets a data annotation as {@link JIPipeDataItemStore}
     *
     * @param row    the row
     * @param column the data annotation column
     * @return the data or null if there is no annotation
     * @deprecated use getDataAnnotationItemStore
     */
    @Deprecated
    public JIPipeDataItemStore getVirtualDataAnnotation(int row, String column) {
        return getDataAnnotationItemStore(row, column);
    }

    /**
     * Gets a data annotation as {@link JIPipeDataItemStore}
     *
     * @param row    the row
     * @param column the data annotation column
     * @return the data or null if there is no annotation
     */
    public JIPipeDataItemStore getDataAnnotationItemStore(int row, String column) {
        long stamp = stampedLock.readLock();
        try {
            List<JIPipeDataItemStore> data = getDataAnnotationColumnArray_(column);
            if (data != null && row < data.size()) {
                return data.get(row);
            } else {
                return null;
            }
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * Sets a virtual data annotation
     *
     * @param row         the row
     * @param column      the data annotation column
     * @param virtualData the data. can be null.
     * @deprecated use setDataAnnotationItemStore
     */
    @Deprecated
    public void setVirtualDataAnnotation(int row, String column, JIPipeDataItemStore virtualData) {
        setDataAnnotationItemStore(row, column, virtualData);
    }

    /**
     * Sets a virtual data annotation
     *
     * @param row         the row
     * @param column      the data annotation column
     * @param virtualData the data. can be null.
     */
    public void setDataAnnotationItemStore(int row, String column, JIPipeDataItemStore virtualData) {
        long stamp = stampedLock.writeLock();
        try {
            setDataAnnotationItemStore_(row, column, virtualData);
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * Sets a virtual data annotation
     *
     * @param row         the row
     * @param column      the data annotation column
     * @param virtualData the data. can be null.
     */
    protected void setDataAnnotationItemStore_(int row, String column, JIPipeDataItemStore virtualData) {
        List<JIPipeDataItemStore> data = getOrCreateDataAnnotationColumnArray_(column);
        if (virtualData != null)
            virtualData.addUser(this);
        JIPipeDataItemStore existing = data.get(row);
        if (existing != null) {
            existing.removeUser(this);
            if (existing.canClose()) {
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
     *
     * @param row  the row
     * @param data the data
     */
    public void setData(int row, JIPipeData data) {
        if (!accepts(data))
            throw new IllegalArgumentException("Tried to add data of type " + data.getClass() + ", but slot only accepts " + acceptedDataType + ". A converter could not be found.");
        long stamp = stampedLock.writeLock();
        try {
            JIPipeDataItemStore existing = this.dataArray.get(row);
            if (existing != null) {
                existing.removeUser(this);
                if (existing.canClose()) {
                    try {
                        existing.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            JIPipeDataItemStore virtualData = new JIPipeDataItemStore(JIPipe.getDataTypes().convert(data, getAcceptedDataType(), new JIPipeProgressInfo()));
            virtualData.addUser(this);
            this.dataArray.set(row, virtualData);

        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * Sets the context of data in given row
     *
     * @param row     the row
     * @param context the new context. if null, a new context is created.
     */
    public void setDataContext(int row, JIPipeDataContext context) {
        if (context == null) {
            context = new JIPipeMutableDataContext();
        }
        long stamp = stampedLock.writeLock();
        try {
            this.dataContextsArray.set(row, context);
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * Sets the data of a specific row
     *
     * @param row         the row
     * @param virtualData the data
     * @deprecated use setDataItemStore
     */
    @Deprecated
    public void setVirtualData(int row, JIPipeDataItemStore virtualData) {
        setDataItemStore(row, virtualData);
    }

    /**
     * Sets the data of a specific row
     *
     * @param row         the row
     * @param virtualData the data
     */
    public void setDataItemStore(int row, JIPipeDataItemStore virtualData) {
        if (!accepts(virtualData.getDataClass()))
            throw new IllegalArgumentException("Tried to add data of type " + virtualData.getDataClass() + ", but slot only accepts " + acceptedDataType + ". A converter could not be found.");

        long stamp = stampedLock.writeLock();
        try {
            JIPipeDataItemStore existing = this.dataArray.get(row);
            if (existing != null) {
                existing.removeUser(this);
                if (existing.canClose()) {
                    try {
                        existing.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            this.dataArray.set(row, virtualData);
            virtualData.addUser(this);
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * Sets a text annotation
     *
     * @param row    the row
     * @param column the column
     * @param value  the value
     */
    public void setTextAnnotation(int row, String column, String value) {
        long stamp = stampedLock.writeLock();
        try {
            List<JIPipeTextAnnotation> columnData = getOrCreateTextAnnotationColumnArray_(column);
            columnData.set(row, new JIPipeTextAnnotation(column, value));
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * Sets a text annotation
     *
     * @param row        the row
     * @param annotation the annotation
     */
    public void setTextAnnotation(int row, JIPipeTextAnnotation annotation) {
        long stamp = stampedLock.writeLock();
        try {
            List<JIPipeTextAnnotation> columnData = getOrCreateTextAnnotationColumnArray_(annotation.getName());
            columnData.set(row, annotation);
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * Sets a data annotation
     *
     * @param row        the row
     * @param annotation the annotation
     */
    public void setDataAnnotation(int row, JIPipeDataAnnotation annotation) {
        setDataAnnotationItemStore(row, annotation.getName(), annotation.getDataItemStore());
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
            setDataAnnotationItemStore(row, column, null);
        else
            setDataAnnotationItemStore(row, column, new JIPipeDataItemStore(data));
    }

    /**
     * Returns annotations of a row as map
     *
     * @param row the row
     * @return map from annotation name to annotation instance. Non-existing annotations are not present.
     */
    public Map<String, JIPipeTextAnnotation> getTextAnnotationMap(int row) {
        Map<String, JIPipeTextAnnotation> result = new HashMap<>();
        for (JIPipeTextAnnotation annotation : getTextAnnotations(row)) {
            result.put(annotation.getName(), annotation);
        }
        return result;
    }

    /**
     * Returns annotations of a row as map
     *
     * @param row the row
     * @return map from annotation name to annotation instance. Non-existing annotations are not present.
     * @deprecated Use getTextAnnotationMap instead
     */
    @Deprecated
    public Map<String, JIPipeTextAnnotation> getAnnotationMap(int row) {
        return getTextAnnotationMap(row);
    }

    /**
     * Returns a map of all data annotations as {@link JIPipeDataItemStore}
     *
     * @param row the row
     * @return map from data annotation column to instance. Non-existing annotations are not present.
     * @deprecated use getDataAnnotationItemStoreMap
     */
    @Deprecated
    public Map<String, JIPipeDataItemStore> getVirtualDataAnnotationMap(int row) {
        return getDataAnnotationItemStoreMap_(row);
    }

    /**
     * Returns a map of all data annotations as {@link JIPipeDataItemStore}
     * NOT THREAD-SAFE. REQUIRES READ LOCK
     *
     * @param row the row
     * @return map from data annotation column to instance. Non-existing annotations are not present.
     */
    protected Map<String, JIPipeDataItemStore> getDataAnnotationItemStoreMap_(int row) {
        Map<String, JIPipeDataItemStore> result = new HashMap<>();
        for (String column : getDataAnnotationColumnNames()) {
            JIPipeDataItemStore virtualData = getDataAnnotationItemStore(row, column);
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
        long stamp = stampedLock.readLock();
        try {
            List<JIPipeDataAnnotation> dataAnnotations = new ArrayList<>();
            for (String dataAnnotationColumn : getDataAnnotationColumnNames()) {
                JIPipeDataItemStore virtualDataAnnotation = getDataAnnotationItemStore(row, dataAnnotationColumn);
                if (virtualDataAnnotation != null) {
                    dataAnnotations.add(new JIPipeDataAnnotation(dataAnnotationColumn, virtualDataAnnotation));
                }
            }
            return dataAnnotations;
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * Gets the list of all data annotations
     *
     * @return list of data annotations
     */
    public List<JIPipeDataAnnotation> getAllDataAnnotations() {
        long stamp = stampedLock.readLock();
        try {
            List<JIPipeDataAnnotation> dataAnnotations = new ArrayList<>();
            for (int row = 0; row < getRowCount_(); row++) {
                for (String dataAnnotationColumn : getDataAnnotationColumnNames()) {
                    JIPipeDataItemStore virtualDataAnnotation = getDataAnnotationItemStore(row, dataAnnotationColumn);
                    if (virtualDataAnnotation != null) {
                        dataAnnotations.add(new JIPipeDataAnnotation(dataAnnotationColumn, virtualDataAnnotation));
                    }
                }
            }
            return dataAnnotations;
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * Gets the list of all data annotations in the specified row
     *
     * @param rows the rows
     * @return list of data annotations
     */
    public List<JIPipeDataAnnotation> getDataAnnotations(Collection<Integer> rows) {
        List<JIPipeDataAnnotation> dataAnnotations = new ArrayList<>();
        for (int row : rows) {
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
        JIPipeDataItemStore virtualData = getDataAnnotationItemStore(row, column);
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
    public List<JIPipeTextAnnotation> getTextAnnotations(int row) {
        List<JIPipeTextAnnotation> result = new ArrayList<>();
        long stamp = stampedLock.readLock();
        try {
            for (String info : textAnnotationColumnNames) {
                List<JIPipeTextAnnotation> columnData = getTextAnnotationColumnArray_(info);
                if (columnData != null && row < columnData.size()) {
                    JIPipeTextAnnotation annotation = columnData.get(row);
                    if (annotation != null)
                        result.add(annotation);
                }
            }
            return result;
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * Gets the list of all text annotations
     *
     * @return Annotations at row
     */
    public List<JIPipeTextAnnotation> getAllTextAnnotations() {
        List<JIPipeTextAnnotation> result = new ArrayList<>();
        long stamp = stampedLock.readLock();
        try {
            for (int row = 0; row < getRowCount_(); row++) {
                for (String info : textAnnotationColumnNames) {
                    List<JIPipeTextAnnotation> columnData = getTextAnnotationColumnArray_(info);
                    if (columnData != null && row < columnData.size()) {
                        JIPipeTextAnnotation annotation = columnData.get(row);
                        if (annotation != null)
                            result.add(annotation);
                    }
                }
            }
        } finally {
            stampedLock.unlock(stamp);
        }
        return result;
    }

    /**
     * Gets the list of annotations for specific data rows
     *
     * @param rows The set of rows
     * @return Annotations at row
     */
    public List<JIPipeTextAnnotation> getTextAnnotations(Collection<Integer> rows) {
        List<JIPipeTextAnnotation> result = new ArrayList<>();
        long stamp = stampedLock.readLock();
        try {
            for (String info : textAnnotationColumnNames) {
                List<JIPipeTextAnnotation> columnData = getTextAnnotationColumnArray_(info);
                for (int row : rows) {
                    if (columnData != null && row < columnData.size()) {
                        JIPipeTextAnnotation annotation = columnData.get(row);
                        if (annotation != null)
                            result.add(annotation);
                    }
                }
            }
        } finally {
            stampedLock.unlock(stamp);
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
     * NOT THREAD-SAFE, REQUIRES A WRITE LOCK!
     *
     * @param columnName Annotation type
     * @return All annotation instances of the provided type. Size is getRowCount()
     */
    protected List<JIPipeTextAnnotation> getOrCreateTextAnnotationColumnArray_(String columnName) {
        ArrayList<JIPipeTextAnnotation> arrayList = textAnnotationArrays.getOrDefault(columnName, null);
        int rowCount = getRowCount_();
        if (arrayList == null || arrayList.size() < rowCount) {
            if (arrayList == null) {
                textAnnotationColumnNames.add(columnName);
                arrayList = new ArrayList<>();
                textAnnotationArrays.put(columnName, arrayList);
            }
            while (arrayList.size() < rowCount) {
                arrayList.add(null);
            }
        }
        return arrayList;
    }

    /**
     * Gets the annotation data column or creates it
     * Ensures that the output size is equal to getRowCount()
     * NOT THREAD-SAFE, REQUIRES A WRITE LOCK!
     *
     * @param columnName Annotation type
     * @return All annotation instances of the provided type. Size is getRowCount()
     */
    protected List<JIPipeDataItemStore> getOrCreateDataAnnotationColumnArray_(String columnName) {
        ArrayList<JIPipeDataItemStore> arrayList = dataAnnotationsArrays.getOrDefault(columnName, null);
        int rowCount = getRowCount_();
        if (arrayList == null || arrayList.size() < rowCount) {
            if (arrayList == null) {
                dataAnnotationColumnNames.add(columnName);
                arrayList = new ArrayList<>();
                dataAnnotationsArrays.put(columnName, arrayList);
            }
            while (arrayList.size() < rowCount) {
                arrayList.add(null);
            }
        }
        return arrayList;
    }

    /**
     * Gets the annotation column or creates it
     * DOES NOT ENSURE that the output size is equal to getRowCount()
     * NOT THREAD-SAFE, REQUIRES A WRITE LOCK!
     *
     * @param columnName Annotation type
     * @return All annotation instances of the provided type or null
     */
    protected List<JIPipeTextAnnotation> getTextAnnotationColumnArray_(String columnName) {
        return textAnnotationArrays.getOrDefault(columnName, null);
    }

    /**
     * Gets the annotation data column
     * DOES NOT ENSURE that the output size is equal to getRowCount()
     * NOT THREAD-SAFE, REQUIRES A READ LOCK!
     *
     * @param columnName Annotation type
     * @return All annotation instances of the provided type or null
     */
    protected List<JIPipeDataItemStore> getDataAnnotationColumnArray_(String columnName) {
        return dataAnnotationsArrays.getOrDefault(columnName, null);
    }

    public List<JIPipeDataContext> getDataContexts() {
        return ImmutableList.copyOf(dataContextsArray);
    }

    /**
     * Adds a data row
     *
     * @param value        The data
     * @param annotations  Optional annotations
     * @param progressInfo progress for data storage
     */
    public void addData(JIPipeData value, List<JIPipeTextAnnotation> annotations, JIPipeTextAnnotationMergeMode mergeStrategy, JIPipeDataContext context, JIPipeProgressInfo progressInfo) {
        if (!accepts(value))
            throw new IllegalArgumentException("Tried to add data of type " + value.getClass() + ", but slot only accepts " + acceptedDataType + ". A converter could not be found.");
        if (!annotations.isEmpty()) {
            annotations = mergeStrategy.merge(annotations);
        }

        long stamp = stampedLock.writeLock();
        try {
            JIPipeDataItemStore virtualData = new JIPipeDataItemStore(JIPipe.getDataTypes().convert(value, getAcceptedDataType(), progressInfo));
            virtualData.addUser(this);
            dataArray.add(virtualData);
            dataContextsArray.add(context != null ? context : new JIPipeMutableDataContext());
            for (JIPipeTextAnnotation annotation : annotations) {
                List<JIPipeTextAnnotation> annotationArray = getOrCreateTextAnnotationColumnArray_(annotation.getName());
                annotationArray.set(getRowCount_() - 1, annotation);
            }
            fireChangedEvent(new TableModelEvent(this));
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * Adds a data row
     *
     * @param value        The data
     * @param annotations  Optional annotations
     * @param progressInfo progress for data storage
     * @deprecated use the overload with the context
     */
    @Deprecated
    public void addData(JIPipeData value, List<JIPipeTextAnnotation> annotations, JIPipeTextAnnotationMergeMode mergeStrategy, JIPipeProgressInfo progressInfo) {
        addData(value, annotations, mergeStrategy, null, progressInfo);
    }

    /**
     * Adds an annotation to all existing data
     *
     * @param annotation The annotation instance
     * @param overwrite  If false, existing annotations of the same type are not overwritten
     */
    public void addTextAnnotationToAllData(JIPipeTextAnnotation annotation, boolean overwrite) {
        long stamp = stampedLock.writeLock();
        try {
            List<JIPipeTextAnnotation> annotationArray = getOrCreateTextAnnotationColumnArray_(annotation.getName());
            int rowCount = getRowCount_();
            for (int i = 0; i < rowCount; ++i) {
                if (!overwrite && annotationArray.get(i) != null)
                    continue;
                annotationArray.set(i, annotation);
            }
            fireChangedEvent(new TableModelEvent(this));
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * Adds an annotation to all existing data
     *
     * @param annotation The annotation instance
     * @param overwrite  If false, existing annotations of the same type are not overwritten
     * @deprecated use addTextAnnotationToAllData()
     */
    @Deprecated
    public void addAnnotationToAllData(JIPipeTextAnnotation annotation, boolean overwrite) {
        addTextAnnotationToAllData(annotation, overwrite);
    }

    /**
     * Removes an annotation column from the data
     *
     * @param column Annotation type
     */
    public void removeAllAnnotationsFromData(String column) {
        long stamp = stampedLock.writeLock();
        try {
            int columnIndex = textAnnotationColumnNames.indexOf(column);
            if (columnIndex != -1) {
                textAnnotationColumnNames.remove(columnIndex);
                textAnnotationArrays.remove(column);
            }
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * Adds a data row
     *
     * @param value        Data
     * @param progressInfo progress for data storage
     * @deprecated use the overload with the context
     */
    @Deprecated
    public void addData(JIPipeData value, JIPipeProgressInfo progressInfo) {
        addData(value, Collections.emptyList(), JIPipeTextAnnotationMergeMode.Merge, progressInfo);
    }

    /**
     * Adds a data row
     *
     * @param value        Data
     * @param progressInfo progress for data storage
     */
    public void addData(JIPipeData value, JIPipeDataContext context, JIPipeProgressInfo progressInfo) {
        addData(value, Collections.emptyList(), JIPipeTextAnnotationMergeMode.Merge, context, progressInfo);
    }

    /**
     * Merges the data from the source table into the current one
     *
     * @param table        the source table
     * @param progressInfo the progress
     * @deprecated use addDataFromTable instead
     */
    @Deprecated
    public void addFromTable(JIPipeDataTable table, JIPipeProgressInfo progressInfo) {
        addDataFromTable(table, progressInfo);
    }

    /**
     * Merges the data from the source table into the current one
     *
     * @param table        the source table
     * @param progressInfo the progress
     */
    public void addDataFromTable(JIPipeDataTable table, JIPipeProgressInfo progressInfo) {
        int rowCount = table.getRowCount();
        for (int row = 0; row < rowCount; row++) {
            JIPipeProgressInfo addDataProgress = progressInfo.resolveAndLog("Add data from table", row, rowCount);
            addData(table.getDataItemStore(row),
                    table.getTextAnnotations(row),
                    JIPipeTextAnnotationMergeMode.OverwriteExisting,
                    table.getDataAnnotations(row),
                    JIPipeDataAnnotationMergeMode.OverwriteExisting,
                    table.getDataContext(row),
                    addDataProgress);
        }
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
        JIPipeDataTableInfo dataTableMetadata = new JIPipeDataTableInfo();
        dataTableMetadata.setAcceptedDataTypeId(JIPipe.getDataTypes().getIdOf(getAcceptedDataType()));

        // Calculate the preview sizes
        List<Dimension> previewSizes = new ArrayList<>();
        for (IntegerAndIntegerPairParameter entry : JIPipeGeneralDataApplicationSettings.getInstance().getExportedPreviewSizes()) {
            previewSizes.add(new Dimension(entry.getKey(), entry.getValue()));
        }

        // We need to create unique and filesystem-safe mappings for data annotation column names
        Map<String, String> dataAnnotationColumnNameMapping = new HashMap<>();
        for (String dataAnnotationColumn : getDataAnnotationColumnNames()) {
            String mappedName = StringUtils.makeUniqueString(StringUtils.makeFilesystemCompatible(dataAnnotationColumn),
                    "_",
                    dataAnnotationColumnNameMapping.values());
            dataAnnotationColumnNameMapping.put(dataAnnotationColumn, mappedName);
        }

        // Save data
        long stamp = stampedLock.readLock();
        try {
            int rowCount = getRowCount_();
            for (int row = 0; row < rowCount; ++row) {
                JIPipeDataTableRowInfo rowMetadata = new JIPipeDataTableRowInfo();
                rowMetadata.setIndex(row);
                rowMetadata.setTrueDataType(JIPipe.getDataTypes().getIdOf(getDataItemStore(row).getDataClass()));
                rowMetadata.setTextAnnotations(getTextAnnotations(row));
                rowMetadata.setDataContext(getDataContext(row));
                JIPipeProgressInfo rowProgress = saveProgress.resolveAndLog("Row", row, rowCount);
                exportDataRow_(storage, row, previewSizes, rowProgress);
                for (JIPipeDataAnnotation dataAnnotation : getDataAnnotations(row)) {
                    JIPipeProgressInfo dataAnnotationProgress = rowProgress.resolveAndLog("Data annotation '" + dataAnnotation.getName() + "'");
                    JIPipeWriteDataStorage dataAnnotationStore = saveDataAnnotationRow_(storage, dataAnnotationProgress, row, previewSizes, rowProgress, dataAnnotation, dataAnnotationColumnNameMapping);
                    JIPipeDataAnnotationInfo dataAnnotationMetadata = new JIPipeDataAnnotationInfo(dataAnnotation.getName(),
                            dataAnnotationStore.getInternalPath(),
                            JIPipe.getDataTypes().getIdOf(dataAnnotation.getDataClass()),
                            rowMetadata);
                    rowMetadata.getDataAnnotations().add(dataAnnotationMetadata);
                }
                dataTableMetadata.add(rowMetadata);
            }
        } finally {
            stampedLock.unlock(stamp);
        }

        try {
            dataTableMetadata.saveAsJson(storage.getFileSystemPath().resolve("data-table.json"));
            dataTableMetadata.saveAsCSV(storage.getFileSystemPath().resolve("data-table.csv"));
        } catch (IOException e) {
            throw new JIPipeValidationRuntimeException(e,
                    "Unable to save data table!",
                    "JIPipe tried to write files into '" + storage + "'.",
                    "Check if you have permissions to write into the path, and if there is enough disk space.");
        }
    }

    /**
     * NOT THREAD-SAFE
     *
     * @param storage                         the storage
     * @param saveProgress                    the progress
     * @param row                             the row
     * @param previewSizes                    the preview sizes
     * @param rowProgress                     the progress
     * @param dataAnnotation                  the data annotation
     * @param dataAnnotationColumnNameMapping the column name mapping
     * @return the storage where the data annotations are stored
     */
    protected JIPipeWriteDataStorage saveDataAnnotationRow_(JIPipeWriteDataStorage storage, JIPipeProgressInfo saveProgress, int row, List<Dimension> previewSizes, JIPipeProgressInfo rowProgress, JIPipeDataAnnotation dataAnnotation, Map<String, String> dataAnnotationColumnNameMapping) {
        JIPipeWriteDataStorage dataAnnotationsStore = storage.resolve("data-annotations").resolve("" + row).resolve(dataAnnotationColumnNameMapping.get(dataAnnotation.getName()));
        JIPipeData dataToExport = dataAnnotation.getData(JIPipeData.class, saveProgress.resolve("Load virtual data"));
        dataToExport.exportData(dataAnnotationsStore,
                dataAnnotationColumnNameMapping.get(dataAnnotation.getName()),
                false,
                rowProgress);

        // Generate and save thumbnail
        Path thumbnailPath = Paths.get("thumbnail").resolve("data-annotations").resolve("" + row).resolve(dataAnnotationColumnNameMapping.get(dataAnnotation.getName()));
        dataToExport.exportThumbnails(storage.resolve(thumbnailPath),
                Paths.get("data-annotations").resolve("" + row).resolve(dataAnnotationColumnNameMapping.get(dataAnnotation.getName())),
                previewSizes,
                rowProgress.resolve("Thumbnail"));

        return dataAnnotationsStore;
    }

    /**
     * NOT THREAD-SAFE
     *
     * @param storage      the storage
     * @param row          the row
     * @param previewSizes preview sizes
     * @param rowProgress  the progress
     */
    protected void exportDataRow_(JIPipeWriteDataStorage storage, int row, List<Dimension> previewSizes, JIPipeProgressInfo rowProgress) {
        JIPipeWriteDataStorage rowStorage = storage.resolve("" + row);
        JIPipeData dataToExport = dataArray.get(row).getData(rowProgress.resolve("Load virtual data"));
        dataToExport.exportData(rowStorage, "data", false, rowProgress);

        // Generate and save thumbnail
        Path thumbnailPath = Paths.get("thumbnail").resolve("" + row);
        dataToExport.exportThumbnails(storage.resolve(thumbnailPath), Paths.get("" + row), previewSizes, rowProgress.resolve("Thumbnail"));
    }

    /**
     * Copies the source slot into this slot.
     * This will only add data and not clear it beforehand.
     * Data is copied without duplication.
     *
     * @param sourceSlot   The other slot
     * @param progressInfo the progress
     * @deprecated use addDataFromSlot
     */
    @Deprecated
    public void addData(JIPipeDataTable sourceSlot, JIPipeProgressInfo progressInfo) {
        addDataFromSlot(sourceSlot, progressInfo);
    }

    /**
     * Copies the source slot into this slot.
     * This will only add data and not clear it beforehand.
     * Data is copied without duplication.
     *
     * @param sourceSlot   The other slot
     * @param progressInfo the progress
     */
    public void addDataFromSlot(JIPipeDataTable sourceSlot, JIPipeProgressInfo progressInfo) {
        String text = "Copying data from " + sourceSlot.getDisplayName() + " to " + getDisplayName();
        int rowCount = sourceSlot.getRowCount();
        for (int row = 0; row < rowCount; ++row) {
            JIPipeProgressInfo addDataProgress = progressInfo.resolveAndLog(text, row, rowCount);
            addData(sourceSlot.getDataItemStore(row),
                    sourceSlot.getTextAnnotations(row),
                    JIPipeTextAnnotationMergeMode.OverwriteExisting,
                    sourceSlot.getDataAnnotations(row),
                    JIPipeDataAnnotationMergeMode.OverwriteExisting,
                    sourceSlot.getDataContext(row),
                    addDataProgress);
        }
    }

    /**
     * Adds data as virtual data reference
     *
     * @param virtualData   the virtual data
     * @param annotations   the annotations
     * @param mergeStrategy merge strategy
     * @param progressInfo  the progress info
     */
    public void addData(JIPipeDataItemStore virtualData, List<JIPipeTextAnnotation> annotations, JIPipeTextAnnotationMergeMode mergeStrategy, JIPipeDataContext context, JIPipeProgressInfo progressInfo) {
        if (!accepts(virtualData.getDataClass())) {
            throw new IllegalArgumentException("Tried to add data of type " + virtualData.getDataClass() + ", but slot only accepts "
                    + acceptedDataType + ". A converter could not be found.");
        }
        long stamp = stampedLock.writeLock();
        try {
            if (!annotations.isEmpty()) {
                annotations = mergeStrategy.merge(annotations);
            }
            virtualData.addUser(this);
            dataArray.add(virtualData);
            dataContextsArray.add(context != null ? context : new JIPipeMutableDataContext());
            for (JIPipeTextAnnotation annotation : annotations) {
                List<JIPipeTextAnnotation> annotationArray = getOrCreateTextAnnotationColumnArray_(annotation.getName());
                annotationArray.set(getRowCount_() - 1, annotation);
            }
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * Adds data as virtual data reference
     *
     * @param virtualData   the virtual data
     * @param annotations   the annotations
     * @param mergeStrategy merge strategy
     * @param progressInfo  the progress info
     * @deprecated use the overload with the context
     */
    @Deprecated
    public void addData(JIPipeDataItemStore virtualData, List<JIPipeTextAnnotation> annotations, JIPipeTextAnnotationMergeMode mergeStrategy, JIPipeProgressInfo progressInfo) {
        addData(virtualData, annotations, mergeStrategy, null, progressInfo);
    }

    /**
     * Gets an annotation at a specific index
     *
     * @param row    the data row
     * @param column the column
     * @return the annotation
     */
    public JIPipeTextAnnotation getTextAnnotation(int row, int column) {
        long stamp = stampedLock.readLock();
        try {
            String annotation = textAnnotationColumnNames.get(column);
            return textAnnotationArrays.get(annotation).get(row);
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * Gets an annotation at a specific column
     *
     * @param row    the data row
     * @param column the column
     * @return the annotation
     */
    public JIPipeTextAnnotation getTextAnnotation(int row, String column) {
        long stamp = stampedLock.readLock();
        try {
            return textAnnotationArrays.get(column).get(row);
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * Converts this data table in-place and sets the accepted data type
     *
     * @param dataClass the target data type
     */
    public void convert(Class<? extends JIPipeData> dataClass, JIPipeProgressInfo progressInfo) {
        long stamp = stampedLock.writeLock();
        try {
            int rowCount = getRowCount_();
            for (int row = 0; row < rowCount; row++) {
                JIPipeProgressInfo rowProgress = progressInfo.resolveAndLog("Convert", row, rowCount);
                JIPipeDataItemStore virtualData = getDataItemStore(row);
                if (!dataClass.isAssignableFrom(virtualData.getDataClass())) {
                    JIPipeData converted = JIPipe.getDataTypes().convert(virtualData.getData(rowProgress), dataClass, progressInfo);
                    virtualData = new JIPipeDataItemStore(converted);
                    dataArray.set(row, virtualData);
                }
            }
            this.acceptedDataType = dataClass;
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * Closes all data and data annotations and replaces their values in the table by null. Text annotations are left unchanged.
     * This method is internally used for exporting data. Please be careful, as {@link JIPipeDataItemStore} items that might be present in
     * other tables might be destroyed.
     * Use clearData() as alternative
     */
    public void destroyData() {
        long stamp = stampedLock.writeLock();
        try {
            for (int i = 0; i < dataArray.size(); ++i) {
                try {
                    dataArray.get(i).removeUser(this);
                    if (dataArray.get(i).canClose())
                        dataArray.get(i).close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                dataArray.set(i, null);
            }
            for (ArrayList<JIPipeDataItemStore> list : dataAnnotationsArrays.values()) {
                for (int i = 0; i < list.size(); i++) {
                    JIPipeDataItemStore dataItemStore = list.get(i);
                    if (dataItemStore != null) {
                        try {
                            dataItemStore.removeUser(this);
                            if (dataItemStore.canClose())
                                dataItemStore.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        list.set(i, null);
                    }
                }
            }
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    public boolean isEmpty() {
        return getRowCount() <= 0;
    }

    /**
     * Creates a new instance that contains only the selected rows.
     * All other attributes are copied.
     *
     * @param rows the rows
     * @return the sliced slot
     */
    public JIPipeDataTable slice(Collection<Integer> rows) {
        JIPipeDataTable result = new JIPipeDataTable(getAcceptedDataType());
        for (int row : rows) {
            result.addData(getDataItemStore(row), getTextAnnotations(row), JIPipeTextAnnotationMergeMode.OverwriteExisting, getDataContext(row), new JIPipeProgressInfo());
            for (Map.Entry<String, JIPipeDataItemStore> entry : getDataAnnotationItemStoreMap_(row).entrySet()) {
                result.setDataAnnotationItemStore(result.getRowCount() - 1, entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * Creates a new instance that contains the filtered items.
     *
     * @param predicate the predicate (the current table, row index)
     * @return filtered table
     */
    public JIPipeDataTable filter(BiPredicate<JIPipeDataTable, Integer> predicate) {
        List<Integer> rows = new ArrayList<>();
        for (int i = 0; i < dataArray.size(); i++) {
            if (predicate.test(this, i)) {
                rows.add(i);
            }
        }
        return slice(rows);
    }

    /**
     * Converts the slot into an annotation table
     *
     * @param withDataAsString if the string representation should be included
     * @return the table
     */
    public AnnotationTableData toAnnotationTable(boolean withDataAsString) {
        AnnotationTableData output = new AnnotationTableData();
        int dataColumn = withDataAsString ? output.addColumn(StringUtils.makeUniqueString("String representation", "_", getTextAnnotationColumnNames()), true) : -1;
        int row = 0;
        int rowCount = getRowCount();
        for (int sourceRow = 0; sourceRow < rowCount; ++sourceRow) {
            output.addRow();
            if (dataColumn >= 0)
                output.setValueAt(getDataItemStore(row).getStringRepresentation(), row, dataColumn);
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
     * @param progressInfo  the progress info
     */
    public void addData(JIPipeDataItemStore virtualData, List<JIPipeTextAnnotation> annotations, JIPipeTextAnnotationMergeMode mergeStrategy,
                        List<JIPipeDataAnnotation> dataAnnotations, JIPipeDataAnnotationMergeMode dataAnnotationMergeStrategy, JIPipeDataContext context, JIPipeProgressInfo progressInfo) {
        if (!accepts(virtualData.getDataClass())) {
            throw new IllegalArgumentException("Tried to add data of type " + virtualData.getDataClass() + ", but slot only accepts "
                    + acceptedDataType + ". A converter could not be found.");
        }
        if (!annotations.isEmpty()) {
            annotations = mergeStrategy.merge(annotations);
        }
        long stamp = stampedLock.writeLock();
        try {
            if (getAcceptedDataType().isAssignableFrom(virtualData.getDataClass())) {
                // Trivial conversion (do nothing)
                dataArray.add(virtualData);
                virtualData.addUser(this);
            } else {
                // Have to convert the data
                JIPipeData data = virtualData.get();
                JIPipeData converted = JIPipe.getDataTypes().convert(data, getAcceptedDataType(), progressInfo);
                JIPipeDataItemStore store = new JIPipeDataItemStore(converted);
                dataArray.add(store);
                store.addUser(this);
            }
            dataContextsArray.add(context != null ? context : new JIPipeMutableDataContext());
            for (JIPipeTextAnnotation annotation : annotations) {
                List<JIPipeTextAnnotation> annotationArray = getOrCreateTextAnnotationColumnArray_(annotation.getName());
                annotationArray.set(getRowCount_() - 1, annotation);
            }
            for (JIPipeDataAnnotation annotation : dataAnnotationMergeStrategy.merge(dataAnnotations)) {
                setDataAnnotationItemStore_(getRowCount_() - 1, annotation.getName(), annotation.getDataItemStore());
            }
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * Adds data as virtual data reference
     *
     * @param virtualData   the virtual data
     * @param annotations   the annotations
     * @param mergeStrategy merge strategy
     * @param progressInfo  the progress info
     * @deprecated use the overload with the context
     */
    @Deprecated
    public void addData(JIPipeDataItemStore virtualData, List<JIPipeTextAnnotation> annotations, JIPipeTextAnnotationMergeMode mergeStrategy,
                        List<JIPipeDataAnnotation> dataAnnotations, JIPipeDataAnnotationMergeMode dataAnnotationMergeStrategy, JIPipeProgressInfo progressInfo) {
        addData(virtualData, annotations, mergeStrategy, dataAnnotations, dataAnnotationMergeStrategy, null, progressInfo);
    }

    /**
     * Adds data as virtual data reference
     *
     * @param data          the data
     * @param annotations   the annotations
     * @param mergeStrategy merge strategy
     * @param progressInfo  the progress info
     */
    public void addData(JIPipeData data, List<JIPipeTextAnnotation> annotations, JIPipeTextAnnotationMergeMode mergeStrategy,
                        List<JIPipeDataAnnotation> dataAnnotations, JIPipeDataAnnotationMergeMode dataAnnotationMergeStrategy, JIPipeDataContext context, JIPipeProgressInfo progressInfo) {
        if (!accepts(data)) {
            throw new IllegalArgumentException("Tried to add data of type " + data.getClass() + ", but slot only accepts "
                    + acceptedDataType + ". A converter could not be found.");
        }
        if (!annotations.isEmpty()) {
            annotations = mergeStrategy.merge(annotations);
        }
        long stamp = stampedLock.writeLock();
        try {
            JIPipeDataItemStore virtualData = new JIPipeDataItemStore(data);
            this.dataArray.add(virtualData);
            this.dataContextsArray.add(context != null ? context : new JIPipeMutableDataContext());
            virtualData.addUser(this);
            for (JIPipeTextAnnotation annotation : annotations) {
                List<JIPipeTextAnnotation> annotationArray = getOrCreateTextAnnotationColumnArray_(annotation.getName());
                annotationArray.set(getRowCount_() - 1, annotation);
            }
            for (JIPipeDataAnnotation annotation : dataAnnotationMergeStrategy.merge(dataAnnotations)) {
                setDataAnnotationItemStore_(getRowCount_() - 1, annotation.getName(), annotation.getDataItemStore());
            }
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * Adds data as virtual data reference
     *
     * @param data          the data
     * @param annotations   the annotations
     * @param mergeStrategy merge strategy
     * @param progressInfo  the progress info
     * @deprecated use the overload with the context
     */
    @Deprecated
    public void addData(JIPipeData data, List<JIPipeTextAnnotation> annotations, JIPipeTextAnnotationMergeMode mergeStrategy,
                        List<JIPipeDataAnnotation> dataAnnotations, JIPipeDataAnnotationMergeMode dataAnnotationMergeStrategy, JIPipeProgressInfo progressInfo) {
        addData(data, annotations, mergeStrategy, dataAnnotations, dataAnnotationMergeStrategy, null, progressInfo);
    }

    /**
     * Gets the true type of the data at given row
     *
     * @param row the row
     * @return the true data type
     */
    public Class<? extends JIPipeData> getDataClass(int row) {
        return dataArray.get(row).getDataClass();
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
    public JIPipeThumbnailData createThumbnail(int width, int height, JIPipeProgressInfo progressInfo) {
        List<JIPipeThumbnailData> thumbnailData = new ArrayList<>();
        int count = Math.min(9, getRowCount());
        for (int i = 0; i < count; i++) {
            JIPipeProgressInfo rowProgress = progressInfo.resolveAndLog("Thumbnail", i, count);
            thumbnailData.add(getData(i, JIPipeData.class, rowProgress).createThumbnail(width, height, rowProgress));
        }
        return new JIPipeGridThumbnailData(thumbnailData);
    }

    @Override
    public String toString() {
        return getRowCount() + " rows of " + JIPipeData.getNameOf(getAcceptedDataType());
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

    /**
     * Gets a data annotation
     *
     * @param row the row
     * @param col the data annotation column
     * @return the data annotation
     */
    public JIPipeDataAnnotation getDataAnnotation(int row, int col) {
        return getDataAnnotation(row, getDataAnnotationColumnNames().get(col));
    }

    /**
     * Returns Information about the true data type that is stored at given row
     *
     * @param row the row
     * @return the data type info (true type stored in the table)
     */
    public JIPipeDataInfo getDataInfo(int row) {
        return JIPipeDataInfo.getInstance(getDataClass(row));
    }
}
