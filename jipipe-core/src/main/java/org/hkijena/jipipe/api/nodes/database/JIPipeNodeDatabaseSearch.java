package org.hkijena.jipipe.api.nodes.database;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeSlotType;

import java.util.List;
import java.util.Set;

public interface JIPipeNodeDatabaseSearch {
    /**
     * Used to report to the engine which entry was selected by the user
     * @param text the text
     * @param role the role
     * @param allowExisting allow selecting existing nodes
     * @param allowNew allow creating new nodes
     * @param pinnedIds bookmarked node IDs
     * @param userSelected the entry the user selected
     */
    void confirmQuery(String text, JIPipeNodeDatabasePipelineVisibility role, boolean allowExisting, boolean allowNew, Set<String> pinnedIds, JIPipeNodeDatabaseEntry userSelected);

    /**
     * Queries ranked database entries
     * @param text the text
     * @param role the role
     * @param allowExisting allow selecting existing nodes
     * @param allowNew allow creating new nodes
     * @param pinnedIds bookmarked node IDs
     * @return ranked entries
     */
    List<JIPipeNodeDatabaseEntry> query(String text, JIPipeNodeDatabasePipelineVisibility role, boolean allowExisting, boolean allowNew, Set<String> pinnedIds);

    /**
     * Used to report to the engine which entry was selected by the user
     * @param text the text
     * @param role the role
     * @param allowExisting allow selecting existing nodes
     * @param allowNew allow creating new nodes
     * @param targetSlotType the type of the slot the user selected as reference
     * @param targetDataType the data type of the slot the user selected as reference
     * @param userSelected the entry the user selected
     */
    void confirmQuery(String text, JIPipeNodeDatabasePipelineVisibility role, boolean allowExisting, boolean allowNew, JIPipeSlotType targetSlotType, Class<? extends JIPipeData> targetDataType, JIPipeNodeDatabaseEntry userSelected);

    /**
     * Queries ranked database entries
     * @param text the text
     * @param role the role
     * @param allowExisting allow selecting existing nodes
     * @param allowNew allow creating new nodes
     * @param targetSlotType the type of the slot the user selected as reference
     * @param targetDataType the data type of the slot the user selected as reference
     * @return ranked entries
     */
    List<JIPipeNodeDatabaseEntry> query(String text, JIPipeNodeDatabasePipelineVisibility role, boolean allowExisting, boolean allowNew, JIPipeSlotType targetSlotType, Class<? extends JIPipeData> targetDataType);
}
