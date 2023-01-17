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
     * @param progressInfo the progress
     */
    void clearOutdated(JIPipeProgressInfo progressInfo);

    /**
     * Clears the cache
     * @param progressInfo the progress
     */
    void clearAll(JIPipeProgressInfo progressInfo);

    void clearAll(UUID nodeUUID, JIPipeProgressInfo progressInfo);

    boolean isEmpty();

    int size();

    class StoredEvent {

    }

    class ClearedEvent {

    }

    class ModifiedEvent {

    }
}
