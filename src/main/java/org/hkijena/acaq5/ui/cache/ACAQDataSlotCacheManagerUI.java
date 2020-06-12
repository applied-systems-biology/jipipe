package org.hkijena.acaq5.ui.cache;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQProjectCache;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbenchPanel;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.util.*;

/**
 * Manages the cache for a specific data slot.
 */
public class ACAQDataSlotCacheManagerUI extends ACAQProjectWorkbenchPanel {

    private final ACAQDataSlot dataSlot;
    private JButton annotationButton;
    private JButton cacheButton;
    private JButton runButton;

    /**
     * @param workbenchUI The workbench UI
     * @param dataSlot the data slot
     */
    public ACAQDataSlotCacheManagerUI(ACAQProjectWorkbench workbenchUI, ACAQDataSlot dataSlot) {
        super(workbenchUI);
        this.dataSlot = dataSlot;
        initialize();
        updateStatus();

        getProject().getCache().getEventBus().register(this);
    }

    private void initialize() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        annotationButton = new JButton(UIUtils.getIconFromResources("data-types/annotation-table.png"));
        UIUtils.makeFlat25x25(annotationButton);
        annotationButton.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
        add(annotationButton);

        cacheButton = new JButton(UIUtils.getIconFromResources("database.png"));
        UIUtils.makeFlat25x25(cacheButton);
        cacheButton.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
        add(cacheButton);

        runButton = new JButton(UIUtils.getIconFromResources("play.png"));
        UIUtils.makeFlat25x25(runButton);
        runButton.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
        add(runButton);
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
        ACAQProjectCache cache = getProject().getCache();
        Map<String, Map<String, ACAQDataSlot>> stateMap = cache.extract((ACAQAlgorithm) getDataSlot().getAlgorithm());
        int dataRows = 0;
        Set<ACAQTraitDeclaration> traitTypes = new HashSet<>();
        if(stateMap != null) {
            for (Map<String, ACAQDataSlot> slotMap : stateMap.values()) {
                ACAQDataSlot equivalentSlot = slotMap.getOrDefault(getDataSlot().getName(), null);
                if(equivalentSlot != null) {
                    dataRows += equivalentSlot.getRowCount();
                    traitTypes.addAll(equivalentSlot.getAnnotationColumns());
                }
            }
        }

        if(dataRows > 0) {
            cacheButton.setVisible(true);
        }
        else {
            cacheButton.setVisible(false);
        }
        if(dataRows > 0 && !traitTypes.isEmpty()) {
            annotationButton.setVisible(true);
        }
        else {
            annotationButton.setVisible(false);
        }
    }
}
