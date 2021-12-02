package org.hkijena.jipipe.ui.history;

import org.hkijena.jipipe.api.history.JIPipeHistoryJournalSnapshot;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.time.LocalDateTime;

/**
 * A snapshot that indicates the current state. It is used inside UI classes.
 */
public class CurrentStateSnapshot implements JIPipeHistoryJournalSnapshot {
    @Override
    public LocalDateTime getCreationTime() {
        return LocalDateTime.now();
    }

    @Override
    public String getName() {
        return "Current state";
    }

    @Override
    public String getDescription() {
        return "The current state of the pipeline";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/help-info.png");
    }

    @Override
    public boolean restore() {
        return false;
    }
}
