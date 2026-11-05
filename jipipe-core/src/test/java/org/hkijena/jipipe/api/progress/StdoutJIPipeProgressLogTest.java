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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link StdoutJIPipeProgressLog}: only records with text-line content ({@code l},
 * {@code e}, {@code w}) are printed, in the shapes of the store's text renderer. Progress,
 * structural, and status records print nothing; all log levels print (stdout = full fidelity).
 */
class StdoutJIPipeProgressLogTest {

    @Test
    void stdoutPrintsOnlyTextLinesInRenderTextShape() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        // A fixed stack trace keeps the rendered stack lines deterministic
        RuntimeException boom = new RuntimeException("boom");
        boom.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("org.example.Worker", "run", "Worker.java", 42),
                new StackTraceElement("org.example.Loop", "step", "Loop.java", 7)});
        try {
            StdoutJIPipeProgressLog store = new StdoutJIPipeProgressLog();
            int node = store.beginNode(store.getRootNodeId(), "Node A", JIPipeProgressNodeKind.ITEM);
            int detached = store.beginNode(node, "", JIPipeProgressNodeKind.ITEM);
            store.progress(node, 1, 2);     // plain progress record: prints nothing
            store.progress(node, 50, 100);  // percentage report point: prints nothing
            store.log(node, JIPipeLogLevel.INFO, "hello");
            store.log(detached, JIPipeLogLevel.INFO, "from detached"); // no empty chain segment
            store.log(node, JIPipeLogLevel.DEBUG, "debug detail");     // all log levels print
            store.error(node, boom);
            store.notification("Heading", "Description");
            store.close();
        } finally {
            System.setOut(originalOut);
        }
        List<String> lines = Arrays.asList(buffer.toString(StandardCharsets.UTF_8).split("\n"));
        assertEquals(List.of(
                "[50/100] Node A | hello",
                "[1/1] Node A | from detached",
                "[50/100] Node A | debug detail",
                "[50/100] Node A | java.lang.RuntimeException: boom",
                "  at org.example.Worker.run(Worker.java:42)",
                "  at org.example.Loop.step(Loop.java:7)",
                "@ Heading: Description"), lines);
    }
}
