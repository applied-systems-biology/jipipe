package org.hkijena.jipipe.desktop.app.plugins;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.artifacts.*;
import org.hkijena.jipipe.api.registries.JIPipeArtifactsRegistry;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunExecuterUI;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunnableQueueButton;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopMessagePanel;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class JIPipeDesktopArtifactManagerUI extends JIPipeDesktopWorkbenchPanel implements JIPipeRunnable.FinishedEventListener {

    private static JFrame CURRENT_WINDOW = null;
    private final List<ArtifactEntry> artifactEntryList = new ArrayList<>();
    private final JList<ArtifactEntry> artifactEntryJList = new JList<>();
    private final JIPipeDesktopFormPanel propertyPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
    private final JIPipeArtifactsRegistry artifactsRegistry = JIPipe.getArtifacts();
    private final JCheckBox onlyCompatibleToggle = new JCheckBox("Only compatible", true);
    private final JIPipeDesktopSearchTextField searchTextField = new JIPipeDesktopSearchTextField();
    private JScrollPane artifactListScrollPane;

    public JIPipeDesktopArtifactManagerUI(JIPipeDesktopWorkbench desktopWorkbench) {
        super(desktopWorkbench);
        initialize();
        artifactsRegistry.getQueue().getFinishedEventEmitter().subscribe(this);
        artifactsRegistry.enqueueUpdateCachedArtifacts();
        updateSelectionPanel();
    }

    public static void show(JIPipeDesktopWorkbench desktopWorkbench) {
        if (CURRENT_WINDOW != null) {
            CURRENT_WINDOW.toFront();
        } else {
            JFrame frame = new JFrame("JIPipe - Artifacts");
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
            frame.setContentPane(new JIPipeDesktopArtifactManagerUI(desktopWorkbench));
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
        searchTextField.addActionListener(e -> updateArtifactsList());

        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(onlyCompatibleToggle);
        onlyCompatibleToggle.setToolTipText("Only show compatible artifacts");
        onlyCompatibleToggle.addActionListener(e -> updateArtifactsList());

        toolbar.add(new JIPipeDesktopRunnableQueueButton(getDesktopWorkbench(), JIPipe.getArtifacts().getQueue()));
        add(toolbar, BorderLayout.NORTH);

        artifactListScrollPane = new JScrollPane(artifactEntryJList);
        artifactListScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        AutoResizeSplitPane splitPane = new AutoResizeSplitPane(AutoResizeSplitPane.LEFT_RIGHT,
                artifactListScrollPane,
                propertyPanel,
                new AutoResizeSplitPane.DynamicSidebarRatio(350, false));
        add(splitPane, BorderLayout.CENTER);

        artifactEntryJList.setCellRenderer(new ArtifactEntryListCellRenderer());
        artifactEntryJList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getX() < 22) {
                    if (artifactEntryJList.getSelectedValue() != null) {
                        artifactEntryJList.getSelectedValue().setToggleInstallationStatus(!artifactEntryJList.getSelectedValue().isToggleInstallationStatus());
                        updateArtifactsList();
                    }
                }

            }
        });
        artifactEntryJList.addListSelectionListener(e -> updateSelectionPanel());
    }

    private boolean hasChanges() {
        return artifactEntryList.stream().anyMatch(ArtifactEntry::isToggleInstallationStatus);
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

        propertyPanel.addGroupHeader("Artifacts", "A variety of external tools are managed via the artifacts system that automatically downloads and applies the " +
                "correct version of the dependency based on project metadata.\n\n" +
                "Artifacts are automatically downloaded to a directory shared across multiple JIPipe instances.", UIUtils.getIconFromResources("actions/help-info.png"));
        propertyPanel.addWideToForm(UIUtils.createButton("Open artifacts directory", UIUtils.getIconFromResources("actions/folder-open.png"), this::openArtifactsDirectory));

        ArtifactEntry selectedValue = artifactEntryJList.getSelectedValue();
        if (selectedValue != null) {
            JIPipeArtifact artifact = selectedValue.artifact;
            propertyPanel.addGroupHeader(artifact.getArtifactId(), UIUtils.getIconFromResources("actions/run-install.png"));
            propertyPanel.addToForm(UIUtils.makeReadonlyBorderlessTextField(artifact.getVersion()), new JLabel("Version"));
            propertyPanel.addToForm(UIUtils.makeReadonlyBorderlessTextField(artifact.getClassifier()), new JLabel("Label"));
            propertyPanel.addToForm(UIUtils.makeReadonlyBorderlessTextField(artifact.getGroupId()), new JLabel("Publisher"));
            propertyPanel.addToForm(UIUtils.makeReadonlyBorderlessTextField(artifact.isCompatible() ? "Yes" : "No"), new JLabel("Compatible"));
            if (artifact.isRequireGPU()) {
                propertyPanel.addToForm(new JLabel("Requires GPU", UIUtils.getIconFromResources("devices/device_pci.png"), JLabel.LEFT), new JLabel("Additional info"));
            }

            if (artifact instanceof JIPipeRemoteArtifact) {
                if (selectedValue.isToggleInstallationStatus()) {
                    propertyPanel.addToForm(UIUtils.createLeftAlignedButton("Mark for installation", UIUtils.getIconFromResources("emblems/checkbox-checked.png"), () -> {
                        selectedValue.setToggleInstallationStatus(false);
                        updateSelectionPanel();
                    }), new JLabel("Status"));
                } else {
                    propertyPanel.addToForm(UIUtils.createLeftAlignedButton("Mark for installation", UIUtils.getIconFromResources("emblems/checkbox-unchecked.png"), () -> {
                        selectedValue.setToggleInstallationStatus(true);
                        updateSelectionPanel();
                    }), new JLabel("Status"));
                }
            } else {
                if (selectedValue.isToggleInstallationStatus()) {
                    propertyPanel.addToForm(UIUtils.createLeftAlignedButton("Keep installed", UIUtils.getIconFromResources("emblems/checkbox-unchecked.png"), () -> {
                        selectedValue.setToggleInstallationStatus(false);
                        updateSelectionPanel();
                    }), new JLabel("Status"));
                } else {
                    propertyPanel.addToForm(UIUtils.createLeftAlignedButton("Keep installed", UIUtils.getIconFromResources("emblems/checkbox-checked.png"), () -> {
                        selectedValue.setToggleInstallationStatus(true);
                        updateSelectionPanel();
                    }), new JLabel("Status"));
                }
            }
        }

        propertyPanel.addVerticalGlue();
    }

    private void revertChanges() {
        for (ArtifactEntry artifactEntry : artifactEntryList) {
            artifactEntry.setToggleInstallationStatus(false);
        }
        repaint();
        updateSelectionPanel();
    }

    private void applyChanges() {
        List<JIPipeLocalArtifact> toUninstall = new ArrayList<>();
        List<JIPipeRemoteArtifact> toInstall = new ArrayList<>();
        long installSize = 0;
        for (ArtifactEntry artifactEntry : artifactEntryList) {
            if (artifactEntry.toggleInstallationStatus) {
                if (artifactEntry.artifact instanceof JIPipeRemoteArtifact) {
                    toInstall.add((JIPipeRemoteArtifact) artifactEntry.artifact);
                    installSize += ((JIPipeRemoteArtifact) artifactEntry.artifact).getSize();
                } else if (artifactEntry.artifact instanceof JIPipeLocalArtifact) {
                    toUninstall.add((JIPipeLocalArtifact) artifactEntry.artifact);
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

    private void openArtifactsDirectory() {
        Path localRepositoryPath = JIPipe.getArtifacts().getLocalUserRepositoryPath();
        try {
            Files.createDirectories(localRepositoryPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            Desktop.getDesktop().open(localRepositoryPath.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateArtifactsList() {
        int scrollPosition = artifactListScrollPane.getVerticalScrollBar().getValue();
        ArtifactEntry selectedValue = artifactEntryJList.getSelectedValue();
        DefaultListModel<ArtifactEntry> model = new DefaultListModel<>();
        for (ArtifactEntry listEntry : artifactEntryList) {
            if (!onlyCompatibleToggle.isSelected() || listEntry.artifact.isCompatible()) {
                if (searchTextField.test(listEntry.artifact.getFullId())) {
                    model.addElement(listEntry);
                }
            }
        }
        artifactEntryJList.setModel(model);
        SwingUtilities.invokeLater(() -> {
            artifactEntryJList.setSelectedValue(selectedValue, true);
            SwingUtilities.invokeLater(() -> {
                artifactListScrollPane.getVerticalScrollBar().setValue(scrollPosition);
            });
        });
        repaint();
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if (event.getRun() instanceof JIPipeArtifactRepositoryOperationRun) {
            artifactEntryList.clear();
            for (JIPipeArtifact artifact : artifactsRegistry.getCachedArtifacts().values().stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList())) {
                artifactEntryList.add(new ArtifactEntry(artifact));
            }
            updateArtifactsList();
        }
    }


    public static class ArtifactEntry {

        private final JIPipeArtifact artifact;
        private boolean toggleInstallationStatus;

        public ArtifactEntry(JIPipeArtifact artifact) {

            this.artifact = artifact;
        }

        public JIPipeArtifact getArtifact() {
            return artifact;
        }

        public boolean isToggleInstallationStatus() {
            return toggleInstallationStatus;
        }

        public void setToggleInstallationStatus(boolean toggleInstallationStatus) {
            this.toggleInstallationStatus = toggleInstallationStatus;
        }
    }

    public static class ArtifactEntryListCellRenderer extends JPanel implements ListCellRenderer<ArtifactEntry> {

        private final JLabel statusLabel = new JLabel();
        private final JLabel nameLabel = new JLabel();
        private final JLabel detailsLabel = new JLabel();
        private final JLabel infoLabel = new JLabel();

        public ArtifactEntryListCellRenderer() {
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
            detailsLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
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
        public Component getListCellRendererComponent(JList<? extends ArtifactEntry> list, ArtifactEntry value, int index, boolean isSelected, boolean cellHasFocus) {
            infoLabel.setForeground(UIManager.getColor("Label.foreground"));
            if (value.getArtifact() instanceof JIPipeRemoteArtifact) {
                if (value.isToggleInstallationStatus()) {
                    statusLabel.setIcon(UIUtils.getIconFromResources("emblems/checkbox-checked.png"));
                    infoLabel.setForeground(new Color(0x26a269));
                    infoLabel.setText("Install");
                } else {
                    statusLabel.setIcon(UIUtils.getIconFromResources("emblems/checkbox-unchecked.png"));
                    infoLabel.setText("Not installed");
                }
            } else {
                if (value.isToggleInstallationStatus()) {
                    statusLabel.setIcon(UIUtils.getIconFromResources("emblems/checkbox-unchecked.png"));
                    infoLabel.setForeground(new Color(0xc64600));
                    infoLabel.setText("Uninstall");
                } else {
                    statusLabel.setIcon(UIUtils.getIconFromResources("emblems/checkbox-checked.png"));
                    infoLabel.setText("Installed");
                }
            }
            String additionalInfo = "";
            if (value.getArtifact().isRequireGPU()) {
                additionalInfo = "(Requires GPU)";
            }
            nameLabel.setText("<html><strong>" + value.getArtifact().getArtifactId() + "</strong> v" + value.getArtifact().getVersion() + " <span style=\"color:#eba834\">" + additionalInfo + "</span></html>");
            detailsLabel.setText(value.getArtifact().getFullId());

            if (isSelected) {
                setBackground(UIManager.getColor("List.selectionBackground"));
            } else {
                setBackground(UIManager.getColor("List.background"));
            }
            return this;
        }
    }
}
