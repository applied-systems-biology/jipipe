package org.hkijena.jipipe.desktop.app.plugins;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifact;
import org.hkijena.jipipe.api.artifacts.JIPipeRemoteArtifact;
import org.hkijena.jipipe.api.registries.JIPipeArtifactsRegistry;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunnableQueueButton;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

public class JIPipeDesktopArtifactManagerUI extends JIPipeDesktopWorkbenchPanel implements JIPipeRunnable.FinishedEventListener {

    private static JFrame CURRENT_WINDOW = null;
    private JScrollPane artifactListScrollPane;
    private final List<ArtifactEntry> artifactEntryList = new ArrayList<>();
    private final JList<ArtifactEntry> artifactEntryJList = new JList<>();
    private final JIPipeDesktopFormPanel propertyPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
    private final JIPipeArtifactsRegistry artifactsRegistry = JIPipe.getInstance().getArtifactsRegistry();
    private final JCheckBox onlyCompatibleToggle = new JCheckBox("Only compatible", true);
    private final JIPipeDesktopSearchTextField searchTextField = new JIPipeDesktopSearchTextField();

    public JIPipeDesktopArtifactManagerUI(JIPipeDesktopWorkbench desktopWorkbench) {
        super(desktopWorkbench);
        initialize();
        artifactsRegistry.getQueue().getFinishedEventEmitter().subscribe(this);
        artifactsRegistry.enqueueUpdateCachedArtifacts();
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

        toolbar.add(new JIPipeDesktopRunnableQueueButton(getDesktopWorkbench(), JIPipe.getInstance().getArtifactsRegistry().getQueue()));
        add(toolbar, BorderLayout.NORTH);

        artifactListScrollPane = new JScrollPane(artifactEntryJList);
        artifactListScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        AutoResizeSplitPane splitPane = new AutoResizeSplitPane(AutoResizeSplitPane.LEFT_RIGHT,
                artifactListScrollPane,
                propertyPanel,
                new AutoResizeSplitPane.DynamicSidebarRatio(300, false));
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

                    if (JIPipe.getInstance().getArtifactsRegistry().getQueue().isEmpty()) {
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
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if (event.getRun() instanceof JIPipeArtifactsRegistry.UpdateCachedArtifactsRun) {
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
            if (value.getArtifact().getClassifier().contains("gpu")) {
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
