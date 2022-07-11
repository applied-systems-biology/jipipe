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
 *
 */

package org.hkijena.jipipe.ui.extensions;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.registries.JIPipeExtensionRegistry;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class JIPipeModernPluginManagerUI extends JIPipeWorkbenchPanel {

    private ExtensionListPanel extensionListPanel;

    private JPanel sidePanel;
    private List<JIPipeDependency> currentlyShownItems = new ArrayList<>(JIPipe.getInstance().getExtensionRegistry().getKnownExtensions());

    public JIPipeModernPluginManagerUI(JIPipeWorkbench workbench) {
        super(workbench);
        initialize();
    }

    private void initialize() {
        // Main panel
        setLayout(new BorderLayout());

        AutoResizeSplitPane splitPane = new AutoResizeSplitPane(JSplitPane.HORIZONTAL_SPLIT, 1.0 / 5.0);
        add(splitPane, BorderLayout.CENTER);

        extensionListPanel = new ExtensionListPanel(getWorkbench(), JIPipe.getInstance().getExtensionRegistry().getKnownExtensions());
        splitPane.setRightComponent(extensionListPanel);

        // Side panel
        sidePanel = new JPanel();
        sidePanel.setLayout(new GridBagLayout());
        splitPane.setLeftComponent(sidePanel);
        initializeSidePanel();
    }

    private void initializeSidePanel() {
        JIPipeExtensionRegistry extensionRegistry = JIPipe.getInstance().getExtensionRegistry();
        addSidePanelButton("All extensions", UIUtils.getIconFromResources("actions/plugins.png"), extensionRegistry::getKnownExtensions);
        addSidePanelButton("Activated extensions", UIUtils.getIconFromResources("actions/checkmark.png"), () -> extensionRegistry.getKnownExtensions().stream().filter(dependency -> extensionRegistry.getActivatedExtensions().contains(dependency.getDependencyId())).collect(Collectors.toList()));
        addSidePanelButton("Deactivated extensions", UIUtils.getIconFromResources("actions/close-tab.png"), () -> extensionRegistry.getKnownExtensions().stream().filter(dependency -> !extensionRegistry.getActivatedExtensions().contains(dependency.getDependencyId())).collect(Collectors.toList()));
        sidePanel.add(new JPanel(), new GridBagConstraints(0, sidePanel.getComponentCount(), 1,1,1,1, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(4,4,4,4), 0,0));
    }

    private void showItems(List<JIPipeDependency> items) {
        this.currentlyShownItems = items;
        extensionListPanel.setPlugins(items);
    }

    private void addSidePanelButton(String label, Icon icon, Supplier<List<JIPipeDependency>> items) {
        JButton button = new JButton(label, icon);
        button.setFont(new Font(Font.DIALOG, Font.PLAIN, 16));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.addActionListener(e -> showItems(items.get()));
        sidePanel.add(button, new GridBagConstraints(0, sidePanel.getComponentCount(), 1,1,1,0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(4,4,4,4), 0,0));
    }
}
