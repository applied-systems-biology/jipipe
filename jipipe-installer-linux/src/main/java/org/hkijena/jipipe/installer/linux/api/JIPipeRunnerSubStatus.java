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

package org.hkijena.jipipe.installer.linux.api;

import java.util.ArrayList;
import java.util.List;

/**
 * A nestable
 */
public class JIPipeRunnerSubStatus {
    private List<String> categories = new ArrayList<>();

    /**
     * Adds a new sub-category to the current task.
     * It will be rendered with an additional pipe
     *
     * @param category the sub-category
     * @return status for current category
     */
    public JIPipeRunnerSubStatus resolve(String category) {
        JIPipeRunnerSubStatus sub = new JIPipeRunnerSubStatus();
        sub.categories = new ArrayList<>(categories);
        sub.categories.add(category);
        return sub;
    }

    @Override
    public String toString() {
        return String.join(" | ", categories);
    }
}
