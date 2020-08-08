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

package org.hkijena.jipipe.ui.grapheditor;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeCompartmentOutput;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.events.ParameterChangedEvent;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.cache.JIPipeDataSlotCacheManagerUI;
import org.hkijena.jipipe.ui.registries.JIPipeUIDatatypeRegistry;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;

/**
 * Slot UI with horizontal direction
 */
public class JIPipeVerticalDataSlotUI extends JIPipeDataSlotUI {
    private JButton assignButton;
    private JLabel nameLabel;
    private JLabel noSaveLabel;
    private JIPipeDataSlotCacheManagerUI cacheManagerUI;

    /**
     * Creates a new UI
     *
     * @param workbench   the workbench
     * @param algorithmUI The parent algorithm UI
     * @param compartment The compartment ID
     * @param slot        The slot instance
     */
    public JIPipeVerticalDataSlotUI(JIPipeWorkbench workbench, JIPipeNodeUI algorithmUI, String compartment, JIPipeDataSlot slot) {
        super(workbench, algorithmUI, compartment, slot);
        initialize();
        reloadButtonStatus();
        slot.getNode().getEventBus().register(this);
    }

    @Override
    protected void reloadButtonStatus() {
        if (getSlot().isInput()) {
            if (getGraph().getSourceSlot(getSlot()) == null) {
                assignButton.setIcon(UIUtils.getIconFromResources("emblems/slot-unconnected-input-vertical.png"));
            } else {
                assignButton.setIcon(UIUtils.getIconFromResources("emblems/slot-connected-vertical.png"));
            }
        } else if (getSlot().isOutput()) {
            if (getGraph().getTargetSlots(getSlot()).isEmpty()) {
                assignButton.setIcon(UIUtils.getIconFromResources("emblems/slot-unconnected-output-vertical.png"));
            } else {
                assignButton.setIcon(UIUtils.getIconFromResources("emblems/slot-connected-vertical.png"));
            }
            if (noSaveLabel != null) {
                if (getSlot().getNode() instanceof JIPipeAlgorithm) {
                    noSaveLabel.setVisible(!((JIPipeAlgorithm) getSlot().getNode()).isSaveOutputs());
                } else {
                    noSaveLabel.setVisible(false);
                }
            }
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());

        this.assignButton = new JButton(UIUtils.getIconFromResources("emblems/slot-connected-vertical.png"));
        new JIPipeConnectionDragAndDropBehavior(this, assignButton);
        assignButton.setPreferredSize(new Dimension(25, 25));
        this.assignButtonMenu = UIUtils.addReloadablePopupMenuToComponent(assignButton, new JPopupMenu(), this::reloadPopupMenu);
        UIUtils.makeFlat(assignButton, UIUtils.getBorderColorFor(getSlot().getNode().getInfo()), 0, 0, 0, 0);

        if (getSlot().getNode() instanceof JIPipeCompartmentOutput) {
            if (getSlot().getNode().getCompartment().equals(getCompartment())) {
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
        nameLabel.setToolTipText(TooltipUtils.getSlotInstanceTooltip(getSlot()));
        nameLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        nameLabel.setIcon(JIPipeUIDatatypeRegistry.getInstance().getIconFor(getSlotDataType()));
        centerPanel.add(nameLabel);
        centerPanel.add(Box.createHorizontalGlue());

        if (getSlot().isOutput() && getSlot().getNode() instanceof JIPipeAlgorithm) {
            noSaveLabel = new JLabel(UIUtils.getIconFromResources("actions/no-save.png"));
            noSaveLabel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
            centerPanel.add(noSaveLabel);
        }

        if (getSlot().isOutput() && getSlot().getNode() instanceof JIPipeAlgorithm && getWorkbench() instanceof JIPipeProjectWorkbench) {


            JIPipeProjectWorkbench projectWorkbench = (JIPipeProjectWorkbench) getWorkbench();
            cacheManagerUI = new JIPipeDataSlotCacheManagerUI(projectWorkbench, getSlot());
            centerPanel.add(cacheManagerUI);
        }

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
    public Dimension calculateGridSize() {
        // First calculate the width caused by the label width
        FontRenderContext frc = new FontRenderContext(null, false, false);
        TextLayout layout = new TextLayout(getDisplayedName(), getFont(), frc);
        double w = layout.getBounds().getWidth();
        int labelWidth = (int) Math.ceil(w * 1.0 / JIPipeGraphViewMode.Vertical.getGridWidth())
                * JIPipeGraphViewMode.Vertical.getGridWidth();
        int width = labelWidth + 75;
        Point inGrid = JIPipeGraphViewMode.Vertical.realLocationToGrid(new Point(width, JIPipeGraphViewMode.Vertical.getGridHeight()), 1.0);
        return new Dimension(inGrid.x, inGrid.y);
    }

    @Subscribe
    public void onNodeParameterChanged(ParameterChangedEvent event) {
        if (event.getSource() == getSlot().getNode() && "jipipe:algorithm:save-outputs".equals(event.getKey())) {
            reloadButtonStatus();
        }
    }

}
