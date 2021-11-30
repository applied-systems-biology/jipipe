package org.hkijena.jipipe.api.history;

import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Base class for all journals that track the history of projects or graphs
 */
public interface JIPipeHistoryJournal {
    /**
     * Creates a new snapshot in the history journal
     * @param name the name of the snapshot
     * @param description the description of the snapshot
     * @param compartment the compartment. can be null.
     * @param icon an icon. can be null.
     */
    void snapshot(String name, String description, UUID compartment, Icon icon);

    /**
     * Snapshot before cutting a compartment
     * @param nodes the nodes
     * @param compartment the compartment. can be null.
     */
    default void snapshotBeforeCutNodes(Set<JIPipeGraphNode> nodes, UUID compartment) {
        if(nodes.size() == 1) {
            snapshot("Cut node",
                    "Node <code>" + nodes.iterator().next().getDisplayName() + "</code>",
                    compartment,
                    UIUtils.getIconFromResources("actions/edit-cut.png"));
        }
        else {
            snapshot("Cut node",
                    "Nodes <ul>" + nodes.stream().map(n -> "<li><code>" + n.getDisplayName() + "</li></code>").collect(Collectors.joining()) + "</ul>",
                    compartment,
                    UIUtils.getIconFromResources("actions/edit-cut.png"));
        }
    }

    /**
     * Snapshot before cutting a compartment
     * @param compartment the compartment
     */
    default void snapshotBeforeCutCompartment(JIPipeProjectCompartment compartment) {
        snapshot("Cut compartment",
                "Compartment <code>" + compartment.getName() + "</code>",
                compartment.getProjectCompartmentUUID(),
                UIUtils.getIconFromResources("actions/edit-cut.png"));
    }

    /**
     * Snapshot before disconnecting two slots
     * @param slot the slot
     * @param compartment the compartment. can be null.
     */
    default void snapshotBeforeDisconnectAll(JIPipeDataSlot slot, UUID compartment) {
        snapshot("Disconnect all slots",
                "Disconnecting all edges from <code>" + slot.getDisplayName() + "</code>",
                compartment,
                UIUtils.getIconFromResources("actions/dialog-close.png"));
    }

    /**
     * Snapshot before disconnecting two slots
     * @param source the source
     * @param target the target
     * @param compartment the compartment. can be null.
     */
    default void snapshotBeforeDisconnect(JIPipeDataSlot source, JIPipeDataSlot target, UUID compartment) {
        snapshot("Disconnect slots",
                "Disconnecting <code>" + source.getDisplayName() + "</code>" + " and <code>" + target.getDisplayName() + "</code>",
                compartment,
                UIUtils.getIconFromResources("actions/dialog-close.png"));
    }

    /**
     * Snapshot before connecting two slots
     * @param source the source
     * @param target the target
     * @param compartment the compartment. can be null.
     */
    default void snapshotBeforeConnect(JIPipeDataSlot source, JIPipeDataSlot target, UUID compartment) {
        snapshot("Connect slots",
                "Connecting <code>" + source.getDisplayName() + "</code>" + " and <code>" + target.getDisplayName() + "</code>",
                compartment,
                UIUtils.getIconFromResources("actions/plug.png"));
    }

    /**
     * Snapshot before adding a slot
     * @param node the node
     * @param info the slot info
     * @param compartment the compartment. can be null.
     */
    default void snapshotBeforeAddSlot(JIPipeGraphNode node, JIPipeDataSlotInfo info, UUID compartment) {
        snapshot("Add slot",
                "Add " + info.getSlotType().name().toLowerCase() + " slot " + info.getName() + " into " + node.getDisplayName(),
                compartment,
                UIUtils.getIconFromResources("actions/list-add.png"));
    }

    /**
     * Snapshot before adding a slot
     * @param node the node
     * @param info the slot info
     * @param compartment the compartment. can be null.
     */
    default void snapshotBeforeRemoveSlot(JIPipeGraphNode node, JIPipeDataSlotInfo info, UUID compartment) {
        snapshot("Remove slot",
                "Remove " + info.getSlotType().name().toLowerCase() + " slot " + info.getName() + " from " + node.getDisplayName(),
                compartment,
                UIUtils.getIconFromResources("actions/delete.png"));
    }

