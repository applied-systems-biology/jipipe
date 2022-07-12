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

import com.google.common.eventbus.Subscribe;
import net.imagej.ui.swing.updater.ImageJUpdater;
import net.imagej.updater.FilesCollection;
import net.imagej.updater.UpdateSite;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeExtension;
import org.hkijena.jipipe.api.registries.JIPipeExtensionRegistry;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.MessagePanel;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.ui.ijupdater.RefreshRepositoryRun;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.ui.running.RunUIWorkerFinishedEvent;
import org.hkijena.jipipe.ui.running.RunUIWorkerInterruptedEvent;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.CoreImageJUtils;
import org.hkijena.jipipe.utils.NaturalOrderComparator;
import org.hkijena.jipipe.utils.NetworkUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Font;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class JIPipeModernPluginManagerUI extends JIPipeWorkbenchPanel {

    private ExtensionListPanel extensionListPanel;

    private FormPanel sidePanel;

    private SearchTextField searchTextField;
    private List<JIPipeExtension> currentlyShownItems = new ArrayList<>(getExtensionRegistry().getKnownExtensions());

    private final MessagePanel messagePanel = new MessagePanel();

    private JLabel currentListHeading;

    private JButton updateSitesButton;
    private RefreshRepositoryRun refreshRepositoryRun;

    private MessagePanel.Message updateSiteMessage;
    private boolean updateSitesReady = false;

    private FilesCollection updateSites;

    private final List<JIPipeExtension> updateSiteWrapperExtensions = new ArrayList<>();

    private AutoResizeSplitPane splitPane;

    private JPanel mainPanel;

    public JIPipeModernPluginManagerUI(JIPipeWorkbench workbench) {
        super(workbench);
        initialize();
        JIPipe.getInstance().getExtensionRegistry().getEventBus().register(this);

        updateMessagePanel();
        if(getExtensionRegistry().getNewExtensions().isEmpty()) {
            showItems(getExtensionRegistry().getKnownExtensions(), "All extensions");
        }
        else {
            showItems(getExtensionRegistry().getNewExtensions().stream().map(id -> getExtensionRegistry().getKnownExtensionById(id)).collect(Collectors.toList()), "New extensions");
        }

        JIPipeRunnerQueue.getInstance().getEventBus().register(this);

        initializeUpdateSites();
    }

    private void initializeUpdateSites() {
        updateSiteMessage = messagePanel.addMessage(MessagePanel.MessageType.Info, "ImageJ update sites are currently being loaded. Until this process is finished, ImageJ plugins cannot be managed.", null);
        if (ImageJUpdater.isDebian()) {
            messagePanel.addMessage(MessagePanel.MessageType.Error, "You are using the Debian packaged version of ImageJ. " +
                    "You should update ImageJ with your system's usual package manager instead.", null);
            setToImageJFailure();
            return;
        }
        if (!NetworkUtils.hasInternetConnection()) {
            messagePanel.addMessage(MessagePanel.MessageType.Error, "Cannot connect to the Internet. Do you have a network connection? " +
                    "Are your proxy settings correct? See also http://forum.imagej.net/t/5070", null);
            setToImageJFailure();
            return;
        }
        if (Files.exists(CoreImageJUtils.getImageJUpdaterRoot().resolve("update"))) {
            messagePanel.addMessage(MessagePanel.MessageType.Warning, "We recommend to restart ImageJ, as some updates were applied.", null);
        }
        refreshRepositoryRun = new RefreshRepositoryRun();
        JIPipeRunnerQueue.getInstance().enqueue(refreshRepositoryRun);
    }

    private void setToImageJFailure() {
        removeUpdateSiteMessage();
        updateSitesButton.setEnabled(false);
        updateSitesButton.setToolTipText("Could not connect to the ImageJ update service");
        updateSitesButton.setIcon(UIUtils.getIconFromResources("emblems/emblem-rabbitvcs-conflicted.png"));
    }

    private void removeUpdateSiteMessage() {
        if(updateSiteMessage != null) {
            messagePanel.removeMessage(updateSiteMessage);
            updateSiteMessage = null;
        }
    }

    private void setToImageJSuccess() {
        removeUpdateSiteMessage();
        createUpdateSitesWrappers();
        updateSitesButton.setEnabled(true);
        updateSitesButton.setIcon(UIUtils.getIconFromResources("actions/web-browser.png"));
        updateSitesReady = true;
    }

    private void createUpdateSitesWrappers() {
        updateSiteWrapperExtensions.clear();
        if(updateSites != null) {
            for (UpdateSite updateSite : updateSites.getUpdateSites(true)) {
                UpdateSiteExtension extension = new UpdateSiteExtension(updateSite);
                updateSiteWrapperExtensions.add(extension);
            }
        }
    }

    public boolean isUpdateSitesReady() {
        return updateSitesReady;
    }

    public FilesCollection getUpdateSites() {
        return updateSites;
    }

    private void initialize() {
        // Main panel
        setLayout(new BorderLayout());

        splitPane = new AutoResizeSplitPane(JSplitPane.HORIZONTAL_SPLIT, 1.0 / 5.0);
        add(splitPane, BorderLayout.CENTER);

        mainPanel = new JPanel(new BorderLayout());
        extensionListPanel = new ExtensionListPanel(this);
        mainPanel.add(extensionListPanel, BorderLayout.CENTER);

        JPanel mainHeaderPanel = new JPanel(new BorderLayout());

        JToolBar mainPanelToolbar = new JToolBar();
        mainPanelToolbar.setFloatable(false);
        mainHeaderPanel.add(mainPanelToolbar, BorderLayout.CENTER);
        mainHeaderPanel.add(messagePanel, BorderLayout.NORTH);

        currentListHeading = new JLabel();
        currentListHeading.setFont(new Font(Font.DIALOG, Font.PLAIN, 32));
        currentListHeading.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        mainHeaderPanel.add(currentListHeading, BorderLayout.SOUTH);

        mainPanel.add(mainHeaderPanel, BorderLayout.NORTH);

        searchTextField = new SearchTextField();
        mainPanelToolbar.add(searchTextField);
        searchTextField.addActionListener(e ->  updateSearch());

        splitPane.setRightComponent(mainPanel);

        // Side panel
        sidePanel = new FormPanel(null, FormPanel.WITH_SCROLLING);
        splitPane.setLeftComponent(sidePanel);

//        sidePanel.addWideToForm(messagePanel, null);
        initializeSidePanel();
        sidePanel.addVerticalGlue();
    }

    private void updateMessagePanel() {
        messagePanel.clear();
        JIPipeExtensionRegistry extensionRegistry = getExtensionRegistry();
        if(!extensionRegistry.getScheduledDeactivateExtensions().isEmpty() || !extensionRegistry.getScheduledActivateExtensions().isEmpty()){
            JButton exitButton = new JButton("Close ImageJ");
            exitButton.addActionListener(e -> System.exit(0));
            messagePanel.addMessage(MessagePanel.MessageType.Info, "To apply the changes, please restart ImageJ.", exitButton);
        }
    }

    private JIPipeExtensionRegistry getExtensionRegistry() {
        return JIPipe.getInstance().getExtensionRegistry();
    }

    private void updateSearch() {
        String[] searchStrings = searchTextField.getSearchStrings();
        if(searchStrings == null || searchStrings.length == 0) {
            extensionListPanel.setPlugins(currentlyShownItems);
        }
        else {
            List<JIPipeExtension> filtered = new ArrayList<>();
            for (JIPipeExtension item : currentlyShownItems) {
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

    @Subscribe
    public void onExtensionActivated(JIPipeExtensionRegistry.ScheduledActivateExtension event) {
        updateMessagePanel();
    }

    @Subscribe
    public void onExtensionDeactivated(JIPipeExtensionRegistry.ScheduledDeactivateExtension event) {
        updateMessagePanel();
    }

    private void initializeSidePanel() {
        JIPipeExtensionRegistry extensionRegistry = JIPipe.getInstance().getExtensionRegistry();

        Set<String> newExtensions = JIPipe.getInstance().getExtensionRegistry().getNewExtensions();
        if(!newExtensions.isEmpty()) {
            addSidePanelButton("New extensions", UIUtils.getIconFromResources("emblems/emblem-important-blue.png"), () -> newExtensions.stream().map(id -> extensionRegistry.getKnownExtensionById(id)).collect(Collectors.toList()), false);
            messagePanel.addMessage(MessagePanel.MessageType.Info, "New extensions are available", null);
        }

        addSidePanelButton("Activated extensions", UIUtils.getIconFromResources("actions/checkmark.png"), () -> extensionRegistry.getKnownExtensions().stream().filter(dependency -> extensionRegistry.getActivatedExtensions().contains(dependency.getDependencyId())).collect(Collectors.toList()), false);
        addSidePanelButton("Deactivated extensions", UIUtils.getIconFromResources("actions/close-tab.png"), () -> extensionRegistry.getKnownExtensions().stream().filter(dependency -> !extensionRegistry.getActivatedExtensions().contains(dependency.getDependencyId())).collect(Collectors.toList()), false);

        // Update sites button
        {
            updateSitesButton = new JButton("ImageJ plugins", UIUtils.getIconFromResources("emblems/hourglass-half.png"));
            updateSitesButton.setEnabled(false);
            updateSitesButton.setFont(new Font(Font.DIALOG, Font.PLAIN, 16));
            updateSitesButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            updateSitesButton.setHorizontalAlignment(SwingConstants.LEFT);
            updateSitesButton.addActionListener(e -> showUpdateSites());
            sidePanel.addWideToForm(updateSitesButton, null);
        }

        addSidePanelButton("All extensions", UIUtils.getIconFromResources("actions/plugins.png"), extensionRegistry::getKnownExtensions, false);

        Set<String> categories = new HashSet<>();
        for (JIPipeDependency extension : JIPipe.getInstance().getExtensionRegistry().getKnownExtensions()) {
            categories.addAll(extension.getMetadata().getProcessedCategories());
        }
        categories.stream().sorted(NaturalOrderComparator.INSTANCE).forEach(category -> {
            addSidePanelButton(category, UIUtils.getIconFromResources("actions/tag.png"), () -> JIPipe.getInstance().getExtensionRegistry().getKnownExtensions().stream().filter(dependency -> dependency.getMetadata().getProcessedCategories().contains(category)).collect(Collectors.toList()), true);
        });

    }

    private void showUpdateSites() {
        showItems(updateSiteWrapperExtensions, "ImageJ plugins");
    }

    private void showItems(List<JIPipeExtension> items, String heading) {
        this.currentlyShownItems = items;
        currentListHeading.setText(heading);
        splitPane.setRightComponent(mainPanel);
        updateSearch();
    }

    private void addSidePanelButton(String label, Icon icon, Supplier<List<JIPipeExtension>> items, boolean small) {
        JButton button = new JButton(label, icon);
        if(small) {
            button.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
            button.setBorder(BorderFactory.createEmptyBorder(2,16,2,4));
        }
        else {
            button.setFont(new Font(Font.DIALOG, Font.PLAIN, 16));
            button.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
        }
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.addActionListener(e -> {
            currentListHeading.setText(label);
            showItems(items.get(), label);
        });
        sidePanel.addWideToForm(button, null);
    }

    @Subscribe
    public void onOperationInterrupted(RunUIWorkerInterruptedEvent event) {
        if (event.getRun() == refreshRepositoryRun) {
            messagePanel.addMessage(MessagePanel.MessageType.Error, "There was an error during the ImageJ update site update.", null);
            getWorkbench().sendStatusBarText("Could not refresh ImageJ plugin information from online resources");
            setToImageJFailure();
        }
    }

    @Subscribe
    public void onOperationFinished(RunUIWorkerFinishedEvent event) {
        if (event.getRun() == refreshRepositoryRun) {
            getWorkbench().sendStatusBarText("Refreshed ImageJ plugin information from online resources");
            this.updateSites = refreshRepositoryRun.getFilesCollection();
            setToImageJSuccess();
        }
    }

    public void showExtensionDetails(JIPipeExtension extension) {
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

        ExtensionInfoPanel infoPanel = new ExtensionInfoPanel(this, extension);
        detailsPanel.add(infoPanel, BorderLayout.CENTER);

        splitPane.setRightComponent(detailsPanel);
    }
}
