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

package org.hkijena.jipipe.utils;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Wrapper around an {@link OutputStream} that prevents closing
 */
public class UnclosableOutputStream extends OutputStream {

    private final OutputStream wrappedStream;

    public UnclosableOutputStream(OutputStream wrappedStream) {
        this.wrappedStream = wrappedStream;
    }

    @Override
    public void write(int b) throws IOException {
        wrappedStream.write(b);
    }

    public OutputStream getWrappedStream() {
        return wrappedStream;
    }
}
