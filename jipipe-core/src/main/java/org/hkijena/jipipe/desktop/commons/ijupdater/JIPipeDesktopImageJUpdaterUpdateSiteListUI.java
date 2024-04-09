/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.commons.ijupdater;

import net.imagej.updater.FilesCollection;
import net.imagej.updater.UpdateSite;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFancyTextField;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.search.RankedData;
import org.hkijena.jipipe.utils.search.RankingFunction;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class JIPipeDesktopImageJUpdaterUpdateSiteListUI extends JPanel {
    private static final RankingFunction<UpdateSite> rankingFunction = new Ranking();
    private final JIPipeDesktopImageJUpdaterPluginManagerUI pluginManager;
    private FilesCollection filesCollection;
    private JIPipeDesktopFormPanel contentPanel;
    private JIPipeDesktopSearchTextField searchTextField;
    private JButton addButton;

    public JIPipeDesktopImageJUpdaterUpdateSiteListUI(JIPipeDesktopImageJUpdaterPluginManagerUI pluginManager) {
        this.pluginManager = pluginManager;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        contentPanel = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.WITH_SCROLLING);
        add(contentPanel, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        searchTextField = new JIPipeDesktopSearchTextField();
        searchTextField.addActionListener(e -> refreshList());
        toolBar.add(searchTextField);

        addButton = new JButton("Add", UIUtils.getIconFromResources("actions/list-add.png"));
        addButton.addActionListener(e -> addUpdateSite());
        toolBar.add(addButton);

        add(toolBar, BorderLayout.NORTH);
    }

    private void addUpdateSite() {
        if (pluginManager.isCurrentlyRunning()) {
            JOptionPane.showMessageDialog(this,
                    "There is already an operation running. Please wait until it is finished.",
                    "Add new update site", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.NONE);
        JIPipeDesktopFancyTextField nameField = new JIPipeDesktopFancyTextField(null, "", true);
        JIPipeDesktopFancyTextField urlField = new JIPipeDesktopFancyTextField(new JLabel(UIUtils.getIconFromResources("actions/web-browser.png")), "", true);
        urlField.styleText(true, false, false);
        formPanel.addToForm(nameField, new JLabel("Name"), null);
        formPanel.addToForm(urlField, new JLabel("URL"), null);
        if (JOptionPane.showConfirmDialog(this,
                formPanel,
                "Add new update site",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            if (StringUtils.isNullOrEmpty(nameField.getText())) {
                JOptionPane.showMessageDialog(this, "The name is empty!", "Add new update site", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (StringUtils.isNullOrEmpty(urlField.getText())) {
                JOptionPane.showMessageDialog(this, "The URL is empty!", "Add new update site", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (filesCollection != null) {
                UpdateSite updateSite = new UpdateSite(nameField.getText(), urlField.getText(), "", "", "", "", 0);
                pluginManager.addUpdateSite(updateSite);
                refreshList();

            }
        }
    }

    public void refreshList() {
        int scrollPosition = contentPanel.getScrollPane().getVerticalScrollBar().getValue();
        Collection<UpdateSite> updateSites = Collections.emptySet();
        if (filesCollection != null)
            updateSites = filesCollection.getUpdateSites(true);
        List<UpdateSite> filteredUpdateSites = RankedData.getSortedAndFilteredData(updateSites, UpdateSite::getName, rankingFunction, searchTextField.getSearchStrings());
        contentPanel.clear();
        for (UpdateSite updateSite : filteredUpdateSites) {
            JIPipeDesktopImageJUpdaterUpdateSiteUI updateSiteUI = new JIPipeDesktopImageJUpdaterUpdateSiteUI(this, updateSite);
            contentPanel.addWideToForm(updateSiteUI, null);
        }
        contentPanel.addVerticalGlue();
        SwingUtilities.invokeLater(() -> contentPanel.getScrollPane().getVerticalScrollBar().setValue(scrollPosition));
    }

    public FilesCollection getFilesCollection() {
        return filesCollection;
    }

    public void setFilesCollection(FilesCollection filesCollection) {
        this.filesCollection = filesCollection;
        this.addButton.setEnabled(filesCollection != null);
        refreshList();
    }

    public JIPipeDesktopImageJUpdaterPluginManagerUI getPluginManager() {
        return pluginManager;
    }

    public static class Ranking implements RankingFunction<UpdateSite> {

        @Override
        public int[] rank(UpdateSite value, String[] filterStrings) {
            int[] result = new int[2];

            if (filterStrings.length == 0)
                return result;

            for (String string : filterStrings) {
                if (value.getName().toLowerCase().contains(string.toLowerCase()))
                    --result[0];
                if (StringUtils.orElse(value.getDescription(), "").toLowerCase().contains(string.toLowerCase()))
                    --result[1];
            }

            if (result[0] == 0 && result[1] == 0) {
                return null;
            } else {
                return result;
            }
        }
    }
}
