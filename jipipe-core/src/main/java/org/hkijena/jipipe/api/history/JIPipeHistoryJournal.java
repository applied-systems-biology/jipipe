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

package org.hkijena.jipipe.api.history;

import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Base class for all journals that track the history of projects or graphs
 */
public interface JIPipeHistoryJournal {

    /**
     * Creates a new snapshot in the history journal
     *
     * @param name        the name of the snapshot
     * @param description the description of the snapshot
     * @param compartment the compartment. can be null.
     * @param icon        an icon. can be null.
     */
    void snapshot(String name, String description, UUID compartment, Icon icon);

    /**
     * Gets the list of all snapshots.
     * The higher the index, the newer the snapshot.
     *
     * @return the list of snapshots
     */
    List<JIPipeHistoryJournalSnapshot> getSnapshots();

    /**
     * Snapshot before cutting a compartment
     *
     * @param nodes       the nodes
     * @param compartment the compartment. can be null.
     */
    default void snapshotBeforeCutNodes(Set<JIPipeGraphNode> nodes, UUID compartment) {
        if (nodes.size() == 1) {
            snapshot("Cut node",
                    "Node '" + nodes.iterator().next().getDisplayName() + "'",
                    compartment,
                    UIUtils.getIconFromResources("actions/edit-cut.png"));
        } else {
            snapshot("Cut node",
                    "Nodes " + nodes.stream().map(n -> "'" + n.getDisplayName() + "'").collect(Collectors.joining(", ")),
                    compartment,
                    UIUtils.getIconFromResources("actions/edit-cut.png"));
        }
    }

    /**
     * Snapshot before cutting a compartment
     *
     * @param compartment the compartment
     */
    default void snapshotBeforeCutCompartment(JIPipeProjectCompartment compartment) {
        snapshot("Cut compartment",
                "Compartment '" + compartment.getName() + "'",
                compartment.getProjectCompartmentUUID(),
                UIUtils.getIconFromResources("actions/edit-cut.png"));
    }

    /**
     * Snapshot before disconnecting two slots
     *
     * @param slot        the slot
     * @param compartment the compartment. can be null.
     */
    default void snapshotBeforeDisconnectAll(JIPipeDataSlot slot, UUID compartment) {
        snapshot("Disconnect all slots",
                "Disconnecting all edges from '" + slot.getDisplayName() + "'",
                compartment,
                UIUtils.getIconFromResources("actions/dialog-close.png"));
    }

    /**
     * Snapshot before disconnecting two slots
     *
     * @param source      the source
     * @param target      the target
     * @param compartment the compartment. can be null.
     */
    default void snapshotBeforeDisconnect(JIPipeDataSlot source, JIPipeDataSlot target, UUID compartment) {
        snapshot("Disconnect slots",
                "Disconnecting '" + source.getDisplayName() + "'" + " and '" + target.getDisplayName() + "'",
                compartment,
                UIUtils.getIconFromResources("actions/dialog-close.png"));
    }

    /**
     * Snapshot before connecting two slots
     *
     * @param source      the source
     * @param target      the target
     * @param compartment the compartment. can be null.
     */
    default void snapshotBeforeConnect(JIPipeDataSlot source, JIPipeDataSlot target, UUID compartment) {
        snapshot("Connect slots",
                "Connecting '" + source.getDisplayName() + "'" + " and '" + target.getDisplayName() + "'",
                compartment,
                UIUtils.getIconFromResources("actions/plug.png"));
    }

    /**
     * Snapshot before adding a slot
     *
     * @param node        the node
     * @param info        the slot info
     * @param compartment the compartment. can be null.
     */
    default void snapshotBeforeAddSlot(JIPipeGraphNode node, JIPipeDataSlotInfo info, UUID compartment) {
        snapshot("Add slot",
                "Add " + info.getSlotType().name().toLowerCase(Locale.ROOT) + " slot " + info.getName() + " into " + node.getDisplayName(),
                compartment,
                UIUtils.getIconFromResources("actions/list-add.png"));
    }

