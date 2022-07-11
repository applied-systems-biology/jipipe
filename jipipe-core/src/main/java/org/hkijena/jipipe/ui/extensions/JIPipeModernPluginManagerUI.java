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
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.NaturalOrderComparator;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class JIPipeModernPluginManagerUI extends JIPipeWorkbenchPanel {

    private ExtensionListPanel extensionListPanel;

    private JPanel sidePanel;

    private SearchTextField searchTextField;
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

        JPanel mainPanel = new JPanel(new BorderLayout());
        extensionListPanel = new ExtensionListPanel(getWorkbench(), JIPipe.getInstance().getExtensionRegistry().getKnownExtensions());
        mainPanel.add(extensionListPanel, BorderLayout.CENTER);

        JToolBar mainPanelToolbar = new JToolBar();
        mainPanelToolbar.setFloatable(false);
        mainPanel.add(mainPanelToolbar, BorderLayout.NORTH);

        searchTextField = new SearchTextField();
        mainPanelToolbar.add(searchTextField);
        searchTextField.addActionListener(e ->  updateSearch());

        splitPane.setRightComponent(mainPanel);

        // Side panel
        sidePanel = new JPanel();
        sidePanel.setLayout(new GridBagLayout());
        splitPane.setLeftComponent(sidePanel);
        initializeSidePanel();
    }

    private void updateSearch() {
        String[] searchStrings = searchTextField.getSearchStrings();
        if(searchStrings == null || searchStrings.length == 0) {
            extensionListPanel.setPlugins(currentlyShownItems);
        }
        else {
            List<JIPipeDependency> filtered = new ArrayList<>();
            for (JIPipeDependency item : currentlyShownItems) {
                String combined = item.getMetadata().getName() + item.getMetadata().getDescription() + String.join("", item.getMetadata().getProcessedCategories());
                combined = combined.toLowerCase();
                for (String searchString : searchStrings) {
                    if(combined.contains(StringUtils.nullToEmpty(searchString).toLowerCase())) {
                        filtered.add(item);
                        break;
                    }
                }
            }
            extensionListPanel.setPlugins(filtered);
        }
    }

    private void initializeSidePanel() {
        JIPipeExtensionRegistry extensionRegistry = JIPipe.getInstance().getExtensionRegistry();
        addSidePanelButton("All extensions", UIUtils.getIconFromResources("actions/plugins.png"), extensionRegistry::getKnownExtensions, false);

        Set<String> categories = new HashSet<>();
        for (JIPipeDependency extension : JIPipe.getInstance().getExtensionRegistry().getKnownExtensions()) {
            categories.addAll(extension.getMetadata().getProcessedCategories());
        }
        categories.stream().sorted(NaturalOrderComparator.INSTANCE).forEach(category -> {
            addSidePanelButton(category, UIUtils.getIconFromResources("actions/tag.png"), () -> JIPipe.getInstance().getExtensionRegistry().getKnownExtensions().stream().filter(dependency -> dependency.getMetadata().getProcessedCategories().contains(category)).collect(Collectors.toList()), true);
        });

        addSidePanelButton("Activated extensions", UIUtils.getIconFromResources("actions/checkmark.png"), () -> extensionRegistry.getKnownExtensions().stream().filter(dependency -> extensionRegistry.getActivatedExtensions().contains(dependency.getDependencyId())).collect(Collectors.toList()), false);
        addSidePanelButton("Deactivated extensions", UIUtils.getIconFromResources("actions/close-tab.png"), () -> extensionRegistry.getKnownExtensions().stream().filter(dependency -> !extensionRegistry.getActivatedExtensions().contains(dependency.getDependencyId())).collect(Collectors.toList()), false);
        sidePanel.add(new JPanel(), new GridBagConstraints(0, sidePanel.getComponentCount(), 1,1,1,1, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(4,4,4,4), 0,0));
    }

    private void showItems(List<JIPipeDependency> items) {
        this.currentlyShownItems = items;
        updateSearch();
    }

    private void addSidePanelButton(String label, Icon icon, Supplier<List<JIPipeDependency>> items, boolean small) {
        JButton button = new JButton(label, icon);
        Insets insets;
        if(small) {
            button.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
            insets = new Insets(4,16,4,4);
            button.setBorder(null);
        }
        else {
            button.setFont(new Font(Font.DIALOG, Font.PLAIN, 16));
            insets = new Insets(4,4,4,4);
        }
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.addActionListener(e -> showItems(items.get()));
        sidePanel.add(button, new GridBagConstraints(0, sidePanel.getComponentCount(), 1,1,1,0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, insets, 0,0));
    }
}
