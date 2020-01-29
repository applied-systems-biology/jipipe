package org.hkijena.acaq5.ui.components;

import org.hkijena.acaq5.api.ACAQAlgorithm;
import org.hkijena.acaq5.api.ACAQDataSlot;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.util.ArrayList;
import java.util.List;

public class ACAQAlgorithmUI extends JPanel {

    /**
     * Height assigned for one slot
     */
    public static final int SLOT_UI_HEIGHT = 75;

    /**
     * Grid width for horizontal direction
     */
    public static final int SLOT_UI_WIDTH = 25;

    private ACAQAlgorithmGraphCanvasUI graphUI;
    private ACAQAlgorithm algorithm;
    private JPanel inputSlotPanel;
    private JPanel outputSlotPanel;
    private List<ACAQDataSlotUI> slotUIList = new ArrayList<>();

    public ACAQAlgorithmUI(ACAQAlgorithmGraphCanvasUI graphUI, ACAQAlgorithm algorithm) {
        this.graphUI = graphUI;
        this.algorithm = algorithm;
        initialize();
        updateAlgorithmUI();
    }

    private void initialize() {
        setBackground(getAlgorithmColor());
        setBorder(BorderFactory.createLineBorder(getAlgorithmBorderColor()));
        setSize(new Dimension(calculateWidth(), calculateHeight()));
        setLayout(new GridBagLayout());
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        inputSlotPanel = new JPanel();
        inputSlotPanel.setOpaque(false);
        outputSlotPanel = new JPanel();
        outputSlotPanel.setOpaque(false);

//        inputSlotPanel.setBorder(BorderFactory.createLineBorder(Color.BLUE));
//        outputSlotPanel.setBorder(BorderFactory.createLineBorder(Color.RED));

        add(inputSlotPanel, new GridBagConstraints() {
            {
                fill = GridBagConstraints.VERTICAL;
                weighty = 1;
            }
        });

        JPanel infoPanel = new JPanel();
//        infoPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        infoPanel.setOpaque(false);
        infoPanel.setLayout(new BorderLayout());

        infoPanel.add(new JLabel(ACAQAlgorithm.getName(algorithm.getClass())) {
            {
                setHorizontalAlignment(JLabel.CENTER);
            }
        });

        add(infoPanel, new GridBagConstraints() {
            {
                fill = GridBagConstraints.BOTH;
                weighty = 1;
                weightx = 1;
            }
        });
        add(outputSlotPanel, new GridBagConstraints() {
            {
                fill = GridBagConstraints.VERTICAL;
                weighty = 1;
            }
        });
    }

    public ACAQAlgorithm getAlgorithm() {
        return algorithm;
    }

    public void updateAlgorithmUI() {
        slotUIList.clear();
        inputSlotPanel.removeAll();
        outputSlotPanel.removeAll();
        if(algorithm.getInputSlots().size() > 0) {
            inputSlotPanel.setLayout(new GridLayout(algorithm.getInputSlots().size(), 1));
            for(ACAQDataSlot<?> slot : algorithm.getInputSlots().values()) {
                ACAQDataSlotUI ui = new ACAQDataSlotUI(slot);
                ui.setOpaque(false);
                slotUIList.add(ui);
                inputSlotPanel.add(ui);
            }
        }
        if(algorithm.getOutputSlots().size() > 0) {
            outputSlotPanel.setLayout(new GridLayout(algorithm.getOutputSlots().size(), 1));
            for(ACAQDataSlot<?> slot : algorithm.getOutputSlots().values()) {
                ACAQDataSlotUI ui = new ACAQDataSlotUI(slot);
                ui.setOpaque(false);
                slotUIList.add(ui);
                outputSlotPanel.add(ui);
            }
        }
        setSize(new Dimension(calculateWidth(), calculateHeight()));
    }

    private int calculateHeight() {
        return Math.max(SLOT_UI_HEIGHT, Math.max(SLOT_UI_HEIGHT * algorithm.getInputSlots().size(),
                SLOT_UI_HEIGHT * algorithm.getOutputSlots().size()));
    }

    private int calculateWidth() {
        FontRenderContext frc = new FontRenderContext(null, false, false);
        double width = 0;

        // Measure width of center
        {
            TextLayout layout = new TextLayout(ACAQAlgorithm.getName(algorithm.getClass()), getFont(), frc);
            width += layout.getBounds().getWidth();
        }

        // Measure slot widths
        {
            double maxInputSlotWidth = 0;
            double maxOutputSlotWidth = 0;
            for(ACAQDataSlotUI ui : slotUIList) {
                switch(ui.getSlot().getType()) {
                    case Input:
                        maxInputSlotWidth = Math.max(maxInputSlotWidth, ui.calculateWidth());
                        break;
                    case Output:
                        maxOutputSlotWidth = Math.max(maxOutputSlotWidth, ui.calculateWidth());
                        break;
                }
            }

            width += maxInputSlotWidth + maxOutputSlotWidth;
        }

        return (int)Math.ceil(width * 1.0 / SLOT_UI_WIDTH) * SLOT_UI_WIDTH + 150;
    }

    /**
     * Gets a hashed color value for the algorithm.
     * The color is the same for all algorithms of the same type
     * @return
     */
    public Color getAlgorithmColor() {
        float h = Math.abs(algorithm.getClass().getCanonicalName().hashCode() % 256) / 255.0f;
        return Color.getHSBColor(h, 0.1f, 0.9f);
    }

    public Color getAlgorithmBorderColor() {
        float h = Math.abs(algorithm.getClass().getCanonicalName().hashCode() % 256) / 255.0f;
        return Color.getHSBColor(h, 0.1f, 0.5f);
    }

    /**
     * Tries to move the node to the provided location
     * A grid is applied to the input coordinates
     * @param x
     * @param y
     * @return
     */
    public boolean trySetLocationInGrid(int x, int y) {
        y = (int)Math.rint(y * 1.0 / ACAQAlgorithmUI.SLOT_UI_HEIGHT) * ACAQAlgorithmUI.SLOT_UI_HEIGHT;
        x = (int)Math.rint(x * 1.0 / ACAQAlgorithmUI.SLOT_UI_WIDTH) * ACAQAlgorithmUI.SLOT_UI_WIDTH;

        // Check for collisions
        Rectangle futureBounds = new Rectangle(x, y, getWidth(), getHeight());
        for(int i = 0; i < graphUI.getComponentCount(); ++i) {
            Component component = graphUI.getComponent(i);
            if(component instanceof ACAQAlgorithmUI) {
                ACAQAlgorithmUI ui = (ACAQAlgorithmUI)component;
                if(ui != this) {
                    if(ui.getBounds().intersects(futureBounds)) {
                        return false;
                    }
                }
            }
        }

        setLocation(x, y);
        return true;
    }

}
