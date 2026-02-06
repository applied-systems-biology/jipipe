package org.hkijena.jipipe.plugins.tunnels.nodes;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeDesktopInteractiveDefaultActionGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.events.DefaultNodeUIActionRequestedEvent;
import org.hkijena.jipipe.desktop.commons.components.panels.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.textfield.JIPipeDesktopFancyTextField;
import org.hkijena.jipipe.plugins.tunnels.JIPipeDataFlowTunnelUtils;
import org.hkijena.jipipe.utils.NaturalOrderComparator;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class JIPipeDataFlowTunnel extends JIPipeGraphNode implements JIPipeDesktopInteractiveDefaultActionGraphNode {
    public JIPipeDataFlowTunnel(JIPipeNodeInfo info, JIPipeSlotConfiguration slotConfiguration) {
        super(info, slotConfiguration);
    }

    public JIPipeDataFlowTunnel(JIPipeNodeInfo info) {
        super(info);
    }

    public JIPipeDataFlowTunnel(JIPipeGraphNode other) {
        super(other);
    }

    public abstract String getTunnelKeyGroup();

    public abstract Set<String> getCompatibleTunnelKeys();

    @Override
    public void onDefaultNodeUIActionRequested(JIPipeDesktopGraphEditorUI graphEditorUI, DefaultNodeUIActionRequestedEvent event) {
        JIPipeDesktopGraphCanvasUI canvasUI = graphEditorUI.getCanvasUI();
        JIPipeGraph graph = canvasUI.getGraph();
        UUID compartmentUUID = canvasUI.getCompartmentUUID();

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(canvasUI));
        dialog.setIconImage(UIUtils.getJIPipeIcon128());
        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);

        JPanel mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        mainPanel.add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        AtomicBoolean okPressed = new AtomicBoolean(false);

        JLabel nameLabel = new JLabel("Shared key", JIPipe.RESOURCES.getIcon16("actions/key.png"), JLabel.LEFT);
        nameLabel.setText(StringUtils.nullToEmpty(getCustomName()));
        buttonPanel.add(Box.createHorizontalStrut(8));
        buttonPanel.add(nameLabel);
        buttonPanel.add(Box.createHorizontalStrut(8));

        JIPipeDesktopFancyTextField nameField = new JIPipeDesktopFancyTextField(null, "Please enter the key", true);
        nameField.getTextField().setFont(new Font(Font.MONOSPACED, Font.PLAIN, ThemeUtils.getCurrentStyle().getFontSizeLarge()));
        buttonPanel.add(nameField);
        buttonPanel.add(Box.createHorizontalStrut(32));

        Set<String> compatibleTunnelKeys = getCompatibleTunnelKeys();
        if (compatibleTunnelKeys.isEmpty()) {
            formPanel.addWideToForm(UIUtils.createInfoLabel("No compatible existing keys found", "Input a key of your choice in the text field below."));
        } else {
            List<String> sortedKeys = compatibleTunnelKeys.stream().sorted(NaturalOrderComparator.INSTANCE).toList();
            for (String sortedKey : sortedKeys) {
                JButton button = new JButton(sortedKey);
                JIPipeDataInfo tunnelDataType = JIPipeDataFlowTunnelUtils.findTunnelDataType(graph, compartmentUUID, getTunnelKeyGroup(), sortedKey);
                button.setIcon(JIPipe.getDataTypes().getIconFor(tunnelDataType.getDataClass()));

                button.setHorizontalAlignment(SwingConstants.LEFT);
                button.addActionListener(e -> {
                    nameField.setText(sortedKey);
                    okPressed.set(true);
                    dialog.setVisible(false);
                });
                formPanel.addWideToForm(button);
            }
        }

        JButton cancelButton = new JButton("Cancel", JIPipe.RESOURCES.getIcon16("actions/cancel.png"));
        cancelButton.addActionListener(e -> {
            dialog.setVisible(false);
        });
        buttonPanel.add(cancelButton);

        JButton okButton = new JButton("OK", JIPipe.RESOURCES.getIcon16("actions/ok.png"));
        okButton.addActionListener(e -> {
            okPressed.set(true);
            dialog.setVisible(false);
        });
        buttonPanel.add(okButton);

        nameField.getTextField().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    okPressed.set(true);
                    dialog.setVisible(false);
                }
            }
        });

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(mainPanel);
        dialog.setTitle("Setup " + getInfo().getName());
        dialog.setModal(true);
        dialog.pack();
        dialog.setSize(new Dimension(800, 600));
        dialog.setLocationRelativeTo(graphEditorUI);
        UIUtils.addEscapeListener(dialog);
        dialog.setVisible(true);

        if (okPressed.get()) {
            setCustomName(nameField.getText().trim());
            emitParameterChangedEvent("jipipe:node:name");
        }
    }

    public boolean hasValidTunnelKey() {
        return !StringUtils.isNullOrEmpty(getTunnelKey());
    }

    public String getTunnelKey() {
        return getCustomName();
    }

    public boolean isInSameGroup(JIPipeDataFlowTunnel other) {
        return Objects.equals(getTunnelKeyGroup(), other.getTunnelKeyGroup());
    }
}
