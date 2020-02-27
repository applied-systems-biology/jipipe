package org.hkijena.acaq5.api.data;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.traits.ACAQDiscriminator;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.*;

/**
 * A data slot holds an {@link ACAQData} instance.
 * Slots are part of an {@link ACAQAlgorithm}
 * @param <T>
 */
public abstract class ACAQDataSlot<T extends ACAQData> implements TableModel {
    private ACAQAlgorithm algorithm;
    private String name;
    private Class<T> acceptedDataType;
    private SlotType slotType;
    private Path storagePath;
    private boolean uniqueData = true;

    private ArrayList<T> data = new ArrayList<>();
    private List<ACAQTraitDeclaration> annotationColumns = new ArrayList<>();
    private Map<ACAQTraitDeclaration, ArrayList<ACAQTrait>> annotations = new HashMap<>();

    public ACAQDataSlot(ACAQAlgorithm algorithm, SlotType slotType, String name, Class<T> acceptedDataType) {
        this.algorithm = algorithm;
        this.name = name;
        this.slotType = slotType;
        this.acceptedDataType = acceptedDataType;
    }

    public Class<? extends ACAQData> getAcceptedDataType() {
        return acceptedDataType;
    }

    public boolean accepts(ACAQData data) {
        if(data == null)
            throw new NullPointerException("Data slots cannot accept null data!");
        return acceptedDataType.isAssignableFrom(data.getClass());
    }

    /**
     * Gets the data stored in a specific row
     * @param row
     * @return
     */
    public T getData(int row) {
        return data.get(row);
    }

    /**
     * Gets the list of annotations for a specific data row
     * @param row
     * @return
     */
    public List<ACAQTrait> getAnnotations(int row) {
        List<ACAQTrait> result = new ArrayList<>();
        for (ACAQTraitDeclaration declaration : annotationColumns) {
            ACAQTrait trait = annotations.get(declaration).get(row);
            if(trait != null)
                result.add(trait);
        }
        return result;
    }

    /**
     * Adds a data row
     * @param value
     * @param traits
     */
    public void addData(ACAQData value, List<ACAQTrait> traits) {
        if(!accepts(value))
            throw new IllegalArgumentException("Tried to add data of type " + value.getClass() + ", but slot only accepts " + acceptedDataType);
        if(uniqueData) {
            if(findRowWithTraits(traits) != -1) {
                uniqueData = false;
            }
        }
        data.add((T) value);
        for (ACAQTrait trait : traits) {
            if(!annotations.containsKey(trait.getDeclaration())) {
                annotationColumns.add(trait.getDeclaration());
                annotations.put(trait.getDeclaration(), new ArrayList<>());
            }
            annotations.get(trait.getDeclaration()).add(trait);
        }
    }

    private int findRowWithTraits(List<ACAQTrait> traits) {
        ACAQTraitDeclaration[] declarationMap = new ACAQTraitDeclaration[traits.size()];
        for(int i = 0; i < traits.size(); ++i) {
            int declarationIndex = annotationColumns.indexOf(traits.get(i).getDeclaration());
            if(declarationIndex == -1)
                return -1;
            declarationMap[i] = annotationColumns.get(declarationIndex);
        }
        for(int row = 0; row < data.size(); ++row) {
            boolean equal = true;
            for(int i = 0; i < traits.size(); ++i) {
                ACAQTraitDeclaration declaration = declarationMap[i];
                ACAQTrait rowTrait = annotations.get(declaration).get(row);
                if(!ACAQTrait.equals(traits.get(i), rowTrait)) {
                    equal = false;
                }
            }
            if(equal)
                return row;
        }
        return -1;
    }

    /**
     * Returns true if all rows are unique according to their traits
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
//        if(isOutput() && storagePath != null && data != null) {
//            data.saveTo(storagePath, getName());
//            data = null;
//        }
    }

    public boolean isInput() {
        switch(slotType) {
            case Input:
                return true;
            case Output:
                return false;
            default:
                throw new RuntimeException("Unknown slot type!");
        }
    }

    public boolean isOutput() {
        switch(slotType) {
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
//        if(isOutput() && storagePath != null && data != null) {
//            data.saveTo(storagePath, getName());
//        }
    }

    public enum SlotType {
        Input,
        Output
    }

    /**
     * Instantiates a slot.
     * This requires from the slot class that it has the same parameters as {@link ACAQDataSlot}, but without acceptedDataType
     * @param algorithm
     * @param definition
     * @return
     */
    public static ACAQDataSlot<?> createInstance(ACAQAlgorithm algorithm, ACAQSlotDefinition definition) {
        Constructor<?> constructor = ConstructorUtils.getMatchingAccessibleConstructor(definition.getSlotClass(), ACAQAlgorithm.class, SlotType.class, String.class);
        try {
            return (ACAQDataSlot<?>) constructor.newInstance(algorithm, definition.getSlotType(), definition.getName());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
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
        return null;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if(columnIndex == 0)
            return acceptedDataType;
        else {
            ACAQTraitDeclaration column = annotationColumns.get(columnIndex);
            if(column.isDiscriminator())
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
        if(columnIndex == 0) {
            return data.get(rowIndex);
        }
        else {
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
}
