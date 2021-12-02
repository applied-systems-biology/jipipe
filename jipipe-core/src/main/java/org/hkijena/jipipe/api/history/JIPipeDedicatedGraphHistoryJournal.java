package org.hkijena.jipipe.api.history;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.extensions.settings.HistoryJournalSettings;

import javax.swing.*;
import java.time.LocalDateTime;
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
    private final HistoryJournalSettings settings;

    public JIPipeDedicatedGraphHistoryJournal(JIPipeGraph graph) {
        this.graph = graph;
        this.settings = HistoryJournalSettings.getInstance();
    }

    @Override
    public void snapshot(String name, String description, UUID compartment, Icon icon) {
        if(settings.getMaxEntries() == 0) {
            return;
        }
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
    public JIPipeHistoryJournalSnapshot getUndoSnapshot() {
        if(currentSnapshotIndex >= 0 && currentSnapshotIndex < snapshots.size()) {
            return snapshots.get(currentSnapshotIndex);
        }
        return null;
    }

    @Override
    public JIPipeHistoryJournalSnapshot getRedoSnapshot() {
        return null;
    }

    @Override
    public JIPipeHistoryJournalSnapshot getCurrentSnapshot() {
        return null;
    }

    @Override
    public void clear() {
        snapshots.clear();
        getEventBus().post(new ChangedEvent(this));
    }

    @Override
    public List<JIPipeHistoryJournalSnapshot> getSnapshots() {
        return ImmutableList.copyOf(snapshots);
    }

    @Override
    public boolean goToSnapshot(JIPipeHistoryJournalSnapshot snapshot, UUID compartment) {
        int targetIndex = snapshots.indexOf((Snapshot)snapshot);
        if(targetIndex == -1) {
            return false;
        }
        if(snapshot.restore()) {
            currentSnapshotIndex = targetIndex - 1;
            getEventBus().post(new ChangedEvent(this));
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

        if(settings.getMaxEntries() > 0) {
            while(snapshots.size() > settings.getMaxEntries()) {
                snapshots.remove(0);
            }
        }

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
            return new Snapshot(historyJournal, LocalDateTime.now(), name, description, icon, copy);
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
        private final LocalDateTime creationTime;
        private final String name;
        private final String description;
        private final Icon icon;
        private final JIPipeGraph graph;

        public Snapshot(JIPipeDedicatedGraphHistoryJournal historyJournal, LocalDateTime creationTime, String name, String description, Icon icon, JIPipeGraph graph) {
            this.historyJournal = historyJournal;
            this.creationTime = creationTime;
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

        @Override
        public LocalDateTime getCreationTime() {
            return creationTime;
        }
    }
}
