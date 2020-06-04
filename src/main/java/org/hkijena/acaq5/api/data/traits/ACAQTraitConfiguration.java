package org.hkijena.acaq5.api.data.traits;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;

import java.util.List;
import java.util.Map;

/**
 * Processes algorithm traits of an {@link ACAQGraphNode}
 */
public interface ACAQTraitConfiguration {

    /**
     * Returns a map from slot name to modification task
     *
     * @return list of tasks
     */
    Map<String, ACAQDataSlotTraitConfiguration> getModificationTasks();

    /**
     * Returns all transfer tasks
     *
     * @return list of tasks
     */
    List<ACAQTraitTransferTask> getTransferTasks();

    /**
     * Applies the traits
     */
    void apply();

    /**
     * Returns the Event bus of this trait configuration
     *
     * @return the event bus
     */
    EventBus getEventBus();
}
