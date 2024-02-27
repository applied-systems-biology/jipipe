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

package org.hkijena.jipipe.api;

import java.io.IOException;
import java.io.OutputStream;

public class JIPipeProgressInfoOutputStream extends OutputStream {

    private final JIPipeProgressInfo progressInfo;
    private StringBuilder builder = new StringBuilder();

    public JIPipeProgressInfoOutputStream(JIPipeProgressInfo progressInfo) {
        this.progressInfo = progressInfo;
    }

    @Override
    public void write(int b) throws IOException {
        if (b == '\r' || b == '\n') {
            flush();
            return;
        }
        if (b > 0) {
            builder.append((char) b);
        }
    }

    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }

    @Override
    public void flush() throws IOException {
        if (builder.length() > 0) {
            progressInfo.log(builder.toString());
            builder.setLength(0);
        }
    }

    @Override
    public void close() throws IOException {
        builder = null;
    }
}
