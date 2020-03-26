package org.hkijena.acaq5.api.data;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.data.traits.ACAQMutableTraitConfiguration;
import org.hkijena.acaq5.api.data.traits.ACAQTraitModificationOperation;
import org.hkijena.acaq5.api.events.SlotAnnotationsChanged;
import org.hkijena.acaq5.api.traits.ACAQDiscriminator;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A data slot holds an {@link ACAQData} instance.
 * Slots are part of an {@link ACAQAlgorithm}
 */
public class ACAQDataSlot implements TableModel {
    private ACAQAlgorithm algorithm;
    private String name;
    private Class<? extends ACAQData> acceptedDataType;
    private SlotType slotType;
    private Path storagePath;
    private boolean uniqueData = true;
    private EventBus eventBus = new EventBus();

    private ArrayList<ACAQData> data = new ArrayList<>();
    private List<ACAQTraitDeclaration> annotationColumns = new ArrayList<>();
    private Map<ACAQTraitDeclaration, ArrayList<ACAQTrait>> annotations = new HashMap<>();

    private Set<ACAQTraitDeclaration> slotAnnotations = new HashSet<>();

    public ACAQDataSlot(ACAQAlgorithm algorithm, SlotType slotType, String name, Class<? extends ACAQData> acceptedDataType) {
        this.algorithm = algorithm;
        this.name = name;
        this.slotType = slotType;
        this.acceptedDataType = acceptedDataType;
    }

    public Class<? extends ACAQData> getAcceptedDataType() {
        return acceptedDataType;
    }

    /**
     * Sets the accepted slot type
     * Please note that this method can cause issues when running the graph
     *
     * @param slotDataType
     */
    public void setAcceptedDataType(Class<? extends ACAQData> slotDataType) {
        acceptedDataType = slotDataType;
    }

    public boolean accepts(ACAQData data) {
        if (data == null)
            throw new NullPointerException("Data slots cannot accept null data!");
        return acceptedDataType.isAssignableFrom(data.getClass());
    }

    /**
     * Gets the data stored in a specific row
     *
     * @param row
     * @return
     */
    public <T> T getData(int row) {
        return (T) data.get(row);
    }

    /**
     * Gets the list of annotations for a specific data row
     *
     * @param row
     * @return
     */
    public List<ACAQTrait> getAnnotations(int row) {
        List<ACAQTrait> result = new ArrayList<>();
        for (ACAQTraitDeclaration declaration : annotationColumns) {
            ACAQTrait trait = getOrCreateAnnotationColumnData(declaration).get(row);
            if (trait != null)
                result.add(trait);
        }
        return result;
    }

    /**
     * Gets the annotation column for the trait declaration or creates it
     * @param declaration
     * @return
     */
    private List<ACAQTrait> getOrCreateAnnotationColumnData(ACAQTraitDeclaration declaration) {
        ArrayList<ACAQTrait> arrayList = annotations.getOrDefault(declaration, null);
        if (arrayList == null) {
            annotationColumns.add(declaration);
            arrayList = new ArrayList<>();
            annotations.put(declaration, arrayList);
        }
        while(arrayList.size() < getRowCount()) {
            arrayList.add(null);
        }
        return arrayList;
    }

    /**
     * Adds a data row
     *
     * @param value
     * @param traits
     */
    public void addData(ACAQData value, List<ACAQTrait> traits) {
        if (!accepts(value))
            throw new IllegalArgumentException("Tried to add data of type " + value.getClass() + ", but slot only accepts " + acceptedDataType);
        if (uniqueData) {
            if (findRowWithTraits(traits) != -1) {
                uniqueData = false;
            }
        }
        data.add(value);
        for (ACAQTrait trait : traits) {
            List<ACAQTrait> traitArray = getOrCreateAnnotationColumnData(trait.getDeclaration());
            traitArray.set(getRowCount() - 1, trait);
        }
    }

