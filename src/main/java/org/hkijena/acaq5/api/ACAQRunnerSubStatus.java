package org.hkijena.acaq5.api;

import java.util.ArrayList;
import java.util.List;

/**
 * A nestable
 */
public class ACAQRunnerSubStatus {
    private List<String> categories = new ArrayList<>();

    /**
     * Adds a new sub-category to the current task.
     * It will be rendered with an additional pipe
     * @param category the sub-category
     * @return status for current category
     */
    public ACAQRunnerSubStatus resolve(String category) {
        ACAQRunnerSubStatus sub = new ACAQRunnerSubStatus();
        sub.categories = new ArrayList<>(categories);
        sub.categories.add(category);
        return sub;
    }

    @Override
    public String toString() {
        return String.join(" | ", categories);
    }
}
