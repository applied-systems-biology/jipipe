package org.hkijena.jipipe.api.cache;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;

import java.util.Map;
import java.util.UUID;

public class JIPipeLocalMemoryCache implements JIPipeCache {

    private final EventBus eventBus = new EventBus();

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public void store(JIPipeGraphNode graphNode, UUID nodeUUID, JIPipeDataTable data, String outputName, JIPipeProgressInfo progressInfo) {

    }

    @Override
    public Map<String, JIPipeDataTable> query(JIPipeGraphNode graphNode, UUID nodeUUID, JIPipeProgressInfo progressInfo) {
        return null;
    }

    @Override
    public void clearOutdated(JIPipeProgressInfo progressInfo) {

    }

    @Override
    public void clearAll(JIPipeProgressInfo progressInfo) {

    }

    @Override
    public void clearAll(UUID nodeUUID, JIPipeProgressInfo progressInfo) {

    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public int size() {
        return 0;
    }

    public void makeNonVirtual(JIPipeProgressInfo progressInfo) {

    }

    public void makeVirtual(JIPipeProgressInfo progressInfo) {

    }
}
