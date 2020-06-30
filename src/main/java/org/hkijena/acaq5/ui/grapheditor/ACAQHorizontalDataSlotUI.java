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
 */

package org.hkijena.acaq5.ui.grapheditor;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQCompartmentOutput;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.cache.ACAQDataSlotCacheManagerUI;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;

import static org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI.SLOT_UI_HEIGHT;
import static org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI.SLOT_UI_WIDTH;

/**
 * Slot UI with horizontal direction
 */
public class ACAQHorizontalDataSlotUI extends ACAQDataSlotUI {
    private AbstractButton assignButton;
    private JLabel nameLabel;
    private ACAQDataSlotCacheManagerUI cacheManagerUI;

    /**
     * Creates a new UI
     *
     * @param workbench   the workbench
     * @param algorithmUI The parent algorithm UI
     * @param compartment The compartment ID
     * @param slot        The slot instance
     */
    public ACAQHorizontalDataSlotUI(ACAQWorkbench workbench, ACAQAlgorithmUI algorithmUI, String compartment, ACAQDataSlot slot) {
        super(workbench, algorithmUI, compartment, slot);
        initialize();
        reloadButtonStatus();
    }

    @Override
    protected void reloadButtonStatus() {
        if (getSlot().isInput()) {
            if (getGraph().getSourceSlot(getSlot()) == null) {
                assignButton.setIcon(UIUtils.getIconFromResources("chevron-right-thin.png"));
            } else {
                assignButton.setIcon(UIUtils.getIconFromResources("chevron-right.png"));
            }
        } else if (getSlot().isOutput()) {
            if (getGraph().getTargetSlots(getSlot()).isEmpty()) {
                assignButton.setIcon(UIUtils.getIconFromResources("chevron-right-thin.png"));
            } else {
                assignButton.setIcon(UIUtils.getIconFromResources("chevron-right.png"));
            }
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());

        this.assignButton = new JButton(UIUtils.getIconFromResources("chevron-right.png"));
        assignButton.setPreferredSize(new Dimension(25, SLOT_UI_HEIGHT));
        this.assignButtonMenu = UIUtils.addReloadablePopupMenuToComponent(assignButton, new JPopupMenu(), this::reloadPopupMenu);
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
//        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setLayout(new GridLayout(2, 1));
        centerPanel.setOpaque(false);

        nameLabel = new JLabel();
        reloadName();
        nameLabel.setToolTipText(TooltipUtils.getSlotInstanceTooltip(getSlot()));
        nameLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        nameLabel.setIcon(ACAQUIDatatypeRegistry.getInstance().getIconFor(getSlotDataType()));
        centerPanel.add(nameLabel);

        if (getSlot().isOutput() && getSlot().getAlgorithm() instanceof ACAQAlgorithm && getWorkbench() instanceof ACAQProjectWorkbench) {
            ACAQProjectWorkbench projectWorkbench = (ACAQProjectWorkbench) getWorkbench();
            cacheManagerUI = new ACAQDataSlotCacheManagerUI(projectWorkbench, getSlot());
            centerPanel.add(cacheManagerUI);
        }

        if (getSlot().isInput()) {
            add(assignButton, BorderLayout.WEST);
            nameLabel.setHorizontalAlignment(JLabel.LEFT);
            nameLabel.setHorizontalTextPosition(JLabel.RIGHT);
        } else if (getSlot().isOutput()) {
            add(assignButton, BorderLayout.EAST);
            nameLabel.setHorizontalAlignment(JLabel.RIGHT);
            nameLabel.setHorizontalTextPosition(JLabel.LEFT);
        }

        add(centerPanel, BorderLayout.CENTER);
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
        int width = labelWidth + 75;

        return width;
    }

}
