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

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeProjectRun;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteDataStorage;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Path;

/**
 * A data slot holds an {@link JIPipeData} instance.
 * Slots are part of an {@link JIPipeGraphNode}
 */
public abstract class JIPipeDataSlot extends JIPipeDataTable {

    /**
     * To be used with getLocation().
     * Returns the node name if a node is set. Otherwise the default value is returned.
     */
    public static final String LOCATION_KEY_NODE_NAME = "jipipe:node:name";
    /**
     * To be used with getLocation().
     * Returns the node display name if a node is set. Otherwise the default value is returned.
     */
    public static final String LOCATION_KEY_NODE_DISPLAY_NAME = "jipipe:node:display-name";
    /**
     * To be used with getLocation().
     * Returns the slot name.
     */
    public static final String LOCATION_KEY_SLOT_NAME = "jipipe:slot:name";
    /**
     * To be used with getLocation().
     * Returns the slot display name (contains the node name, and the slot's custom and internal name)
     */
    public static final String LOCATION_KEY_SLOT_DISPLAY_NAME = "jipipe:slot:display-name";
    /**
     * To be used with getLocation().
     * Returns "Input" if the slot is and input. Returns "Output" if the slot is an output.
     */
    public static final String LOCATION_KEY_SLOT_IO = "jipipe:slot:io";

    private JIPipeGraphNode node;
    private JIPipeDataSlotInfo info;
    private String name;
    private JIPipeSlotType slotType;
    private Path slotStoragePath;

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
    public static JIPipeInputDataSlot createSingletonSlot(JIPipeData data, JIPipeGraphNode node) {
        JIPipeInputDataSlot slot = new JIPipeInputDataSlot(new JIPipeDataSlotInfo(data.getClass(),
                JIPipeSlotType.Input,
                "Data",
                ""), node);
        slot.addData(data, new JIPipeProgressInfo());
        return slot;
    }

    @Override
    public String getLocation(String key, String defaultValue) {
        if (getNode() != null && LOCATION_KEY_NODE_NAME.equals(key)) {
            return getNode().getName();
        }
        if (getNode() != null && LOCATION_KEY_NODE_DISPLAY_NAME.equals(key)) {
            return getNode().getDisplayName();
        }
        if (LOCATION_KEY_SLOT_NAME.equals(key)) {
            return getName();
        }
        if (LOCATION_KEY_SLOT_DISPLAY_NAME.equals(key)) {
            return getDisplayName();
        }
        if (LOCATION_KEY_SLOT_IO.equals(key)) {
            return isInput() ? "Input" : "Output";
        }
        return super.getLocation(key, defaultValue);
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
     * @param saveProgress progress that reports the saving
     */
    public void flush(JIPipeProgressInfo saveProgress) {
        if (getNode() instanceof JIPipeAlgorithm) {
            if (getInfo().isSaveOutputs()) {
                exportToSlotStoragePath(saveProgress);
            }
        } else {
            exportToSlotStoragePath(saveProgress);
        }
        destroyData();
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
    public Path getSlotStoragePath() {
        return slotStoragePath;
    }

    /**
     * Sets storage path that is used during running the algorithm for saving the results
     *
     * @param slotStoragePath Data storage paths
     */
    public void setSlotStoragePath(Path slotStoragePath) {
        this.slotStoragePath = slotStoragePath;
    }

    /**
     * Gets the storage path of a data row from a result. This is not used during project creation.
     *
     * @param index row index
     * @return path where the row's data is stored
     */
    public Path getRowStoragePath(int index) {
        return slotStoragePath.resolve("" + index);
    }

    /**
     * Saves the data to the storage path
     * Info: This method only works for output slots and saves to the storage path.
     *
     * @param saveProgress the progress for saving
     */
    public void exportToSlotStoragePath(JIPipeProgressInfo saveProgress) {
        if (isOutput() && slotStoragePath != null) {
            exportData(new JIPipeFileSystemWriteDataStorage(saveProgress, slotStoragePath), saveProgress);
        }
    }

    public JIPipeDataSlotInfo getInfo() {
        return info;
    }

    /**
     * Only valid for slots generated by a {@link JIPipeProjectRun}.
     * Returns the path to the data table file
     *
     * @return the data table path
     */
    public Path getStorageDataTablePath() {
        return getSlotStoragePath().resolve("data-table.json");
    }

    /**
     * Only valid for slots generated by a {@link JIPipeProjectRun}.
     * Returns the {@link JIPipeDataTableMetadata} from the data-table.json file
     *
     * @return the data table
     */
    public JIPipeDataTableMetadata getStorageDataTable() {
        return JIPipeDataTableMetadata.loadFromJson(getStorageDataTablePath());
    }

    @Override
    public String toString() {
        return String.format("%s: %s (%d rows, %d annotation columns, %d data annotation columns)", getSlotType(), getName(), getRowCount(), getTextAnnotationColumns().size(), getDataAnnotationColumns().size());
    }

    public String getDescription() {
        return getInfo().getDescription();
    }
}
