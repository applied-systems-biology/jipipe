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

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class JIPipeDummyGraphHistoryJournal implements JIPipeHistoryJournal {

    private final HistoryChangedEventEmitter historyChangedEventEmitter = new HistoryChangedEventEmitter();

    @Override
    public HistoryChangedEventEmitter getHistoryChangedEventEmitter() {
        return historyChangedEventEmitter;
    }

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
}