    /**
     * Snapshot before adding a slot
     *
     * @param node        the node
     * @param info        the slot info
     * @param compartment the compartment. can be null.
     */
    default void snapshotBeforeRemoveSlot(JIPipeGraphNode node, JIPipeDataSlotInfo info, UUID compartment) {
        snapshot("Remove slot",
                "Remove " + info.getSlotType().name().toLowerCase(Locale.ROOT) + " slot " + info.getName() + " from " + node.getDisplayName(),
                compartment,
                UIUtils.getIconFromResources("actions/delete.png"));
    }

    /**
     * Snapshot before adding a slot
     *
     * @param slot        the slot
     * @param compartment the compartment. can be null.
     */
    default void snapshotBeforeLabelSlot(JIPipeDataSlot slot, UUID compartment) {
        snapshot("Label slot",
                "Set the label of slot " + slot.getDisplayName(),
                compartment,
                UIUtils.getIconFromResources("actions/tag.png"));
    }

    /**
     * Snapshot before adding a slot
     *
     * @param slot        the slot
     * @param compartment the compartment. can be null.
     */
    default void snapshotBeforeMoveSlot(JIPipeDataSlot slot, UUID compartment) {
        snapshot("Move slot",
                "Move slot " + slot.getDisplayName(),
                compartment,
                UIUtils.getIconFromResources("actions/transform-move.png"));
    }

    /**
     * Create snapshot before a slot edit
     *
     * @param slot the slot
     */
    default void snapshotBeforeEditSlot(JIPipeDataSlot slot, UUID compartment) {
        snapshot("Edit slot", "Edited slot " + slot.getDisplayName(), compartment, UIUtils.getIconFromResources("actions/document-edit.png"));
    }

    /**
     * Create a snapshot for adding a node
     *
     * @param nodes       the nodes
     * @param compartment the compartment. can be null.
     */
    default void snapshotBeforeAddNodes(Collection<JIPipeGraphNode> nodes, UUID compartment) {
        snapshot("Add " + nodes.size() + " nodes",
                "Added following nodes into the graph: " + nodes.stream().map(s -> "'" + s.getName() + "'").collect(Collectors.joining(", ")),
                compartment,
                UIUtils.getIconFromResources("actions/list-add.png"));
    }

    /**
     * Create a snapshot for adding a node
     *
     * @param nodes       the nodes
     * @param compartment the compartment. can be null.
     */
    default void snapshotBeforePasteNodes(Collection<JIPipeGraphNode> nodes, UUID compartment) {
        snapshot("Paste " + nodes.size() + " nodes",
                "Added following nodes into the graph: " + nodes.stream().map(s -> "'" + s.getName() + "'").collect(Collectors.joining(", ")),
                compartment,
                UIUtils.getIconFromResources("actions/edit-paste.png"));
    }

    /**
     * Create a snapshot for adding a node
     *
     * @param node        the node
     * @param compartment the compartment. can be null.
     */
    default void snapshotBeforeAddNode(JIPipeGraphNode node, UUID compartment) {
        snapshot("Add node",
                "Added a node '" + node.getName() + "' into the graph.",
                compartment,
                UIUtils.getIconFromResources("actions/list-add.png"));
    }

    /**
     * Create a snapshot for adding a node
     *
     * @param compartment the compartment.
     */
    default void snapshotBeforeAddCompartment(String compartment) {
        snapshot("Add compartment",
                "Added a compartment '" + compartment + "'.",
                null,
                UIUtils.getIconFromResources("actions/list-add.png"));
    }

    /**
     * Create a snapshot for adding a node
     *
     * @param nodes       the nodes
     * @param compartment the compartment. can be null.
     */
    default void snapshotBeforeRemoveNodes(Collection<JIPipeGraphNode> nodes, UUID compartment) {
        snapshot("Remove " + nodes.size() + " nodes",
                "Removed following nodes from the graph: " + nodes.stream().map(s -> "'" + s.getName() + "'").collect(Collectors.joining(", ")),
                compartment,
                UIUtils.getIconFromResources("actions/delete.png"));
    }

