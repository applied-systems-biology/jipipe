/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.acaq5.api.history;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.events.GraphHistoryChangedEvent;

import java.util.Stack;

/**
 * Manages a Undo-history
 */
public class ACAQAlgorithmGraphHistory {

    private final EventBus eventBus = new EventBus();
    private final Stack<ACAQAlgorithmGraphHistorySnapshot> snapshots = new Stack<>();
    private int cursor = -1;

    /**
     * Adds a snapshot into the stack
     *
     * @param snapshot the snapshot that was created before applying the operation
     */
    public void addSnapshotBefore(ACAQAlgorithmGraphHistorySnapshot snapshot) {
        while (cursor < snapshots.size() - 1) {
            snapshots.pop();
        }

        snapshots.push(snapshot);
        ++cursor;
        eventBus.post(new GraphHistoryChangedEvent(this));
    }

    /**
     * Removes a snapshot.
     * This will automatically remove all following snapshots
     *
     * @param snapshot the snapshot
     */
    public void removeSnapshot(ACAQAlgorithmGraphHistorySnapshot snapshot) {
        int index = snapshots.indexOf(snapshot);
        if (index >= 0) {
            if (snapshots.size() > index) {
                snapshots.subList(index, snapshots.size()).clear();
            }
            eventBus.post(new GraphHistoryChangedEvent(this));
            cursor = Math.min(cursor, index - 1);
        }
    }

    /**
     * Reverts the last operation
     *
     * @return if successful
     */
    public boolean undo() {
        if (cursor >= 0) {
            snapshots.get(cursor).undo();
            --cursor;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Repeats the last reverted operation
     *
     * @return if successful
     */
    public boolean redo() {
        if (cursor < snapshots.size() - 1) {
            snapshots.get(cursor + 1).redo();
            ++cursor;
            return true;
        } else {
            return false;
        }
    }

    public EventBus getEventBus() {
        return eventBus;
    }
}
