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
import org.hkijena.jipipe.ui.components.ZoomFlatIconButton;
import org.hkijena.jipipe.ui.components.ZoomIcon;
import org.hkijena.jipipe.ui.components.ZoomLabel;
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
public class JIPipeHorizontalDataSlotUI extends JIPipeDataSlotUI {
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
    public JIPipeHorizontalDataSlotUI(JIPipeWorkbench workbench, JIPipeNodeUI algorithmUI, String compartment, JIPipeDataSlot slot) {
        super(workbench, algorithmUI, compartment, slot);
        initialize();
        reloadButtonStatus();
        slot.getNode().getEventBus().register(this);
    }

    @Override
    protected void reloadButtonStatus() {
        if (getSlot().isInput()) {
            if (getGraph().getSourceSlot(getSlot()) == null) {
                assignButton.setIcon(new ZoomIcon(UIUtils.getIconFromResources("emblems/slot-unconnected-input-horizontal.png"), getGraphUI()));
            } else {
                assignButton.setIcon(new ZoomIcon(UIUtils.getIconFromResources("emblems/slot-connected-horizontal.png"), getGraphUI()));
            }
        } else if (getSlot().isOutput()) {
            if (getGraph().getTargetSlots(getSlot()).isEmpty()) {
                assignButton.setIcon(new ZoomIcon(UIUtils.getIconFromResources("emblems/slot-unconnected-output-horizontal.png"), getGraphUI()));
            } else {
                assignButton.setIcon(new ZoomIcon(UIUtils.getIconFromResources("emblems/slot-connected-horizontal.png"), getGraphUI()));
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

        this.assignButton = new JButton();
        UIUtils.makeFlat(assignButton, UIUtils.getBorderColorFor(getSlot().getNode().getInfo()), 0, 0, 0, 0);
        new JIPipeConnectionDragAndDropBehavior(this, assignButton);
        this.assignButtonMenu = UIUtils.addReloadablePopupMenuToComponent(assignButton, new JPopupMenu(), this::reloadPopupMenu);

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
//        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setLayout(new GridLayout(2, 1));
        centerPanel.setOpaque(false);

        nameLabel = new ZoomLabel("", null, getGraphUI());
        reloadName();
        nameLabel.setToolTipText(TooltipUtils.getSlotInstanceTooltip(getSlot()));
        nameLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        nameLabel.setIcon(new ZoomIcon(JIPipeUIDatatypeRegistry.getInstance().getIconFor(getSlotDataType()), getGraphUI()));
        centerPanel.add(nameLabel);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
        bottomPanel.add(Box.createHorizontalGlue());
        centerPanel.add(bottomPanel);

        if (getSlot().isOutput() && getSlot().getNode() instanceof JIPipeAlgorithm) {
            noSaveLabel = new ZoomLabel("", new ZoomIcon(UIUtils.getIconFromResources("actions/no-save.png"), getGraphUI()), getGraphUI());
            noSaveLabel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
            noSaveLabel.setMaximumSize(new Dimension(16, 16));
            bottomPanel.add(noSaveLabel);
        }

        if (getSlot().isOutput() && getSlot().getNode() instanceof JIPipeAlgorithm && getWorkbench() instanceof JIPipeProjectWorkbench) {
            JIPipeProjectWorkbench projectWorkbench = (JIPipeProjectWorkbench) getWorkbench();
            cacheManagerUI = new JIPipeDataSlotCacheManagerUI(projectWorkbench, getSlot(), getGraphUI());
            bottomPanel.add(cacheManagerUI);
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
    public Dimension calculateGridSize() {
        // First calculate the width caused by the label width
        FontRenderContext frc = new FontRenderContext(null, false, false);
        TextLayout layout = new TextLayout(getDisplayedName(), getFont(), frc);
        double w = layout.getBounds().getWidth();
        int labelWidth = (int) Math.ceil(w * 1.0 / JIPipeGraphViewMode.Horizontal.getGridWidth())
                * JIPipeGraphViewMode.Horizontal.getGridWidth();
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
