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

package org.hkijena.jipipe.api.progress;

import java.nio.file.Path;

/**
 * A progress store that discards records: no file, no output, no writer thread.
 * The node index and the tail are still maintained.
 */
public class SilentJIPipeProgressLog extends AbstractJIPipeProgressLog {

    /**
     * Creates the store.
     */
    public SilentJIPipeProgressLog() {
        super(null, null);
    }

    @Override
    public Path getFile() {
        return null;
    }

    @Override
    protected void writeRecord(JIPipeProgressRecord record) {
        // Records are discarded; index and tail are maintained by the abstract base class
    }
}
