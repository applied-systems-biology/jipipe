package org.hkijena.acaq5.ui.grapheditor;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQCompartmentOutput;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;

import static org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI.SLOT_UI_WIDTH;

/**
 * Slot UI with horizontal direction
 */
public class ACAQVerticalDataSlotUI extends ACAQDataSlotUI {
    private AbstractButton assignButton;
    private JLabel nameLabel;
    private ACAQDataSlotTraitUI traitUI;

    /**
     * Creates a new UI
     *
     * @param algorithmUI The parent algorithm UI
     * @param graph       The graph
     * @param compartment The compartment ID
     * @param slot        The slot instance
     */
    public ACAQVerticalDataSlotUI(ACAQAlgorithmUI algorithmUI, ACAQAlgorithmGraph graph, String compartment, ACAQDataSlot slot) {
        super(algorithmUI, graph, compartment, slot, ACAQAlgorithmGraphCanvasUI.Direction.Vertical);
        initialize();
        reloadPopupMenu();
        reloadButtonStatus();
    }

    @Override
    protected void reloadButtonStatus() {
        if (getSlot().isInput()) {
            if (getGraph().getSourceSlot(getSlot()) == null) {
                assignButton.setIcon(UIUtils.getIconFromResources("chevron-bottom-thin.png"));
            } else {
                assignButton.setIcon(UIUtils.getIconFromResources("chevron-bottom.png"));
            }
        } else if (getSlot().isOutput()) {
            if (getGraph().getTargetSlots(getSlot()).isEmpty()) {
                assignButton.setIcon(UIUtils.getIconFromResources("chevron-bottom-thin.png"));
            } else {
                assignButton.setIcon(UIUtils.getIconFromResources("chevron-bottom.png"));
            }
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());

        this.assignButton = new JButton(UIUtils.getIconFromResources("chevron-bottom.png"));
        assignButton.setPreferredSize(new Dimension(25, 25));
        this.assignButtonMenu = UIUtils.addPopupMenuToComponent(assignButton);
        UIUtils.makeFlat(assignButton);

        if (getSlot().getAlgorithm() instanceof ACAQCompartmentOutput) {
            if (getSlot().getAlgorithm().getCompartment().equals(getCompartment())) {
                if (getSlot().isOutput()) {
                    assignButton.setEnabled(false);
                }
            } else {
                if (getSlot().isInput()) {
                    assignButton.setEnabled(false);
                }
            }
        }

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.X_AXIS));
        centerPanel.setOpaque(false);

        nameLabel = new JLabel();
        reloadName();
        nameLabel.setToolTipText(TooltipUtils.getSlotInstanceTooltip(getSlot(), false));
        nameLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        nameLabel.setIcon(ACAQUIDatatypeRegistry.getInstance().getIconFor(getSlotDataType()));
        centerPanel.add(nameLabel);
        centerPanel.add(Box.createHorizontalGlue());
        traitUI = new ACAQDataSlotTraitUI(getGraph(), getSlot());
        centerPanel.add(traitUI);

        add(centerPanel, BorderLayout.CENTER);
        if (getSlot().isInput())
            add(assignButton, BorderLayout.NORTH);
        else
            add(assignButton, BorderLayout.SOUTH);
    }

    @Override
    protected void reloadName() {
        if (!StringUtils.isNullOrEmpty(getSlot().getDefinition().getCustomName())) {
            nameLabel.setText(getDisplayedName());
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.ITALIC));
        } else {
            nameLabel.setText(getDisplayedName());
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.PLAIN));
        }
    }

    @Override
    public int calculateWidth() {
        // First calculate the width caused by the label width
        FontRenderContext frc = new FontRenderContext(null, false, false);
        TextLayout layout = new TextLayout(getDisplayedName(), getFont(), frc);
        double w = layout.getBounds().getWidth();
        int labelWidth = (int) Math.ceil(w * 1.0 / SLOT_UI_WIDTH) * SLOT_UI_WIDTH;
        int traitWidth = (int) Math.ceil(traitUI.calculateWidth() * 1.0 / SLOT_UI_WIDTH) * SLOT_UI_WIDTH;
        int width = Math.max(labelWidth, traitWidth) + 75;

        return width;
    }

}
