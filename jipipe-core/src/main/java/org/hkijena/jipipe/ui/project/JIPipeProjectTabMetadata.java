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

package org.hkijena.jipipe.ui.project;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.grapheditor.JIPipePipelineGraphEditorUI;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Metadata that allows the restoration of tabs
 */
public class JIPipeProjectTabMetadata {

    public static String METADATA_KEY = "org.hkijena.jipipe.ui:project-tabs";
    private List<String> openTabs = new ArrayList<>();
    private String selectedTab;

    public JIPipeProjectTabMetadata() {

    }

    public JIPipeProjectTabMetadata(JIPipeProjectWorkbench workbench) {
        DocumentTabPane documentTabPane = workbench.getDocumentTabPane();
        for (int i = 0; i < documentTabPane.getTabCount(); i++) {
            Component component = documentTabPane.getTabbedPane().getComponentAt(i);
            DocumentTabPane.DocumentTab tab = documentTabPane.getTabContainingContent(component);
            String singletonTabId = documentTabPane.getSingletonTabs().inverse().getOrDefault(tab, null);
            String id = null;
            if (singletonTabId != null) {
                id = "singleton:" + singletonTabId;
            } else if (component instanceof JIPipePipelineGraphEditorUI) {
                JIPipePipelineGraphEditorUI graphCompartmentUI = (JIPipePipelineGraphEditorUI) component;
                id = "graph-compartment:" + graphCompartmentUI.getCompartment();
            }

            if (id != null) {
                openTabs.add(id);
                if (component == documentTabPane.getCurrentContent()) {
                    selectedTab = id;
                }
            }
        }
    }

    public void restore(JIPipeProjectWorkbench workbench) {
        DocumentTabPane documentTabPane = workbench.getDocumentTabPane();
//        documentTabPane.closeAllTabs();
        Map<String, DocumentTabPane.DocumentTab> tabIds = new HashMap<>();
        for (String id : openTabs) {
            if (id.startsWith("singleton:")) {
                String singletonId = id.substring("singleton:".length());
                DocumentTabPane.DocumentTab tab = documentTabPane.selectSingletonTab(singletonId);
                tabIds.put(id, tab);
            } else if (id.startsWith("graph-compartment:")) {
                String compartmentId = id.substring("graph-compartment:".length());
                try {
                    JIPipeProjectCompartment compartment = workbench.getProject().findCompartment(compartmentId);
                    if (compartment != null) {
                        DocumentTabPane.DocumentTab tab = workbench.getOrOpenPipelineEditorTab(compartment, false);
                        if (tab != null) {
                            tabIds.put(id, tab);
                        }
                    }
                } catch (IllegalArgumentException e) {
                }
            }
        }
        if (!StringUtils.isNullOrEmpty(selectedTab)) {
            DocumentTabPane.DocumentTab tab = tabIds.getOrDefault(selectedTab, null);
            if (tab != null) {
                documentTabPane.switchToContent(tab.getContent());
            }
        }

        // Workaround slow computers not loading properly
        SwingUtilities.invokeLater(() -> {
            documentTabPane.revalidate();
            documentTabPane.repaint();
        });
    }

    @JsonGetter("open-tabs")
    public List<String> getOpenTabs() {
        return openTabs;
    }

    @JsonSetter("open-tabs")
    public void setOpenTabs(List<String> openTabs) {
        this.openTabs = openTabs;
    }

    @JsonGetter("selected-tab")
    public String getSelectedTab() {
        return selectedTab;
    }

    @JsonSetter("selected-tab")
    public void setSelectedTab(String selectedTab) {
        this.selectedTab = selectedTab;
    }
}
