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
 * A progress store that renders records as compat lines to {@code System.out} on the calling thread.
 * Non-persistent: no file is written, and read-back serves the tail only. Intended for low-volume
 * use (the caller thread does the rendering).
 */
public class StdoutJIPipeProgressLog extends AbstractJIPipeProgressLog {

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
    protected void writeRecord(JIPipeProgressRecord record) {
        String line = renderCompatLine(record);
        if (line != null) {
            System.out.println(line);
        }
    }

    /**
     * Renders one record as a compat line. Structural records ({@code head}, {@code n}, {@code N},
     * {@code s}) render nothing. Temporary until the shared text renderer (Task 3) replaces it.
     *
     * @param record the record
     * @return the line, or {@code null} if the record renders nothing
     */
    private String renderCompatLine(JIPipeProgressRecord record) {
        if (record instanceof JIPipeProgressRecord.Log logRecord) {
            return logRecord.m();
        } else if (record instanceof JIPipeProgressRecord.Progress progressRecord) {
            JIPipeProgressNode node = getNode(progressRecord.id());
            String label = node != null && node.getLabel() != null ? node.getLabel() : "";
            return "[" + progressRecord.v() + "/" + progressRecord.mx() + "] " + label;
        } else if (record instanceof JIPipeProgressRecord.Error errorRecord) {
            StringBuilder builder = new StringBuilder();
            builder.append(errorRecord.c()).append(": ").append(errorRecord.m());
            if (errorRecord.st() != null) {
                for (String stackLine : errorRecord.st().split("\n")) {
                    builder.append("\n  ").append(stackLine);
                }
            }
            return builder.toString();
        } else if (record instanceof JIPipeProgressRecord.Notification notificationRecord) {
            return "@ " + notificationRecord.h() + ": " + notificationRecord.d();
        }
        return null;
    }
}
