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

package org.hkijena.jipipe.ui.datatracer;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.theme.ModernMetalTheme;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.data.WeakStore;
import org.jdesktop.swingx.border.DropShadowBorder;
import org.scijava.Disposable;

import javax.swing.*;
import java.awt.*;
import java.util.UUID;

public class DataTrackerNodeOutputUI extends JIPipeProjectWorkbenchPanel implements Disposable {

    public static final Color COLOR_HIGHLIGHT = new Color(0xBAE8BA);
    private final String nodeUUID;
    private final String outputSlotName;
    private final JIPipeDataTable dataTable;
    private final boolean highlighted;

    public DataTrackerNodeOutputUI(JIPipeProjectWorkbench workbench, String nodeUUID, String outputSlotName, JIPipeDataTable dataTable, boolean highlighted) {
        super(workbench);
        this.nodeUUID = nodeUUID;
        this.outputSlotName = outputSlotName;
        this.dataTable = dataTable;
        this.highlighted = highlighted;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(new DropShadowBorder(Color.BLACK,
                5,
                0.2f,
                12,
                true,
                true,
                true,
                true), BorderFactory.createLineBorder(ModernMetalTheme.MEDIUM_GRAY)));

        JIPipeGraphNode node = getProjectWorkbench().getProject().getGraph().getNodeByUUID(UUID.fromString(nodeUUID));
        JIPipeOutputDataSlot outputSlot = node.getOutputSlot(outputSlotName);
        JPanel topPanel = new JPanel();
        topPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
        if(highlighted) {
            topPanel.setBackground(COLOR_HIGHLIGHT);
        }

        JButton compartmentButton = new JButton(node.getCompartmentDisplayName(), UIUtils.getIconFromResources("actions/graph-compartment.png"));
        compartmentButton.setOpaque(false);
        initializeContextMenu(UIUtils.addPopupMenuToButton(compartmentButton));
        topPanel.add(compartmentButton);

        topPanel.add(new JLabel("⏵"));

        JButton nodeButton = new JButton(node.getName(), node.getInfo().getIcon());
        nodeButton.setOpaque(false);
        initializeContextMenu(UIUtils.addPopupMenuToButton(nodeButton));
        topPanel.add(nodeButton);

        topPanel.add(new JLabel("⏵"));

        JButton slotButton = new JButton(outputSlot.getName(), JIPipe.getDataTypes().getIconFor(outputSlot.getAcceptedDataType()));
        slotButton.setOpaque(false);
        initializeContextMenu(UIUtils.addPopupMenuToButton(slotButton));
        topPanel.add(slotButton);

        add(topPanel, BorderLayout.NORTH);

        DataTracerDataTableUI tableUI = new DataTracerDataTableUI(getWorkbench(), new WeakStore<>(dataTable));
        add(tableUI, BorderLayout.SOUTH);

    }

    private void initializeContextMenu(JPopupMenu contextMenu) {
        contextMenu.add(UIUtils.createMenuItem("Go to node", "Jumps to the referenced node", UIUtils.getIconFromResources("actions/go-jump.png"), this::goToNode));
    }

    private void goToNode() {
        JIPipeGraphNode node = getProjectWorkbench().getProject().getGraph().getNodeByUUID(UUID.fromString(nodeUUID));
        if(node != null) {
            GraphNodeValidationReportContext context = new GraphNodeValidationReportContext(node);
            context.navigate(getWorkbench());
        }
        else {
            JOptionPane.showMessageDialog(getWorkbench().getWindow(), "The node could not be found!", "Go to node", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void dispose() {
        Disposable.super.dispose();
    }
}
