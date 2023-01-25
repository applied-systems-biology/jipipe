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

package org.hkijena.jipipe.ui.grapheditor.general.nodeui;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeCompartmentOutput;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.extensions.core.nodes.JIPipeCommentNode;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.cache.JIPipeDataSlotCacheManagerUI;
import org.hkijena.jipipe.ui.components.ZoomLabel;
import org.hkijena.jipipe.ui.components.icons.ZoomIcon;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphViewMode;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.util.Objects;
import java.util.UUID;

/**
 * Slot UI with horizontal direction
 */
@Deprecated
public class JIPipeVerticalDataSlotUI extends JIPipeDataSlotUI_old {
    private JButton assignButton;
    private JLabel nameLabel;
    private JLabel noSaveLabel;
    private JIPipeDataSlotCacheManagerUI cacheManagerUI;

    private String cachedGridSizeDisplayName;
    private Dimension cachedGridSize;

    /**
     * Creates a new UI
     *
     * @param workbench   the workbench
     * @param algorithmUI The parent algorithm UI
     * @param compartment The compartment ID
     * @param slot        The slot instance
     */
    public JIPipeVerticalDataSlotUI(JIPipeWorkbench workbench, JIPipeNodeUI algorithmUI, UUID compartment, JIPipeDataSlot slot) {
        super(workbench, algorithmUI, compartment, slot);
        initialize();
        reloadButtonStatus();
    }

