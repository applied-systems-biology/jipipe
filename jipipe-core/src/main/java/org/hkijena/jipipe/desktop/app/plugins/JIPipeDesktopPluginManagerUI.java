package org.hkijena.jipipe.desktop.app.plugins;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.JIPipePlugin;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.artifacts.*;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunExecuterUI;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunnableQueueButton;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopMessagePanel;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopMarkdownReader;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.List;

public class JIPipeDesktopPluginManagerUI extends JIPipeDesktopWorkbenchPanel implements JIPipeRunnable.FinishedEventListener {

    private static JFrame CURRENT_WINDOW = null;
    private final List<PluginEntry> pluginEntryList = new ArrayList<>();
    private final JList<PluginEntry> pluginEntryJList = new JList<>();
    private final JIPipeDesktopFormPanel propertyPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
    private final JCheckBox onlyNewToggle = new JCheckBox("Only new", false);
    private final JIPipeDesktopSearchTextField searchTextField = new JIPipeDesktopSearchTextField();
    private JScrollPane pluginsListScrollPane;
    private static boolean SHOW_RESTART_PROMPT;

    public JIPipeDesktopPluginManagerUI(JIPipeDesktopWorkbench desktopWorkbench) {
        super(desktopWorkbench);
        initialize();
        loadAvailablePlugins();
        updatePluginsList();
        updateSelectionPanel();
        JIPipeRunnableQueue.getInstance().getFinishedEventEmitter().subscribe(this);
    }

