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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.extensions.settings.VirtualDataSettings;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Path;

/**
 * A data slot holds an {@link JIPipeData} instance.
 * Slots are part of an {@link JIPipeGraphNode}
 */
public class JIPipeDataSlot extends JIPipeDataTable {
    private JIPipeGraphNode node;
    private JIPipeDataSlotInfo info;
    private String name;
    private JIPipeSlotType slotType;
    private Path storagePath;

    /**
     * Creates a new slot
     *
     * @param info the slot definition
     * @param node The algorithm that contains the slot
     */
    public JIPipeDataSlot(JIPipeDataSlotInfo info, JIPipeGraphNode node) {
        super(info.getDataClass());
        this.info = info;
        this.node = node;
        this.name = info.getName();
        this.slotType = info.getSlotType();
    }

    public JIPipeDataSlot(JIPipeDataSlot other, boolean shallow, JIPipeProgressInfo progressInfo) {
        super(other, shallow, progressInfo);
    }

    /**
     * Creates a new input slot that contains only one data item
     *
     * @param data the data
     * @return the slot
     */
    public static JIPipeDataSlot createSingletonSlot(JIPipeData data, JIPipeGraphNode node) {
        JIPipeDataSlot slot = new JIPipeDataSlot(new JIPipeDataSlotInfo(data.getClass(),
                JIPipeSlotType.Input,
                "Data",
                null), node);
        slot.addData(data, new JIPipeProgressInfo());
        return slot;
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
        String nodeName = node != null ? node.getName() : "";
        if (!StringUtils.isNullOrEmpty(info.getCustomName()))
            return info.getCustomName() + " [" + getName() + "] " + " (" + nodeName + ")";
        else
            return getName() + " (" + nodeName + ")";
    }

    /**
     * @return The node that contains the slot
     */
    public JIPipeGraphNode getNode() {
        return node;
    }

    /**
     * Reassigns the node
     *
     * @param node the node
     */
    public void setNode(JIPipeGraphNode node) {
        this.node = node;
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
                saveToStoragePath(basePath, saveProgress);
            }
        } else {
            saveToStoragePath(basePath, saveProgress);
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
     * Info: This method only works for output slots and saves to the storage path.
     *
     * @param basePath     the base path to where all results are stored relative to. If null, there is no base path
     * @param saveProgress the progress for saving
     */
    public void saveToStoragePath(Path basePath, JIPipeProgressInfo saveProgress) {
        if (isOutput() && storagePath != null) {
            save(storagePath, basePath, saveProgress);
        }
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
        return String.format("%s: %s (%d rows, %d annotation columns, %d data annotation columns)", getSlotType(), getName(), getRowCount(), getAnnotationColumns().size(), getDataAnnotationColumns().size());
    }

    public boolean isVirtual() {
        return info.isVirtual();
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
            makeDataNonVirtual(progressInfo, false);
        }
    }
}
