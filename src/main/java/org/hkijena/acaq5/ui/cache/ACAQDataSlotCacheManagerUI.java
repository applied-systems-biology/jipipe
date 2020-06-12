package org.hkijena.acaq5.ui.cache;

import com.google.common.eventbus.Subscribe;
import com.google.common.html.HtmlEscapers;
import org.hkijena.acaq5.api.ACAQProjectCache;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbenchPanel;
import org.hkijena.acaq5.ui.registries.ACAQUITraitRegistry;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages the cache for a specific data slot.
 */
public class ACAQDataSlotCacheManagerUI extends ACAQProjectWorkbenchPanel {

    private final ACAQDataSlot dataSlot;
    private JButton annotationButton;
    private JButton cacheButton;

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
        Map<ACAQProjectCache.State, Map<String, ACAQDataSlot>> stateMap = cache.extract((ACAQAlgorithm) getDataSlot().getAlgorithm());
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
            generateCacheButtonTooltip(dataRows, stateMap);
        }
        else {
            cacheButton.setVisible(false);
        }
        if(dataRows > 0 && !traitTypes.isEmpty()) {
            annotationButton.setVisible(true);
            generateAnnotationButtonTooltip(traitTypes);
        }
        else {
            annotationButton.setVisible(false);
        }
    }

    private void generateAnnotationButtonTooltip(Set<ACAQTraitDeclaration> traitTypes) {
        StringBuilder builder = new StringBuilder();
        builder.append("<html>");
        builder.append("This output data is annotated in at least one snapshot.<br/><br/>");
        builder.append("<table>");
        for (ACAQTraitDeclaration declaration : traitTypes.stream().sorted(Comparator.comparing(ACAQTraitDeclaration::getName)).collect(Collectors.toList())) {
            builder.append("<tr><td><img src=\"").append(ACAQUITraitRegistry.getInstance().getIconURLFor(declaration)).append("\"/><td>");
            builder.append(HtmlEscapers.htmlEscaper().escape(declaration.getName())).append("</td></tr>");
        }
        builder.append("<table>");
        builder.append("</html>");
        annotationButton.setToolTipText(builder.toString());
    }

    private void generateCacheButtonTooltip(int dataRows, Map<ACAQProjectCache.State, Map<String, ACAQDataSlot>> stateMap) {
        StringBuilder builder = new StringBuilder();
        builder.append("<html>");
        builder.append("The cache currently contains ").append(dataRows).append(" data rows that are spread across ").append(stateMap.keySet().size()).append(" snapshots.<br/><br/>");
        builder.append("<table>");
        for (ACAQProjectCache.State state : stateMap.keySet().stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
            Map<String, ACAQDataSlot> slotMap = stateMap.get(state);
            ACAQDataSlot cacheSlot = slotMap.getOrDefault(getDataSlot().getName(), null);
            if(cacheSlot != null) {
                builder.append("<tr><td><strong>Snapshot @ ").append(state.getGenerationTime().format(DateTimeFormatter.ISO_LOCAL_DATE)).append(" ")
                        .append(state.getGenerationTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"))).append("</strong></td>");
                builder.append("<td>").append(cacheSlot.getRowCount()).append(" data rows</td>");
                builder.append("</tr>");
            }
        }
        builder.append("<table>");
        builder.append("</html>");
        cacheButton.setToolTipText(builder.toString());
    }
}
