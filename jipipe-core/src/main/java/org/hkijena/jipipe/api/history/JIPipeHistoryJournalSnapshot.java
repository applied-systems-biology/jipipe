package org.hkijena.jipipe.api.history;

import javax.swing.*;

public interface JIPipeHistoryJournalSnapshot {
    String getName();
    String getDescription();
    Icon getIcon();
    boolean restore();
}
