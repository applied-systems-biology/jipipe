package org.hkijena.jipipe.api.history;

import javax.swing.*;
import java.util.UUID;

public class JIPipeDedicatedGraphHistoryJournal implements JIPipeHistoryJournal {
    @Override
    public void snapshot(String name, String description, UUID compartment, Icon icon) {

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