    /**
     * Snapshot before adding a slot
     * @param slot the slot
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
     * @param slot the slot
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
     * @param slot the slot
     */
    default void snapshotBeforeEditSlot(JIPipeDataSlot slot, UUID compartment) {
        snapshot("Edit slot", "Edited slot " + slot.getDisplayName(), compartment, UIUtils.getIconFromResources("actions/document-edit.png"));
    }

    /**
     * Create a snapshot for adding a node
     * @param nodes the nodes
     * @param compartment the compartment. can be null.
     */
    default void snapshotBeforeAddNodes(Collection<JIPipeGraphNode> nodes, UUID compartment) {
        snapshot("Add " + nodes.size() + " nodes",
                "Added following nodes into the graph: <ul>" +  nodes.stream().map(s -> "<li><code>" + s.getName() + "</code></li>").collect(Collectors.joining()) + "</ul>",
                compartment,
                UIUtils.getIconFromResources("actions/list-add.png"));
    }

    /**
     * Create a snapshot for adding a node
     * @param nodes the nodes
     * @param compartment the compartment. can be null.
     */
    default void snapshotBeforePasteNodes(Collection<JIPipeGraphNode> nodes, UUID compartment) {
        snapshot("Paste " + nodes.size() + " nodes",
                "Added following nodes into the graph: <ul>" +  nodes.stream().map(s -> "<li><code>" + s.getName() + "</code></li>").collect(Collectors.joining()) + "</ul>",
                compartment,
                UIUtils.getIconFromResources("actions/edit-paste.png"));
    }

    /**
     * Create a snapshot for adding a node
     * @param node the node
     * @param compartment the compartment. can be null.
     */
    default void snapshotBeforeAddNode(JIPipeGraphNode node, UUID compartment) {
        snapshot("Add node",
                "Added a node <code>" + node.getName() + "</code> into the graph.",
                compartment,
                UIUtils.getIconFromResources("actions/list-add.png"));
    }

    /**
     * Create a snapshot for adding a node
     * @param compartment the compartment.
     */
    default void snapshotBeforeAddCompartment(String compartment) {
        snapshot("Add node",
                "Added a compartment <code>" + compartment + "</code>.",
                null,
                UIUtils.getIconFromResources("actions/list-add.png"));
    }

    /**
     * Create a snapshot for adding a node
     * @param nodes the nodes
     * @param compartment the compartment. can be null.
     */
    default void snapshotBeforeRemoveNodes(Collection<JIPipeGraphNode> nodes, UUID compartment) {
        snapshot("Remove " + nodes.size() + " nodes",
                "Removed following nodes from the graph: <ul>" +  nodes.stream().map(s -> "<li><code>" + s.getName() + "</code></li>") + "</ul>",
                compartment,
                UIUtils.getIconFromResources("actions/delete.png"));
    }

    /**
     * Create a snapshot for moving nodes
     * @param nodes the nodes
     * @param compartment the compartment
     */
    default void snapshotBeforeMoveNodes(Set<JIPipeGraphNode> nodes, UUID compartment) {
        snapshot("Moved " + nodes.size() + " nodes",
                "Moved following nodes: <ul>" +  nodes.stream().map(s -> "<li><code>" + s.getName() + "</code></li>") + "</ul>",
                compartment,
                UIUtils.getIconFromResources("actions/transform-move.png"));
    }

    /**
     * Create a snapshot for removing a node
     * @param node the node
     * @param compartment the compartment. can be null.
     */
    default void snapshotBeforeRemoveNode(JIPipeGraphNode node, UUID compartment) {
        snapshot("Remove node",
                "Removed a node <code>" + node.getName() + "</code> from the graph.",
                compartment,
                UIUtils.getIconFromResources("actions/delete.png"));
    }

    /**
     * Create a snapshot for removing a node
     * @param compartment the node
     */
    default void snapshotBeforeRemoveCompartment(JIPipeProjectCompartment compartment) {
        snapshot("Remove compartment",
                "Removed compartment <code>" + compartment.getName() + "</code> from the graph.",
                compartment.getProjectCompartmentUUID(),
                UIUtils.getIconFromResources("actions/delete.png"));
    }

    /**
     * Redo the last undo operation
     * @param compartment the compartment. can be null.
     * @return if redo was successful
     */
    boolean redo(UUID compartment);

    /**
     * Undo the last undo operation
     * @param compartment the compartment. can be null.
     * @return if undo was successful
     */
    boolean undo(UUID compartment);


}
