package org.hkijena.jipipe.api.history;

import com.google.common.eventbus.EventBus;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class JIPipeDummyGraphHistoryJournal implements JIPipeHistoryJournal {

    private final EventBus eventBus = new EventBus();

    @Override
    public void snapshot(String name, String description, UUID compartment, Icon icon) {

    }

    @Override
    public List<JIPipeHistoryJournalSnapshot> getSnapshots() {
        return Collections.emptyList();
    }

    @Override
    public boolean goToSnapshot(JIPipeHistoryJournalSnapshot snapshot, UUID compartment) {
        return false;
    }

    @Override
    public JIPipeHistoryJournalSnapshot getUndoSnapshot() {
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

    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }
}
