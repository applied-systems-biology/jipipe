package org.hkijena.jipipe.ui.history;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.history.JIPipeHistoryJournal;
import org.hkijena.jipipe.api.history.JIPipeHistoryJournalSnapshot;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;

public class HistoryJournalUI extends JPanel {
    private final JIPipeHistoryJournal historyJournal;
    private JList<JIPipeHistoryJournalSnapshot> snapshotJList;

    public HistoryJournalUI(JIPipeHistoryJournal historyJournal) {
        this.historyJournal = historyJournal;
        initialize();
        reloadList();
        historyJournal.getEventBus().register(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        snapshotJList = new JList<>();
        snapshotJList.setCellRenderer(new JIPipeHistorySnapshotListCellRenderer());
        add(new JScrollPane(snapshotJList), BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton undoButton = new JButton("Undo", UIUtils.getIconFromResources("actions/edit-undo.png"));
        undoButton.addActionListener(e -> getHistoryJournal().undo(null));
        toolBar.add(undoButton);

        JButton redoButton = new JButton("Redo", UIUtils.getIconFromResources("actions/edit-redo.png"));
        redoButton.addActionListener(e -> getHistoryJournal().redo(null));
        toolBar.add(redoButton);

        JButton selectButton = new JButton("Go to", UIUtils.getIconFromResources("actions/view-calendar-time-spent.png"));
        selectButton.addActionListener(e -> {
            JIPipeHistoryJournalSnapshot snapshot = snapshotJList.getSelectedValue();
            if (snapshot != null && !(snapshot instanceof CurrentStateSnapshot)) {
                getHistoryJournal().goToSnapshot(snapshot, null);
            }
        });
        toolBar.add(selectButton);

        toolBar.add(Box.createHorizontalGlue());

        JButton clearButton = new JButton("Clear", UIUtils.getIconFromResources("actions/clear-brush.png"));
        clearButton.addActionListener(e -> clearSnapshots());
        toolBar.add(clearButton);

        JButton createSnapshotButton = new JButton("Create snapshot", UIUtils.getIconFromResources("actions/save.png"));
        createSnapshotButton.addActionListener(e -> createSnapshot());
        toolBar.add(createSnapshotButton);

        add(toolBar, BorderLayout.NORTH);
    }

    private void clearSnapshots() {
        if (JOptionPane.showConfirmDialog(this,
                "Do you really want to clear the journal?",
                "Clear journal",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
            getHistoryJournal().clear();
        }
    }

    private void createSnapshot() {
        historyJournal.snapshot("Manual snapshot", "Created via the Journal interface", null, UIUtils.getIconFromResources("actions/save.png"));
    }

    public void reloadList() {
        DefaultListModel<JIPipeHistoryJournalSnapshot> model = new DefaultListModel<>();
        JIPipeHistoryJournalSnapshot currentSnapshot = historyJournal.getCurrentSnapshot();
        for (JIPipeHistoryJournalSnapshot snapshot : historyJournal.getSnapshots()) {
            model.add(0, snapshot);
        }
        if (currentSnapshot == null) {
            currentSnapshot = new CurrentStateSnapshot();
            model.add(0, currentSnapshot);
        }
        snapshotJList.setModel(model);
        snapshotJList.setSelectedValue(currentSnapshot, true);
    }

    @Subscribe
    public void onHistoryJournalChanged(JIPipeHistoryJournal.ChangedEvent event) {
        reloadList();
    }

    public JIPipeHistoryJournal getHistoryJournal() {
        return historyJournal;
    }
}
