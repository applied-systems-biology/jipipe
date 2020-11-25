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

package org.hkijena.jipipe.api.data;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.extensions.settings.VirtualDataSettings;
import org.hkijena.jipipe.utils.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A data slot holds an {@link JIPipeData} instance.
 * Slots are part of an {@link JIPipeGraphNode}
 */
public class JIPipeDataSlot {
    private JIPipeGraphNode node;
    private JIPipeDataSlotInfo info;
    private String name;
    private Class<? extends JIPipeData> acceptedDataType;
    private JIPipeSlotType slotType;
    private Path storagePath;
    private boolean uniqueData = true;
    private EventBus eventBus = new EventBus();

    private ArrayList<JIPipeVirtualData> data = new ArrayList<>();
    private List<String> annotationColumns = new ArrayList<>();
    private Map<String, ArrayList<JIPipeAnnotation>> annotations = new HashMap<>();

    /**
     * Creates a new slot
     *
     * @param info the slot definition
     * @param node The algorithm that contains the slot
     */
    public JIPipeDataSlot(JIPipeDataSlotInfo info, JIPipeGraphNode node) {
        this.info = info;
        this.node = node;
        this.name = info.getName();
        this.slotType = info.getSlotType();
        this.acceptedDataType = info.getDataClass();
    }