    private void loadAvailablePlugins() {
        Set<String> newPlugins = new HashSet<>(JIPipe.getInstance().getPluginRegistry().getNewPlugins());
        newPlugins.removeAll(JIPipe.getInstance().getPluginRegistry().getSettings().getSilencedPlugins());
        if(!newPlugins.isEmpty()) {
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
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.add(searchTextField);
        searchTextField.addActionListener(e -> updatePluginsList());

        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(onlyNewToggle);
        onlyNewToggle.setToolTipText("Only show newly available plugins");
        onlyNewToggle.addActionListener(e -> updatePluginsList());

        toolbar.add(new JIPipeDesktopRunnableQueueButton(getDesktopWorkbench()));
        add(toolbar, BorderLayout.NORTH);

        pluginsListScrollPane = new JScrollPane(pluginEntryJList);
        pluginsListScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        AutoResizeSplitPane splitPane = new AutoResizeSplitPane(AutoResizeSplitPane.LEFT_RIGHT,
                pluginsListScrollPane,
                propertyPanel,
                new AutoResizeSplitPane.DynamicSidebarRatio(350, false));
        add(splitPane, BorderLayout.CENTER);

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

        if (hasChanges()) {
            JIPipeDesktopMessagePanel messagePanel = new JIPipeDesktopMessagePanel();
            messagePanel.addMessage(JIPipeDesktopMessagePanel.MessageType.Success,
                    "You have pending changes",
                    false,
                    true,
                    UIUtils.createButton("Revert", UIUtils.getIconFromResources("actions/edit-undo.png"), this::revertChanges),
                    UIUtils.createButton("Apply", UIUtils.getIconFromResources("actions/check.png"), this::applyChanges));
            propertyPanel.addWideToForm(messagePanel);
        }

        PluginEntry selectedValue = pluginEntryJList.getSelectedValue();
        if (selectedValue != null) {
            JIPipePlugin plugin = selectedValue.plugin;
            propertyPanel.addGroupHeader(plugin.getMetadata().getName(), plugin.getMetadata().getDescription().getHtml(), UIUtils.getIconFromResources("actions/puzzle-piece.png"));
            if(plugin.isCorePlugin()) {
                propertyPanel.addToForm(new JLabel("Always active"), new JLabel("Status"));
            }
            else {
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

            if(!StringUtils.isNullOrEmpty(plugin.getMetadata().getCitation())) {
                propertyPanel.addToForm(UIUtils.makeBorderlessReadonlyTextPane(plugin.getMetadata().getCitation(), false),
                        new JLabel("Citation"));
            }
            for (String dependencyCitation : plugin.getMetadata().getDependencyCitations()) {
                propertyPanel.addToForm(UIUtils.makeBorderlessReadonlyTextPane(dependencyCitation, false),
                        new JLabel("Also cite"));
            }
            propertyPanel.addToForm(UIUtils.makeBorderlessReadonlyTextPane(plugin.getDependencyVersion(), false),
                    new JLabel("Version"));
            if(!StringUtils.isNullOrEmpty(plugin.getMetadata().getLicense())) {
                propertyPanel.addToForm(UIUtils.makeBorderlessReadonlyTextPane(plugin.getMetadata().getLicense(), false),
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
        }
        else {
            propertyPanel.addGroupHeader("Plugins", "The JIPipe feature set can be extended with a variety of plugins. " +
                    "Select items on the left-hand side to show more information about a plugin.", UIUtils.getIconFromResources("actions/help-info.png"));
        }

        propertyPanel.addVerticalGlue();
    }

    private void installImageJUpdateSite(JIPipeImageJUpdateSiteDependency updateSiteDependency) {
        // TODO
    }

    private void showImageJUpdateSiteInfo(JIPipeImageJUpdateSiteDependency updateSiteDependency) {
        String message = "# " + updateSiteDependency.getName() + "\n\n" +
                "Description: " + updateSiteDependency.getDescription() + "\n\n" +
                "URL: " + updateSiteDependency.getUrl() + "\n\n" +
                "Maintainer: " + updateSiteDependency.getMaintainer();
        JIPipeDesktopMarkdownReader.showDialog(new MarkdownText(message), true, "Update site info", this, false);
    }

    private void addAuthors(JIPipeAuthorMetadata.List authors, String label) {
        if(!authors.isEmpty()) {
            JPanel authorsPanel = new JPanel(new GridLayout((int)Math.ceil(1.0 * authors.size() / 2), 2));
            authorsPanel.setLayout(new BoxLayout(authorsPanel, BoxLayout.Y_AXIS));
            for (JIPipeAuthorMetadata author : authors) {
                JButton button = UIUtils.createButton(author.toString(), UIUtils.getIconFromResources("actions/im-user.png"), () -> {
                    JIPipeAuthorMetadata.openAuthorInfoWindow(getDesktopWorkbench().getWindow(), authors, author);
                });
                UIUtils.makeFlat(button);
                authorsPanel.add(button);
            }
            propertyPanel.addToForm(authorsPanel, new JLabel(label));
        }
    }

    private void revertChanges() {
        for (PluginEntry pluginEntry : pluginEntryList) {
            pluginEntry.setToggleInstallationStatus(false);
        }
        repaint();
        updateSelectionPanel();
    }

    private void applyChanges() {
        List<JIPipeLocalArtifact> toUninstall = new ArrayList<>();
        List<JIPipeRemoteArtifact> toInstall = new ArrayList<>();
        long installSize = 0;
        for (PluginEntry pluginEntry : pluginEntryList) {
            if (pluginEntry.toggleInstallationStatus) {
                if (pluginEntry.plugin instanceof JIPipeRemoteArtifact) {
                    toInstall.add((JIPipeRemoteArtifact) pluginEntry.plugin);
                    installSize += ((JIPipeRemoteArtifact) pluginEntry.plugin).getSize();
                } else if (pluginEntry.plugin instanceof JIPipeLocalArtifact) {
                    toUninstall.add((JIPipeLocalArtifact) pluginEntry.plugin);
                }
            }
        }

        if (toInstall.isEmpty() && toUninstall.isEmpty()) {
            return;
        }
        StringBuilder message = new StringBuilder();
        if (!toInstall.isEmpty()) {
            message.append(StringUtils.formatPluralS(toInstall.size(), "artifact")).append(" will be installed (").append(installSize / 1024 / 1024).append("MB to download)\n");
        }
        if (!toUninstall.isEmpty()) {
            message.append(" ").append(StringUtils.formatPluralS(toUninstall.size(), "artifact")).append(" will be removed\n");
        }
        message.append("\nDo you want to continue?");
        if (JOptionPane.showConfirmDialog(this, message.toString(), "Apply changes", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            JIPipeArtifactRepositoryApplyInstallUninstallRun run = new JIPipeArtifactRepositoryApplyInstallUninstallRun(toInstall, toUninstall);
            JIPipeDesktopRunExecuterUI.runInDialog(getDesktopWorkbench(), this, run, JIPipe.getArtifacts().getQueue());
        }
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
        if(event.getRun() instanceof JIPipeDesktopActivateAndApplyUpdateSiteRun || event.getRun() instanceof JIPipeDesktopDeactivateAndApplyUpdateSiteRun) {
            SHOW_RESTART_PROMPT = true;
            updateSelectionPanel();
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
            if(plugin.isCorePlugin()) {
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
            if(value.getPlugin().isCorePlugin()) {
                statusLabel.setIcon(UIUtils.getIconFromResources("emblems/checkbox-checked.png"));
                infoLabel.setText("Core plugin");
            }
            else {
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
