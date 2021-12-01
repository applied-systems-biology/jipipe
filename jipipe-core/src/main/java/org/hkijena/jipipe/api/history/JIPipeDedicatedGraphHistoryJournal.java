package org.hkijena.jipipe.api.history;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class JIPipeDedicatedGraphHistoryJournal implements JIPipeHistoryJournal {

    private final EventBus eventBus = new EventBus();
    private final JIPipeGraph graph;
    private final List<Snapshot> snapshots = new ArrayList<>();
    private int currentSnapshotIndex = -1;
    private Worker currentWorker;

    public JIPipeDedicatedGraphHistoryJournal(JIPipeGraph graph) {
        this.graph = graph;
    }

    @Override
    public void snapshot(String name, String description, UUID compartment, Icon icon) {
        if(currentWorker != null) {
            try {
                currentWorker.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        currentWorker = new Worker(this, name, description, icon);
        currentWorker.execute();
    }

    @Override
    public List<JIPipeHistoryJournalSnapshot> getSnapshots() {
        return ImmutableList.copyOf(snapshots);
    }

    @Override
    public boolean redo(UUID compartment) {
        int redoIndex = currentSnapshotIndex + 1;
        if(redoIndex >= 0 && redoIndex < snapshots.size()) {
            return goToSnapshot(snapshots.get(redoIndex), compartment);
        }
        else {
            return false;
        }
    }

    @Override
    public boolean undo(UUID compartment) {
        int undoIndex = currentSnapshotIndex;
        if(undoIndex < snapshots.size() && undoIndex >= 0) {
            return goToSnapshot(snapshots.get(undoIndex), compartment);
        }
        else {
            return false;
        }
    }

    @Override
    public boolean goToSnapshot(JIPipeHistoryJournalSnapshot snapshot, UUID compartment) {
        int targetIndex = snapshots.indexOf((Snapshot)snapshot);
        if(targetIndex == -1) {
            return false;
        }
        if(snapshot.restore()) {
            currentSnapshotIndex = targetIndex - 1;
            return true;
        }
        return false;
    }

    public JIPipeGraph getGraph() {
        return graph;
    }

    /**
     * Removes all snapshots that would be triggered by a "redo"
     */
    private void clearRedoStack() {
        while(snapshots.size() > (currentSnapshotIndex + 1)) {
            snapshots.remove(snapshots.size() - 1);
        }
    }

    /**
     * Adds a snapshot
     * @param snapshot the snapshot
     */
    private void addSnapshot(Snapshot snapshot) {
        currentWorker = null;
        clearRedoStack();
        snapshots.add(snapshot);
        currentSnapshotIndex = snapshots.size() - 1;
        getEventBus().post(new ChangedEvent(this));
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    public static class Worker extends SwingWorker<Snapshot, Object> {

        private final JIPipeDedicatedGraphHistoryJournal historyJournal;
        private final String name;
        private final String description;
        private final Icon icon;

        public Worker(JIPipeDedicatedGraphHistoryJournal historyJournal, String name, String description, Icon icon) {
            this.historyJournal = historyJournal;
            this.name = name;
            this.description = description;
            this.icon = icon;
        }

        @Override
        protected Snapshot doInBackground() throws Exception {
            JIPipeGraph copy = new JIPipeGraph(historyJournal.graph);
            return new Snapshot(historyJournal, name, description, icon, copy);
        }

        @Override
        protected void done() {
            try {
                historyJournal.addSnapshot(get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Stores all information about the state of a
     */
    public static class Snapshot implements JIPipeHistoryJournalSnapshot {
        private final JIPipeDedicatedGraphHistoryJournal historyJournal;
        private final String name;
        private final String description;
        private final Icon icon;
        private final JIPipeGraph graph;

        public Snapshot(JIPipeDedicatedGraphHistoryJournal historyJournal, String name, String description, Icon icon, JIPipeGraph graph) {
            this.historyJournal = historyJournal;
            this.name = name;
            this.description = description;
            this.icon = icon;
            this.graph = graph;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public Icon getIcon() {
            return icon;
        }

        @Override
        public boolean restore() {
            historyJournal.getGraph().replaceWith(new JIPipeGraph(graph));
            return true;
        }

        public JIPipeGraph getGraph() {
            return graph;
        }

        public JIPipeDedicatedGraphHistoryJournal getHistoryJournal() {
            return historyJournal;
        }
    }
}
