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
        if(b == '\r' || b == '\n') {
            flush();
            return;
        }
        if(b > 0) {
            builder.append((char)b);
        }
    }

    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }

    @Override
    public void flush() throws IOException {
        if(builder.length() > 0) {
            progressInfo.log(builder.toString());
            builder.setLength(0);
        }
    }

    @Override
    public void close() throws IOException {
        builder = null;
    }
}
