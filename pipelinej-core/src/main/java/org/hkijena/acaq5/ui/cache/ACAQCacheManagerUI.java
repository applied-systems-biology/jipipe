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

package org.hkijena.acaq5.ui.cache;

import com.google.common.eventbus.Subscribe;
import com.google.common.html.HtmlEscapers;
import org.hkijena.acaq5.api.ACAQProjectCache;
import org.hkijena.acaq5.api.data.ACAQDataDeclaration;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbenchPanel;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.util.Map;

/**
 * UI that monitors the queue
 */
public class ACAQCacheManagerUI extends ACAQProjectWorkbenchPanel {

    private JLabel statusLabel;
    private JButton clearButton;

    /**
     * Creates new instance
     *
     * @param workbenchUI the workbench
     */
    public ACAQCacheManagerUI(ACAQProjectWorkbench workbenchUI) {
        super(workbenchUI);
        initialize();
        updateStatus();

        getProject().getCache().getEventBus().register(this);
    }

    private void initialize() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setOpaque(false);
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)));


        statusLabel = new JLabel();
        statusLabel.setIcon(UIUtils.getIconFromResources("database.png"));
        add(statusLabel);

        clearButton = new JButton(UIUtils.getIconFromResources("clear-brush.png"));
        add(Box.createHorizontalStrut(4));
        add(clearButton);
        UIUtils.makeBorderlessWithoutMargin(clearButton);
        JPopupMenu clearMenu = UIUtils.addPopupMenuToComponent(clearButton);

        JMenuItem clearOutdated = new JMenuItem("Clear outdated", UIUtils.getIconFromResources("clock.png"));
        clearOutdated.setToolTipText("Removes all cached items that are have no representation in the project graph, anymore. " +
                "This includes items where the algorithm parameters have been changed.");
        clearOutdated.addActionListener(e -> getProject().getCache().autoClean(false, true));
        clearMenu.add(clearOutdated);

        JMenuItem clearAll = new JMenuItem("Clear all", UIUtils.getIconFromResources("delete.png"));
        clearAll.setToolTipText("Removes all cached items.");
        clearAll.addActionListener(e -> getProject().getCache().clear());
        clearMenu.add(clearAll);

    }

    /**
     * Updates the UI status
     */
    public void updateStatus() {
        if (getProject().getCache().isEmpty()) {
            statusLabel.setText("Nothing cached");
            statusLabel.setToolTipText("There is currently no data in the cache.");
            clearButton.setVisible(false);
        } else {
            statusLabel.setText(getProject().getCache().getCachedRowNumber() + " items cached");
            StringBuilder cacheInfo = new StringBuilder();
            cacheInfo.append("<html>");
            cacheInfo.append("Currently there are ").append(getProject().getCache().getCachedRowNumber()).append(" data rows stored in the cache.<br/><br/>");
            cacheInfo.append("<table>");
            for (Map.Entry<ACAQDataDeclaration, Integer> entry : getProject().getCache().getCachedDataTypes().entrySet()) {
                cacheInfo.append("<tr><td>").append("<img src=\"").append(ACAQUIDatatypeRegistry.getInstance().getIconURLFor(entry.getKey())).append("\"/></td>");
                cacheInfo.append("<td>").append(HtmlEscapers.htmlEscaper().escape(entry.getKey().getName())).append("</td>");
                cacheInfo.append("<td>").append(entry.getValue()).append(" rows").append("</td></tr>");
            }
            cacheInfo.append("</table>");
            cacheInfo.append("</html>");
            statusLabel.setToolTipText(cacheInfo.toString());
            clearButton.setVisible(true);
        }
    }

    /**
     * Triggered when the cache was updated
     *
     * @param event generated event
     */
    @Subscribe
    public void onCacheUpdated(ACAQProjectCache.ModifiedEvent event) {
        updateStatus();
    }

}
