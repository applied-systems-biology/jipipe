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
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Wrapper around an {@link OutputStream} that prevents closing
 */
public class UnclosableInputStream extends InputStream {

    private final InputStream wrappedStream;

    public UnclosableInputStream(InputStream wrappedStream) {
        this.wrappedStream = wrappedStream;
    }

    public InputStream getWrappedStream() {
        return wrappedStream;
    }

    @Override
    public int read() throws IOException {
        return wrappedStream.read();
    }
}
