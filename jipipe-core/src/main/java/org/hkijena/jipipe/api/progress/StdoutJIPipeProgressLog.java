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
 * A progress store that prints records with text-line content ({@code l}, {@code e}, {@code w}) to
 * {@code System.out} on the calling thread, in the shapes of the shared text renderer
 * ({@code [v/mx] <chain> | message}, errors with the two-space-indented stack). All log levels
 * print (stdout = full fidelity). Structural records ({@code head}, {@code n}, {@code N},
 * {@code s}) and progress records ({@code p}) print nothing. Non-persistent: no file is written,
 * and read-back serves the tail only. Intended for low-volume use (the caller thread does the
 * printing).
 */
public class StdoutJIPipeProgressLog extends AbstractJIPipeProgressLog {

    /**
     * Walk state of the printed records - the same walk the text renderer uses, fed in submission
     * order, so the printed prefixes (running progress, label chain) match a full render.
     */
    private final RenderWalk walk = new RenderWalk(this, false);

    /**
     * Creates the store.
     */
    public StdoutJIPipeProgressLog() {
        super(null, null);
    }

    @Override
    public Path getFile() {
        return null;
    }

    @Override
    protected void writeRecord(long line, JIPipeProgressRecord record) {
        // All log levels print (stdout = full fidelity; no level filter)
        walk.render(record, JIPipeLogLevel.DEBUG, System.out::println);
    }
}
