/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.cache;

import com.google.common.eventbus.Subscribe;
import com.google.common.html.HtmlEscapers;
import org.hkijena.jipipe.api.JIPipeProjectCache;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.*;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.components.DocumentTabPane;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages the cache for a specific data slot.
 */
public class JIPipeDataSlotCacheManagerUI extends JIPipeProjectWorkbenchPanel {

    private final JIPipeDataSlot dataSlot;
    private JButton annotationButton;
    private JButton cacheButton;
    private JPopupMenu contextMenu;

    /**
     * @param workbenchUI The workbench UI
     * @param dataSlot    the data slot
     */
    public JIPipeDataSlotCacheManagerUI(JIPipeProjectWorkbench workbenchUI, JIPipeDataSlot dataSlot) {
        super(workbenchUI);
        this.dataSlot = dataSlot;
        initialize();
        updateStatus();

        getProject().getCache().getEventBus().register(this);
    }

    private void initialize() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        contextMenu = new JPopupMenu();

        annotationButton = new JButton(UIUtils.getIconFromResources("data-types/annotation-table.png"));
        UIUtils.makeFlat25x25(annotationButton);
        annotationButton.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        UIUtils.addReloadablePopupMenuToComponent(annotationButton, contextMenu, this::reloadContextMenu);
        add(annotationButton);

