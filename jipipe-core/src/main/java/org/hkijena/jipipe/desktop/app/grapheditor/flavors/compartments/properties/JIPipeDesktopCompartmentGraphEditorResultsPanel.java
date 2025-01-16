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

package org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.properties;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartmentOutput;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.cache.JIPipeDesktopAlgorithmCacheBrowserUI;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.JIPipeDesktopCompartmentsGraphEditorUI;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.utils.UIUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class JIPipeDesktopCompartmentGraphEditorResultsPanel extends JIPipeDesktopProjectWorkbenchPanel {

    private final JIPipeProjectCompartment compartment;
    private final JIPipeDesktopCompartmentsGraphEditorUI graphEditorUI;
    private final Map<String, JIPipeDesktopAlgorithmCacheBrowserUI> cacheBrowsers = new HashMap<>();
    private final Map<String, JIPipeDesktopTabPane.DocumentTab> cacheBrowserTabs = new HashMap<>();
    private final JIPipeDesktopTabPane tabPane = new JIPipeDesktopTabPane(true, JIPipeDesktopTabPane.TabPlacement.Bottom);

    public JIPipeDesktopCompartmentGraphEditorResultsPanel(JIPipeDesktopProjectWorkbench workbench, JIPipeProjectCompartment compartment, JIPipeDesktopCompartmentsGraphEditorUI graphEditorUI) {
        super(workbench);
        this.compartment = compartment;
        this.graphEditorUI = graphEditorUI;
        initialize();
        refreshTables();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(tabPane, BorderLayout.CENTER);
    }

    public void refreshTables() {
        for (String key : ImmutableList.copyOf(cacheBrowsers.keySet())) {
            if (!compartment.getOutputNodes().containsKey(key)) {
                tabPane.forceCloseTab(cacheBrowserTabs.get(key));
                cacheBrowsers.remove(key);
                cacheBrowserTabs.remove(key);
            }
        }
        for (Map.Entry<String, JIPipeProjectCompartmentOutput> entry : compartment.getOutputNodes().entrySet()) {
            JIPipeDesktopAlgorithmCacheBrowserUI ui = cacheBrowsers.getOrDefault(entry.getKey(), null);
            if (ui == null) {
                ui = new JIPipeDesktopAlgorithmCacheBrowserUI(getDesktopProjectWorkbench(), entry.getValue(), graphEditorUI.getCanvasUI());
                cacheBrowsers.put(entry.getKey(), ui);
                cacheBrowserTabs.put(entry.getKey(),
                        tabPane.addTab(entry.getKey(),
                                UIUtils.getIconFromResources("actions/graph-compartment.png"),
                                ui,
                                JIPipeDesktopTabPane.CloseMode.withoutCloseButton));
            }
            ui.refreshTable();
        }
    }
}
