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

package org.hkijena.jipipe.api.settings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Concurrency smoke test for {@link SettingsDocument}'s threading contract:
 * mutating threads (the EDT / feature threads that write through bindings) and
 * a merging thread (the settings service's foreign-change executor) operate on
 * the same document at the same time.
 * <p>
 * What it proves: with the document's mutators and listener registration
 * synchronized on the document instance, a concurrent mix of whole-block
 * merges ({@code setBlock}, like the foreign merge), sub-path writes (like a
 * binding), listener registration (like {@code bind()}/{@code attachSheet()})
 * and reads never throws (no
 * {@link java.util.ConcurrentModificationException}, no Jackson tree
 * corruption crash) and never loses a registered listener. Listeners also
 * write back into the document from inside their callback, exercising the
 * reentrant-lock path.
 * <p>
 * This is a smoke test, not a linearizability proof: without the
 * synchronization it fails reliably on this box (the listener
 * {@link ArrayList} is iterated during dispatch while another thread appends to
 * it, and Jackson's {@code ObjectNode} fields map is mutated while read), with
 * it the run is clean.
 */
class SettingsDocumentConcurrencyTest {

    @Test void concurrentMergesWritesAndListenerRegistrationAreSafe() throws Exception {
        SettingsDocument doc = new SettingsDocument(SettingsScope.APPLICATION);
        doc.setBlock("registry:seed", IntNode.valueOf(0));

        int threads = 6;
        int iterations = 400;
        CountDownLatch start = new CountDownLatch(1);
        List<Throwable> failures = new ArrayList<>();
        AtomicInteger listenerCalls = new AtomicInteger();

        List<Thread> workers = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            final int id = t;
            Thread worker = new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < iterations; i++) {
                        // A merge-style whole-block replacement.
                        doc.setBlock("registry:shared", IntNode.valueOf(i));
                        // A binding-style sub-path write into the same and other blocks.
                        doc.set("registry:writer-" + id, "k", IntNode.valueOf(i));
                        doc.set("registry:writer-" + id, "k", IntNode.valueOf(i + 1));
                        // A listener with a write-back from inside its callback.
                        String path = "registry:cb-" + id + "-" + i;
                        doc.addPathListener(path + "/z", v -> {
                            listenerCalls.incrementAndGet();
                            // Reentrant write from inside a listener (must not deadlock).
                            doc.set("registry:shared", "nested", IntNode.valueOf(1));
                        });
                        // A write that triggers that listener back on the writer thread.
                        doc.set(path, "z", IntNode.valueOf(i));
                        // Reads interleaved with all of the above.
                        JsonNode v = doc.getBlock("registry:shared");
                        assertNotNull(v);
                        Set<String> blocks = doc.blockPaths();
                        assertTrue(!blocks.isEmpty());
                    }
                } catch (Throwable e) {
                    synchronized (failures) {
                        failures.add(e);
                    }
                }
            }, "doc-worker-" + t);
            worker.start();
            workers.add(worker);
        }
        start.countDown();
        // The merging thread interleaves merges while the writers run.
        Thread merger = new Thread(() -> {
            try {
                start.await();
                for (int i = 0; i < iterations; i++) {
                    doc.setBlock("registry:merged", IntNode.valueOf(i));
                    doc.remove("registry:merged", "");
                    doc.setBlock("registry:merged", IntNode.valueOf(i));
                }
            } catch (Throwable e) {
                synchronized (failures) {
                    failures.add(e);
                }
            }
        }, "doc-merger");
        merger.start();
        workers.add(merger);
        for (Thread w : workers) {
            w.join(30_000);
        }

        synchronized (failures) {
            assertTrue(failures.isEmpty(), "no concurrent use may fail: " + failures);
        }
        assertTrue(listenerCalls.get() >= threads * iterations,
                "every registered listener must have been called; calls=" + listenerCalls.get());
        // The tree is still structurally readable after the storm.
        assertNotNull(doc.getBlock("registry:shared"));
        assertNotNull(doc.getBlock("registry:merged"));
    }
}