    @Override
    protected void reloadButtonStatus() {
        if (getSlot().isInput()) {
            if (getGraph().getInputIncomingSourceSlots(getSlot()).isEmpty()) {
                if (getSlot().getInfo().isOptional()) {
                    assignButton.setIcon(new ZoomIcon(UIUtils.getIconFromResources("emblems/slot-unconnected-output-vertical.png"), getGraphUI()));
                } else {
                    assignButton.setIcon(new ZoomIcon(UIUtils.getIconFromResources("emblems/slot-unconnected-input-vertical.png"), getGraphUI()));
                }
            } else {
                assignButton.setIcon(new ZoomIcon(UIUtils.getIconFromResources("emblems/slot-connected-vertical.png"), getGraphUI()));
            }
        } else if (getSlot().isOutput()) {
            if (getGraph().getOutputOutgoingTargetSlots(getSlot()).isEmpty()) {
                assignButton.setIcon(new ZoomIcon(UIUtils.getIconFromResources("emblems/slot-unconnected-output-vertical.png"), getGraphUI()));
            } else {
                assignButton.setIcon(new ZoomIcon(UIUtils.getIconFromResources("emblems/slot-connected-vertical.png"), getGraphUI()));
            }
            if (noSaveLabel != null) {
                if (getSlot().getNode() instanceof JIPipeAlgorithm) {
                    noSaveLabel.setVisible(!getSlot().getInfo().isSaveOutputs());
                } else {
                    noSaveLabel.setVisible(false);
                }
            }
//            if (virtualLabel != null) {
//                if (getSlot().getNode() instanceof JIPipeAlgorithm) {
//                    virtualLabel.setVisible(getSlot().isNewDataVirtual());
//                } else {
//                    virtualLabel.setVisible(false);
//                }
//            }
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());

        if (getSlot().getNode() instanceof JIPipeCommentNode) {
            setOpaque(false);
        }

        this.assignButton = new JButton();
        UIUtils.redirectDragEvents(assignButton, getGraphUI());
        UIUtils.makeFlat(assignButton, UIUtils.getBorderColorFor(getSlot().getNode().getInfo()), 0, 0, 0, 0);
        this.assignButtonMenu = UIUtils.addReloadablePopupMenuToComponent(assignButton, new JPopupMenu(), this::reloadPopupMenu);

        if (getSlot().getNode() instanceof JIPipeCompartmentOutput) {
            if (Objects.equals(getSlot().getNode().getCompartmentUUIDInParentGraph(), getCompartment())) {
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

        assignButton.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        centerPanel.add(assignButton);

        nameLabel = new ZoomLabel("", null, getGraphUI());
        UIUtils.redirectDragEvents(nameLabel, getGraphUI());
        reloadName();
        nameLabel.setToolTipText(TooltipUtils.getDataTableTooltip(getSlot()));
        nameLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        nameLabel.setIcon(new ZoomIcon(JIPipe.getDataTypes().getIconFor(getSlotDataType()), getGraphUI()));
        if (!(getSlot().getNode() instanceof JIPipeCommentNode)) {
            centerPanel.add(nameLabel);
        }
        centerPanel.add(Box.createHorizontalGlue());

        if (getSlot().isOutput() && getSlot().getNode() instanceof JIPipeAlgorithm) {
//            virtualLabel = new ZoomLabel("", new ZoomIcon(UIUtils.getIconFromResources("actions/rabbitvcs-drive.png"), getGraphUI()), getGraphUI());
//            virtualLabel.setToolTipText("Data is stored on hard drive when not in use (reduced memory mode). Enable 'Reduce memory' at the bottom of the window for this to take effect.");
//            virtualLabel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
//            UIUtils.redirectDragEvents(virtualLabel, getGraphUI());
//            centerPanel.add(virtualLabel);

            noSaveLabel = new ZoomLabel("", new ZoomIcon(UIUtils.getIconFromResources("actions/no-save.png"), getGraphUI()), getGraphUI());
            noSaveLabel.setToolTipText("Data is not saved to hard drive during full run");
            noSaveLabel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
            UIUtils.redirectDragEvents(noSaveLabel, getGraphUI());
            centerPanel.add(noSaveLabel);
        }

        if (getSlot().isOutput() && getSlot().getNode() instanceof JIPipeAlgorithm && getWorkbench() instanceof JIPipeProjectWorkbench) {
            JIPipeProjectWorkbench projectWorkbench = (JIPipeProjectWorkbench) getWorkbench();
            cacheManagerUI = new JIPipeDataSlotCacheManagerUI(projectWorkbench, getSlot(), getGraphUI());
            UIUtils.redirectDragEvents(cacheManagerUI, getGraphUI());
            centerPanel.add(cacheManagerUI);
        }

        add(centerPanel, BorderLayout.CENTER);

//        new JIPipeConnectionDragAndDropBehavior(this, assignButton, nameLabel);
    }

    @Override
    protected void reloadName() {
        if (!StringUtils.isNullOrEmpty(getSlot().getInfo().getCustomName())) {
            nameLabel.setText(getDisplayedName());
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.ITALIC));
        } else {
            nameLabel.setText(getDisplayedName());
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.PLAIN));
        }
    }

    @Override
    public boolean needsRecalculateGridSize() {
        return !Objects.equals(cachedGridSizeDisplayName, getDisplayedName()) || cachedGridSize == null;
    }

    @Override
    public Dimension calculateGridSize() {
        if (needsRecalculateGridSize()) {
            // First calculate the width caused by the label width
            FontRenderContext frc = new FontRenderContext(null, false, false);
            TextLayout layout = new TextLayout(getDisplayedName(), getFont(), frc);
            double w = layout.getBounds().getWidth();
            int labelWidth = (int) Math.ceil(w / JIPipeGraphViewMode.VerticalCompact.getGridWidth())
                    * JIPipeGraphViewMode.VerticalCompact.getGridWidth();
            int width = labelWidth + 75;
            Point inGrid = JIPipeGraphViewMode.VerticalCompact.realLocationToGrid(new Point(width, JIPipeGraphViewMode.VerticalCompact.getGridHeight()), 1.0);

            cachedGridSize = new Dimension(inGrid.x, inGrid.y);
            cachedGridSizeDisplayName = getDisplayedName();
        }
        return cachedGridSize;
    }

}
