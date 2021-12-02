package org.hkijena.jipipe.api.history;

import javax.swing.*;
import java.time.LocalDateTime;

public interface JIPipeHistoryJournalSnapshot {
    LocalDateTime getCreationTime();
    String getName();
    String getDescription();
    Icon getIcon();
    boolean restore();
}
