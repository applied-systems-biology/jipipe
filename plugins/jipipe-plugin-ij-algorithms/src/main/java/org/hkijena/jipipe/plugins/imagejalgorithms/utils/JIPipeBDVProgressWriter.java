package org.hkijena.jipipe.plugins.imagejalgorithms.utils;

import bdv.export.ProgressWriter;
import org.hkijena.jipipe.api.JIPipeProgressInfo;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

/**
 * ProgressWriter implementation that forwards messages into a JIPipeProgressInfo.
 *
 * <p>
 * All messages written to {@link #out()} are sent to {@code progressInfo.log(String)} and
 * all messages written to {@link #err()} are sent to {@code progressInfo.error(String)}.
 * <br>
 * The last completion ratio set via {@link #setProgress(double)} is always prepended
 * (as a percentage) to every logged line in the form {@code [x%] message}.
 * </p>
 */
public class JIPipeBDVProgressWriter implements ProgressWriter {

    private final JIPipeProgressInfo progressInfo;

    private final PrintStream out;

    private final PrintStream err;

    // last completion ratio in [0, 1], used when formatting messages
    private volatile double completionRatio = 0.0;

    public JIPipeBDVProgressWriter(JIPipeProgressInfo progressInfo) {
        this.progressInfo = Objects.requireNonNull(progressInfo, "progressInfo");
        this.out = createPrintStream(this::log);
        this.err = createPrintStream(this::error);
    }

    @Override
    public PrintStream out() {
        return out;
    }

    @Override
    public PrintStream err() {
        return err;
    }

    @Override
    public void setProgress(double completionRatio) {
        // Cancellation guard
        if(progressInfo.isCancelled()) {
            throw new CancellationException();
        }
        this.completionRatio = completionRatio;
    }

    // ---- internal helpers ---------------------------------------------------

    private void log(String message) {
        progressInfo.log(formatMessage(message));
    }

    private void error(String message) {
        progressInfo.error(formatMessage(message));
    }

    private String formatMessage(String message) {
        double pct = completionRatio * 100.0;
        return String.format("[%.0f%%] %s", pct, message);
    }

    private PrintStream createPrintStream(Consumer<String> consumer) {
        OutputStream os = new LineBufferingOutputStream(consumer);
        // autoFlush = true; charset default is fine here
        return new PrintStream(os, true);
    }

    /**
     * OutputStream that buffers bytes until a newline and then forwards complete
     * lines (UTF-8 decoded) to the provided consumer.
     */
    private static class LineBufferingOutputStream extends OutputStream {

        private final Consumer<String> consumer;

        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        LineBufferingOutputStream(Consumer<String> consumer) {
            this.consumer = Objects.requireNonNull(consumer);
        }

        @Override
        public synchronized void write(int b) {
            if (b == '\n') {
                flushBuffer();
            } else if (b != '\r') {
                buffer.write(b);
            }
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) {
            for (int i = off; i < off + len; i++) {
                write(b[i]);
            }
        }

        @Override
        public synchronized void flush() {
            flushBuffer();
        }

        @Override
        public synchronized void close() {
            flushBuffer();
        }

        private void flushBuffer() {
            if (buffer.size() == 0)
                return;

            String line = new String(buffer.toByteArray(), StandardCharsets.UTF_8).trim();
            buffer.reset();

            if (!line.isEmpty()) {
                consumer.accept(line);
            }
        }
    }
}

