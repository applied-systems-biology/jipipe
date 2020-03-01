package org.hkijena.acaq5.api.traits.global;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;

import java.util.List;
import java.util.Map;

/**
 * Processes algorithm traits of an {@link ACAQAlgorithm}
 */
public interface ACAQTraitConfiguration {

    /**
     * Returns a map from slot name to modification task
     * @return
     */
    Map<String, List<ACAQTraitModificationTask>> getModificationTasks();

    /**
     * Returns all transfer tasks
     * @return
     */
    List<ACAQTraitTransferTask> getTransferTasks();

    /**
     * Applies the traits
     */
    void apply();

    /**
     * Returns the Event bus of this trait configuration
     * @return
     */
    EventBus getEventBus();
}
