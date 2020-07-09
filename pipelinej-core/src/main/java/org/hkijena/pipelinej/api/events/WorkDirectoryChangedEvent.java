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

package org.hkijena.pipelinej.api.events;

import java.nio.file.Path;

/**
 * Triggered when the work directory of a project or algorithm was changed
 */
public class WorkDirectoryChangedEvent {
    private Path workDirectory;

    /**
     * @param workDirectory the work directory
     */
    public WorkDirectoryChangedEvent(Path workDirectory) {
        this.workDirectory = workDirectory;
    }

    public Path getWorkDirectory() {
        return workDirectory;
    }
}
