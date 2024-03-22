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

package org.hkijena.jipipe.desktop.app.history;

import org.hkijena.jipipe.api.history.JIPipeHistoryJournal;
import org.hkijena.jipipe.api.history.JIPipeHistoryJournalSnapshot;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopHistoryJournalUI extends JPanel implements JIPipeHistoryJournal.HistoryChangedEventListener {
    private final JIPipeHistoryJournal historyJournal;
    private JList<JIPipeHistoryJournalSnapshot> snapshotJList;

    public JIPipeDesktopHistoryJournalUI(JIPipeHistoryJournal historyJournal) {
        this.historyJournal = historyJournal;
        initialize();
        reloadList();
        historyJournal.getHistoryChangedEventEmitter().subscribeWeak(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        snapshotJList = new JList<>();
        snapshotJList.setCellRenderer(new JIPipeDesktopHistorySnapshotListCellRenderer());
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
            if (snapshot != null && !(snapshot instanceof JIPipeDesktopCurrentStateSnapshot)) {
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
            currentSnapshot = new JIPipeDesktopCurrentStateSnapshot();
            model.add(0, currentSnapshot);
        }
        snapshotJList.setModel(model);
        snapshotJList.setSelectedValue(currentSnapshot, true);
    }

    public JIPipeHistoryJournal getHistoryJournal() {
        return historyJournal;
    }

    @Override
    public void onHistoryChangedEvent(JIPipeHistoryJournal.HistoryChangedEvent event) {
        reloadList();
    }
}