    public List<String> getAnnotationColumns() {
        return Collections.unmodifiableList(annotationColumns);
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
     * Gets the list of annotations for a specific data row
     *
     * @param row The row
     * @return Annotations at row
     */
    public synchronized List<JIPipeAnnotation> getAnnotations(int row) {
        List<JIPipeAnnotation> result = new ArrayList<>();
        for (String info : annotationColumns) {
            JIPipeAnnotation trait = getOrCreateAnnotationColumnData(info).get(row);
            if (trait != null)
                result.add(trait);
        }
        return result;
    }

    /**
     * Gets the list of annotations for specific data rows
     *
     * @param rows The set of rows
     * @return Annotations at row
     */
    public synchronized List<JIPipeAnnotation> getAnnotations(Set<Integer> rows) {
        List<JIPipeAnnotation> result = new ArrayList<>();
        for (String info : annotationColumns) {
            for (int row : rows) {
                JIPipeAnnotation trait = getOrCreateAnnotationColumnData(info).get(row);
                if (trait != null)
                    result.add(trait);
            }
        }
        return result;
    }

    /**
     * Returns the annotation of specified type or the alternative value.
     *
     * @param row    data row
     * @param type   annotation type
     * @param orElse alternative value
     * @return annotation of type 'type' or 'orElse'
     */
    public JIPipeAnnotation getAnnotationOr(int row, String type, JIPipeAnnotation orElse) {
        return getAnnotations(row).stream().filter(a -> a != null && Objects.equals(a.getName(), type)).findFirst().orElse(orElse);
    }

    /**
     * Gets the annotation column for the trait info or creates it
     * Ensures that the output size is equal to getRowCount()
     *
     * @param info Annotation type
     * @return All trait instances of the provided type. Size is getRowCount()
     */
    private synchronized List<JIPipeAnnotation> getOrCreateAnnotationColumnData(String info) {
        ArrayList<JIPipeAnnotation> arrayList = annotations.getOrDefault(info, null);
        if (arrayList == null) {
            annotationColumns.add(info);
            arrayList = new ArrayList<>();
            annotations.put(info, arrayList);
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
     * @param annotations  Optional traits
     * @param progressInfo progress for data storage
     */
    public synchronized void addData(JIPipeData value, List<JIPipeAnnotation> annotations, JIPipeAnnotationMergeStrategy mergeStrategy, JIPipeProgressInfo progressInfo) {
        if (!accepts(value))
            throw new IllegalArgumentException("Tried to add data of type " + value.getClass() + ", but slot only accepts " + acceptedDataType + ". A converter could not be found.");
        if (uniqueData) {
            if (findRowWithAnnotations(annotations) != -1) {
                uniqueData = false;
            }
        }
        if (!annotations.isEmpty()) {
            annotations = mergeStrategy.merge(annotations);
        }
        JIPipeVirtualData virtualData = new JIPipeVirtualData(JIPipe.getDataTypes().convert(value, getAcceptedDataType()));
        data.add(virtualData);
        for (JIPipeAnnotation trait : annotations) {
            List<JIPipeAnnotation> traitArray = getOrCreateAnnotationColumnData(trait.getName());
            traitArray.set(getRowCount() - 1, trait);
        }
        if (info.isVirtual())
            virtualData.makeVirtual(progressInfo, false);
    }

    /**
     * Adds an annotation to all existing data
     *
     * @param annotation The annotation instance
     * @param overwrite  If false, existing annotations of the same type are not overwritten
     */
    public synchronized void addAnnotationToAllData(JIPipeAnnotation annotation, boolean overwrite) {
        List<JIPipeAnnotation> traitArray = getOrCreateAnnotationColumnData(annotation.getName());
        for (int i = 0; i < getRowCount(); ++i) {
            if (!overwrite && traitArray.get(i) != null)
                continue;
            traitArray.set(i, annotation);
        }
    }

    /**
     * Removes an annotation column from the data
     *
     * @param info Annotation type
     */
    public synchronized void removeAllAnnotationsFromData(String info) {
        int columnIndex = annotationColumns.indexOf(info);
        if (columnIndex != -1) {
            annotationColumns.remove(columnIndex);
            annotations.remove(info);
        }
    }

    /**
     * Adds a data row
     *
     * @param value        Data
     * @param progressInfo progress for data storage
     */
    public synchronized void addData(JIPipeData value, JIPipeProgressInfo progressInfo) {
        addData(value, Collections.emptyList(), JIPipeAnnotationMergeStrategy.Merge, progressInfo);
    }

    /**
     * Finds the row that matches the given traits
     *
     * @param traits A valid annotation list with size equals to getRowCount()
     * @return row index >= 0 if found, otherwise -1
     */
    public int findRowWithAnnotations(List<JIPipeAnnotation> traits) {
        String[] infoMap = new String[traits.size()];
        for (int i = 0; i < traits.size(); ++i) {
            int infoIndex = annotationColumns.indexOf(traits.get(i).getName());
            if (infoIndex == -1)
                return -1;
            infoMap[i] = annotationColumns.get(infoIndex);
        }
        for (int row = 0; row < data.size(); ++row) {
            boolean equal = true;
            for (int i = 0; i < traits.size(); ++i) {
                String info = infoMap[i];
                JIPipeAnnotation rowTrait = annotations.get(info).get(row);
                if (!JIPipeAnnotation.nameEquals(traits.get(i), rowTrait)) {
                    equal = false;
                }
            }
            if (equal)
                return row;
        }
        return -1;
    }

    /**
     * Returns true if all rows are unique according to their traits
     *
     * @return if all rows are unique according to their traits
     */
    public boolean isDataUnique() {
        return uniqueData;
    }

    /**
     * @return The unique slot name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a name that includes the algorithm name and the slot name.
     * Should not be used outside of UI.
     *
     * @return Display name that includes the algorithm name, as well as the slot name.
     */
    public String getDisplayName() {
        if (!StringUtils.isNullOrEmpty(info.getCustomName()))
            return info.getCustomName() + " [" + getName() + "] " + " (" + node.getName() + ")";
        else
            return getName() + " (" + node.getName() + ")";
    }

    /**
     * @return The algorithm that contains the slot
     */
    public JIPipeGraphNode getNode() {
        return node;
    }

    /**
     * @return The slot type
     */
    public JIPipeSlotType getSlotType() {
        return slotType;
    }

    /**
     * Saves the stored data to the provided storage path and sets data to null
     * Warning: Ensure that depending input slots do not use this slot, anymore!
     *
     * @param basePath     the base path to where all results are stored relative to. If null, there is no base path
     * @param saveProgress progress that reports the saving
     */
    public void flush(Path basePath, JIPipeProgressInfo saveProgress) {
        if (getNode() instanceof JIPipeAlgorithm) {
            if (getInfo().isSaveOutputs()) {
                save(basePath, saveProgress);
            }
        } else {
            save(basePath, saveProgress);
        }
        destroy();
    }

    /**
     * @return True if this slot is an input slot
     */
    public boolean isInput() {
        switch (slotType) {
            case Input:
                return true;
            case Output:
                return false;
            default:
                throw new RuntimeException("Unknown slot type!");
        }
    }

    /**
     * @return True if this slot is an output
     */
    public boolean isOutput() {
        switch (slotType) {
            case Input:
                return false;
            case Output:
                return true;
            default:
                throw new RuntimeException("Unknown slot type!");
        }
    }

    /**
     * Gets the storage path that is used during running the algorithm for saving the results
     * This is not used during project creation
     *
     * @return Data storage path
     */
    public Path getStoragePath() {
        return storagePath;
    }

    /**
     * Sets storage path that is used during running the algorithm for saving the results
     *
     * @param storagePath Data storage paths
     */
    public void setStoragePath(Path storagePath) {
        this.storagePath = storagePath;
    }

    /**
     * Gets the storage path of a data row from a result. This is not used during project creation.
     *
     * @param index row index
     * @return path where the row's data is stored
     */
    public Path getRowStoragePath(int index) {
        return storagePath.resolve("" + index);
    }

    /**
     * Saves the data to the storage path
     *
     * @param basePath     the base path to where all results are stored relative to. If null, there is no base path
     * @param saveProgress the progress for saving
     */
    public void save(Path basePath, JIPipeProgressInfo saveProgress) {
        if (isOutput() && storagePath != null && data != null) {

            // Save data
            List<Integer> indices = new ArrayList<>();
            for (int row = 0; row < getRowCount(); ++row) {
                JIPipeProgressInfo rowProgress = saveProgress.resolveAndLog("Row", row, getRowCount());
                Path path = storagePath.resolve("" + row);
                if (!Files.isDirectory(path)) {
                    try {
                        Files.createDirectories(path);
                    } catch (IOException e) {
                        throw new UserFriendlyRuntimeException(e, "Unable to create directory '" + path + "'!",
                                "Data slot '" + getDisplayName() + "'", "The path might be invalid, or you might not have the permissions to write in a parent folder.",
                                "Check if the path is valid, and you have write-access.");
                    }
                }

                indices.add(row);
                data.get(row).getData(saveProgress.resolve("Load virtual data")).saveTo(path, getName(), false, rowProgress);
            }

            JIPipeExportedDataTable dataTable = new JIPipeExportedDataTable(this, basePath, indices);
            try {
                dataTable.saveAsJson(storagePath.resolve("data-table.json"));
                dataTable.saveAsCSV(storagePath.resolve("data-table.csv"));
            } catch (IOException e) {
                throw new UserFriendlyRuntimeException(e, "Unable to save data table!",
                        "Data slot '" + getDisplayName() + "'", "JIPipe tried to write files into '" + storagePath + "'.",
                        "Check if you have permissions to write into the path, and if there is enough disk space.");
            }
        }
    }

    /**
     * Copies the source slot into this slot.
     * This will only add data and not clear it beforehand.
     * Data is copied without duplication.
     *
     * @param sourceSlot The other slot
     */
    public void addData(JIPipeDataSlot sourceSlot) {
        for (int row = 0; row < sourceSlot.getRowCount(); ++row) {
            addData(sourceSlot.getVirtualData(row), sourceSlot.getAnnotations(row), JIPipeAnnotationMergeStrategy.Merge);
        }
    }

    /**
     * Adds data as virtual data reference
     *
     * @param virtualData   the virtual data
     * @param annotations   the annotations
     * @param mergeStrategy merge strategy
     */
    public void addData(JIPipeVirtualData virtualData, List<JIPipeAnnotation> annotations, JIPipeAnnotationMergeStrategy mergeStrategy) {
        if (!accepts(virtualData.getDataClass()))
            throw new IllegalArgumentException("Tried to add data of type " + virtualData.getDataClass() + ", but slot only accepts " + acceptedDataType + ". A converter could not be found.");
        if (uniqueData) {
            if (findRowWithAnnotations(annotations) != -1) {
                uniqueData = false;
            }
        }
        if (!annotations.isEmpty()) {
            annotations = mergeStrategy.merge(annotations);
        }
        data.add(virtualData);
        for (JIPipeAnnotation trait : annotations) {
            List<JIPipeAnnotation> traitArray = getOrCreateAnnotationColumnData(trait.getName());
            traitArray.set(getRowCount() - 1, trait);
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

    public int getRowCount() {
        return data.size();
    }

    /**
     * @return The event bus
     */
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Removes all data from this slot
     */
    public void clearData() {
        data.clear();
        annotationColumns.clear();
        annotations.clear();
        System.gc();
    }

    public JIPipeDataSlotInfo getInfo() {
        return info;
    }

    /**
     * Only valid for slots generated by a {@link org.hkijena.jipipe.api.JIPipeRun}.
     * Returns the path to the data table file
     *
     * @return the data table path
     */
    public Path getStorageDataTablePath() {
        return getStoragePath().resolve("data-table.json");
    }

    /**
     * Only valid for slots generated by a {@link org.hkijena.jipipe.api.JIPipeRun}.
     * Returns the {@link JIPipeExportedDataTable} from the data-table.json file
     *
     * @return the data table
     */
    public JIPipeExportedDataTable getStorageDataTable() {
        return JIPipeExportedDataTable.loadFromJson(getStorageDataTablePath());
    }

    @Override
    public String toString() {
        return String.format("%s: %s (%d rows, %d annotation columns)", getSlotType(), getName(), getRowCount(), getAnnotationColumns().size());
    }

    public boolean isVirtual() {
        return info.isVirtual();
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
     * @param progressInfo the progress
     */
    public void makeDataNonVirtual(JIPipeProgressInfo progressInfo) {
        JIPipeProgressInfo subProgress = progressInfo.resolve("Loading virtual data to memory");
        for (int row = 0; row < getRowCount(); row++) {
            JIPipeVirtualData virtualData = getVirtualData(row);
            if (virtualData.isVirtual()) {
                virtualData.makeNonVirtual(subProgress.resolveAndLog("Row", row, getRowCount()));
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
        }
    }

    /**
     * If virtual, put all data into the virtual storage
     * If not, fetch all virtual data from storage
     * This function reacts to the virtual mode setting in {@link VirtualDataSettings} and will refuse to make data virtual
     *
     * @param progressInfo the progress
     */
    public void applyVirtualState(JIPipeProgressInfo progressInfo) {
        if (info.isVirtual() && VirtualDataSettings.getInstance().isVirtualMode()) {
            makeDataVirtual(progressInfo);
        } else {
            makeDataNonVirtual(progressInfo);
        }
    }

    public JIPipeAnnotation getAnnotation(int row, int column) {
        String annotation = annotationColumns.get(column);
        return annotations.get(annotation).get(row);
    }

    /**
     * Similar to flush(), but only destroys the data.
     * This will keep the annotations, and replace all data items by null
     */
    public void destroy() {
        for (int i = 0; i < data.size(); ++i) {
            data.set(i, null);
        }
        System.gc();
    }
}
