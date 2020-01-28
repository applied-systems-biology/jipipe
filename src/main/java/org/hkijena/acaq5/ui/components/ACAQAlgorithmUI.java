package org.hkijena.acaq5.ui.components;

import org.hkijena.acaq5.api.ACAQAlgorithm;
import org.hkijena.acaq5.ui.ACAQAlgorithmGraphUI;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;

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

        inputSlotPanel = new JPanel();
        outputSlotPanel = new JPanel();

        inputSlotPanel.setBorder(BorderFactory.createLineBorder(Color.BLUE));
        outputSlotPanel.setBorder(BorderFactory.createLineBorder(Color.RED));

        add(inputSlotPanel, new GridBagConstraints() {
            {
                fill = GridBagConstraints.VERTICAL;
                weighty = 1;
            }
        });

        JPanel infoPanel = new JPanel();
        infoPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
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

    }

    private int calculateHeight() {
        return Math.max(SLOT_UI_HEIGHT, Math.max(SLOT_UI_HEIGHT * algorithm.getInputSlots().size(),
                SLOT_UI_HEIGHT * algorithm.getOutputSlots().size()));
    }

    private int calculateWidth() {
        FontRenderContext frc = new FontRenderContext(null, false, false);
        TextLayout layout = new TextLayout(ACAQAlgorithm.getName(algorithm.getClass()), getFont(), frc);
        double w = layout.getBounds().getWidth();
        return (int)Math.ceil(w * 1.0 / SLOT_UI_WIDTH) * SLOT_UI_WIDTH + 100;
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
