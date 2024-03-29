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

package org.hkijena.jipipe.api.cache;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;

import java.util.Map;
import java.util.UUID;

/**
 * A class that implements a data cache
 */
public interface JIPipeCache {

    void store(JIPipeGraphNode graphNode, UUID nodeUUID, JIPipeDataTable data, String outputName, JIPipeProgressInfo progressInfo);

    Map<String, JIPipeDataTable> query(JIPipeGraphNode graphNode, UUID nodeUUID, JIPipeProgressInfo progressInfo);

    /**
     * Removes all outdated entries
     *
     * @param progressInfo the progress
     */
    void clearOutdated(JIPipeProgressInfo progressInfo);

    /**
     * Clears the cache
     *
     * @param progressInfo the progress
     */
    void clearAll(JIPipeProgressInfo progressInfo);

    /**
     * Clears the cache of the specified node
     *
     * @param nodeUUID           the node
     * @param invalidateChildren whether all nodes utilizing that node's data will be invalidated (remove structure)
     * @param progressInfo       the progress
     */
    void clearAll(UUID nodeUUID, boolean invalidateChildren, JIPipeProgressInfo progressInfo);

    boolean isEmpty();

    int size();

    StoredEventEmitter getStoredEventEmitter();

    ClearedEventEmitter getClearedEventEmitter();

    ModifiedEventEmitter getModifiedEventEmitter();

    interface StoredEventListener {
        void onCacheStoredEvent(StoredEvent event);
    }

    interface ClearedEventListener {
        void onCacheClearedEvent(ClearedEvent event);
    }

    interface ModifiedEventListener {
        void onCacheModified(ModifiedEvent event);
    }

    class StoredEvent extends AbstractJIPipeEvent {
        private final JIPipeCache cache;
        private final UUID nodeUUID;
        private final JIPipeDataTable data;
        private final String outputName;

        public StoredEvent(JIPipeCache cache, UUID nodeUUID, JIPipeDataTable data, String outputName) {
            super(cache);
            this.cache = cache;
            this.nodeUUID = nodeUUID;
            this.data = data;
            this.outputName = outputName;
        }

        public JIPipeCache getCache() {
            return cache;
        }

        public UUID getNodeUUID() {
            return nodeUUID;
        }

        public JIPipeDataTable getData() {
            return data;
        }

        public String getOutputName() {
            return outputName;
        }
    }

    class StoredEventEmitter extends JIPipeEventEmitter<StoredEvent, StoredEventListener> {

        @Override
        protected void call(StoredEventListener storedEventListener, StoredEvent event) {
            storedEventListener.onCacheStoredEvent(event);
        }
    }

    class ClearedEvent extends AbstractJIPipeEvent {
        private final JIPipeCache cache;
        private final UUID nodeUUID;

        public ClearedEvent(JIPipeCache cache, UUID nodeUUID) {
            super(cache);
            this.cache = cache;
            this.nodeUUID = nodeUUID;
        }

        public JIPipeCache getCache() {
            return cache;
        }

        public UUID getNodeUUID() {
            return nodeUUID;
        }
    }

    class ClearedEventEmitter extends JIPipeEventEmitter<ClearedEvent, ClearedEventListener> {
        @Override
        protected void call(ClearedEventListener clearedEventListener, ClearedEvent event) {
            clearedEventListener.onCacheClearedEvent(event);
        }
    }

    class ModifiedEvent extends AbstractJIPipeEvent {
        private final JIPipeCache cache;

        public ModifiedEvent(JIPipeCache cache) {
            super(cache);
            this.cache = cache;
        }

        public JIPipeCache getCache() {
            return cache;
        }
    }

    class ModifiedEventEmitter extends JIPipeEventEmitter<ModifiedEvent, ModifiedEventListener> {

        @Override
        protected void call(ModifiedEventListener modifiedEventListener, ModifiedEvent event) {
            modifiedEventListener.onCacheModified(event);
        }
    }
}
