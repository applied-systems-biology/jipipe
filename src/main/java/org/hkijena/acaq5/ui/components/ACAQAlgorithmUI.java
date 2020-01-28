package org.hkijena.acaq5.ui.components;

import org.hkijena.acaq5.api.ACAQAlgorithm;

import javax.swing.*;
import java.awt.*;

public class ACAQAlgorithmUI extends JPanel {

    /**
     * Height assigned for one slot
     */
    public static final int SLOT_UI_HEIGHT = 50;

    private ACAQAlgorithm algorithm;

    public ACAQAlgorithmUI(ACAQAlgorithm algorithm) {
        this.algorithm = algorithm;
        initialize();
        updateAlgorithmUI();
    }

    private void initialize() {
        setBackground(getAlgorithmColor());
        setBorder(BorderFactory.createLineBorder(getAlgorithmBorderColor()));
        setSize(new Dimension(100, calculateHeight()));
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
}
