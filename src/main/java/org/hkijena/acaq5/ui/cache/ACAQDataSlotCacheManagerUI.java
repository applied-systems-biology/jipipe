package org.hkijena.acaq5.ui.cache;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQProjectCache;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbenchPanel;

/**
 * Manages the cache for a specific data slot.
 */
public class ACAQDataSlotCacheManagerUI extends ACAQProjectWorkbenchPanel {

    private final ACAQDataSlot dataSlot;

    /**
     * @param workbenchUI The workbench UI
     * @param dataSlot the data slot
     */
    public ACAQDataSlotCacheManagerUI(ACAQProjectWorkbench workbenchUI, ACAQDataSlot dataSlot) {
        super(workbenchUI);
        this.dataSlot = dataSlot;

        getProject().getCache().getEventBus().register(this);
    }

    public ACAQDataSlot getDataSlot() {
        return dataSlot;
    }

    /**
     * Triggered when the cache was updated
     * @param event generated event
     */
    @Subscribe
    public void onCacheUpdated(ACAQProjectCache.ModifiedEvent event) {
        updateStatus();
    }

    private void updateStatus() {

    }
}
