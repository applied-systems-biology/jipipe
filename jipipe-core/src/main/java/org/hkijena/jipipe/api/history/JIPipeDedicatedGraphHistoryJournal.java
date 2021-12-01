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
        return false;
    }

    @Override
    public boolean undo(UUID compartment) {
        return false;
    }

    public JIPipeGraph getGraph() {
        return graph;
    }

    private void onWorkerFinished(Snapshot snapshot) {
        currentWorker = null;
        snapshots.add(snapshot);
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
            return new Snapshot(name, description, icon, copy);
        }

        @Override
        protected void done() {
            try {
                historyJournal.onWorkerFinished(get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Stores all information about the state of a
     */
    public static class Snapshot implements JIPipeHistoryJournalSnapshot {
        private final String name;
        private final String description;
        private final Icon icon;
        private final JIPipeGraph graph;

        public Snapshot(String name, String description, Icon icon, JIPipeGraph graph) {
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

        public JIPipeGraph getGraph() {
            return graph;
        }
    }
}
