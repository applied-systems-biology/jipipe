package org.hkijena.acaq5.ui.grapheditor.contextmenu;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.ui.events.AlgorithmUIActionRequestedEvent;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;

import static org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI.REQUEST_RUN_AND_SHOW_RESULTS;
import static org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI.REQUEST_RUN_ONLY;

/**
 * Adds run/update cache
 */
public class RunAndShowResultsAlgorithmContextMenuFeature implements ACAQAlgorithmUIContextMenuFeature {
    @Override
    public void install(ACAQAlgorithmUI ui, JPopupMenu contextMenu) {
        ACAQGraphNode algorithm = ui.getAlgorithm();
        if (algorithm instanceof ACAQAlgorithm && algorithm.getCategory() != ACAQAlgorithmCategory.Internal) {
            JMenuItem runAndShowResultsItem = new JMenuItem("Run & show results", UIUtils.getIconFromResources("play.png"));
            runAndShowResultsItem.setToolTipText("Runs the pipeline up until this algorithm and shows the results.");
            runAndShowResultsItem.addActionListener(e ->
                    ui.getEventBus().post(new AlgorithmUIActionRequestedEvent(ui, REQUEST_RUN_AND_SHOW_RESULTS)));
            contextMenu.add(runAndShowResultsItem);

            JMenuItem runOnlyItem = new JMenuItem("Update cache", UIUtils.getIconFromResources("database.png"));
            runOnlyItem.setToolTipText("Runs the pipeline up until this algorithm and caches the results. Nothing is written to disk.");
            runOnlyItem.addActionListener(e -> ui.getEventBus().post(new AlgorithmUIActionRequestedEvent(ui, REQUEST_RUN_ONLY)));
            contextMenu.add(runOnlyItem);
        }
    }

    @Override
    public void update(ACAQAlgorithmUI ui) {

    }

    @Override
    public boolean withSeparator() {
        return false;
    }
}
