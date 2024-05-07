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

import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.plugins.settings.JIPipeHistoryJournalApplicationSettings;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JIPipeProjectHistoryJournal implements JIPipeHistoryJournal {
    private final HistoryChangedEventEmitter historyChangedEventEmitter = new HistoryChangedEventEmitter();
    private final JIPipeProject project;
    private final JIPipeHistoryJournalApplicationSettings settings;
    private final List<Snapshot> undoStack = new ArrayList<>();
    private final List<Snapshot> redoStack = new ArrayList<>();
    private Snapshot currentSnapshot;

    public JIPipeProjectHistoryJournal(JIPipeProject project) {
        this.project = project;
        this.settings = JIPipeHistoryJournalApplicationSettings.getInstance();
    }

    public JIPipeProject getProject() {
        return project;
    }

    @Override
    public void snapshot(String name, String description, UUID compartment, Icon icon) {
        if (settings.getMaxEntries() == 0) {
            return;
        }
        addSnapshot(new Snapshot(this,
                LocalDateTime.now(),
                name,
                description,
                icon,
                new JIPipeGraph(project.getGraph()),
                new JIPipeGraph(project.getCompartmentGraph())));
    }

    @Override
    public List<JIPipeHistoryJournalSnapshot> getSnapshots() {
        List<JIPipeHistoryJournalSnapshot> snapshots = new ArrayList<>(undoStack);
        for (int i = redoStack.size() - 1; i >= 0; i--) {
            Snapshot snapshot = redoStack.get(i);
            snapshots.add(snapshot);
        }
        return snapshots;
    }

    @Override
    public void clear() {
        undoStack.clear();
        redoStack.clear();
        currentSnapshot = null;
        historyChangedEventEmitter.emit(new HistoryChangedEvent(this));
    }

    @Override
    public boolean goToSnapshot(JIPipeHistoryJournalSnapshot snapshot, UUID compartment) {
        if (undoStack.contains((Snapshot) snapshot)) {
            boolean createSnapshot = redoStack.isEmpty();

            // If the redo stack was empty at the beginning, create a new snapshot
            if (createSnapshot) {
                JIPipeGraph copy = new JIPipeGraph(getProject().getGraph());
                JIPipeGraph compartmentGraph = new JIPipeGraph(project.getCompartmentGraph());
                redoStack.add(new Snapshot(this,
                        LocalDateTime.now(),
                        "Before undo",
                        "A snapshot of the current version",
                        UIUtils.getIconFromResources("actions/edit-undo.png"),
                        copy,
                        compartmentGraph));
            }

            // Shift other undo operations into the redo stack
            while (!undoStack.isEmpty()) {
                Snapshot pop = undoStack.remove(undoStack.size() - 1);
                redoStack.add(pop);
                if (pop == snapshot)
                    break;
            }

            currentSnapshot = (Snapshot) snapshot;

            historyChangedEventEmitter.emit(new HistoryChangedEvent(this));
            return currentSnapshot.restore();
        } else if (redoStack.contains((Snapshot) snapshot)) {
            // Shift other undo operations into the undo stack
            while (!redoStack.isEmpty()) {
                Snapshot pop = redoStack.remove(redoStack.size() - 1);
                undoStack.add(pop);
                if (pop == snapshot)
                    break;
            }
            currentSnapshot = (Snapshot) snapshot;

            historyChangedEventEmitter.emit(new HistoryChangedEvent(this));
            return currentSnapshot.restore();
        }

        return false;
    }

    @Override
    public JIPipeHistoryJournalSnapshot getCurrentSnapshot() {
        return currentSnapshot;
    }

    @Override
    public JIPipeHistoryJournalSnapshot getUndoSnapshot() {
        if (undoStack.isEmpty()) {
            return null;
        } else {
            for (int i = undoStack.size() - 1; i >= 0; i--) {
                if (undoStack.get(i) != currentSnapshot) {
                    return undoStack.get(i);
                }
            }
            return null;
        }
    }

    @Override
    public JIPipeHistoryJournalSnapshot getRedoSnapshot() {
        if (redoStack.isEmpty()) {
            return null;
        } else {
            for (int i = redoStack.size() - 1; i >= 0; i--) {
                if (redoStack.get(i) != currentSnapshot) {
                    return redoStack.get(i);
                }
            }
            return null;
        }
    }

    /**
     * Adds a snapshot
     *
     * @param snapshot the snapshot
     */
    private void addSnapshot(Snapshot snapshot) {
        redoStack.clear();
        if (settings.getMaxEntries() > 0) {
            while (undoStack.size() > settings.getMaxEntries()) {
                Snapshot removed = undoStack.remove(0);
                if (currentSnapshot == removed) {
                    currentSnapshot = null;
                }
            }
        }
        undoStack.add(snapshot);
        historyChangedEventEmitter.emit(new HistoryChangedEvent(this));
    }

    @Override
    public HistoryChangedEventEmitter getHistoryChangedEventEmitter() {
        return historyChangedEventEmitter;
    }

    /**
     * Stores all information about the state of a
     */
    public static class Snapshot implements JIPipeHistoryJournalSnapshot {
        private final JIPipeProjectHistoryJournal historyJournal;
        private final LocalDateTime creationTime;
        private final String name;
        private final String description;
        private final Icon icon;
        private final JIPipeGraph graph;
        private final JIPipeGraph compartmentGraph;

        public Snapshot(JIPipeProjectHistoryJournal historyJournal, LocalDateTime creationTime, String name, String description, Icon icon, JIPipeGraph graph, JIPipeGraph compartmentGraph) {
            this.historyJournal = historyJournal;
            this.creationTime = creationTime;
            this.name = name;
            this.description = description;
            this.icon = icon;
            this.graph = graph;
            this.compartmentGraph = compartmentGraph;
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
            historyJournal.getProject().getGraph().replaceWith(new JIPipeGraph(graph));
            historyJournal.getProject().getCompartmentGraph().replaceWith(new JIPipeGraph(compartmentGraph));
            historyJournal.getProject().rebuildCompartmentsFromGraph();
            return true;
        }

        public JIPipeGraph getGraph() {
            return graph;
        }

        public JIPipeProjectHistoryJournal getHistoryJournal() {
            return historyJournal;
        }

        public JIPipeGraph getCompartmentGraph() {
            return compartmentGraph;
        }

        @Override
        public LocalDateTime getCreationTime() {
            return creationTime;
        }
    }
}
