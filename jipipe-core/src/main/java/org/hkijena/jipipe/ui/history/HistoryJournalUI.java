package org.hkijena.jipipe.ui.history;

import org.hkijena.jipipe.api.history.JIPipeHistoryJournal;
import org.hkijena.jipipe.api.history.JIPipeHistoryJournalSnapshot;

import javax.swing.*;
import java.awt.*;

public class HistoryJournalUI extends JPanel {
    private final JIPipeHistoryJournal historyJournal;
    private JList<JIPipeHistoryJournalSnapshot> snapshotJList;

    public HistoryJournalUI(JIPipeHistoryJournal historyJournal) {
        this.historyJournal = historyJournal;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        snapshotJList = new JList<>();

    }

    public JIPipeHistoryJournal getHistoryJournal() {
        return historyJournal;
    }
}
