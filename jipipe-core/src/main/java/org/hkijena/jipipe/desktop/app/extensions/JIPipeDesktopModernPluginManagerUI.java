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

package org.hkijena.jipipe.desktop.app.extensions;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipePlugin;
import org.hkijena.jipipe.api.registries.JIPipeExtensionRegistry;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopMessagePanel;
import org.hkijena.jipipe.desktop.commons.components.icons.JIPipeDesktopAnimatedIcon;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.NaturalOrderComparator;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class JIPipeDesktopModernPluginManagerUI extends JIPipeDesktopWorkbenchPanel implements JIPipeDesktopModernPluginManager.UpdateSitesReadyEventListener, JIPipeDesktopModernPluginManager.UpdateSitesFailedEventListener {

    private final JIPipeDesktopMessagePanel messagePanel = new JIPipeDesktopMessagePanel();
    private final JIPipeDesktopModernPluginManager pluginManager;
    private JIPipeDesktopExtensionListPanel extensionListPanel;
    private JIPipeDesktopFormPanel sidePanel;
    private JIPipeDesktopSearchTextField searchTextField;
    private List<JIPipePlugin> currentlyShownItems = new ArrayList<>(getExtensionRegistry().getKnownExtensionsList());
    private JLabel currentListHeading;
    private JButton updateSitesButton;
    private AutoResizeSplitPane splitPane;
    private JPanel mainPanel;

    public JIPipeDesktopModernPluginManagerUI(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        this.pluginManager = new JIPipeDesktopModernPluginManager(workbench, this, messagePanel);

        initialize();
//        JIPipe.getInstance().getExtensionRegistry().getEventBus().register(this);
        if (getExtensionRegistry().getNewExtensions().isEmpty()) {
            showItems(getExtensionRegistry().getKnownExtensionsList(), "All extensions");
        } else {
            showItems(getExtensionRegistry().getNewExtensions().stream().map(id -> getExtensionRegistry().getKnownExtensionById(id)).collect(Collectors.toList()), "New extensions");
            // Acknowledge the extensions
            getExtensionRegistry().dismissNewExtensions();
        }

//        JIPipeRunnerQueue.getInstance().getEventBus().register(this);

        pluginManager.getUpdateSitesReadyEventEmitter().subscribeWeak(this);
        pluginManager.getUpdateSitesFailedEventEmitter().subscribeWeak(this);
        pluginManager.initializeUpdateSites();
    }

    @Override
    public void onPluginManagerUpdateSitesReady(JIPipeDesktopModernPluginManager.UpdateSitesReadyEvent event) {
        updateSitesButton.setIcon(UIUtils.getIconFromResources("actions/web-browser.png"));
    }

    @Override
    public void onPluginManagerUpdateSitesFailed(JIPipeDesktopModernPluginManager.UpdateSitesFailedEvent event) {
        updateSitesButton.setToolTipText("Could not connect to the ImageJ update service");
        updateSitesButton.setIcon(UIUtils.getIconFromResources("emblems/emblem-rabbitvcs-conflicted.png"));
    }

    private void initialize() {
        // Main panel
        setLayout(new BorderLayout());

        splitPane = new AutoResizeSplitPane(JSplitPane.HORIZONTAL_SPLIT, 1.0 / 5.0);
        add(splitPane, BorderLayout.CENTER);

        mainPanel = new JPanel(new BorderLayout());
        extensionListPanel = new JIPipeDesktopExtensionListPanel(this);
        mainPanel.add(extensionListPanel, BorderLayout.CENTER);

        JPanel mainHeaderPanel = new JPanel(new BorderLayout());

        JToolBar mainPanelToolbar = new JToolBar();
        mainPanelToolbar.setFloatable(false);
        mainHeaderPanel.add(mainPanelToolbar, BorderLayout.CENTER);
        mainHeaderPanel.add(messagePanel, BorderLayout.NORTH);

        currentListHeading = new JLabel();
        currentListHeading.setFont(new Font(Font.DIALOG, Font.PLAIN, 32));
        currentListHeading.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        mainHeaderPanel.add(currentListHeading, BorderLayout.SOUTH);

        mainPanel.add(mainHeaderPanel, BorderLayout.NORTH);

        searchTextField = new JIPipeDesktopSearchTextField();
        mainPanelToolbar.add(searchTextField);
        searchTextField.addActionListener(e -> updateSearch());

        splitPane.setRightComponent(mainPanel);

        // Side panel
        sidePanel = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.WITH_SCROLLING);
        splitPane.setLeftComponent(sidePanel);

//        sidePanel.addWideToForm(messagePanel, null);
        initializeSidePanel();
        sidePanel.addVerticalGlue();
    }

    public JIPipeDesktopModernPluginManager getPluginManager() {
        return pluginManager;
    }

    private JIPipeExtensionRegistry getExtensionRegistry() {
        return JIPipe.getInstance().getExtensionRegistry();
    }

    private void updateSearch() {
        String[] searchStrings = searchTextField.getSearchStrings();
        if (searchStrings == null || searchStrings.length == 0) {
            extensionListPanel.setPlugins(currentlyShownItems);
        } else {
            List<JIPipePlugin> filtered = new ArrayList<>();
            for (JIPipePlugin item : currentlyShownItems) {
                String combined = item.getMetadata().getName() + item.getMetadata().getDescription() + String.join("", item.getMetadata().getProcessedCategories());
                combined = combined.toLowerCase();
                for (String searchString : searchStrings) {
                    if (combined.contains(StringUtils.nullToEmpty(searchString).toLowerCase())) {
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

        Set<String> newExtensions = JIPipe.getInstance().getExtensionRegistry().getNewExtensions();
        if (!newExtensions.isEmpty()) {
            addSidePanelButton("New extensions", UIUtils.getIconFromResources("emblems/emblem-important-blue.png"), () -> newExtensions.stream().map(id -> extensionRegistry.getKnownExtensionById(id)).collect(Collectors.toList()), false);
            messagePanel.addMessage(JIPipeDesktopMessagePanel.MessageType.Info, "New extensions are available", true, true);
        }

        addSidePanelButton("Activated extensions", UIUtils.getIconFromResources("actions/checkmark.png"), () -> extensionRegistry.getKnownExtensionsList().stream().filter(dependency -> extensionRegistry.getActivatedExtensions().contains(dependency.getDependencyId())).collect(Collectors.toList()), false);
        addSidePanelButton("Deactivated extensions", UIUtils.getIconFromResources("actions/close-tab.png"), () -> extensionRegistry.getKnownExtensionsList().stream().filter(dependency -> !extensionRegistry.getActivatedExtensions().contains(dependency.getDependencyId())).collect(Collectors.toList()), false);

        // Update sites button
        JIPipeDesktopAnimatedIcon hourglassAnimation = new JIPipeDesktopAnimatedIcon(this, UIUtils.getIconFromResources("actions/hourglass-half.png"),
                UIUtils.getIconFromResources("emblems/hourglass-half.png"),
                100, 0.05);
        hourglassAnimation.start();
        updateSitesButton = addImageJSidePanelButton("ImageJ plugins", hourglassAnimation, site -> true, false);
        addImageJSidePanelButton("Activated", UIUtils.getIconFromResources("actions/checkmark.png"), site -> site.isActivated(), true);
        addImageJSidePanelButton("Deactivated", UIUtils.getIconFromResources("actions/close-tab.png"), site -> !site.isActivated(), true);

        addSidePanelButton("All extensions", UIUtils.getIconFromResources("actions/plugins.png"), extensionRegistry::getKnownExtensionsList, false);

        Set<String> categories = new HashSet<>();
        for (JIPipeDependency extension : JIPipe.getInstance().getExtensionRegistry().getKnownExtensionsList()) {
            categories.addAll(extension.getMetadata().getProcessedCategories());
        }
        categories.stream().sorted(NaturalOrderComparator.INSTANCE).forEach(category -> {
            addSidePanelButton(category, UIUtils.getIconFromResources("actions/tag.png"), () -> JIPipe.getInstance().getExtensionRegistry().getKnownExtensionsList().stream().filter(dependency -> dependency.getMetadata().getProcessedCategories().contains(category)).collect(Collectors.toList()), true);
        });

    }

    private void showItems(List<JIPipePlugin> items, String heading) {
        this.currentlyShownItems = items;
        currentListHeading.setText(heading);
        splitPane.setRightComponent(mainPanel);
        updateSearch();
    }

    private void addSidePanelButton(String label, Icon icon, Supplier<List<JIPipePlugin>> items, boolean small) {
        JButton button = new JButton(label, icon);
        if (small) {
            button.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
            button.setBorder(BorderFactory.createEmptyBorder(2, 16, 2, 4));
        } else {
            button.setFont(new Font(Font.DIALOG, Font.PLAIN, 16));
            button.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        }
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.addActionListener(e -> {
            currentListHeading.setText(label);
            showItems(items.get(), label);
        });
        sidePanel.addWideToForm(button, null);
    }

    private JButton addImageJSidePanelButton(String label, Icon icon, Predicate<JIPipeDesktopUpdateSitePlugin> filter, boolean small) {
        JButton button = new JButton(label, icon);
        if (small) {
            button.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
            button.setBorder(BorderFactory.createEmptyBorder(2, 16, 2, 4));
        } else {
            button.setFont(new Font(Font.DIALOG, Font.PLAIN, 16));
            button.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        }
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.addActionListener(e -> {
            if (pluginManager.isUpdateSitesReady()) {
                currentListHeading.setText(label);
                showItems(pluginManager.getUpdateSiteWrapperExtensions().stream().filter(filter).collect(Collectors.toList()), label);
            }
        });
        sidePanel.addWideToForm(button, null);
//        updateSiteButtons.add(button);
        return button;
    }

    public void showExtensionDetails(JIPipePlugin extension) {
        JPanel detailsPanel = new JPanel(new BorderLayout());
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton backButton = new JButton("Go back", UIUtils.getIconFromResources("actions/back.png"));
        backButton.setFont(new Font(Font.DIALOG, Font.PLAIN, 20));
        backButton.addActionListener(e -> {
            splitPane.setRightComponent(mainPanel);
        });
        toolBar.add(backButton);
        detailsPanel.add(toolBar, BorderLayout.NORTH);

        JIPipeDesktopExtensionInfoPanel infoPanel = new JIPipeDesktopExtensionInfoPanel(this, extension);
        detailsPanel.add(infoPanel, BorderLayout.CENTER);

        splitPane.setRightComponent(detailsPanel);
    }


}
