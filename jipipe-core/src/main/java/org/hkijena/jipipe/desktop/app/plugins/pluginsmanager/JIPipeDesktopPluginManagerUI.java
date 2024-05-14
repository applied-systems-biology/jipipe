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

package org.hkijena.jipipe.desktop.app.plugins.pluginsmanager;

import net.imagej.updater.UpdateSite;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.JIPipePlugin;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.plugins.artifactsmanager.JIPipeDesktopApplyPluginManagerRun;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunExecuteUI;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunnableQueueButton;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopMessagePanel;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopMarkdownReader;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.CoreImageJUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class JIPipeDesktopPluginManagerUI extends JIPipeDesktopWorkbenchPanel implements JIPipeRunnable.FinishedEventListener, JIPipeRunnable.InterruptedEventListener {

    private static JFrame CURRENT_WINDOW = null;
    private final List<PluginEntry> pluginEntryList = new ArrayList<>();
    private final JList<PluginEntry> pluginEntryJList = new JList<>();
    private final JIPipeDesktopFormPanel propertyPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
    private final JCheckBox onlyNewToggle = new JCheckBox("Only new", false);
    private final JIPipeDesktopSearchTextField searchTextField = new JIPipeDesktopSearchTextField();
    private JScrollPane pluginsListScrollPane;
    private static boolean SHOW_RESTART_PROMPT;
    private final JIPipeDesktopImageJUpdateSitesRepository updateSitesRepository;
    private final JPanel managerPanel = new JPanel(new BorderLayout());

    public JIPipeDesktopPluginManagerUI(JIPipeDesktopWorkbench desktopWorkbench) {
        super(desktopWorkbench);
        this.updateSitesRepository = new JIPipeDesktopImageJUpdateSitesRepository(this);
        initialize();
        loadAvailablePlugins();
        updatePluginsList();
        updateSelectionPanel();
        switchToManager();
        JIPipeRunnableQueue.getInstance().getFinishedEventEmitter().subscribe(this);
        JIPipeRunnableQueue.getInstance().getInterruptedEventEmitter().subscribe(this);
    }

    private void switchToManager() {
        removeAll();
        add(managerPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void loadAvailablePlugins() {
        Set<String> newPlugins = new HashSet<>(JIPipe.getInstance().getPluginRegistry().getNewPlugins());
        newPlugins.removeAll(JIPipe.getInstance().getPluginRegistry().getSettings().getSilencedPlugins());
        if (!newPlugins.isEmpty()) {
            onlyNewToggle.setSelected(true);
        }
        for (JIPipePlugin plugin : JIPipe.getInstance().getPluginRegistry().getKnownPluginsList()) {
            PluginEntry pluginEntry = new PluginEntry(plugin);
            pluginEntry.setNewPlugin(newPlugins.contains(plugin.getDependencyId()));
            pluginEntryList.add(pluginEntry);
        }
        pluginEntryList.sort(Comparator.comparing((PluginEntry entry) -> entry.getPlugin().isCorePlugin()).thenComparing((PluginEntry entry) -> entry.getPlugin().getMetadata().getName()));
        JIPipe.getInstance().getPluginRegistry().dismissNewPlugins();
    }

    public static void show(JIPipeDesktopWorkbench desktopWorkbench) {
        if (CURRENT_WINDOW != null) {
            CURRENT_WINDOW.toFront();
        } else {
            JFrame frame = new JFrame("JIPipe - Plugins");
            frame.setIconImage(UIUtils.getJIPipeIcon128());
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    super.windowClosing(e);

                    if (JIPipe.getArtifacts().getQueue().isEmpty()) {
                        frame.setVisible(false);
                        frame.dispose();
                        CURRENT_WINDOW = null;
                    }
                }
            });
            frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            frame.setContentPane(new JIPipeDesktopPluginManagerUI(desktopWorkbench));
            frame.pack();
            frame.setSize(1024, 768);
            frame.setLocationRelativeTo(desktopWorkbench.getWindow());
            frame.setVisible(true);
            CURRENT_WINDOW = frame;
        }

    }

    private void initialize() {
        setLayout(new BorderLayout());

        // Setup manager panel
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.add(searchTextField);
        searchTextField.addActionListener(e -> updatePluginsList());

        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(onlyNewToggle);
        onlyNewToggle.setToolTipText("Only show newly available plugins");
        onlyNewToggle.addActionListener(e -> updatePluginsList());

        toolbar.add(new JIPipeDesktopRunnableQueueButton(getDesktopWorkbench()));
        managerPanel.add(toolbar, BorderLayout.NORTH);

        pluginsListScrollPane = new JScrollPane(pluginEntryJList);
        pluginsListScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        AutoResizeSplitPane splitPane = new AutoResizeSplitPane(AutoResizeSplitPane.LEFT_RIGHT,
                pluginsListScrollPane,
                propertyPanel,
                new AutoResizeSplitPane.DynamicSidebarRatio(350, false));
        managerPanel.add(splitPane, BorderLayout.CENTER);

        pluginEntryJList.setCellRenderer(new PluginEntryListCellRenderer());
        pluginEntryJList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getX() < 22) {
                    if (pluginEntryJList.getSelectedValue() != null) {
                        pluginEntryJList.getSelectedValue().setToggleInstallationStatus(!pluginEntryJList.getSelectedValue().isToggleInstallationStatus());
                        updatePluginsList();
                    }
                }

            }
        });
        pluginEntryJList.addListSelectionListener(e -> updateSelectionPanel());
    }

    private boolean hasChanges() {
        return pluginEntryList.stream().anyMatch(PluginEntry::isToggleInstallationStatus);
    }

    private void updateSelectionPanel() {
        propertyPanel.clear();

        // Look for the updater's update directory
        if (Files.exists(CoreImageJUtils.getImageJUpdaterRoot().resolve("update"))) {
            SHOW_RESTART_PROMPT = true;
        }

        if (SHOW_RESTART_PROMPT) {
            JIPipeDesktopMessagePanel messagePanel = new JIPipeDesktopMessagePanel();
            messagePanel.addMessage(JIPipeDesktopMessagePanel.MessageType.Warning,
                    "You need to restart ImageJ/JIPipe",
                    false,
                    false,
                    UIUtils.createButton("Close", UIUtils.getIconFromResources("actions/gtk-close.png"), this::closeImageJ));
            propertyPanel.addWideToForm(messagePanel);
            return;
        }

        if (hasChanges()) {
            JIPipeDesktopMessagePanel messagePanel = new JIPipeDesktopMessagePanel();
            messagePanel.addMessage(JIPipeDesktopMessagePanel.MessageType.Success,
                    "You have pending changes",
                    false,
                    false,
                    UIUtils.createButton("Revert", UIUtils.getIconFromResources("actions/edit-undo.png"), this::revertChanges),
                    UIUtils.createButton("Apply", UIUtils.getIconFromResources("actions/check.png"), this::applyChanges));
            propertyPanel.addWideToForm(messagePanel);
        }

        PluginEntry selectedValue = pluginEntryJList.getSelectedValue();
        if (selectedValue != null) {
            JIPipePlugin plugin = selectedValue.plugin;
            propertyPanel.addGroupHeader(plugin.getMetadata().getName(), plugin.getMetadata().getDescription().getHtml(), UIUtils.getIconFromResources("actions/puzzle-piece.png"));
            if (plugin.isCorePlugin()) {
                propertyPanel.addToForm(new JLabel("Always active"), new JLabel("Status"));
            } else {
                if (!plugin.isActivated()) {
                    if (selectedValue.isToggleInstallationStatus()) {
                        propertyPanel.addToForm(UIUtils.createLeftAlignedButton("Mark for activation", UIUtils.getIconFromResources("emblems/checkbox-checked.png"), () -> {
                            selectedValue.setToggleInstallationStatus(false);
                            updateSelectionPanel();
                            pluginEntryJList.repaint(50);
                        }), new JLabel("Status"));
                    } else {
                        propertyPanel.addToForm(UIUtils.createLeftAlignedButton("Mark for activation", UIUtils.getIconFromResources("emblems/checkbox-unchecked.png"), () -> {
                            selectedValue.setToggleInstallationStatus(true);
                            updateSelectionPanel();
                            pluginEntryJList.repaint(50);
                        }), new JLabel("Status"));
                    }
                } else {
                    if (selectedValue.isToggleInstallationStatus()) {
                        propertyPanel.addToForm(UIUtils.createLeftAlignedButton("Keep activated", UIUtils.getIconFromResources("emblems/checkbox-unchecked.png"), () -> {
                            selectedValue.setToggleInstallationStatus(false);
                            updateSelectionPanel();
                            pluginEntryJList.repaint(50);
                        }), new JLabel("Status"));
                    } else {
                        propertyPanel.addToForm(UIUtils.createLeftAlignedButton("Keep activated", UIUtils.getIconFromResources("emblems/checkbox-checked.png"), () -> {
                            selectedValue.setToggleInstallationStatus(true);
                            updateSelectionPanel();
                            pluginEntryJList.repaint(50);
                        }), new JLabel("Status"));
                    }
                }
            }

            addAuthors(plugin.getMetadata().getAuthors(), "Plugin authors");
            addAuthors(plugin.getMetadata().getAcknowledgements(), "Acknowledgements");

            if (!StringUtils.isNullOrEmpty(plugin.getMetadata().getCitation())) {
                propertyPanel.addToForm(UIUtils.createBorderlessReadonlyTextPane(plugin.getMetadata().getCitation(), false),
                        new JLabel("Citation"));
            }
            for (String dependencyCitation : plugin.getMetadata().getDependencyCitations()) {
                propertyPanel.addToForm(UIUtils.createBorderlessReadonlyTextPane(dependencyCitation, false),
                        new JLabel("Also cite"));
            }
            propertyPanel.addToForm(UIUtils.createBorderlessReadonlyTextPane(plugin.getDependencyVersion(), false),
                    new JLabel("Version"));
            if (!StringUtils.isNullOrEmpty(plugin.getMetadata().getLicense())) {
                propertyPanel.addToForm(UIUtils.createBorderlessReadonlyTextPane(plugin.getMetadata().getLicense(), false),
                        new JLabel("License"));
            }
            for (JIPipeImageJUpdateSiteDependency updateSiteDependency : plugin.getAllImageJUpdateSiteDependencies()) {
                JButton button = UIUtils.createButton(updateSiteDependency.getName(), UIUtils.getIconFromResources("apps/imagej.png"), () -> {

                });
                button.setHorizontalAlignment(SwingConstants.LEFT);
                JPopupMenu popupMenu = UIUtils.addPopupMenuToButton(button);
                popupMenu.add(UIUtils.createMenuItem("Show info", "Shows information about the update site", UIUtils.getIconFromResources("actions/document-preview.png"), () -> {
                    showImageJUpdateSiteInfo(updateSiteDependency);
                }));
                popupMenu.add(UIUtils.createMenuItem("(Re-)install", "(Re-)installs the update site", UIUtils.getIconFromResources("actions/run-build-install.png"), () -> {
                    installImageJUpdateSite(updateSiteDependency);
                }));
                propertyPanel.addToForm(button, new JLabel("ImageJ dependency"));
            }
        } else {
            propertyPanel.addGroupHeader("Plugins", "The JIPipe feature set can be extended with a variety of plugins. " +
                    "Select items on the left-hand side to show more information about a plugin.", UIUtils.getIconFromResources("actions/help-info.png"));
        }

        propertyPanel.addVerticalGlue();
    }

    private void closeImageJ() {
        if (JOptionPane.showConfirmDialog(this, "Do you really want to close ImageJ/JIPipe?\n" +
                        "All your currently running calculations will be cancelled and unsaved changes will be lost.",
                "Close ImageJ/JIPipe",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
            JIPipe.exitLater(0);
        }
    }

    private void installImageJUpdateSite(JIPipeImageJUpdateSiteDependency updateSiteDependency) {
        if (JOptionPane.showConfirmDialog(this, "Do you really want to (re-)install the update site '" + updateSiteDependency.getName() + "'?",
                "Install update site", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
            switchToRun(new JIPipeDesktopImageJUpdateSitesRepository.ActivateDeactivateRun(updateSitesRepository, Collections.emptyList(), Collections.singletonList(updateSiteDependency.toUpdateSite())));
        }
    }

    private void switchToRun(JIPipeRunnable run) {
        removeAll();
        add(new JIPipeDesktopRunExecuteUI(getDesktopWorkbench(), run));
        revalidate();
        repaint();
        SwingUtilities.invokeLater(() -> {
            JIPipeRunnableQueue.getInstance().enqueue(run);
        });
    }

    private void showImageJUpdateSiteInfo(JIPipeImageJUpdateSiteDependency updateSiteDependency) {
        String message = "# " + updateSiteDependency.getName() + "\n\n" +
                "Description: " + updateSiteDependency.getDescription() + "\n\n" +
                "URL: " + updateSiteDependency.getUrl() + "\n\n" +
                "Maintainer: " + updateSiteDependency.getMaintainer();
        JIPipeDesktopMarkdownReader.showDialog(new MarkdownText(message), true, "Update site info", this, false);
    }

    private void addAuthors(JIPipeAuthorMetadata.List authors, String label) {
        if (!authors.isEmpty()) {
            JPanel authorsPanel = new JPanel(new GridLayout((int) Math.ceil(1.0 * authors.size() / 2), 2));
            authorsPanel.setLayout(new BoxLayout(authorsPanel, BoxLayout.Y_AXIS));
            for (JIPipeAuthorMetadata author : authors) {
                JButton button = UIUtils.createButton(author.toString(), UIUtils.getIconFromResources("actions/im-user.png"), () -> {
                    JIPipeAuthorMetadata.openAuthorInfoWindow(getDesktopWorkbench().getWindow(), authors, author);
                });
                UIUtils.makeButtonFlat(button);
                authorsPanel.add(button);
            }
            propertyPanel.addToForm(authorsPanel, new JLabel(label));
        }
    }

    public JIPipeDesktopImageJUpdateSitesRepository getUpdateSitesRepository() {
        return updateSitesRepository;
    }

    private void revertChanges() {
        for (PluginEntry pluginEntry : pluginEntryList) {
            pluginEntry.setToggleInstallationStatus(false);
        }
        repaint();
        updateSelectionPanel();
    }

    private void applyChanges() {
        // Collect plugins to install/uninstall
        Set<JIPipePlugin> pluginsToInstall = new HashSet<>();
        Set<JIPipePlugin> pluginsToUninstall = new HashSet<>();
        Set<String> pluginIdsNotFound = new HashSet<>();

        for (PluginEntry pluginEntry : pluginEntryList) {
            if (pluginEntry.isToggleInstallationStatus() && !pluginEntry.plugin.isCorePlugin()) {
                if (pluginEntry.plugin.isActivated()) {
                    pluginsToUninstall.add(pluginEntry.plugin);
                } else {
                    pluginsToInstall.add(pluginEntry.plugin);

                    // Get all dependencies and add them
                    for (JIPipeDependency dependency : pluginEntry.plugin.getAllDependencies()) {
                        JIPipePlugin pluginById = JIPipe.getInstance().getPluginRegistry().getKnownPluginById(dependency.getDependencyId());
                        if (pluginById != null) {
                            if (!pluginById.isCorePlugin() && !pluginById.isActivated()) {
                                pluginsToInstall.add(pluginById);
                            }
                        } else {
                            pluginIdsNotFound.add(dependency.getDependencyId());
                        }
                    }

                }
            }
        }

        pluginsToInstall.removeAll(pluginsToUninstall);

        // Collect ImageJ update sites
        Map<String, JIPipeImageJUpdateSiteDependency> updateSiteDependencies = new HashMap<>();
        Map<String, JCheckBox> updateSiteCheckBoxes = new HashMap<>();
        for (JIPipePlugin plugin : pluginsToInstall) {
            for (JIPipeImageJUpdateSiteDependency updateSiteDependency : plugin.getAllImageJUpdateSiteDependencies()) {
                updateSiteDependencies.put(updateSiteDependency.getName(), updateSiteDependency);
            }
        }

        // Create form
        Insets insets = new Insets(2, 2, 2, 2);
        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
        if (!pluginsToInstall.isEmpty()) {
            formPanel.addGroupHeader("The following plugins will be activated", UIUtils.getIconFromResources("status/package-install.png"));
            pluginsToInstall.stream().sorted(Comparator.comparing((JIPipePlugin plugin) -> plugin.getMetadata().getName())).forEach((JIPipePlugin plugin) -> {
                createPluginInfoEntry(plugin, insets, formPanel);
            });
        }
        if (!pluginsToUninstall.isEmpty()) {
            formPanel.addGroupHeader("The following plugins will be deactivated", UIUtils.getIconFromResources("status/package-remove.png"));
            pluginsToUninstall.stream().sorted(Comparator.comparing((JIPipePlugin plugin) -> plugin.getMetadata().getName())).forEach((JIPipePlugin plugin) -> {
                createPluginInfoEntry(plugin, insets, formPanel);
            });
        }
        if (!pluginIdsNotFound.isEmpty()) {
            formPanel.addGroupHeader("The following plugins could not be found", UIUtils.getIconFromResources("status/package-broken.png"));
        }
        if (!updateSiteDependencies.isEmpty()) {
            JIPipeDesktopFormPanel.GroupHeaderPanel groupHeader = formPanel.addGroupHeader("The following ImageJ update sites will be activated", UIUtils.getIconFromResources("apps/imagej.png"));
            for (String name : updateSiteDependencies.keySet()) {
                JCheckBox checkBox = new JCheckBox(updateSiteDependencies.get(name).getName() + " (" + updateSiteDependencies.get(name).getUrl() + ")");
                checkBox.setSelected(true);
                updateSiteCheckBoxes.put(name, checkBox);
                formPanel.addWideToForm(checkBox);
            }
            groupHeader.addColumn(UIUtils.createButton("Select all", UIUtils.getIconFromResources("actions/edit-select-all.png"), () -> {
                for (JCheckBox checkBox : updateSiteCheckBoxes.values()) {
                    checkBox.setSelected(true);
                }
            }));
            groupHeader.addColumn(UIUtils.createButton("Clear selection", UIUtils.getIconFromResources("actions/edit-select-none.png"), () -> {
                for (JCheckBox checkBox : updateSiteCheckBoxes.values()) {
                    checkBox.setSelected(false);
                }
            }));
        }
        formPanel.addVerticalGlue();

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this));

        // Create the title
        JLabel titleLabel = UIUtils.createJLabel("Please review the following changes", 22);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Create the button bar
        AtomicBoolean userAccepted = new AtomicBoolean(false);
        JPanel buttonBar = UIUtils.boxHorizontal(Box.createHorizontalGlue(),
                UIUtils.createButton("Cancel", UIUtils.getIconFromResources("actions/dialog-cancel.png"), () -> {
                    dialog.setVisible(false);
                }),
                UIUtils.createButton("Apply", UIUtils.getIconFromResources("actions/dialog-ok.png"), () -> {
                    userAccepted.set(true);
                    dialog.setVisible(false);
                }));

        // Create the dialog
        dialog.setIconImage(UIUtils.getJIPipeIcon128());
        dialog.setTitle("Confirm changes");
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(titleLabel, BorderLayout.NORTH);
        dialog.getContentPane().add(formPanel, BorderLayout.CENTER);
        dialog.getContentPane().add(buttonBar, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        dialog.setModal(true);
        UIUtils.addEscapeListener(dialog);
        dialog.setVisible(true);

        if (userAccepted.get()) {
            Set<UpdateSite> updateSites = new HashSet<>();
            for (Map.Entry<String, JIPipeImageJUpdateSiteDependency> entry : updateSiteDependencies.entrySet()) {
                if (updateSiteCheckBoxes.get(entry.getKey()).isSelected()) {
                    updateSites.add(entry.getValue().toUpdateSite());
                }
            }
            switchToRun(new JIPipeDesktopApplyPluginManagerRun(this, pluginsToInstall, pluginsToUninstall, updateSites));
        }
    }

    private void createPluginInfoEntry(JIPipePlugin plugin, Insets insets, JIPipeDesktopFormPanel formPanel) {
        JPanel pluginPanel = new JPanel(new GridBagLayout());
        pluginPanel.add(new JLabel(UIUtils.getIconFromResources("actions/puzzle-piece.png")), new GridBagConstraints(0,
                0,
                1,
                1,
                0,
                0,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.NONE,
                insets,
                0,
                0));
        pluginPanel.add(new JLabel(plugin.getMetadata().getName()), new GridBagConstraints(1,
                0,
                1,
                1,
                1,
                0,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL,
                insets,
                0,
                0));
        JLabel descriptionLabel = new JLabel(plugin.getMetadata().getDescription().getHtml());
        descriptionLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
        pluginPanel.add(descriptionLabel, new GridBagConstraints(1,
                1,
                1,
                1,
                1,
                0,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL,
                insets,
                0,
                0));
        formPanel.addWideToForm(pluginPanel);
    }

    private void updatePluginsList() {
        int scrollPosition = pluginsListScrollPane.getVerticalScrollBar().getValue();
        PluginEntry selectedValue = pluginEntryJList.getSelectedValue();
        DefaultListModel<PluginEntry> model = new DefaultListModel<>();
        for (PluginEntry listEntry : pluginEntryList) {
            if (!onlyNewToggle.isSelected() || listEntry.isNewPlugin()) {
                if (searchTextField.test(listEntry.plugin.getMetadata().getName())) {
                    model.addElement(listEntry);
                }
            }
        }
        pluginEntryJList.setModel(model);
        SwingUtilities.invokeLater(() -> {
            pluginEntryJList.setSelectedValue(selectedValue, true);
            SwingUtilities.invokeLater(() -> {
                pluginsListScrollPane.getVerticalScrollBar().setValue(scrollPosition);
            });
        });
        repaint();
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if (event.getRun() instanceof JIPipeDesktopImageJUpdateSitesRepository.ActivateDeactivateRun || event.getRun() instanceof JIPipeDesktopApplyPluginManagerRun) {
            switchToManager();
            SHOW_RESTART_PROMPT = true;
            updateSelectionPanel();
        }
    }

    @Override
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {
        if (event.getRun() instanceof JIPipeDesktopImageJUpdateSitesRepository.ActivateDeactivateRun) {
            switchToManager();
        }
    }

    public static class PluginEntry {

        private final JIPipePlugin plugin;
        private boolean newPlugin;
        private boolean toggleInstallationStatus;

        public PluginEntry(JIPipePlugin plugin) {
            this.plugin = plugin;
        }

        public boolean isNewPlugin() {
            return newPlugin;
        }

        public void setNewPlugin(boolean newPlugin) {
            this.newPlugin = newPlugin;
        }

        public JIPipePlugin getPlugin() {
            return plugin;
        }

        public boolean isToggleInstallationStatus() {
            return toggleInstallationStatus;
        }

        public void setToggleInstallationStatus(boolean toggleInstallationStatus) {
            if (plugin.isCorePlugin()) {
                return;
            }
            this.toggleInstallationStatus = toggleInstallationStatus;
        }
    }

    public static class PluginEntryListCellRenderer extends JPanel implements ListCellRenderer<PluginEntry> {

        private final JLabel statusLabel = new JLabel();
        private final JLabel nameLabel = new JLabel();
        private final JLabel detailsLabel = new JLabel();
        private final JLabel infoLabel = new JLabel();

        public PluginEntryListCellRenderer() {
            initialize();
        }

        private void initialize() {
            setLayout(new GridBagLayout());
            setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
            final Insets insets = new Insets(2, 5, 2, 5);
            add(statusLabel, new GridBagConstraints(0,
                    0,
                    1,
                    1,
                    0.0,
                    0.0,
                    GridBagConstraints.WEST,
                    GridBagConstraints.NONE,
                    insets,
                    0,
                    0));
            add(nameLabel, new GridBagConstraints(1,
                    0,
                    1,
                    1,
                    1,
                    0.0,
                    GridBagConstraints.WEST,
                    GridBagConstraints.HORIZONTAL,
                    insets,
                    0,
                    0));
            add(detailsLabel, new GridBagConstraints(1,
                    1,
                    1,
                    1,
                    1,
                    0.0,
                    GridBagConstraints.WEST,
                    GridBagConstraints.HORIZONTAL,
                    insets,
                    0,
                    0));
            detailsLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
            add(infoLabel, new GridBagConstraints(2,
                    0,
                    1,
                    1,
                    0,
                    0.0,
                    GridBagConstraints.WEST,
                    GridBagConstraints.HORIZONTAL,
                    insets,
                    0,
                    0));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends PluginEntry> list, PluginEntry value, int index, boolean isSelected, boolean cellHasFocus) {
            infoLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            if (value.getPlugin().isCorePlugin()) {
                statusLabel.setIcon(UIUtils.getIconFromResources("emblems/checkbox-checked.png"));
                infoLabel.setText("Core plugin");
            } else {
                if (!value.getPlugin().isActivated()) {
                    if (value.isToggleInstallationStatus()) {
                        statusLabel.setIcon(UIUtils.getIconFromResources("emblems/checkbox-checked.png"));
                        infoLabel.setForeground(new Color(0x26a269));
                        infoLabel.setText("Activate");
                    } else {
                        statusLabel.setIcon(UIUtils.getIconFromResources("emblems/checkbox-unchecked.png"));
                        infoLabel.setText("Not activated");
                    }
                } else {
                    if (value.isToggleInstallationStatus()) {
                        statusLabel.setIcon(UIUtils.getIconFromResources("emblems/checkbox-unchecked.png"));
                        infoLabel.setForeground(new Color(0xc64600));
                        infoLabel.setText("Deactivate");
                    } else {
                        statusLabel.setIcon(UIUtils.getIconFromResources("emblems/checkbox-checked.png"));
                        infoLabel.setText("Activated");
                    }
                }
            }
            String additionalInfo = "";
            if (value.isNewPlugin()) {
                additionalInfo = "(New)";
            }
            nameLabel.setText("<html><strong>" + value.getPlugin().getMetadata().getName() + "</strong> " + " <span style=\"color:#eba834\">" + additionalInfo + "</span></html>");
            detailsLabel.setText(value.getPlugin().getMetadata().getDescription().getHtml());

            if (isSelected) {
                setBackground(UIManager.getColor("List.selectionBackground"));
            } else {
                setBackground(UIManager.getColor("List.background"));
            }
            return this;
        }
    }
}
