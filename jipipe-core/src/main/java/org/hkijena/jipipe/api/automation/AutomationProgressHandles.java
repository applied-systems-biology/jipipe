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

package org.hkijena.jipipe.api.automation;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.progress.JIPipeProgressLog;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The shared per-project progress chain of the automation layer (MCP): every
 * {@link AutomationContext} built for the same project key reads and writes ONE progress chain,
 * so the diagnostics operations (get_log, get_errors) see what earlier operations, in
 * particular MCP-triggered runs, recorded.
 * <p>
 * Trace conclusion (review fix 2026-11-16): the run progress of run_node, run_compartment, and
 * run_pipeline flows through the context's {@link JIPipeProgressInfo}
 * ({@code JIPipeAutomationAPI.run*} hands it to the {@code InstrumentationRunEngine}, which sets
 * it on the {@code JIPipeGraphRun}), and the run adopts its file-backed store
 * ({@code logs/<run-uuid>.ndjson} in the project temp tree) INTO that same handle at the start of
 * the run ({@link JIPipeProgressInfo#setStore}). Sharing one chain per project key therefore
 * makes a later get_log call see the run's records without any additional wiring at the
 * run-engine level.
 * <p>
 * Mechanics: {@link #create(String)} hands out a fresh root handle on the chain's current store.
 * A fresh handle per call keeps the cancellation flag and the progress counters isolated: a
 * cancel_job cancels only the operation that owns the job, never unrelated later operations of
 * the same project (sharing one handle INSTANCE would propagate the cancellation flag, which has
 * no reset). A store adoption of such a handle (the run's file-backed store) is propagated back
 * into the chain via the handle's store-adoption callback, so the next handle continues from the
 * adopted store and reads the run's records. Between runs the chain is a bounded in-memory
 * store; after a run it is the run's file-backed store, which stays readable but accepts no
 * further writes (it is closed when the run finishes) and remains current until the next
 * adoption. Records written outside a run therefore reach the chain only while its current
 * store is open.
 * <p>
 * Two MCP-triggered runs of the same project that overlap adopt separate run stores; the chain
 * follows the most recent adoption, so the diagnostics reflect the most recent run. The key map
 * is bounded (LRU); evicted entries keep working for their in-flight handles. A null project key
 * (registry-global operations routed without a project) gets a fresh private handle: these
 * operations never touch a project, so there is nothing to share.
 */
public class AutomationProgressHandles {

    /**
     * Maximum number of tracked project keys. Open project windows are few; the bound exists
     * so a long-lived GUI session cannot accumulate unbounded entries.
     */
    private static final int MAX_TRACKED_KEYS = 32;

    /**
     * The current store of one project key. Volatile: the adoption callback writes it from the
     * thread that adopts the store, while later create() calls read it.
     */
    private static final class ProjectChain {
        private volatile JIPipeProgressLog store = JIPipeProgressLog.silent();
    }

    private final Map<String, ProjectChain> chains = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ProjectChain> eldest) {
            return size() > MAX_TRACKED_KEYS;
        }
    };

    /**
     * Returns a progress handle for the given project key on the shared chain. Two calls with
     * the same non-null key return handles on the same store (the same chain); a null key
     * returns a fresh private handle.
     *
     * @param projectKey the project key (may be null)
     * @return the progress handle for the context
     */
    public JIPipeProgressInfo create(String projectKey) {
        if (projectKey == null) {
            return new JIPipeProgressInfo();
        }
        ProjectChain chain;
        synchronized (this) {
            chain = chains.computeIfAbsent(projectKey, key -> new ProjectChain());
        }
        JIPipeProgressInfo handle = new JIPipeProgressInfo(chain.store);
        // A run adopting its file-backed store (JIPipeGraphRun.run -> setStorageBase) must move
        // the chain with it: later contexts then read the run's records. setStore fires the
        // callback after the handle's store was replaced.
        handle.addStoreAdoptedCallback(() -> chain.store = handle.getStore());
        return handle;
    }
}
