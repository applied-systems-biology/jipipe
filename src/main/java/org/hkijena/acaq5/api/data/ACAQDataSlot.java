package org.hkijena.acaq5.api.data;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.data.traits.ACAQMutableTraitConfiguration;
import org.hkijena.acaq5.api.data.traits.ACAQTraitModificationOperation;
import org.hkijena.acaq5.api.events.SlotAnnotationsChanged;
import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
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
    private ACAQSlotDefinition definition;
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

    /**
     * Creates a new slot
     *
     * @param definition
     * @param algorithm  The algorithm that contains the slot
     */
    public ACAQDataSlot(ACAQSlotDefinition definition, ACAQAlgorithm algorithm) {
        this.definition = definition;
        this.algorithm = algorithm;
        this.name = definition.getName();
        this.slotType = definition.getSlotType();
        this.acceptedDataType = definition.getDataClass();
    }

    public List<ACAQTraitDeclaration> getAnnotationColumns() {
        return Collections.unmodifiableList(annotationColumns);
    }

    /**
     * @return the slot's data type
     */
    public Class<? extends ACAQData> getAcceptedDataType() {
        return acceptedDataType;
    }

    /**
     * Sets the accepted slot type
     * Please note that this method can cause issues when running the graph
     *
     * @param slotDataType the new data type
     */
    public void setAcceptedDataType(Class<? extends ACAQData> slotDataType) {
        acceptedDataType = slotDataType;
    }

    /**
     * Returns true if the slot can carry the provided data.
     * This will also look up if the data can be converted
     *
     * @param data Data
     * @return True if the slot accepts the data
     */
    public boolean accepts(ACAQData data) {
        if (data == null)
            throw new NullPointerException("Data slots cannot accept null data!");
        return ACAQDatatypeRegistry.getInstance().isConvertible(data.getClass(), getAcceptedDataType());
    }

    /**
     * Gets the data stored in a specific row
     *
     * @param <T>       Data type
     * @param row       The row
     * @param dataClass the class to return
     * @return Data at row
     */
    public <T extends ACAQData> T getData(int row, Class<T> dataClass) {
        return (T) ACAQDatatypeRegistry.getInstance().convert(data.get(row), dataClass);
    }

    /**
     * Gets the list of annotations for a specific data row
     *
     * @param row The row
     * @return Annotations at row
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
     * Ensures that the output size is equal to getRowCount()
     *
     * @param declaration Annotation type
     * @return All trait instances of the provided type. Size is getRowCount()
     */
    private List<ACAQTrait> getOrCreateAnnotationColumnData(ACAQTraitDeclaration declaration) {
        ArrayList<ACAQTrait> arrayList = annotations.getOrDefault(declaration, null);
        if (arrayList == null) {
            annotationColumns.add(declaration);
            arrayList = new ArrayList<>();
            annotations.put(declaration, arrayList);
        }
        while (arrayList.size() < getRowCount()) {
            arrayList.add(null);
        }
        return arrayList;
    }

    /**
     * Adds a data row
     *
     * @param value  The data
     * @param traits Optional traits
     */
    public void addData(ACAQData value, List<ACAQTrait> traits) {
        if (!accepts(value))
            throw new IllegalArgumentException("Tried to add data of type " + value.getClass() + ", but slot only accepts " + acceptedDataType + ". A converter could not be found.");
        if (uniqueData) {
            if (findRowWithTraits(traits) != -1) {
                uniqueData = false;
            }
        }
        data.add(ACAQDatatypeRegistry.getInstance().convert(value, getAcceptedDataType()));
        for (ACAQTrait trait : traits) {
            List<ACAQTrait> traitArray = getOrCreateAnnotationColumnData(trait.getDeclaration());
            traitArray.set(getRowCount() - 1, trait);
        }
    }

    /**
     * Adds an annotation to all existing data
     *
     * @param trait     The trait instance
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
     * @param declaration Annotation type
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
     * @param value Data
     */
    public void addData(ACAQData value) {
        addData(value, Collections.emptyList());
    }

    /**
     * Finds the row that matches the given traits
     *
     * @param traits A valid annotation list with size equals to getRowCount()
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
     * Finds rows that match the given traits
     *
     * @param traits A valid annotation list with size equals to getRowCount()
     * @return list of rows
     */
    public List<Integer> findRowsWithTraits(List<ACAQTrait> traits) {
        ACAQTraitDeclaration[] declarationMap = new ACAQTraitDeclaration[traits.size()];
        for (int i = 0; i < traits.size(); ++i) {
            int declarationIndex = annotationColumns.indexOf(traits.get(i).getDeclaration());
            if (declarationIndex == -1)
                return new ArrayList<>();
            declarationMap[i] = annotationColumns.get(declarationIndex);
        }
        List<Integer> result = new ArrayList<>();
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
                result.add(row);
        }
        return result;
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
    public String getNameWithAlgorithmName() {
        return algorithm.getName() + " \uD83E\uDC92 " + getName();
    }

    /**
     * @return The algorithm that contains the slot
     */
    public ACAQAlgorithm getAlgorithm() {
        return algorithm;
    }

    /**
     * @return The slot type
     */
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
                        throw new UserFriendlyRuntimeException(e, "Unable to create directory '" + path + "'!",
                                "Data slot '" + getNameWithAlgorithmName() + "'", "The path might be invalid, or you might not have the permissions to write in a parent folder.",
                                "Check if the path is valid, and you have write-access.");
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
                throw new UserFriendlyRuntimeException(e, "Unable to save data table!",
                        "Data slot '" + getNameWithAlgorithmName() + "'", "ACAQ tried to write files into '" + storagePath + "'.",
                        "Check if you have permissions to write into the path, and if there is enough disk space.");
            }
        }
    }

    /**
     * Returns all trait declarations, sorted by their information
     *
     * @return Information-sorted trait types
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
     * @param traitDeclaration Trait type
     * @return Shannon-Information of this trait type
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
     * @param sourceSlot The other slot
     */
    public void copyFrom(ACAQDataSlot sourceSlot) {
        for (int row = 0; row < sourceSlot.getRowCount(); ++row) {
            addData(sourceSlot.getData(row, ACAQData.class), sourceSlot.getAnnotations(row));
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
     * @return Slot annotations
     */
    public Set<ACAQTraitDeclaration> getSlotAnnotations() {
        return Collections.unmodifiableSet(slotAnnotations);
    }

    /**
     * Adds an annotation to this slot
     *
     * @param declaration Annotation type
     */
    public void addSlotAnnotation(ACAQTraitDeclaration declaration) {
        slotAnnotations.add(declaration);
        eventBus.post(new SlotAnnotationsChanged(this));
    }

    /**
     * Removes an annotation from this slot
     *
     * @param declaration Annotation type
     */
    public void removeSlotAnnotation(ACAQTraitDeclaration declaration) {
        if (slotAnnotations.remove(declaration))
            eventBus.post(new SlotAnnotationsChanged(this));
    }

    /**
     * Removes the annotation, as well as any other annotation that inherits from it from this slot
     *
     * @param declaration Annotation type
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

    /**
     * @return The event bus
     */
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Updates the trait declaration to add this trait
     *
     * @param declaration Annotation type
     * @param operation   The operation
     */
    public void setSlotTraitToTraitConfiguration(ACAQTraitDeclaration declaration, ACAQTraitModificationOperation operation) {
        if (algorithm.getTraitConfiguration() instanceof ACAQMutableTraitConfiguration) {
            ((ACAQMutableTraitConfiguration) algorithm.getTraitConfiguration()).setTraitModification(getName(), declaration, operation);
        }
    }

    /**
     * Removes all data from this slot
     */
    public void clearData() {
        data.clear();
        annotationColumns.clear();
        annotations.clear();
    }

    public ACAQSlotDefinition getDefinition() {
        return definition;
    }

    /**
     * The slot type
     */
    public enum SlotType {
        Input,
        Output
    }
}