    /**
     * Create a snapshot for moving nodes
     *
     * @param nodes       the nodes
     * @param compartment the compartment
     */
    default void snapshotBeforeMoveNodes(Set<JIPipeGraphNode> nodes, UUID compartment) {
        snapshot("Moved " + nodes.size() + " nodes",
                "Moved following nodes: " + nodes.stream().map(s -> "'" + s.getName() + "'").collect(Collectors.joining(", ")),
                compartment,
                UIUtils.getIconFromResources("actions/transform-move.png"));
    }

    /**
     * Create a snapshot for removing a node
     *
     * @param node        the node
     * @param compartment the compartment. can be null.
     */
    default void snapshotBeforeRemoveNode(JIPipeGraphNode node, UUID compartment) {
        snapshot("Remove node",
                "Removed a node '" + node.getName() + "' from the graph.",
                compartment,
                UIUtils.getIconFromResources("actions/delete.png"));
    }

    /**
     * Create a snapshot for removing a node
     *
     * @param compartment the node
     */
    default void snapshotBeforeRemoveCompartment(JIPipeProjectCompartment compartment) {
        snapshot("Remove compartment",
                "Removed compartment '" + compartment.getName() + "' from the graph.",
                compartment.getProjectCompartmentUUID(),
                UIUtils.getIconFromResources("actions/delete.png"));
    }

    /**
     * Redo the last undo operation
     *
     * @param compartment the compartment. can be null.
     * @return if redo was successful
     */
    default boolean redo(UUID compartment) {
        JIPipeHistoryJournalSnapshot snapshot = getRedoSnapshot();
        if (snapshot != null) {
            return goToSnapshot(snapshot, compartment);
        }
        return false;
    }

    /**
     * Undo the last undo operation
     *
     * @param compartment the compartment. can be null.
     * @return if undo was successful
     */
    default boolean undo(UUID compartment) {
        JIPipeHistoryJournalSnapshot snapshot = getUndoSnapshot();
        if (snapshot != null) {
            return goToSnapshot(snapshot, compartment);
        }
        return false;
    }

    /**
     * Attempts to go to a specified snapshot
     *
     * @param snapshot    the snapshot
     * @param compartment the compartment. can be null.
     * @return if the undo/redo was successful
     */
    boolean goToSnapshot(JIPipeHistoryJournalSnapshot snapshot, UUID compartment);

    /**
     * Gets the snapshot for the next undo operation
     *
     * @return the snapshot. can be null.
     */
    JIPipeHistoryJournalSnapshot getUndoSnapshot();

    /**
     * Gets the snapshot for the next redo operation
     *
     * @return the snapshot. can be null
     */
    JIPipeHistoryJournalSnapshot getRedoSnapshot();

    /**
     * Gets the snapshot that represents the current state of the data
     *
     * @return the snapshot. can be null
     */
    JIPipeHistoryJournalSnapshot getCurrentSnapshot();

    HistoryChangedEventEmitter getHistoryChangedEventEmitter();

    /**
     * Clears the journal
     */
    void clear();

    interface HistoryChangedEventListener {
        void onHistoryChangedEvent(HistoryChangedEvent event);
    }

    /**
     * Event when the history log was changed
     */
    class HistoryChangedEvent extends AbstractJIPipeEvent {
        private final JIPipeHistoryJournal historyJournal;

        public HistoryChangedEvent(JIPipeHistoryJournal historyJournal) {
            super(historyJournal);
            this.historyJournal = historyJournal;
        }

        public JIPipeHistoryJournal getHistoryJournal() {
            return historyJournal;
        }
    }

    class HistoryChangedEventEmitter extends JIPipeEventEmitter<HistoryChangedEvent, HistoryChangedEventListener> {

        @Override
        protected void call(HistoryChangedEventListener historyChangedEventListener, HistoryChangedEvent event) {
            historyChangedEventListener.onHistoryChangedEvent(event);
        }
    }

}