    /**
     * Adds an annotation to all existing data
     *
     * @param trait
     * @param overwrite If false, existing annotations of the same type are not overwritten
     */
    public void addAnnotationToAllData(ACAQTrait trait, boolean overwrite) {
        List<ACAQTrait> traitArray = getOrCreateAnnotationColumnData(trait.getDeclaration());
        for (int i = 0; i < getRowCount(); ++i) {
            if (!overwrite && traitArray.get(i) != null)
                continue;
            traitArray.set(i, trait);
        }
    }

    /**
     * Removes an annotation column from the data
     *
     * @param declaration
     */
    public void removeAllAnnotationsFromData(ACAQTraitDeclaration declaration) {
        int columnIndex = annotationColumns.indexOf(declaration);
        if (columnIndex != -1) {
            annotationColumns.remove(columnIndex);
            annotations.remove(declaration);
        }
    }

    /**
     * Adds a data row
     *
     * @param value
     */
    public void addData(ACAQData value) {
        addData(value, Collections.emptyList());
    }

    /**
     * Finds the row that matches the given traits
     *
     * @param traits
     * @return row index >= 0 if found, otherwise -1
     */
    public int findRowWithTraits(List<ACAQTrait> traits) {
        ACAQTraitDeclaration[] declarationMap = new ACAQTraitDeclaration[traits.size()];
        for (int i = 0; i < traits.size(); ++i) {
            int declarationIndex = annotationColumns.indexOf(traits.get(i).getDeclaration());
            if (declarationIndex == -1)
                return -1;
            declarationMap[i] = annotationColumns.get(declarationIndex);
        }
        for (int row = 0; row < data.size(); ++row) {
            boolean equal = true;
            for (int i = 0; i < traits.size(); ++i) {
                ACAQTraitDeclaration declaration = declarationMap[i];
                ACAQTrait rowTrait = annotations.get(declaration).get(row);
                if (!ACAQTrait.equals(traits.get(i), rowTrait)) {
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
     * @return
     */
    public boolean isDataUnique() {
        return uniqueData;
    }

    public String getName() {
        return name;
    }

    public String getNameWithAlgorithmName() {
        return algorithm.getName() + " \uD83E\uDC92 " + getName();
    }

    public ACAQAlgorithm getAlgorithm() {
        return algorithm;
    }

    public SlotType getSlotType() {
        return slotType;
    }

    /**
     * Saves the stored data to the provided storage path and sets data to null
     * Warning: Ensure that depending input slots do not use this slot, anymore!
     */
    public void flush() {
        save();
        for (int i = 0; i < data.size(); ++i) {
            data.set(i, null);
        }
    }

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
     * @return
     */
    public Path getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(Path storagePath) {
        this.storagePath = storagePath;
    }

    /**
     * Saves the data to the storage path
     */
    public void save() {
        if (isOutput() && storagePath != null && data != null) {

            // Save data
            List<Path> dataOutputPaths = new ArrayList<>();
            for (int row = 0; row < getRowCount(); ++row) {
                Path pathName = Paths.get(getName() + " " + row);
                Path path = storagePath.resolve(pathName);
                if (!Files.isDirectory(path)) {
                    try {
                        Files.createDirectories(path);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                dataOutputPaths.add(pathName);
                data.get(row).saveTo(path, getName());
            }

            ACAQExportedDataTable dataTable = new ACAQExportedDataTable(this, dataOutputPaths);
            try {
                dataTable.saveAsJson(storagePath.resolve("data-table.json"));
                dataTable.saveAsCSV(storagePath.resolve("data-table.csv"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Returns all trait declarations, sorted by their information
     *
     * @return
     */
    public List<ACAQTraitDeclaration> getTraitsSortedByInformation() {
        Map<ACAQTraitDeclaration, Double> informations = new HashMap<>();
        for (ACAQTraitDeclaration traitDeclaration : annotationColumns) {
            informations.put(traitDeclaration, getInformationOf(traitDeclaration));
        }
        return annotationColumns.stream().sorted(Comparator.comparing(informations::get)).collect(Collectors.toList());
    }

    /**
     * Calculates the information of a trait
     *
     * @param traitDeclaration
     * @return
     */
    public double getInformationOf(ACAQTraitDeclaration traitDeclaration) {
        Map<Object, Integer> frequencies = new HashMap<>();
        for (ACAQTrait trait : annotations.get(traitDeclaration)) {
            if (trait instanceof ACAQDiscriminator) {
                String value = ((ACAQDiscriminator) trait).getValue();
                frequencies.put(value, frequencies.getOrDefault(value, 0) + 1);
            } else if (trait == null) {
                frequencies.put(false, frequencies.getOrDefault(false, 0) + 1);
            } else {
                frequencies.put(true, frequencies.getOrDefault(true, 0) + 1);
            }
        }
        double I = 0;
        for (Map.Entry<Object, Integer> entry : frequencies.entrySet()) {
            if (entry.getValue() == 0)
                continue;
            double Ie = Math.log(1.0 / entry.getValue());
            I += Ie;
        }
        return I;
    }

    /**
     * Copies the source slot into this slot
     *
     * @param sourceSlot
     */
    public void copyFrom(ACAQDataSlot sourceSlot) {
        for (int row = 0; row < sourceSlot.getRowCount(); ++row) {
            addData(sourceSlot.getData(row), sourceSlot.getAnnotations(row));
        }
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return annotationColumns.size() + 1;
    }

    @Override
    public String getColumnName(int columnIndex) {
        if (columnIndex == 0)
            return "Data";
        else
            return annotationColumns.get(columnIndex - 1).getName();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0)
            return ACAQData.class;
        else {
            ACAQTraitDeclaration column = annotationColumns.get(columnIndex);
            if (column.isDiscriminator())
                return ACAQDiscriminator.class;
            else
                return ACAQTrait.class;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            return data.get(rowIndex);
        } else {
            return annotations.get(annotationColumns.get(columnIndex)).get(rowIndex);
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

    }

    @Override
    public void addTableModelListener(TableModelListener l) {

    }

    @Override
    public void removeTableModelListener(TableModelListener l) {

    }

    /**
     * Returns all traits that are not associated to data, but instead associated to the slot itself
     *
     * @return
     */
    public Set<ACAQTraitDeclaration> getSlotAnnotations() {
        return Collections.unmodifiableSet(slotAnnotations);
    }

    /**
     * Adds an annotation to this slot
     *
     * @param declaration
     */
    public void addSlotAnnotation(ACAQTraitDeclaration declaration) {
        slotAnnotations.add(declaration);
        eventBus.post(new SlotAnnotationsChanged(this));
    }

    /**
     * Removes an annotation from this slot
     *
     * @param declaration
     */
    public void removeSlotAnnotation(ACAQTraitDeclaration declaration) {
        if (slotAnnotations.remove(declaration))
            eventBus.post(new SlotAnnotationsChanged(this));
    }

    /**
     * Removes the annotation, as well as any other annotation that inherits from it from this slot
     *
     * @param declaration
     */
    public void removeSlotAnnotationCategory(ACAQTraitDeclaration declaration) {
        if (slotAnnotations.remove(declaration) || slotAnnotations.removeIf(t -> t.getInherited().contains(declaration)))
            eventBus.post(new SlotAnnotationsChanged(this));
    }

    /**
     * Removes all slot annotations
     */
    public void clearSlotAnnotations() {
        if (!slotAnnotations.isEmpty()) {
            slotAnnotations.clear();
            eventBus.post(new SlotAnnotationsChanged(this));
        }
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Updates the trait declaration to add this trait
     *
     * @param declaration
     */
    public void setSlotTraitToTraitConfiguration(ACAQTraitDeclaration declaration, ACAQTraitModificationOperation operation) {
        if (algorithm.getTraitConfiguration() instanceof ACAQMutableTraitConfiguration) {
            ((ACAQMutableTraitConfiguration) algorithm.getTraitConfiguration()).setTraitModification(getName(), declaration, operation);
        }
    }

    public void clearData() {
        data.clear();
        annotationColumns.clear();
        annotations.clear();
    }

    public enum SlotType {
        Input,
        Output
    }
}
