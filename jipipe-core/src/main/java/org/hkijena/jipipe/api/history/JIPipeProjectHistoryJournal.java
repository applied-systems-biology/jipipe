package org.hkijena.jipipe.api.history;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeProject;

import javax.swing.*;
import java.util.List;
import java.util.UUID;

public class JIPipeProjectHistoryJournal implements JIPipeHistoryJournal {
    private final EventBus eventBus = new EventBus();
    private final JIPipeProject project;

    public JIPipeProjectHistoryJournal(JIPipeProject project) {
        this.project = project;
    }

    public JIPipeProject getProject() {
        return project;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public void snapshot(String name, String description, UUID compartment, Icon icon) {

    }

    @Override
    public List<JIPipeHistoryJournalSnapshot> getSnapshots() {
        return null;
    }

    @Override
    public boolean redo(UUID compartment) {
        return false;
    }

    @Override
    public boolean undo(UUID compartment) {
        return false;
    }
}
