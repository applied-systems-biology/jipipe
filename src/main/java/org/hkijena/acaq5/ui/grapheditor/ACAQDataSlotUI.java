package org.hkijena.acaq5.ui.grapheditor;

import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.ACAQData;
import org.hkijena.acaq5.api.ACAQDataSlot;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;

import static org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI.SLOT_UI_HEIGHT;
import static org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI.SLOT_UI_WIDTH;

public class ACAQDataSlotUI extends JPanel {
    private ACAQDataSlot<?> slot;

    public ACAQDataSlotUI(ACAQDataSlot<?> slot) {
        this.slot = slot;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        JButton assignButton = new JButton(UIUtils.getIconFromResources("chevron-right.png"));
        assignButton.setPreferredSize(new Dimension(25, SLOT_UI_HEIGHT));
        UIUtils.makeFlat(assignButton);

        JLabel nameLabel = new JLabel(slot.getName());
        nameLabel.setToolTipText(ACAQData.getName(slot.getAcceptedDataType()));
        nameLabel.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
        nameLabel.setIcon(ACAQRegistryService.getInstance().getUIDatatypeRegistry().getIconFor(slot.getAcceptedDataType()));
        add(nameLabel, BorderLayout.CENTER);

        if(slot.isInput()) {
            add(assignButton, BorderLayout.WEST);
            nameLabel.setHorizontalAlignment(JLabel.LEFT);
            nameLabel.setHorizontalTextPosition(JLabel.RIGHT);
        }
        else if(slot.isOutput()) {
            add(assignButton, BorderLayout.EAST);
            nameLabel.setHorizontalAlignment(JLabel.RIGHT);
            nameLabel.setHorizontalTextPosition(JLabel.LEFT);
        }
    }

    public int calculateWidth() {
        FontRenderContext frc = new FontRenderContext(null, false, false);
        TextLayout layout = new TextLayout(ACAQData.getName(slot.getAcceptedDataType()), getFont(), frc);
        double w = layout.getBounds().getWidth();
        return (int)Math.ceil(w * 1.0 / SLOT_UI_WIDTH) * SLOT_UI_WIDTH + 75;
    }

    public ACAQDataSlot<?> getSlot() {
        return slot;
    }
}