        cacheButton = new JButton(UIUtils.getIconFromResources("actions/database.png"));
        UIUtils.makeFlat25x25(cacheButton);
        cacheButton.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        UIUtils.addReloadablePopupMenuToComponent(cacheButton, contextMenu, this::reloadContextMenu);
        add(cacheButton);
    }

    private void reloadContextMenu() {
        contextMenu.removeAll();
        JIPipeProjectCache.State currentState = getProject().getStateIdOf((JIPipeAlgorithm) getDataSlot().getNode(), getProject().getGraph().traverseAlgorithms());

        Map<JIPipeProjectCache.State, Map<String, JIPipeDataSlot>> stateMap = getProject().getCache().extract((JIPipeAlgorithm) getDataSlot().getNode());
        if (stateMap != null) {
            JMenuItem openCurrent = createOpenStateButton(stateMap, currentState, "Open current snapshot");
            if (openCurrent != null) {
                contextMenu.add(openCurrent);
            }
            JMenu previousMenu = new JMenu("All snapshots");
            previousMenu.setIcon(UIUtils.getIconFromResources("actions/clock.png"));
            for (JIPipeProjectCache.State state : stateMap.keySet().stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
                JMenuItem item = createOpenStateButton(stateMap, state, "Open snapshot from " + state.getGenerationTime().format(DateTimeFormatter.ISO_LOCAL_DATE) + " " +
                        state.getGenerationTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                if (item != null) {
                    previousMenu.add(item);
                }
            }
            if (previousMenu.getItemCount() > 0) {
                contextMenu.add(previousMenu);
                contextMenu.addSeparator();
            }
        }

        JMenuItem clearOutdated = new JMenuItem("Clear outdated", UIUtils.getIconFromResources("actions/clock.png"));
        clearOutdated.setToolTipText("Removes all cached items that are have no representation in the project graph, anymore. " +
                "This includes items where the algorithm parameters have been changed.");
        clearOutdated.addActionListener(e -> getProject().getCache().autoClean(false, true));
        contextMenu.add(clearOutdated);

        JMenuItem clearAll = new JMenuItem("Clear all", UIUtils.getIconFromResources("actions/delete.png"));
        clearAll.setToolTipText("Removes all cached items.");
        clearAll.addActionListener(e -> getProject().getCache().clear());
        contextMenu.add(clearAll);
    }

    private JMenuItem createOpenStateButton(Map<JIPipeProjectCache.State, Map<String, JIPipeDataSlot>> stateMap, JIPipeProjectCache.State state, String label) {
        Map<String, JIPipeDataSlot> slotMap = stateMap.getOrDefault(state, null);
        if (slotMap == null)
            return null;
        JIPipeDataSlot cachedSlot = slotMap.getOrDefault(getDataSlot().getName(), null);
        if (cachedSlot == null)
            return null;

        JMenuItem item = new JMenuItem(label);
        item.setIcon(UIUtils.getIconFromResources("actions/camera.png"));
        item.setToolTipText("Opens the currently cached data as table");
        item.addActionListener(e -> openData(state));
        return item;
    }

    private void openData(JIPipeProjectCache.State state) {
//        JIPipeCacheDataSlotTableUI cacheTable = new JIPipeCacheDataSlotTableUI(getProjectWorkbench(), cachedSlot);
//        String tabName = getDataSlot().getAlgorithm().getName() + "/" + getDataSlot().getName() + " @ " + state.getGenerationTime().format(DateTimeFormatter.ISO_LOCAL_DATE) + " " +
//                state.getGenerationTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        JIPipeCacheBrowserUI cacheTable = new JIPipeCacheBrowserUI(getProjectWorkbench());
        cacheTable.getTree().selectDataSlot(state, getDataSlot());
        getWorkbench().getDocumentTabPane().addTab("Cache browser",
                UIUtils.getIconFromResources("actions/database.png"),
                cacheTable,
                DocumentTabPane.CloseMode.withSilentCloseButton,
                true);
        getWorkbench().getDocumentTabPane().switchToLastTab();
    }

    public JIPipeDataSlot getDataSlot() {
        return dataSlot;
    }

    /**
     * Triggered when the cache was updated
     *
     * @param event generated event
     */
    @Subscribe
    public void onCacheUpdated(JIPipeProjectCache.ModifiedEvent event) {
        updateStatus();
    }

    private void updateStatus() {
        JIPipeProjectCache cache = getProject().getCache();
        Map<JIPipeProjectCache.State, Map<String, JIPipeDataSlot>> stateMap = cache.extract((JIPipeAlgorithm) getDataSlot().getNode());
        int dataRows = 0;
        Set<String> traitTypes = new HashSet<>();
        if (stateMap != null) {
            for (Map<String, JIPipeDataSlot> slotMap : stateMap.values()) {
                JIPipeDataSlot equivalentSlot = slotMap.getOrDefault(getDataSlot().getName(), null);
                if (equivalentSlot != null) {
                    dataRows += equivalentSlot.getRowCount();
                    traitTypes.addAll(equivalentSlot.getAnnotationColumns());
                }
            }
        }

        if (dataRows > 0) {
            cacheButton.setVisible(true);
            generateCacheButtonTooltip(dataRows, stateMap);
        } else {
            cacheButton.setVisible(false);
        }
        if (dataRows > 0 && !traitTypes.isEmpty()) {
            annotationButton.setVisible(true);
            generateAnnotationButtonTooltip(traitTypes);
        } else {
            annotationButton.setVisible(false);
        }
    }

    private void generateAnnotationButtonTooltip(Set<String> traitTypes) {
        StringBuilder builder = new StringBuilder();
        builder.append("<html>");
        builder.append("This output data is annotated in at least one snapshot.<br/><br/>");
        builder.append("<table>");
        for (String info : traitTypes.stream().sorted().collect(Collectors.toList())) {
            builder.append("<tr><td><img src=\"").append(UIUtils.getIconFromResources("data-types/annotation.png")).append("\"/><td><strong>");
            builder.append(HtmlEscapers.htmlEscaper().escape(info)).append("</strong></td></tr>");
        }
        builder.append("<table>");
        builder.append("</html>");
        annotationButton.setToolTipText(builder.toString());
    }

    private void generateCacheButtonTooltip(int dataRows, Map<JIPipeProjectCache.State, Map<String, JIPipeDataSlot>> stateMap) {
        StringBuilder builder = new StringBuilder();
        builder.append("<html>");
        builder.append("The cache currently contains ").append(dataRows).append(" data rows that are spread across ").append(stateMap.keySet().size()).append(" snapshots.<br/><br/>");
        builder.append("<table>");
        for (JIPipeProjectCache.State state : stateMap.keySet().stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
            Map<String, JIPipeDataSlot> slotMap = stateMap.get(state);
            JIPipeDataSlot cacheSlot = slotMap.getOrDefault(getDataSlot().getName(), null);
            if (cacheSlot != null) {
                builder.append("<tr><td><strong>Snapshot @ ").append(state.renderGenerationTime()).append("</strong></td>");
                builder.append("<td>").append(cacheSlot.getRowCount()).append(" data rows</td>");
                builder.append("</tr>");
            }
        }
        builder.append("<table>");
        builder.append("</html>");
        cacheButton.setToolTipText(builder.toString());
    }
}
