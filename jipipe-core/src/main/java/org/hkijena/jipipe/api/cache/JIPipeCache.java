package org.hkijena.jipipe.api.cache;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;

import java.util.Map;
import java.util.UUID;

/**
 * A class that implements a data cache
 */
public interface JIPipeCache {

    EventBus getEventBus();

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
     * @param progressInfo the progress
     */
    void clearAll(JIPipeProgressInfo progressInfo);

    /**
     * Clears the cache of the specified node
     * @param nodeUUID the node
     * @param invalidateChildren whether all nodes utilizing that node's data will be invalidated (remove structure)
     * @param progressInfo the progress
     */
    void clearAll(UUID nodeUUID, boolean invalidateChildren, JIPipeProgressInfo progressInfo);

    boolean isEmpty();

    int size();

    class StoredEvent {
        private final JIPipeCache cache;
        private final UUID nodeUUID;
        private final JIPipeDataTable data;
        private final String outputName;

        public StoredEvent(JIPipeCache cache, UUID nodeUUID, JIPipeDataTable data, String outputName) {
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

    class ClearedEvent {
        private final JIPipeCache cache;
        private final UUID nodeUUID;

        public ClearedEvent(JIPipeCache cache, UUID nodeUUID) {
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

    class ModifiedEvent {
        private final JIPipeCache cache;

        public ModifiedEvent(JIPipeCache cache) {
            this.cache = cache;
        }

        public JIPipeCache getCache() {
            return cache;
        }
    }
}
